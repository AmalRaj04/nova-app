package com.genvision.nova.camera

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.objdetect.Objdetect
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Runs on a phone or emulator: generates marker images with OpenCV and reads them back. */
@RunWith(AndroidJUnit4::class)
class ArucoMarkerDetectorTest {

    /** A marker image with a white border, like the printed ones, as a [GrayFrame]. */
    private fun frameWithMarker(id: Int, markerPx: Int = 200, border: Int = 60, rotate90: Boolean = false): GrayFrame {
        assertTrue(OpenCVLoader.initLocal())
        val dictionary = Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50)
        var marker = Mat()
        Objdetect.generateImageMarker(dictionary, id, markerPx, marker, 1)
        val padded = Mat()
        Core.copyMakeBorder(marker, padded, border, border, border, border, Core.BORDER_CONSTANT, Scalar(255.0))
        if (rotate90) {
            val turned = Mat()
            Core.rotate(padded, turned, Core.ROTATE_90_CLOCKWISE)
            marker = turned
        } else {
            marker = padded
        }
        val bytes = ByteArray(marker.cols() * marker.rows())
        marker.get(0, 0, bytes)
        return GrayFrame(marker.cols(), marker.rows(), bytes)
    }

    private fun detector() = ArucoMarkerDetector(dictionaryName = { "DICT_4X4_50" })

    @Test
    fun readsEveryMarkerIdFrom0To10() {
        val d = detector()
        for (id in 0..10) {
            val found = d.detect(frameWithMarker(id))
            assertEquals(listOf(id), found.map { it.id }, "marker $id")
            assertEquals(4, found.single().corners.size)
        }
        d.release()
    }

    @Test
    fun readsARotatedMarker() {
        assertEquals(listOf(7), detector().detect(frameWithMarker(7, rotate90 = true)).map { it.id })
    }

    @Test
    fun blankImageFindsNothing() {
        val blank = GrayFrame(320, 240, ByteArray(320 * 240) { 255.toByte() })
        assertTrue(detector().detect(blank).isEmpty())
    }

    @Test
    fun worksAgainAfterRelease() {
        val d = detector()
        d.detect(frameWithMarker(3))
        d.release()
        assertEquals(listOf(3), d.detect(frameWithMarker(3)).map { it.id })
    }

    @Test
    fun tooSmallMarkerIsIgnoredWhenALimitIsSet() {
        val strict = ArucoMarkerDetector(dictionaryName = { "DICT_4X4_50" }, minMarkerSidePx = 500)
        assertTrue(strict.detect(frameWithMarker(2)).isEmpty())
    }

    @Test
    fun differentSizeFramesAreHandled() {
        val d = detector()
        assertEquals(listOf(1), d.detect(frameWithMarker(1, markerPx = 200)).map { it.id })
        assertEquals(listOf(1), d.detect(frameWithMarker(1, markerPx = 120, border = 40)).map { it.id })
    }
}
