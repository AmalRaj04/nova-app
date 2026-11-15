package com.genvision.nova

import com.genvision.nova.camera.FrameThrottle
import com.genvision.nova.camera.RateMeter
import com.genvision.nova.camera.copyLuminance
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FrameMathTest {
    @Test fun throttleDropsFramesCloserThanTheInterval() {
        val t = FrameThrottle(150)
        assertTrue(t.shouldProcess(0))
        assertFalse(t.shouldProcess(100))
        assertFalse(t.shouldProcess(149))
        assertTrue(t.shouldProcess(150))
        assertFalse(t.shouldProcess(200))
        assertTrue(t.shouldProcess(300))
    }

    @Test fun throttleAcceptsTheFirstFrameAtTimeZero() {
        assertTrue(FrameThrottle(150).shouldProcess(0))
    }

    @Test fun rateMeterReportsOncePerWindow() {
        val m = RateMeter(1000)
        assertNull(m.onFrame(0))
        assertNull(m.onFrame(300))
        assertNull(m.onFrame(600))
        assertEquals(4.0, m.onFrame(1000)!!, 1e-9)
        assertNull(m.onFrame(1300))
    }

    @Test fun copyRemovesRowPaddingFromTheStride() {
        // 3 pixels wide, 2 rows, rows start 5 bytes apart (2 bytes of padding per row).
        val source = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 9, 9, 4, 5, 6, 9, 9))
        val out = ByteArray(6)
        copyLuminance(source, width = 3, height = 2, rowStride = 5, out = out)
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6), out)
    }

    @Test fun copyWorksWhenStrideEqualsWidth() {
        val out = ByteArray(4)
        copyLuminance(ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4)), 2, 2, 2, out)
        assertContentEquals(byteArrayOf(1, 2, 3, 4), out)
    }

    @Test fun copyWorksWhenTheLastRowHasNoPadding() {
        // Real buffers often end right after the last pixel of the last row.
        val out = ByteArray(6)
        copyLuminance(ByteBuffer.wrap(byteArrayOf(1, 2, 3, 9, 9, 4, 5, 6)), 3, 2, 5, out)
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6), out)
    }
}
