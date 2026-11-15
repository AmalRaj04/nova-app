package com.genvision.nova.core.speech

import com.genvision.nova.core.Config
import com.genvision.nova.core.geometry.Bearing
import com.genvision.nova.core.geometry.TurnClassifier
import com.genvision.nova.core.geometry.TurnWord
import com.genvision.nova.core.model.EdgeKind
import com.genvision.nova.core.model.MapData
import com.genvision.nova.core.route.Leg
import com.genvision.nova.core.route.LegBuilder
import kotlin.math.floor

/** Builds the spoken text for a leg from the route and the map. Nothing is typed into the code. */
class InstructionBuilder(
    private val map: MapData,
    private val legBuilder: LegBuilder,
    private val stepLengthM: Double = Config.WALKING_STEP_LENGTH_M,
    private val minWalkMentionM: Double = Config.MIN_WALK_MENTION_M,
    private val classifier: TurnClassifier = TurnClassifier(),
) {
    /**
     * The instruction for the leg that starts at `route[startIndex]`, given the facing of the
     * marker that was seen. Returns null if that node is the end of the route.
     */
    fun legInstruction(route: List<String>, startIndex: Int, facingDeg: Double): String? {
        val leg = legBuilder.build(route, startIndex) ?: return null
        return legInstruction(leg, facingDeg)
    }

    fun legInstruction(leg: Leg, facingDeg: Double): String {
        val start = map.node(leg.startNodeId)!!
        val sentences = mutableListOf(Phrases.placeSentence(start.speechName, start.isRoom))

        val turn = classifier.classify(Bearing.turnAngle(facingDeg, leg.firstHeadingDeg))
        var movementSpoken = false

        // Walk edges next to each other are added together and rounded once, at the end.
        var walkMetres = 0.0
        var firstGroup = true
        fun flushWalk() {
            if (walkMetres >= minWalkMentionM) {
                val n = roundHalfUp(walkMetres / stepLengthM)
                sentences += when {
                    firstGroup && turn == TurnWord.STRAIGHT -> Phrases.walkStraight(n)
                    firstGroup -> Phrases.walk(n)
                    else -> Phrases.thenWalk(n)
                }
                movementSpoken = true
            }
            walkMetres = 0.0
        }

        if (turn != TurnWord.STRAIGHT) {
            sentences += Phrases.turnSentence(turn)
        }
        for (edge in leg.edges) {
            if (edge.kind == EdgeKind.STAIRS) {
                flushWalk()
                firstGroup = false
                val n = edge.steps!! // the map loader guarantees stairs have a step count
                sentences += if (movementSpoken) Phrases.thenClimb(n) else Phrases.climb(n)
                movementSpoken = true
            } else {
                walkMetres += edge.weight
            }
        }
        flushWalk()

        // Hints from an instructor, if any, come last, one sentence each.
        leg.edges.map { it.hint.trim() }.filter { it.isNotEmpty() }.forEach { sentences += it }
        return sentences.joinToString(" ")
    }

    fun arrival(destinationId: String): String {
        val node = map.node(destinationId)!!
        return Phrases.arrival(node.speechName, node.doorSide)
    }

    private fun roundHalfUp(x: Double): Int = floor(x + 0.5 + 1e-9).toInt()
}
