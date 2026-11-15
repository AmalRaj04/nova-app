package com.genvision.nova

import com.genvision.nova.camera.MarkerGeometry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkerGeometryTest {
    private val square100 = listOf(10f to 10f, 110f to 10f, 110f to 110f, 10f to 110f)

    @Test fun sideOfASquare() {
        assertEquals(100f, MarkerGeometry.sidePx(square100), 1e-3f)
    }

    @Test fun limitOfZeroMeansNoLimit() {
        assertTrue(MarkerGeometry.bigEnough(square100, 0))
        assertTrue(MarkerGeometry.bigEnough(listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f), 0))
    }

    @Test fun smallMarkerIsRejectedWhenALimitIsSet() {
        assertTrue(MarkerGeometry.bigEnough(square100, 100))
        assertFalse(MarkerGeometry.bigEnough(square100, 101))
    }

    @Test fun wrongNumberOfCornersHasNoSide() {
        assertEquals(0f, MarkerGeometry.sidePx(listOf(0f to 0f, 1f to 1f)))
    }

    // A 640 x 480 image shown in a 300 x 300 view: scale 0.46875, bars above and below.
    @Test fun upright_fitsAndCentres() {
        val (x, y) = MarkerGeometry.toView(0f, 0f, 640, 480, 0, 300f, 300f)
        assertEquals(0f, x, 1e-3f)
        assertEquals(37.5f, y, 1e-3f)
        val (x2, y2) = MarkerGeometry.toView(640f, 480f, 640, 480, 0, 300f, 300f)
        assertEquals(300f, x2, 1e-3f)
        assertEquals(262.5f, y2, 1e-3f)
    }

    // Rotated 90 degrees clockwise the image is 480 wide and 640 tall.
    @Test fun rotated90_swapsAxes() {
        // Image top-left (0,0) ends up at the top-right of the upright image.
        val (x, y) = MarkerGeometry.toView(0f, 0f, 640, 480, 90, 480f, 640f)
        assertEquals(480f, x, 1e-3f)
        assertEquals(0f, y, 1e-3f)
        // Image bottom-left (0,480) ends up at the top-left.
        val (x2, y2) = MarkerGeometry.toView(0f, 480f, 640, 480, 90, 480f, 640f)
        assertEquals(0f, x2, 1e-3f)
        assertEquals(0f, y2, 1e-3f)
    }

    @Test fun rotated180_flipsBothAxes() {
        val (x, y) = MarkerGeometry.toView(0f, 0f, 640, 480, 180, 640f, 480f)
        assertEquals(640f, x, 1e-3f)
        assertEquals(480f, y, 1e-3f)
    }

    @Test fun rotated270_isTheOppositeTurn() {
        // Image top-left (0,0) ends up at the bottom-left of the upright image.
        val (x, y) = MarkerGeometry.toView(0f, 0f, 640, 480, 270, 480f, 640f)
        assertEquals(0f, x, 1e-3f)
        assertEquals(640f, y, 1e-3f)
    }
}
