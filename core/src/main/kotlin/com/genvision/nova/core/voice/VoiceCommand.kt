package com.genvision.nova.core.voice

/** What the student asked for by voice. Voice never replaces the buttons; it does the same things. */
sealed interface VoiceCommand {
    data class GoTo(val destinationId: String) : VoiceCommand
    data object Repeat : VoiceCommand
    data object WhereAmI : VoiceCommand
    data object Stop : VoiceCommand
    data object ChooseAnother : VoiceCommand
}
