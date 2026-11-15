package com.genvision.nova.camera

import com.genvision.nova.core.Config
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.objdetect.ArucoDetector
import org.opencv.objdetect.Objdetect

/**
 * ArUco detection with OpenCV. This is the ONLY class that uses OpenCV for detection.
 * It uses OpenCV's default detector parameters; change them only because of field-test
 * results, and record the change in DECISIONS.md.
 *
 * Matrices are reused between frames and freed by [release]. A later [detect] allocates again.
 */
class ArucoMarkerDetector(
    /** From `markers.json`, e.g. "DICT_4X4_50". It must match what is printed. */
    private val dictionaryName: () -> String,
    private val minMarkerSidePx: Int = Config.MIN_MARKER_SIDE_PX,
) : MarkerDetector {

    private var detector: ArucoDetector? = null
    private var gray: Mat? = null
    private var ids: Mat? = null

    private fun detector(): ArucoDetector {
        detector?.let { return it }
        check(OpenCVLoader.initLocal()) { "OpenCV could not start." }
        val dictionary = Objdetect.getPredefinedDictionary(dictionaryId(dictionaryName()))
        return ArucoDetector(dictionary).also { detector = it }
    }

    override fun detect(frame: GrayFrame): List<DetectedMarker> {
        val aruco = detector()

        var image = gray
        if (image == null || image.cols() != frame.width || image.rows() != frame.height) {
            image?.release()
            image = Mat(frame.height, frame.width, CvType.CV_8UC1)
            gray = image
        }
        image.put(0, 0, frame.data)

        val idMat = ids ?: Mat().also { ids = it }
        val corners = ArrayList<Mat>()
        try {
            aruco.detectMarkers(image, corners, idMat)
            val found = ArrayList<DetectedMarker>()
            // With no markers OpenCV leaves `ids` empty (not always with 0 rows), so count the corners.
            if (corners.isEmpty() || idMat.empty()) return found
            for (i in 0 until minOf(corners.size, idMat.rows())) {
                val id = (idMat.get(i, 0) ?: continue)[0].toInt()
                val points = corners[i].let { c ->
                    (0 until 4).map { j -> c.get(0, j).let { Pair(it[0].toFloat(), it[1].toFloat()) } }
                }
                if (MarkerGeometry.bigEnough(points, minMarkerSidePx)) found += DetectedMarker(id, points)
            }
            return found
        } finally {
            corners.forEach { it.release() }
        }
    }

    override fun release() {
        gray?.release(); gray = null
        ids?.release(); ids = null
    }

    companion object {
        /** Dictionaries the app knows how to open. Others fail loudly instead of guessing. */
        fun dictionaryId(name: String): Int = when (name) {
            "DICT_4X4_50" -> Objdetect.DICT_4X4_50
            "DICT_4X4_100" -> Objdetect.DICT_4X4_100
            "DICT_4X4_250" -> Objdetect.DICT_4X4_250
            "DICT_4X4_1000" -> Objdetect.DICT_4X4_1000
            else -> throw IllegalArgumentException("Unsupported marker dictionary: $name")
        }
    }
}
