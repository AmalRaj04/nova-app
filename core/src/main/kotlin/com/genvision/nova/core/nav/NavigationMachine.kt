package com.genvision.nova.core.nav

import com.genvision.nova.core.Config
import com.genvision.nova.core.data.LoadResult
import com.genvision.nova.core.data.LoadedData
import com.genvision.nova.core.route.LegBuilder
import com.genvision.nova.core.route.RouteFinder
import com.genvision.nova.core.speech.InstructionBuilder
import com.genvision.nova.core.speech.Phrases

enum class NavState { LOADING, HOME, PERMISSION, WAITING, GUIDING, ARRIVED, ERROR }

/** Which step failed, so Retry knows what to run again. */
enum class ErrorKind { DATA, CAMERA, SPEECH }

sealed interface NavEvent {
    data class DataLoaded(val result: LoadResult) : NavEvent
    data class DestinationChosen(val destinationId: String, val cameraPermissionGranted: Boolean) : NavEvent
    data class PermissionResult(val granted: Boolean) : NavEvent
    /** An accepted sighting (after SightingFilter), real or simulated. */
    data class Sighting(val markerId: Int) : NavEvent
    /** Call regularly (about once a second) so reminders can fire. */
    data object Tick : NavEvent
    data object RepeatPressed : NavEvent
    data object WhereAmIPressed : NavEvent
    data object StopPressed : NavEvent
    data object ChooseAnotherPressed : NavEvent
    data class ErrorOccurred(val kind: ErrorKind, val message: String) : NavEvent
    data object RetryPressed : NavEvent
    data object BackPressed : NavEvent
    data object AppBackgrounded : NavEvent
    data object AppForegrounded : NavEvent
}

sealed interface Action {
    /**
     * Speak [text]. [interrupt] false means queue after what is playing. [protected] true means
     * nothing may interrupt it (arrival).
     */
    data class Speak(val text: String, val interrupt: Boolean = true, val protected: Boolean = false) : Action
    data object StartCamera : Action
    data object StopCamera : Action
    data object PauseCamera : Action
    data object ResumeCamera : Action
    data object RequestPermission : Action
    data object ShowSettingsButton : Action
    data class ShowError(val message: String, val kind: ErrorKind) : Action
    /** Run the failed step again. */
    data class RetryStep(val kind: ErrorKind) : Action
    data class Log(val event: String, val fields: Map<String, String> = emptyMap()) : Action
}

/**
 * The whole guidance logic as one pure state machine (no Android classes).
 * Events go in through [handle]; the returned actions are carried out by the caller.
 * Rows in comments refer to the state table in NOVA_APP_SPEC.md.
 */
