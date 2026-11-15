package com.genvision.nova.core.sighting

import com.genvision.nova.core.Config

/**
 * Turns raw per-frame detections into accepted sightings.
 *
 * - FR-06: a marker counts only when seen in [confirmFrames] analysed frames within [confirmWindowMs].
 * - FR-07: after a sighting at a node, repeats at that same node are ignored for [sameNodeCooldownMs],
 *   unless a different node was sighted in between.
 */
class SightingFilter(
    private val nodeOfMarker: (Int) -> String?,
    private val confirmFrames: Int = Config.CONFIRM_FRAMES,
    private val confirmWindowMs: Long = Config.CONFIRM_WINDOW_MS,
    private val sameNodeCooldownMs: Long = Config.SAME_NODE_COOLDOWN_MS,
) {
    private val frameTimes = mutableMapOf<Int, ArrayDeque<Long>>()
    private var lastNode: String? = null
    private var lastAcceptedAt = 0L

    /**
     * Call once per analysed frame with every marker ID found in it.
     * Returns the marker IDs accepted by this frame. IDs not in `markers.json` are ignored here
     * (the caller logs them).
     */
    fun onFrame(markerIds: Collection<Int>, nowMs: Long): List<Int> {
        val accepted = mutableListOf<Int>()
        for (id in markerIds.distinct()) {
            val node = nodeOfMarker(id) ?: continue
            val times = frameTimes.getOrPut(id) { ArrayDeque() }
            times.addLast(nowMs)
            while (times.isNotEmpty() && nowMs - times.first() > confirmWindowMs) times.removeFirst()
            if (times.size < confirmFrames) continue

            times.clear()
            val repeat = node == lastNode && nowMs - lastAcceptedAt < sameNodeCooldownMs
            if (repeat) continue
            lastNode = node
            lastAcceptedAt = nowMs
            accepted += id
        }
        return accepted
    }

    /** Forget everything, e.g. when a new guidance session starts. */
    fun reset() {
        frameTimes.clear()
        lastNode = null
        lastAcceptedAt = 0L
    }
}
