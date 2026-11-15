package com.genvision.nova.core

/** Every tunable number lives here so the team can change it without touching other code. */
object Config {
    const val ANALYSIS_INTERVAL_MS = 150L
    const val ANALYSIS_WIDTH = 1280
    const val ANALYSIS_HEIGHT = 720
    const val CONFIRM_FRAMES = 2
    const val CONFIRM_WINDOW_MS = 1000L
    const val SAME_NODE_COOLDOWN_MS = 10_000L
    const val MIN_MARKER_SIDE_PX = 0
    const val WALKING_STEP_LENGTH_M = 0.70
    const val MIN_WALK_MENTION_M = 0.5
    const val STRAIGHT_MAX_DEG = 20.0
    const val BEAR_MAX_DEG = 50.0
    const val TURN_MAX_DEG = 130.0
    const val SHARP_MAX_DEG = 170.0
    const val NO_MARKER_REMINDER_MS = 12_000L
    const val REMINDER_REPEAT_MS = 15_000L
    const val MAX_REMINDERS = 3
    const val SPEECH_RATE = 1.0
    const val DEVELOPER_MODE = false
    const val LOG_MAX_BYTES = 1_000_000L
}
