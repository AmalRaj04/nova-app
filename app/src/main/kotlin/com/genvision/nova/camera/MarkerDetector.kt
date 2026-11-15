package com.genvision.nova.camera

/** A marker found in one camera frame. Corners are in image pixels (used only for the developer overlay). */
data class DetectedMarker(val id: Int, val corners: List<Pair<Float, Float>> = emptyList())

/** A grey (luminance) camera frame with the row stride already removed: `data.size == width * height`. */
class GrayFrame(
    val width: Int,
    val height: Int,
    val data: ByteArray,
    /** Clockwise degrees to turn the image upright. Detection ignores it; the developer overlay uses it. */
    val rotationDegrees: Int = 0,
)

/** Finds ArUco markers in a frame. [ArucoMarkerDetector] is the only OpenCV user. */
interface MarkerDetector {
    fun detect(frame: GrayFrame): List<DetectedMarker>
    /** Free native memory. Called when analysis stops. [detect] must still work afterwards. */
    fun release() {}
}

/** Stand-in for tests and for running without OpenCV. Returns whatever IDs it was told to. */
class FakeMarkerDetector(var idsToReturn: List<Int> = emptyList()) : MarkerDetector {
    override fun detect(frame: GrayFrame): List<DetectedMarker> = idsToReturn.map { DetectedMarker(it) }
}
