package com.genvision.nova.core.geometry

import kotlin.test.Test
import kotlin.test.assertEquals

class BearingTest {
    private fun bearing(dx: Double, dy: Double) = Bearing.between(0.0, 0.0, dx, dy)

    @Test
    fun compassDirections() {
        assertEquals(0.0, bearing(0.0, -1.0), 1e-9)   // up on the plan = north
        assertEquals(90.0, bearing(1.0, 0.0), 1e-9)   // east
        assertEquals(180.0, bearing(0.0, 1.0), 1e-9)  // south
        assertEquals(270.0, bearing(-1.0, 0.0), 1e-9) // west
    }

    @Test
    fun demoRouteHeadingsMatchTheBrief() {
        // MAIN_STAIRS_TOP -> WALK_SD
        assertEquals(74.5, Bearing.between(0.0, 10.18, 5.07, 8.77), 0.1)
        // WALK_SD -> WALK_FD
        assertEquals(30.0, Bearing.between(5.07, 8.77, 10.13, 0.0), 0.1)
        // WALK_FD -> D11
        assertEquals(120.0, Bearing.between(10.13, 0.0, 16.97, 3.95), 0.1)
        // ENTRANCE_PLAZA -> STAIRS_FOOT goes straight up the plan
        assertEquals(0.0, Bearing.between(0.0, 20.63, 0.0, 19.28), 0.1)
    }

    @Test
    fun resultIsNeverNegativeOr360() {
        val b = Bearing.between(0.0, 0.0, -0.0001, -1.0)
        assert(b >= 0.0 && b < 360.0)
    }

    @Test
    fun turnAngleWraps() {
        assertEquals(20.0, Bearing.turnAngle(350.0, 10.0), 1e-9)
        assertEquals(-20.0, Bearing.turnAngle(10.0, 350.0), 1e-9)
    }

    @Test
    fun turnAngleSignsMatchTheBrief() {
        assertEquals(74.5, Bearing.turnAngle(0.0, 74.5), 1e-9)   // right at top of stairs
        assertEquals(-60.0, Bearing.turnAngle(90.0, 30.0), 1e-9) // left at marker 6
        assertEquals(90.0, Bearing.turnAngle(30.0, 120.0), 1e-9) // right at marker 7
        assertEquals(0.0, Bearing.turnAngle(120.0, 120.0), 1e-9)
    }

    @Test
    fun turnAngleRangeIsMinus180ExclusiveToPlus180() {
        assertEquals(180.0, Bearing.turnAngle(0.0, 180.0), 1e-9)
        assertEquals(180.0, Bearing.turnAngle(180.0, 0.0), 1e-9)
    }
}
