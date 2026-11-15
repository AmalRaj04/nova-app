package com.genvision.nova

import com.genvision.nova.core.nav.NavState
import com.genvision.nova.speech.SpeechRoute
import com.genvision.nova.speech.SpeechRouting
import kotlin.test.Test
import kotlin.test.assertEquals

class SpeechRoutingTest {
    @Test fun talkBackOffAlwaysUsesOwnSpeech() {
        for (state in NavState.entries) {
            assertEquals(SpeechRoute.OWN_SPEECH, SpeechRouting.route(false, state), "state $state")
        }
    }

    @Test fun talkBackOnWhileGuidingUsesOwnSpeech() {
        assertEquals(SpeechRoute.OWN_SPEECH, SpeechRouting.route(true, NavState.WAITING))
        assertEquals(SpeechRoute.OWN_SPEECH, SpeechRouting.route(true, NavState.GUIDING))
    }

    @Test fun talkBackOnElsewhereUsesTheAccessibilitySystem() {
        for (state in listOf(NavState.LOADING, NavState.HOME, NavState.PERMISSION, NavState.ARRIVED, NavState.ERROR)) {
            assertEquals(SpeechRoute.ACCESSIBILITY, SpeechRouting.route(true, state), "state $state")
        }
    }
}
