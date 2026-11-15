package com.genvision.nova

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.genvision.nova.core.Config
import com.genvision.nova.core.data.LoadResult
import com.genvision.nova.core.data.LoadedData
import com.genvision.nova.core.data.MapLoader
import com.genvision.nova.core.nav.Action
import com.genvision.nova.core.nav.Clock
import com.genvision.nova.core.nav.ErrorKind
import com.genvision.nova.core.nav.NavEvent
import com.genvision.nova.core.nav.NavState
import com.genvision.nova.core.nav.NavigationMachine
import com.genvision.nova.core.sighting.SightingFilter
import com.genvision.nova.core.speech.Phrases
import com.genvision.nova.log.EventLog
import com.genvision.nova.settings.Settings
import com.genvision.nova.settings.SettingsStore
import com.genvision.nova.speech.AndroidSpeaker
import com.genvision.nova.speech.Speaker
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DestinationItem(val id: String, val name: String)

/** Everything the screens need. The screens hold no logic of their own. */
data class UiState(
    val navState: NavState = NavState.LOADING,
    val destinations: List<DestinationItem> = emptyList(),
    val destinationName: String = "",
    /** Text for the Guidance screen: the last instruction, or the start prompt. */
    val guidanceText: String = "",
    val currentPlace: String = "none yet",
    val errorMessage: String = "",
    val errorKind: ErrorKind = ErrorKind.DATA,
    val showSettingsButton: Boolean = false,
    val requestPermission: Boolean = false,
    val settingsOpen: Boolean = false,
    /** Marker analysis frames per second, shown in the developer panel. */
    val analysisRate: Double = 0.0,
    val settings: Settings = Settings(),
    val markerIds: List<Int> = emptyList(),
    val logLines: List<String> = emptyList(),
)

private object SystemClockSource : Clock {
    override fun nowMs() = SystemClock.elapsedRealtime()
}

/**
 * Connects the pure [NavigationMachine] to Android: it sends events in and carries out the
 * returned actions with the speaker, the event log and the UI state.
 */
class NovaViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsStore = SettingsStore(app)
    private val log = EventLog(File(app.filesDir, "logs"), Config.LOG_MAX_BYTES)
    private val machine = NavigationMachine(SystemClockSource) { settingsStore.settings.value.stepLengthM }
    private var speaker: Speaker = createSpeaker()
    private var loaded: LoadedData? = null
    private var filter = SightingFilter({ null })

    private val _ui = MutableStateFlow(UiState(settings = settingsStore.settings.value))
    val ui: StateFlow<UiState> = _ui

    private val _cameraActive = MutableStateFlow(false)
    /** True while the camera should run (WAITING or GUIDING, and the app is in the foreground). */
    val cameraActive: StateFlow<Boolean> = _cameraActive
    private val _cameraGeneration = MutableStateFlow(0)
    /** Bumped by Retry after a camera error so the camera is started again. */
    val cameraGeneration: StateFlow<Int> = _cameraGeneration
    private var analysisRate = 0.0
    private var cameraWanted = false
    private var cameraPaused = false

    private var errorMessage = ""
    private var errorKind = ErrorKind.DATA
    private var showSettingsButton = false
    private var requestPermission = false
    private var settingsOpen = false

    init {
        log.log("app_start", mapOf("version" to "1"))
        speaker.setRate(settingsStore.settings.value.speechRate.toFloat())
        viewModelScope.launch { log.tail.collect { publish() } }
        viewModelScope.launch { settingsStore.settings.collect { speaker.setRate(it.speechRate.toFloat()); publish() } }
        loadData()
        viewModelScope.launch { // reminders need the machine to see time pass
            while (true) {
                delay(1000)
                dispatch(NavEvent.Tick)
            }
        }
    }

    private fun createSpeaker(): Speaker = AndroidSpeaker(getApplication()) { message ->
        dispatch(NavEvent.ErrorOccurred(ErrorKind.SPEECH, message))
    }

    private fun loadData() {
        val assets = getApplication<Application>().assets
        val result = try {
            val map = assets.open("map.json").bufferedReader().use { it.readText() }
            val markers = assets.open("markers.json").bufferedReader().use { it.readText() }
            MapLoader.load(map, markers)
        } catch (e: Exception) {
            LoadResult.Failure(listOf("The map files could not be opened: ${e.message}"))
        }
        if (result is LoadResult.Success) {
            loaded = result.data
            val nodeOfMarker = result.data.markers.markers.associate { it.id to it.nodeId }
            filter = SightingFilter({ nodeOfMarker[it] })
        }
        dispatch(NavEvent.DataLoaded(result))
    }

    // ---- Calls from the screens ----

    fun chooseDestination(id: String, cameraPermissionGranted: Boolean) {
        showSettingsButton = false
        dispatch(NavEvent.DestinationChosen(id, cameraPermissionGranted))
    }

    fun onPermissionResult(granted: Boolean) = dispatch(NavEvent.PermissionResult(granted))
    fun permissionRequestHandled() { requestPermission = false; publish() }
    fun repeat() = dispatch(NavEvent.RepeatPressed)
    fun whereAmI() = dispatch(NavEvent.WhereAmIPressed)
    fun stop() = dispatch(NavEvent.StopPressed)
    fun chooseAnother() = dispatch(NavEvent.ChooseAnotherPressed)
    fun retry() = dispatch(NavEvent.RetryPressed)
    fun back() = dispatch(NavEvent.BackPressed)
    fun appBackgrounded() = dispatch(NavEvent.AppBackgrounded)
    fun appForegrounded() = dispatch(NavEvent.AppForegrounded)

    /** Developer mode: enters the same code path as a real sighting (FR-18). */
    fun simulateSighting(markerId: Int) {
        log.log("simulated_sighting", mapOf("marker" to markerId.toString()))
        dispatch(NavEvent.Sighting(markerId))
    }

    /** Detections from one analysed camera frame (used from Step 6 on). */
    fun onFrameDetections(markerIds: List<Int>) {
        val data = loaded ?: return
        markerIds.filter { data.markers.marker(it) == null }.distinct().forEach {
            log.log("unknown_marker", mapOf("marker" to it.toString()))
        }
        filter.onFrame(markerIds, SystemClockSource.nowMs()).forEach { dispatch(NavEvent.Sighting(it)) }
    }

    fun onCameraError(message: String) = dispatch(NavEvent.ErrorOccurred(ErrorKind.CAMERA, message))
    fun onAnalysisRate(rate: Double) { analysisRate = rate; publish() }
    fun logCamera(event: String) = log.log(event)

    fun openSettings() { settingsOpen = true; publish() }
    fun closeSettings() { settingsOpen = false; publish() }
    fun changeSettings(change: (Settings) -> Settings) = settingsStore.update(change)
    fun logFile(): File = log.file

    // ---- Machine plumbing ----

    private fun dispatch(event: NavEvent) {
        val actions = machine.handle(event)
        actions.forEach(::apply)
        publish()
    }

    private fun apply(action: Action) {
        when (action) {
            is Action.Speak -> {
                log.log("speak", mapOf("text" to action.text))
                speaker.speak(action.text, action.interrupt, action.protected)
            }
            Action.StartCamera -> { filter.reset(); cameraWanted = true; cameraPaused = false; updateCamera() }
            Action.StopCamera -> { cameraWanted = false; updateCamera() }
            Action.PauseCamera -> { cameraPaused = true; updateCamera() }
            Action.ResumeCamera -> { cameraPaused = false; updateCamera() }
            Action.RequestPermission -> requestPermission = true
            Action.ShowSettingsButton -> showSettingsButton = true
            is Action.ShowError -> { errorMessage = action.message; errorKind = action.kind }
            is Action.RetryStep -> retryStep(action.kind)
            is Action.Log -> log.log(action.event, action.fields)
        }
    }

    private fun retryStep(kind: ErrorKind) {
        when (kind) {
            ErrorKind.DATA -> loadData()
            ErrorKind.SPEECH -> { speaker.shutdown(); speaker = createSpeaker(); speaker.setRate(settingsStore.settings.value.speechRate.toFloat()) }
            ErrorKind.CAMERA -> { cameraWanted = true; updateCamera(); _cameraGeneration.value++ }
        }
    }

    private fun updateCamera() { _cameraActive.value = cameraWanted && !cameraPaused }

    private fun publish() {
        val data = loaded
        val dest = machine.destinationId
        val destName = dest?.let { data?.map?.node(it)?.speechName } ?: ""
        val place = machine.currentNodeId?.let { data?.map?.node(it)?.speechName } ?: "none yet"
        _ui.update {
            UiState(
                navState = machine.state,
                destinations = data?.map?.destinations?.map { id ->
                    DestinationItem(id, data.map.node(id)?.speechName ?: id)
                } ?: emptyList(),
                destinationName = destName,
                guidanceText = machine.lastInstruction ?: if (destName.isEmpty()) "" else Phrases.destinationChosen(destName),
                currentPlace = place,
                errorMessage = errorMessage,
                errorKind = errorKind,
                showSettingsButton = showSettingsButton,
                requestPermission = requestPermission,
                settingsOpen = settingsOpen,
                analysisRate = analysisRate,
                settings = settingsStore.settings.value,
                markerIds = data?.markers?.markers?.map { it.id }?.sorted() ?: emptyList(),
                logLines = log.tail.value,
            )
        }
    }

    override fun onCleared() {
        speaker.shutdown()
    }
}
