package com.genvision.nova.camera

import java.nio.ByteBuffer

/** Decides which camera frames to analyse: at least [intervalMs] apart. Others are dropped, never queued (FR-04). */
class FrameThrottle(private val intervalMs: Long) {
    private var lastAcceptedMs = Long.MIN_VALUE

    fun shouldProcess(nowMs: Long): Boolean {
        if (lastAcceptedMs != Long.MIN_VALUE && nowMs - lastAcceptedMs < intervalMs) return false
        lastAcceptedMs = nowMs
        return true
    }

    fun reset() { lastAcceptedMs = Long.MIN_VALUE }
}

/** Counts analysed frames and reports frames per second once per [windowMs]. Shown in the developer panel. */
class RateMeter(private val windowMs: Long = 1000) {
    private var windowStartMs = Long.MIN_VALUE
    private var count = 0

    /** Call for each analysed frame. Returns the rate when a window has just finished, else null. */
    fun onFrame(nowMs: Long): Double? {
        if (windowStartMs == Long.MIN_VALUE) windowStartMs = nowMs
        count++
        val elapsed = nowMs - windowStartMs
        if (elapsed < windowMs) return null
        val rate = count * 1000.0 / elapsed
        windowStartMs = nowMs
        count = 0
        return rate
    }

    fun reset() { windowStartMs = Long.MIN_VALUE; count = 0 }
}

/**
 * Copies the luminance (Y) plane into [out] as `width * height` bytes with no padding.
 * Each row in the source starts [rowStride] bytes after the previous one, and the stride can be
 * larger than the width, so rows must be copied one at a time.
 */
fun copyLuminance(source: ByteBuffer, width: Int, height: Int, rowStride: Int, out: ByteArray) {
    require(out.size >= width * height) { "Output buffer is too small" }
    for (row in 0 until height) {
        source.position(row * rowStride)
        source.get(out, row * width, width)
    }
}
