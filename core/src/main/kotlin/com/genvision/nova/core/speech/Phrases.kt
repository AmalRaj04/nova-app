package com.genvision.nova.core.speech

import com.genvision.nova.core.geometry.TurnWord

/**
 * Every sentence the app speaks, in one place. To add Tamil later, replace the strings here
 * (or move them to a resource file); no other code builds spoken text.
 */
object Phrases {
    const val READY = "NOVA is ready. Where do you want to go?"
    const val OFF_ROUTE = "This is not on your route. New route."
    const val NO_MARKER_REMINDER = "I have not seen a marker. Point the phone forward and move slowly."
    const val HELP_AFTER_REMINDERS = "Still no marker. Press Repeat, or ask someone nearby for help."
    const val WHERE_AM_I_NOTHING = "No marker seen yet."
    const val GUIDANCE_STOPPED = "Guidance stopped."
    const val ASKING_CAMERA_PERMISSION =
        "NOVA needs to use the camera to read markers. Please allow it when asked."
    const val RETRYING = "Trying again."
    const val PERMISSION_DENIED =
        "NOVA needs the camera to read markers. Please allow the camera in settings."

    fun destinationChosen(destination: String) =
        "Going to $destination. Point the phone forward at a marker."

    fun arrival(destination: String, doorSide: String?) =
        if (doorSide == null) "You have arrived." else "You have arrived. $destination is on your $doorSide."

    fun destinationBehind(destination: String) =
        "$destination is behind you. This version cannot guide backwards. " +
            "Ask someone nearby to help, or return to the entrance."

    fun backAt(place: String) = "You are back at $place."

    fun whereAmI(place: String, secondsAgo: Long) = "Last marker: $place, $secondsAgo seconds ago."

    fun placeSentence(name: String, isRoom: Boolean) =
        if (isRoom) "You are beside $name." else "You are at $name."

    fun turn(word: TurnWord) = when (word) {
        TurnWord.STRAIGHT -> "Walk straight"
        TurnWord.BEAR_RIGHT -> "Bear right"
        TurnWord.BEAR_LEFT -> "Bear left"
        TurnWord.TURN_RIGHT -> "Turn right"
        TurnWord.TURN_LEFT -> "Turn left"
        TurnWord.SHARP_RIGHT -> "Turn sharp right"
        TurnWord.SHARP_LEFT -> "Turn sharp left"
        TurnWord.AROUND -> "Turn around"
    }

    private fun steps(n: Int) = if (n == 1) "1 step" else "$n steps"

    /** "Walk straight about 2 steps." (no turn needed) */
    fun walkStraight(n: Int) = "Walk straight about ${steps(n)}."

    /** "Walk about 8 steps." (after a turn sentence, or after stairs) */
    fun walk(n: Int) = "Walk about ${steps(n)}."

    fun thenWalk(n: Int) = "Then walk about ${steps(n)}."

    fun turnSentence(word: TurnWord) = "${turn(word)}."

    /** Stairs step counts are exact, so no "about". */
    fun climb(n: Int) = "Climb ${steps(n)} up."

    fun thenClimb(n: Int) = "Then climb ${steps(n)} up."
}
