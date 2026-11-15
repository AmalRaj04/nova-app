package com.genvision.nova.camera

import kotlin.math.hypot
import kotlin.math.min

/** Small pure maths for marker size and the developer overlay. */
object MarkerGeometry {
    /** Average side length in pixels of the quadrilateral given by 4 corners. */
    fun sidePx(corners: List<Pair<Float, Float>>): Float {
        if (corners.size != 4) return 0f
        var sum = 0f
        for (i in 0 until 4) {
            val a = corners[i]
            val b = corners[(i + 1) % 4]
            sum += hypot(b.first - a.first, b.second - a.second)
        }
        return sum / 4f
    }

    /** True if the marker is big enough. A limit of 0 means no limit (default). */
    fun bigEnough(corners: List<Pair<Float, Float>>, minSidePx: Int): Boolean =
        minSidePx <= 0 || sidePx(corners) >= minSidePx

    /**
     * Maps a point in the camera image to a point in a view that shows the upright image with
     * FIT_CENTER (the whole image visible, centred, with bars where it does not fill the view).
     * [rotationDegrees] is how far the image must turn clockwise to be upright (0, 90, 180, 270).
     */
    fun toView(
        x: Float, y: Float,
        imageWidth: Int, imageHeight: Int,
        rotationDegrees: Int,
        viewWidth: Float, viewHeight: Float,
    ): Pair<Float, Float> {
        val w = imageWidth.toFloat()
        val h = imageHeight.toFloat()
        // Turn the point so it sits in the upright image.
        val (ux, uy, uw, uh) = when (((rotationDegrees % 360) + 360) % 360) {
            90 -> listOf(h - y, x, h, w)
            180 -> listOf(w - x, h - y, w, h)
            270 -> listOf(y, w - x, h, w)
            else -> listOf(x, y, w, h)
        }
        val scale = min(viewWidth / uw, viewHeight / uh)
        val offsetX = (viewWidth - uw * scale) / 2f
        val offsetY = (viewHeight - uh * scale) / 2f
        return Pair(offsetX + ux * scale, offsetY + uy * scale)
    }
}
