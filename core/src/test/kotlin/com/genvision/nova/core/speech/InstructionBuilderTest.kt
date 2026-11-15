package com.genvision.nova.core.speech

import com.genvision.nova.core.TestData
import com.genvision.nova.core.model.Edge
import com.genvision.nova.core.model.MapData
import com.genvision.nova.core.model.Node
import com.genvision.nova.core.route.LegBuilder
import com.genvision.nova.core.route.RouteFinder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InstructionBuilderTest {
    private val data = TestData.loaded
    private val markerNodes = data.markers.markers.map { it.nodeId }.toSet()
    private val finder = RouteFinder(data.map)

    private fun builder(step: Double = 0.70) =
        InstructionBuilder(data.map, LegBuilder(data.map, markerNodes), stepLengthM = step)

    /** Text spoken when marker [markerId] is seen on the way to [destination]. */
    private fun spoken(markerId: Int, destination: String, step: Double = 0.70): String? {
        val marker = data.markers.marker(markerId)!!
        val route = finder.find(marker.nodeId, destination)!!
        return builder(step).legInstruction(route, 0, marker.facingDeg)
    }

    @Test
    fun goldenStringsForTheDemoRoute() {
        val entrance = "You are at the entrance. Walk straight about 2 steps. Then climb 16 steps up."
        assertEquals(entrance, spoken(0, "D13"))
        assertEquals(entrance, spoken(1, "D13"))
        val landing = "You are at the stairs landing. Walk straight about 2 steps. Then climb 10 steps up."
        assertEquals(landing, spoken(2, "D13"))
        assertEquals(landing, spoken(3, "D13"))
        val top = "You are at the top of the main stairs. Turn right. Walk about 8 steps."
        assertEquals(top, spoken(4, "D13"))
        assertEquals(top, spoken(5, "D13"))
        assertEquals("You are at the walkway corner near the stairs. Turn left. Walk about 14 steps.", spoken(6, "D13"))
        assertEquals("You are at the start of the D block path. Turn right. Walk about 11 steps.", spoken(7, "D13"))
        assertEquals("You are beside D11. Walk straight about 15 steps.", spoken(8, "D12"))
        assertEquals("You are beside D11. Walk straight about 15 steps.", spoken(8, "D13"))
        assertEquals("You are beside D12. Walk straight about 15 steps.", spoken(9, "D13"))
    }

    @Test
    fun arrivalGoldenStrings() {
        val b = builder()
        assertEquals("You have arrived. D11 is on your right.", b.arrival("D11"))
        assertEquals("You have arrived. D12 is on your right.", b.arrival("D12"))
        assertEquals("You have arrived. D13 is on your right.", b.arrival("D13"))
    }

    @Test
    fun noInstructionAtTheEndOfTheRoute() {
        assertNull(spoken(10, "D13"))
    }

    @Test
    fun differentStepLengthChangesStepCounts() {
        assertEquals("You are at the top of the main stairs. Turn right. Walk about 9 steps.", spoken(4, "D13", 0.60))
        assertEquals("You are at the walkway corner near the stairs. Turn left. Walk about 17 steps.", spoken(6, "D13", 0.60))
    }

    // --- Small synthetic maps for rounding, summing and hints -----------------------------

    private fun line(vararg walks: Pair<Double, String>): MapData {
        val nodes = mutableListOf<Node>()
        val edges = mutableListOf<Edge>()
        var y = 0.0
        nodes += Node("N0", "start", "junction", x = 0.0, y = 0.0)
        walks.forEachIndexed { i, (metres, hint) ->
            y -= metres // going up the plan = bearing 0
            nodes += Node("N${i + 1}", "place ${i + 1}", "junction", x = 0.0, y = y)
            edges += Edge("N$i", "N${i + 1}", metres, "walk", null, hint)
        }
        return MapData(nodes = nodes, edges = edges, destinations = emptyList())
    }

    private fun text(map: MapData, step: Double = 0.70): String {
        val route = map.nodes.map { it.id }
        // Only the first node is a marker node, so the leg covers the whole line.
        return InstructionBuilder(map, LegBuilder(map, setOf("N0")), stepLengthM = step)
            .legInstruction(route, 0, 0.0)!!
    }

    @Test
    fun halvesRoundUp() {
        // 0.75 m / 0.5 m = 1.5 steps -> 2
        assertEquals("You are at start. Walk straight about 2 steps.", text(line(0.75 to ""), step = 0.5))
    }

    @Test
    fun walksAreSummedBeforeRounding() {
        // Each 0.35 m is 0.5 step (rounds to 1), but together 0.70 m is exactly 1 step.
        assertEquals("You are at start. Walk straight about 1 step.", text(line(0.35 to "", 0.35 to "")))
    }

    @Test
    fun shortWalkIsNotMentioned() {
        assertEquals("You are at start.", text(line(0.4 to "")))
    }

    @Test
    fun hintsAreAddedLastEachOnItsOwn() {
        val result = text(line(7.0 to "Keep the wall on your left.", 7.0 to "Watch for the pillar."))
        assertEquals(
            "You are at start. Walk straight about 20 steps. Keep the wall on your left. Watch for the pillar.",
            result,
        )
    }
}