class NavigationMachine(
    private val clock: Clock,
    /** Read on every instruction so a settings change applies to the next one. */
    private val stepLengthM: () -> Double = { Config.WALKING_STEP_LENGTH_M },
) {
    var state: NavState = NavState.LOADING
        private set
    var destinationId: String? = null
        private set
    var currentNodeId: String? = null
        private set
    var lastInstruction: String? = null
        private set
    var lastSightingNodeId: String? = null
        private set

    private var data: LoadedData? = null
    private var route: List<String>? = null
    private var routeIndex = 0
    private var lastSightingAtMs = 0L

    private var pendingDestination: String? = null
    private var timerStartMs = 0L
    private var reminderCount = 0
    private var helpSpoken = false
    private var paused = false

    private var stateBeforeError = NavState.HOME
    private var failedKind = ErrorKind.DATA

    fun handle(event: NavEvent): List<Action> {
        val out = mutableListOf<Action>()
        when (event) {
            is NavEvent.DataLoaded -> onDataLoaded(event.result, out)
            is NavEvent.DestinationChosen -> onDestinationChosen(event, out)
            is NavEvent.PermissionResult -> onPermission(event.granted, out)
            is NavEvent.Sighting -> onSighting(event.markerId, out)
            NavEvent.Tick -> onTick(out)
            NavEvent.RepeatPressed -> onRepeat(out)
            NavEvent.WhereAmIPressed -> onWhereAmI(out)
            NavEvent.StopPressed -> onStop(out)
            NavEvent.ChooseAnotherPressed -> onChooseAnother(out)
            is NavEvent.ErrorOccurred -> onError(event, out)
            NavEvent.RetryPressed -> onRetry(out)
            NavEvent.BackPressed -> onBack(out)
            NavEvent.AppBackgrounded -> onBackgrounded(out)
            NavEvent.AppForegrounded -> onForegrounded(out)
        }
        return out
    }

    private val guiding get() = state == NavState.WAITING || state == NavState.GUIDING

    // Rows 1 and 2
    private fun onDataLoaded(result: LoadResult, out: MutableList<Action>) {
        if (state != NavState.LOADING) return
        when (result) {
            is LoadResult.Success -> {
                data = result.data
                state = NavState.HOME
                out += Action.Speak(Phrases.READY)
            }
            is LoadResult.Failure -> raiseError(ErrorKind.DATA, result.errors.joinToString(" "), out)
        }
    }

    // Rows 3 and 4
    private fun onDestinationChosen(e: NavEvent.DestinationChosen, out: MutableList<Action>) {
        val d = data ?: return
        if (state != NavState.HOME || e.destinationId !in d.map.destinations) return
        if (e.cameraPermissionGranted) {
            startGuidance(e.destinationId, out)
        } else {
            pendingDestination = e.destinationId
            state = NavState.PERMISSION
            out += Action.Speak(Phrases.ASKING_CAMERA_PERMISSION)
            out += Action.RequestPermission
        }
    }

    // Rows 5 and 6
    private fun onPermission(granted: Boolean, out: MutableList<Action>) {
        if (state != NavState.PERMISSION) return
        val dest = pendingDestination
        pendingDestination = null
        if (granted && dest != null) {
            startGuidance(dest, out)
        } else {
            state = NavState.HOME
            out += Action.Speak(Phrases.PERMISSION_DENIED)
            out += Action.ShowSettingsButton
        }
    }

    private fun startGuidance(dest: String, out: MutableList<Action>) {
        destinationId = dest
        route = null
        routeIndex = 0
        currentNodeId = null
        lastInstruction = null
        lastSightingNodeId = null
        paused = false
        state = NavState.WAITING
        resetTimer()
        out += Action.Log("destination_chosen", mapOf("id" to dest))
        out += Action.StartCamera
        out += Action.Speak(Phrases.destinationChosen(nodeName(dest)))
    }

    // Rows 7 to 13
    private fun onSighting(markerId: Int, out: MutableList<Action>) {
        val d = data ?: return
        if (!guiding || paused) return
        val marker = d.markers.marker(markerId)
        if (marker == null) {
            out += Action.Log("unknown_marker", mapOf("marker" to markerId.toString()))
            return
        }
        val nodeId = marker.nodeId
        currentNodeId = nodeId
        lastSightingNodeId = nodeId
        lastSightingAtMs = clock.nowMs()
        resetTimer()
        out += Action.Log("sighting", mapOf("marker" to markerId.toString(), "node" to nodeId))

        val dest = destinationId ?: return
        if (nodeId == dest) { // rows 7 and 10
            arrive(dest, out)
            return
        }
        if (state == NavState.WAITING) { // rows 8 and 9
            plan(nodeId, marker.facingDeg, null, out)
            return
        }

        val current = route
        val index = current?.indexOf(nodeId) ?: -1
        when {
            current != null && index > routeIndex -> { // row 11: ahead on the planned route
                routeIndex = index
                speakInstruction(null, instructionBuilder().legInstruction(current, index, marker.facingDeg)!!, out)
            }
            current != null && index in 0 until routeIndex -> { // row 12: back at an earlier place
                plan(nodeId, marker.facingDeg, Phrases.backAt(nodeName(nodeId)), out)
            }
            current != null && index == routeIndex -> {
                // Same place again after the cooldown. The brief has no row for this: stay quiet.
                out += Action.Log("repeat_sighting", mapOf("node" to nodeId))
            }
            else -> { // row 13: not on the planned route
                out += Action.Log("off_route", mapOf("marker" to markerId.toString(), "node" to nodeId))
                plan(nodeId, marker.facingDeg, Phrases.OFF_ROUTE, out)
            }
        }
    }

    /** Plans from [nodeId] and speaks the first leg. With no forward path, speaks "behind you". */
    private fun plan(nodeId: String, facingDeg: Double, prefix: String?, out: MutableList<Action>) {
        val d = data ?: return
        val dest = destinationId ?: return
        val newRoute = RouteFinder(d.map).find(nodeId, dest)
        if (newRoute == null) {
            out += Action.Speak(Phrases.destinationBehind(nodeName(dest)))
            return
        }
        route = newRoute
        routeIndex = 0
        state = NavState.GUIDING
        val text = instructionBuilder().legInstruction(newRoute, 0, facingDeg)!!
        speakInstruction(prefix, text, out)
    }

    private fun speakInstruction(prefix: String?, text: String, out: MutableList<Action>) {
        if (prefix == null) {
            out += Action.Speak(text)
            lastInstruction = text
        } else {
            out += Action.Speak(prefix)
            out += Action.Speak(text, interrupt = false)
            lastInstruction = "$prefix $text"
        }
        resetTimer()
    }

    private fun arrive(dest: String, out: MutableList<Action>) {
        state = NavState.ARRIVED
        out += Action.Log("arrival", mapOf("node" to dest))
        out += Action.Speak(instructionBuilder().arrival(dest), protected = true)
        out += Action.StopCamera
    }

    // Rows 14 and 15
    private fun onTick(out: MutableList<Action>) {
        if (!guiding || paused) return
        val elapsed = clock.nowMs() - timerStartMs
        val due = Config.NO_MARKER_REMINDER_MS + reminderCount * Config.REMINDER_REPEAT_MS
        if (reminderCount < Config.MAX_REMINDERS) {
            if (elapsed >= due) {
                reminderCount++
                out += Action.Log("reminder", mapOf("count" to reminderCount.toString()))
                out += Action.Speak(Phrases.NO_MARKER_REMINDER)
            }
        } else if (!helpSpoken && elapsed >= due) {
            helpSpoken = true
            out += Action.Log("help")
            out += Action.Speak(Phrases.HELP_AFTER_REMINDERS)
        }
    }

    // Row 16
    private fun onRepeat(out: MutableList<Action>) {
        if (state != NavState.GUIDING) return
        val text = lastInstruction ?: return
        resetTimer()
        out += Action.Speak(text)
    }

    // Row 17
    private fun onWhereAmI(out: MutableList<Action>) {
        if (!guiding) return
        val node = lastSightingNodeId
        out += Action.Speak(
            if (node == null) Phrases.WHERE_AM_I_NOTHING
            else Phrases.whereAmI(nodeName(node), (clock.nowMs() - lastSightingAtMs) / 1000),
        )
    }

    // Row 18
    private fun onStop(out: MutableList<Action>) {
        if (!guiding) return
        state = NavState.HOME
        out += Action.StopCamera
        out += Action.Speak(Phrases.GUIDANCE_STOPPED)
    }

    // Row 19
    private fun onChooseAnother(out: MutableList<Action>) {
        if (state != NavState.ARRIVED) return
        state = NavState.HOME
        out += Action.StopCamera
        out += Action.Speak(Phrases.READY)
    }

    // Row 20
    private fun onError(e: NavEvent.ErrorOccurred, out: MutableList<Action>) {
        // While an error is already showing, later ones (for example one per failed camera frame)
        // would only repeat the same speech.
        if (state == NavState.ERROR) return
        // A camera error that arrives after the camera was stopped is a late echo, not news.
        if (e.kind == ErrorKind.CAMERA && !guiding) return
        raiseError(e.kind, e.message, out)
    }

    private fun raiseError(kind: ErrorKind, message: String, out: MutableList<Action>) {
        if (state != NavState.ERROR) stateBeforeError = state
        failedKind = kind
        state = NavState.ERROR
        out += Action.Log("error", mapOf("kind" to kind.name, "message" to message))
        out += Action.ShowError(message, kind)
        out += Action.Speak(message)
    }

    // Row 21
    private fun onRetry(out: MutableList<Action>) {
        if (state != NavState.ERROR) return
        state = when (stateBeforeError) {
            NavState.LOADING, NavState.WAITING, NavState.GUIDING -> stateBeforeError
            else -> NavState.HOME
        }
        if (guiding) resetTimer()
        out += Action.Speak(Phrases.RETRYING)
        out += Action.RetryStep(failedKind)
    }

    /** The error screen's Back button. */
    private fun onBack(out: MutableList<Action>) {
        if (state != NavState.ERROR) return
        state = NavState.HOME
        out += Action.StopCamera
    }

    // Row 22
    private fun onBackgrounded(out: MutableList<Action>) {
        if (!guiding || paused) return
        paused = true
        out += Action.PauseCamera
    }

    private fun onForegrounded(out: MutableList<Action>) {
        if (!guiding || !paused) return
        paused = false
        resetTimer() // do not fire a reminder for time spent in the background
        out += Action.ResumeCamera
    }

    /** Reminder counts and timers reset on every accepted sighting and every spoken instruction. */
    private fun resetTimer() {
        timerStartMs = clock.nowMs()
        reminderCount = 0
        helpSpoken = false
    }

    private fun nodeName(id: String) = data?.map?.node(id)?.speechName ?: id

    private fun instructionBuilder(): InstructionBuilder {
        val d = data!!
        val markerNodes = d.markers.markers.map { it.nodeId }.toSet()
        return InstructionBuilder(d.map, LegBuilder(d.map, markerNodes), stepLengthM())
    }
}
