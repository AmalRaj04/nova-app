package com.genvision.nova.core.geometry

import com.genvision.nova.core.Config
import kotlin.math.abs

/** What the student is told to do with their body. Wording lives in `Phrases`, not here. */
enum class TurnWord { STRAIGHT, BEAR_RIGHT, BEAR_LEFT, TURN_RIGHT, TURN_LEFT, SHARP_RIGHT, SHARP_LEFT, AROUND }

/** Turns an angle into a [TurnWord]. Each upper limit belongs to the lower band (20 is still straight). */
class TurnClassifier(
    private val straightMaxDeg: Double = Config.STRAIGHT_MAX_DEG,
    private val bearMaxDeg: Double = Config.BEAR_MAX_DEG,
    private val turnMaxDeg: Double = Config.TURN_MAX_DEG,
    private val sharpMaxDeg: Double = Config.SHARP_MAX_DEG,
) {
    fun classify(turnAngleDeg: Double): TurnWord {
        val size = abs(turnAngleDeg)
        val right = turnAngleDeg > 0
        return when {
            size <= straightMaxDeg -> TurnWord.STRAIGHT
            size <= bearMaxDeg -> if (right) TurnWord.BEAR_RIGHT else TurnWord.BEAR_LEFT
            size <= turnMaxDeg -> if (right) TurnWord.TURN_RIGHT else TurnWord.TURN_LEFT
            size <= sharpMaxDeg -> if (right) TurnWord.SHARP_RIGHT else TurnWord.SHARP_LEFT
            else -> TurnWord.AROUND
        }
    }
}
