package com.genvision.nova.core.route

import com.genvision.nova.core.TestData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RouteFinderTest {
    private val finder = RouteFinder(TestData.loaded.map)
    private val start = "ENTRANCE_PLAZA" // node of marker 0

    private val toD11 = listOf(
        "ENTRANCE_PLAZA", "STAIRS_FOOT", "STAIRS_LANDING_DOWN", "STAIRS_LANDING_UP",
        "MAIN_STAIRS_TOP", "WALK_SD", "WALK_FD", "D11",
    )

    @Test
    fun routesMatchTheChainInTheBrief() {
        assertEquals(toD11, finder.find(start, "D11"))
        assertEquals(toD11 + "D12", finder.find(start, "D12"))
        assertEquals(toD11 + "D12" + "D13", finder.find(start, "D13"))
    }

    @Test
    fun goingBackwardFindsNoRoute() {
        assertNull(finder.find("D13", "D11"))
        assertNull(finder.find("D11", "ENTRANCE_PLAZA"))
    }

    @Test
    fun routeFromANodeToItselfIsThatNode() {
        assertEquals(listOf("D12"), finder.find("D12", "D12"))
    }

    @Test
    fun unknownNodesFindNoRoute() {
        assertNull(finder.find("NOWHERE", "D11"))
        assertNull(finder.find(start, "NOWHERE"))
    }
}
