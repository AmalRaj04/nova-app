package com.genvision.nova.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One place on the route. Coordinates: x right, y down, metres (same as the team's plan). */
@Serializable
data class Node(
    val id: String,
    val name: String,
    val type: String,
    /** A label only. Never used for routing. */
    val floor: Int = 0,
    val x: Double,
    val y: Double,
    /** "left" or "right", as seen walking forward along the route. */
    val doorSide: String? = null,
    /** Optional text used for speech only, if the phone says `name` badly. */
    val spoken: String? = null,
) {
    val isRoom: Boolean get() = type == NodeType.ROOM
    /** What the speaker should say for this place. */
    val speechName: String get() = spoken ?: name
}

object NodeType {
    const val ROOM = "room"
}

object EdgeKind {
    const val WALK = "walk"
    const val STAIRS = "stairs"
}

/** One-way link between two nodes. */
@Serializable
data class Edge(
    val from: String,
    val to: String,
    val weight: Double,
    val kind: String,
    /** Exact step count. Required for stairs, null for walks. */
    val steps: Int? = null,
    /** Optional spoken hint, e.g. from an orientation and mobility instructor. */
    val hint: String = "",
)

@Serializable
data class MapMeta(val version: Int = 1, val route: String = "")

@Serializable
data class MapData(
    val meta: MapMeta = MapMeta(),
    val nodes: List<Node>,
    val edges: List<Edge>,
    val destinations: List<String>,
) {
    private val nodesById: Map<String, Node> by lazy { nodes.associateBy { it.id } }
    fun node(id: String): Node? = nodesById[id]
    fun edgesFrom(id: String): List<Edge> = edges.filter { it.from == id }
}

/** A printed marker and the place it stands for. */
@Serializable
data class Marker(
    val id: Int,
    val nodeId: String,
    /** Bearing (degrees clockwise from map north) a person faces when looking at the marker. */
    val facingDeg: Double,
    @SerialName("where") val where: String = "",
)

@Serializable
data class MarkersMeta(val version: Int = 1, val dictionary: String)

@Serializable
data class MarkersData(
    val meta: MarkersMeta,
    val markers: List<Marker>,
) {
    fun marker(id: Int): Marker? = markers.firstOrNull { it.id == id }
}
