package com.genvision.nova.speech

import com.genvision.nova.core.nav.NavState

enum class SpeechRoute {
    /** The app's own TextToSpeech. */
    OWN_SPEECH,
    /** Handed to the accessibility system, so TalkBack reads it in turn and does not talk over it. */
    ACCESSIBILITY,
}

/**
 * Accessibility requirement 7: TalkBack and the app's own speech must not talk over each other.
 *
 * - While guiding (WAITING or GUIDING) the app's own speech is used: instructions and reminders
 *   must arrive on time, and the student is not exploring the screen then.
 * - On the other screens, when touch exploration (TalkBack) is on, the message goes through the
 *   accessibility system. With TalkBack off, the app's own speech is used everywhere.
 */
object SpeechRouting {
    fun route(touchExplorationOn: Boolean, state: NavState): SpeechRoute =
        if (touchExplorationOn && state != NavState.WAITING && state != NavState.GUIDING) {
            SpeechRoute.ACCESSIBILITY
        } else {
            SpeechRoute.OWN_SPEECH
        }
}
