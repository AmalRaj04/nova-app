package com.genvision.nova.camera

import android.content.Context
import android.os.SystemClock
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.genvision.nova.core.Config
import java.util.concurrent.Executors

/**
 * Runs the back camera for marker analysis. `ImageAnalysis` always runs; `Preview` only when a
 * surface is given (developer mode). Frames closer than [Config.ANALYSIS_INTERVAL_MS] are
 * dropped, and old frames are never queued (keep-only-latest).
 *
 * Callbacks [onDetections], [onRate] and [onError] are called on the MAIN thread.
 */
class CameraSession(
    private val context: Context,
    private val detector: MarkerDetector,
    private val onDetections: (List<Int>) -> Unit,
    private val onRate: (Double) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val throttle = FrameThrottle(Config.ANALYSIS_INTERVAL_MS)
    private val meter = RateMeter()

    private var provider: ProcessCameraProvider? = null
    /** Bumped on every start/stop so a slow start can notice it has been cancelled. */
    private var token = 0
    private var grayBuffer = ByteArray(0)

    /** Starts (or restarts) the camera, tied to [owner]'s lifecycle. */
    fun start(owner: LifecycleOwner, previewSurface: Preview.SurfaceProvider?) {
        val myToken = ++token
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (myToken != token) return@addListener // stopped while the camera was starting
            try {
                val cameraProvider = future.get()
                provider = cameraProvider
                cameraProvider.unbindAll()
                throttle.reset()
                meter.reset()

                val selector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(Config.ANALYSIS_WIDTH, Config.ANALYSIS_HEIGHT),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build()
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(selector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor, ::analyze)

                if (previewSurface != null) {
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewSurface }
                    cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } else {
                    cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, analysis)
                }
            } catch (e: Exception) {
                onError("The camera could not start: ${e.message ?: "unknown problem"}")
            }
        }, mainExecutor)
    }

    /** Stops the camera and frees detector memory. Safe to call at any time. */
    fun stop() {
        token++
        provider?.unbindAll()
        detector.release()
    }

    fun shutdown() {
        stop()
        analysisExecutor.shutdown()
    }

    /** Runs on the analysis thread, once per camera frame. */
    private fun analyze(image: ImageProxy) {
        try {
            val now = SystemClock.elapsedRealtime()
            if (!throttle.shouldProcess(now)) return // dropped; the finally block closes it

            val plane = image.planes[0]
            val size = image.width * image.height
            if (grayBuffer.size != size) grayBuffer = ByteArray(size)
            copyLuminance(plane.buffer, image.width, image.height, plane.rowStride, grayBuffer)

            val ids = detector.detect(GrayFrame(image.width, image.height, grayBuffer)).map { it.id }
            mainExecutor.execute { onDetections(ids) }
            meter.onFrame(now)?.let { rate -> mainExecutor.execute { onRate(rate) } }
        } catch (e: Exception) {
            mainExecutor.execute { onError("The camera stopped working: ${e.message ?: "unknown problem"}") }
        } finally {
            image.close() // always, or the camera stops delivering frames
        }
    }
}
