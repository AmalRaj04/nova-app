package com.genvision.nova.core.geometry

import kotlin.math.atan2

/**
 * Angles on the map. x points right, y points down, so "up" on the plan (map north) is negative y.
 * A bearing is degrees clockwise from map north, from 0 up to but not including 360.
 */
object Bearing {
    /** Bearing of the move from point 1 to point 2: atan2(dx, -dy) mod 360. */
    fun between(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val degrees = Math.toDegrees(atan2(x2 - x1, -(y2 - y1)))
        return normalize360(degrees)
    }

    /** Wraps any angle into 0 (inclusive) to 360 (exclusive). */
    fun normalize360(degrees: Double): Double {
        val r = degrees % 360.0
        return if (r < 0) r + 360.0 else r
    }

    /**
     * Turn needed to go from [facingDeg] to [headingDeg], wrapped to -180 (exclusive) .. +180.
     * Positive means turn right, negative means turn left.
     */
    fun turnAngle(facingDeg: Double, headingDeg: Double): Double {
        val a = normalize360(headingDeg - facingDeg)
        return if (a > 180.0) a - 360.0 else a
    }
}
