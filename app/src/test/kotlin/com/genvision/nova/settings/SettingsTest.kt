package com.genvision.nova.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsTest {
    @Test fun stepLengthStaysInRange() {
        assertEquals(0.90, Settings().withStepLength(5.0).stepLengthM)
        assertEquals(0.50, Settings().withStepLength(0.1).stepLengthM)
    }

    @Test fun plusAndMinusStepsDoNotDrift() {
        var s = Settings()
        repeat(3) { s = s.withStepLength(s.stepLengthM + Settings.STEP_DELTA) }
        assertEquals(0.85, s.stepLengthM)
        repeat(7) { s = s.withStepLength(s.stepLengthM - Settings.STEP_DELTA) }
        assertEquals(0.50, s.stepLengthM)
    }

    @Test fun speechRateStaysInRange() {
        assertEquals(1.5, Settings().withSpeechRate(3.0).speechRate)
        assertEquals(0.7, Settings().withSpeechRate(0.0).speechRate)
        assertEquals(1.1, Settings().withSpeechRate(1.0 + Settings.RATE_DELTA).speechRate)
    }
}
