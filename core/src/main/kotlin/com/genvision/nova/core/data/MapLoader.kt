package com.genvision.nova.core.data

import com.genvision.nova.core.model.EdgeKind
import com.genvision.nova.core.model.MapData
import com.genvision.nova.core.model.MarkersData
import kotlinx.serialization.json.Json

/** Map and markers that passed every data check. */
data class LoadedData(val map: MapData, val markers: MarkersData)

sealed interface LoadResult {
    data class Success(val data: LoadedData) : LoadResult
    /** Every problem found, each one a plain sentence that can be shown and spoken. */
    data class Failure(val errors: List<String>) : LoadResult
}

/** Parses `map.json` and `markers.json` and runs the data checks. Guidance must not start on Failure. */
object MapLoader {
    private val json = Json { ignoreUnknownKeys = true }

    /** The marker whose node is the start of the route, used for the reachability check. */
    private const val START_MARKER_ID = 0

    fun load(mapJson: String, markersJson: String): LoadResult {
        val map = try {
            json.decodeFromString<MapData>(mapJson)
        } catch (e: Exception) {
            return LoadResult.Failure(listOf("map.json cannot be read: ${e.message}"))
        }
        val markers = try {
            json.decodeFromString<MarkersData>(markersJson)
        } catch (e: Exception) {
            return LoadResult.Failure(listOf("markers.json cannot be read: ${e.message}"))
        }
        val errors = check(map, markers)
        return if (errors.isEmpty()) LoadResult.Success(LoadedData(map, markers)) else LoadResult.Failure(errors)
    }

    fun check(map: MapData, markers: MarkersData): List<String> {
        val errors = mutableListOf<String>()
        val nodeIds = map.nodes.map { it.id }.toSet()

        // Rule 1: every edge joins two existing nodes.
        for (e in map.edges) {
            if (e.from !in nodeIds) errors += "Edge ${e.from} to ${e.to} starts at unknown node ${e.from}."
            if (e.to !in nodeIds) errors += "Edge ${e.from} to ${e.to} ends at unknown node ${e.to}."
        }

        // Rule 2: every marker points at an existing node, and marker IDs are unique.
        for (m in markers.markers) {
            if (m.nodeId !in nodeIds) errors += "Marker ${m.id} points at unknown node ${m.nodeId}."
        }
        markers.markers.groupBy { it.id }.filter { it.value.size > 1 }.keys.forEach {
            errors += "Marker ID $it is used more than once."
        }

        // Rule 3: every destination is an existing room.
        for (d in map.destinations) {
            val node = map.node(d)
            if (node == null) errors += "Destination $d is not a known node."
            else if (!node.isRoom) errors += "Destination $d is not a room."
        }

        // Rule 4: positive weights, and stairs edges carry a step count.
        for (e in map.edges) {
            if (e.weight <= 0.0) errors += "Edge ${e.from} to ${e.to} must have a weight above 0."
            if (e.kind == EdgeKind.STAIRS && e.steps == null) {
                errors += "Stairs edge ${e.from} to ${e.to} has no steps value."
            }
        }

        // Rule 5: every destination can be reached from the node of marker 0.
        val start = markers.marker(START_MARKER_ID)?.nodeId
        if (start == null) {
            errors += "Marker $START_MARKER_ID is missing, so the route start is unknown."
        } else if (start in nodeIds) {
            val reachable = reachableFrom(map, start)
            for (d in map.destinations) {
                if (map.node(d) != null && d !in reachable) {
                    errors += "Destination $d cannot be reached from the entrance."
                }
            }
        }
        return errors
    }

    private fun reachableFrom(map: MapData, start: String): Set<String> {
        val seen = mutableSetOf(start)
        val queue = ArrayDeque(listOf(start))
        while (queue.isNotEmpty()) {
            for (e in map.edgesFrom(queue.removeFirst())) {
                if (seen.add(e.to)) queue.add(e.to)
            }
        }
        return seen
    }
}
