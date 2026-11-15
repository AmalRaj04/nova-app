package com.genvision.nova.core.route

import com.genvision.nova.core.model.MapData

/** Finds the shortest forward route over the one-way graph, using edge `weight` (Dijkstra). */
class RouteFinder(private val map: MapData) {

    /**
     * Node IDs from [from] to [to], both included, or null when no forward path exists.
     * A route from a node to itself is just that node.
     */
    fun find(from: String, to: String): List<String>? {
        if (map.node(from) == null || map.node(to) == null) return null
        val dist = mutableMapOf(from to 0.0)
        val previous = mutableMapOf<String, String>()
        val done = mutableSetOf<String>()
        while (true) {
            // The graph is tiny, so a simple scan for the closest unfinished node is clear and fast enough.
            val current = dist.filterKeys { it !in done }.minByOrNull { it.value }?.key ?: return null
            if (current == to) break
            done += current
            for (e in map.edgesFrom(current)) {
                val candidate = dist.getValue(current) + e.weight
                if (candidate < (dist[e.to] ?: Double.MAX_VALUE)) {
                    dist[e.to] = candidate
                    previous[e.to] = current
                }
            }
        }
        val route = ArrayDeque<String>()
        var step: String? = to
        while (step != null) {
            route.addFirst(step)
            step = previous[step]
        }
        return route.toList()
    }
}
