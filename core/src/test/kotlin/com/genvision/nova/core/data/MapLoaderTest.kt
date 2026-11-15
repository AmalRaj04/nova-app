package com.genvision.nova.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MapLoaderTest {
    private fun resource(name: String) =
        javaClass.classLoader.getResource(name)!!.readText()

    private val mapJson = resource("map.json")
    private val markersJson = resource("markers.json")

    private fun failures(map: String = mapJson, markers: String = markersJson): List<String> {
        val result = MapLoader.load(map, markers)
        assertIs<LoadResult.Failure>(result)
        return result.errors
    }

    @Test
    fun theTwoFilesFromTheBriefLoad() {
        val result = MapLoader.load(mapJson, markersJson)
        assertIs<LoadResult.Success>(result)
        assertEquals(10, result.data.map.nodes.size)
        assertEquals(9, result.data.map.edges.size)
        assertEquals(listOf("D11", "D12", "D13"), result.data.map.destinations)
        assertEquals(11, result.data.markers.markers.size)
    }

    @Test
    fun unknownEdgeNodeFails() {
        val errors = failures(map = mapJson.replace("\"to\": \"D12\"", "\"to\": \"NOWHERE\""))
        assertTrue(errors.any { "unknown node NOWHERE" in it })
    }

    @Test
    fun unknownMarkerNodeFails() {
        val errors = failures(markers = markersJson.replace("\"nodeId\": \"D13\"", "\"nodeId\": \"D99\""))
        assertTrue(errors.any { "Marker 10 points at unknown node D99" in it })
    }

    @Test
    fun duplicateMarkerIdFails() {
        val errors = failures(markers = markersJson.replace("\"id\": 10,", "\"id\": 9,"))
        assertTrue(errors.any { "Marker ID 9 is used more than once" in it })
    }

    @Test
    fun destinationThatIsNotARoomFails() {
        val errors = failures(map = mapJson.replace("\"destinations\": [\"D11\", \"D12\", \"D13\"]", "\"destinations\": [\"WALK_SD\"]"))
        assertTrue(errors.any { "WALK_SD is not a room" in it })
    }

    @Test
    fun unknownDestinationFails() {
        val errors = failures(map = mapJson.replace("\"D13\"]", "\"D14\"]"))
        assertTrue(errors.any { "D14 is not a known node" in it })
    }

    @Test
    fun zeroWeightFails() {
        val errors = failures(map = mapJson.replace("\"weight\": 1.35", "\"weight\": 0"))
        assertTrue(errors.any { "weight above 0" in it })
    }

    @Test
    fun stairsEdgeWithoutStepsFails() {
        val errors = failures(map = mapJson.replace("\"kind\": \"stairs\", \"steps\": 16", "\"kind\": \"stairs\", \"steps\": null"))
        assertTrue(errors.any { "no steps value" in it })
    }

    @Test
    fun unreachableDestinationFails() {
        // Remove the last edge so D13 cannot be reached.
        val cut = mapJson.replace(
            """,
    {"from": "D12", "to": "D13", "weight": 10.3, "kind": "walk", "steps": null, "hint": ""}""", "",
        )
        val errors = failures(map = cut)
        assertTrue(errors.any { "Destination D13 cannot be reached" in it })
    }

    @Test
    fun missingMarkerZeroFails() {
        val errors = failures(markers = markersJson.replace("\"id\": 0,", "\"id\": 20,"))
        assertTrue(errors.any { "Marker 0 is missing" in it })
    }

    @Test
    fun brokenJsonGivesAClearMessage() {
        assertTrue(failures(map = "not json").single().startsWith("map.json cannot be read"))
        assertTrue(failures(markers = "{").single().startsWith("markers.json cannot be read"))
    }
}
