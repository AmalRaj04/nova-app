package com.genvision.nova.settings

import com.genvision.nova.core.Config
import kotlin.math.roundToInt

/** User-changeable values. The ranges come from the Configuration section of the brief. */
data class Settings(
    val stepLengthM: Double = Config.WALKING_STEP_LENGTH_M,
    val speechRate: Double = Config.SPEECH_RATE,
    val developerMode: Boolean = Config.DEVELOPER_MODE,
    /** Stretch goal: spoken commands. Off by default; the buttons always stay. */
    val voiceCommands: Boolean = false,
) {
    fun withStepLength(m: Double) = copy(stepLengthM = round(m.coerceIn(STEP_MIN, STEP_MAX), 100.0))
    fun withSpeechRate(r: Double) = copy(speechRate = round(r.coerceIn(RATE_MIN, RATE_MAX), 10.0))

    companion object {
        const val STEP_MIN = 0.50
        const val STEP_MAX = 0.90
        const val STEP_DELTA = 0.05
        const val RATE_MIN = 0.7
        const val RATE_MAX = 1.5
        const val RATE_DELTA = 0.1

        /** Rounds away float drift such as 0.7000000001. */
        private fun round(v: Double, scale: Double) = (v * scale).roundToInt() / scale
    }
}
