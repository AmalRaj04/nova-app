package com.genvision.nova.core.geometry

import com.genvision.nova.core.geometry.TurnWord.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TurnClassifierTest {
    private val c = TurnClassifier()

    @Test
    fun upperLimitBelongsToTheLowerBand() {
        assertEquals(STRAIGHT, c.classify(20.0))
        assertEquals(BEAR_RIGHT, c.classify(20.1))
        assertEquals(BEAR_RIGHT, c.classify(50.0))
        assertEquals(TURN_RIGHT, c.classify(50.1))
        assertEquals(TURN_RIGHT, c.classify(130.0))
        assertEquals(SHARP_RIGHT, c.classify(130.1))
        assertEquals(SHARP_RIGHT, c.classify(170.0))
        assertEquals(AROUND, c.classify(170.1))
    }

    @Test
    fun leftMirrorsRight() {
        assertEquals(STRAIGHT, c.classify(-20.0))
        assertEquals(BEAR_LEFT, c.classify(-50.0))
        assertEquals(TURN_LEFT, c.classify(-130.0))
        assertEquals(SHARP_LEFT, c.classify(-170.0))
        assertEquals(AROUND, c.classify(-170.1))
        assertEquals(AROUND, c.classify(180.0))
    }

    @Test
    fun demoRouteTurns() {
        assertEquals(STRAIGHT, c.classify(0.0))
        assertEquals(TURN_RIGHT, c.classify(74.5))  // top of stairs
        assertEquals(TURN_LEFT, c.classify(-60.0))  // marker 6
        assertEquals(TURN_RIGHT, c.classify(90.0))  // marker 7
    }
}
