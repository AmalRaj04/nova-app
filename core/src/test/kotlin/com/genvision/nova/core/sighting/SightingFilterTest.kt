package com.genvision.nova.core.sighting

import com.genvision.nova.core.TestData
import kotlin.test.Test
import kotlin.test.assertEquals

class SightingFilterTest {
    private val nodes = TestData.loaded.markers.markers.associate { it.id to it.nodeId }
    private fun filter() = SightingFilter({ nodes[it] })

    @Test
    fun oneFrameDoesNotAccept() {
        assertEquals(emptyList(), filter().onFrame(listOf(0), 0))
    }

    @Test
    fun twoFramesInsideTheWindowAccept() {
        val f = filter()
        assertEquals(emptyList(), f.onFrame(listOf(0), 0))
        assertEquals(listOf(0), f.onFrame(listOf(0), 300))
    }

    @Test
    fun twoFramesOutsideTheWindowDoNotAccept() {
        val f = filter()
        f.onFrame(listOf(0), 0)
        assertEquals(emptyList(), f.onFrame(listOf(0), 1500))
        // ...but the second one now starts a new window, so a third soon after accepts.
        assertEquals(listOf(0), f.onFrame(listOf(0), 1800))
    }

    @Test
    fun cooldownIgnoresRepeatsAtTheSameNode() {
        val f = filter()
        f.onFrame(listOf(0), 0); f.onFrame(listOf(0), 150)
        f.onFrame(listOf(0), 2000)
        assertEquals(emptyList(), f.onFrame(listOf(0), 2150))
        // Marker 1 is at the same node as marker 0, so it is a repeat too.
        f.onFrame(listOf(1), 3000)
        assertEquals(emptyList(), f.onFrame(listOf(1), 3150))
    }

    @Test
    fun repeatIsAcceptedAfterTheCooldown() {
        val f = filter()
        f.onFrame(listOf(0), 0); f.onFrame(listOf(0), 150)
        f.onFrame(listOf(0), 10_200)
        assertEquals(listOf(0), f.onFrame(listOf(0), 10_350))
    }

    @Test
    fun aDifferentNodeInBetweenResetsTheCooldown() {
        val f = filter()
        f.onFrame(listOf(0), 0); assertEquals(listOf(0), f.onFrame(listOf(0), 150))
        f.onFrame(listOf(2), 1000); assertEquals(listOf(2), f.onFrame(listOf(2), 1150))
        f.onFrame(listOf(0), 2000)
        assertEquals(listOf(0), f.onFrame(listOf(0), 2150)) // well inside 10 s of the first sighting
    }

    @Test
    fun unknownMarkerIdsAreIgnored() {
        val f = filter()
        f.onFrame(listOf(42), 0)
        assertEquals(emptyList(), f.onFrame(listOf(42), 100))
    }

    @Test
    fun resetForgetsEverything() {
        val f = filter()
        f.onFrame(listOf(0), 0); f.onFrame(listOf(0), 150)
        f.reset()
        f.onFrame(listOf(0), 200)
        assertEquals(listOf(0), f.onFrame(listOf(0), 350))
    }
}
