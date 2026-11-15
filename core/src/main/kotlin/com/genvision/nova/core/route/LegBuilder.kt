package com.genvision.nova.core.route

import com.genvision.nova.core.geometry.Bearing
import com.genvision.nova.core.model.Edge
import com.genvision.nova.core.model.MapData

/** One stretch of the route from a marker node to the next marker node (or the route's end). */
data class Leg(
    val startNodeId: String,
    val endNodeId: String,
    /** The graph edges walked in order. May be several, e.g. a walk then stairs. */
    val edges: List<Edge>,
    /** Bearing of the first edge, used to work out the turn at the start marker. */
    val firstHeadingDeg: Double,
)

/** Cuts a planned route into legs. Guidance is spoken once per leg. */
class LegBuilder(private val map: MapData, private val markerNodeIds: Set<String>) {

    /** The leg that starts at `route[startIndex]`, or null if that is the last node of the route. */
    fun build(route: List<String>, startIndex: Int): Leg? {
        if (startIndex !in 0 until route.lastIndex) return null
        var end = startIndex + 1
        while (end < route.lastIndex && route[end] !in markerNodeIds) end++

        val edges = (startIndex until end).map { i ->
            map.edges.first { it.from == route[i] && it.to == route[i + 1] }
        }
        val a = map.node(route[startIndex])!!
        val b = map.node(route[startIndex + 1])!!
        return Leg(route[startIndex], route[end], edges, Bearing.between(a.x, a.y, b.x, b.y))
    }
}
