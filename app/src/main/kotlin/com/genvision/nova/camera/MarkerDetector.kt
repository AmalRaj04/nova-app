package com.genvision.nova.camera

/** A marker found in one camera frame. Corners are in image pixels (used only for the developer overlay). */
data class DetectedMarker(val id: Int, val corners: List<Pair<Float, Float>> = emptyList())

/** A grey (luminance) camera frame with the row stride already removed: `data.size == width * height`. */
class GrayFrame(val width: Int, val height: Int, val data: ByteArray)

/** Finds ArUco markers in a frame. The OpenCV implementation (Step 7) is the only OpenCV user. */
interface MarkerDetector {
    fun detect(frame: GrayFrame): List<DetectedMarker>
    /** Free native memory. Called when analysis stops. */
    fun release() {}
}

/** Stand-in used before OpenCV is wired in, and in tests. Returns whatever IDs it was told to. */
class FakeMarkerDetector(var idsToReturn: List<Int> = emptyList()) : MarkerDetector {
    override fun detect(frame: GrayFrame): List<DetectedMarker> = idsToReturn.map { DetectedMarker(it) }
}
