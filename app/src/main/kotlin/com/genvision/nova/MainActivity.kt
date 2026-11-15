package com.genvision.nova

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.camera.view.PreviewView
import com.genvision.nova.camera.CameraSession
import com.genvision.nova.voice.DeepLink
import com.genvision.nova.voice.VoiceListener
import com.genvision.nova.voice.VoiceProblem
import android.view.KeyEvent
import android.content.Intent as AndroidIntent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import com.genvision.nova.camera.ArucoMarkerDetector
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.genvision.nova.core.nav.NavState
import com.genvision.nova.ui.ArrivalScreen
import com.genvision.nova.ui.ErrorScreen
import com.genvision.nova.ui.GuidanceScreen
import com.genvision.nova.ui.HomeScreen
import com.genvision.nova.ui.LoadingScreen
import com.genvision.nova.ui.NovaTheme
import com.genvision.nova.ui.SettingsScreen

/** The only activity. All logic lives in [NovaViewModel]; this file wires Android things to it. */
class MainActivity : ComponentActivity() {
    private val vm: NovaViewModel by viewModels()

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { vm.onPermissionResult(it) }

    private val detector = ArucoMarkerDetector(dictionaryName = { vm.markerDictionary })
    private val camera by lazy {
        CameraSession(this, detector, vm::onFrameResult, vm::onAnalysisRate, vm::onCameraError)
    }
    private var previewView by mutableStateOf<PreviewView?>(null)

    /** A destination from a link or shortcut, waiting until the Home screen is ready. */
    private var pendingDestination by mutableStateOf<String?>(null)

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startVoice() else vm.onVoiceProblem(VoiceProblem.NEEDS_PERMISSION)
        }
    private val voice by lazy { VoiceListener(this, vm::onVoiceHeard, vm::onVoiceProblem) }

    /** Starts listening for one spoken command (button, or the Volume up key). */
    private fun startVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        vm.beforeListening()
        voice.start()
    }

    /** Volume up is the talk key, but only when the person has switched voice commands on. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && vm.ui.value.voiceReady) {
            if (event.repeatCount == 0) startVoice()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onNewIntent(intent: AndroidIntent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readDeepLink(intent)
    }

    private fun readDeepLink(intent: AndroidIntent?) {
        DeepLink.destinationId(intent?.dataString)?.let { pendingDestination = it }
    }

    override fun onDestroy() {
        camera.shutdown()
        voice.cancel()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.hasCameraPermission = ::hasCameraPermission
        // Leaving the app pauses the camera; coming back resumes it (FR-17).
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                voice.cancel()
                vm.appBackgrounded()
            }
            override fun onStart(owner: LifecycleOwner) = vm.appForegrounded()
        })
        readDeepLink(intent)
        setContent { NovaTheme { NovaApp() } }
    }

    @androidx.compose.runtime.Composable
    private fun NovaApp() {
        val ui by vm.ui.collectAsStateWithLifecycle()

        LaunchedEffect(ui.requestPermission) {
            if (ui.requestPermission) {
                vm.permissionRequestHandled()
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        // A link, shortcut or voice assistant asked for a destination: start it once Home is ready.
        LaunchedEffect(ui.navState, pendingDestination) {
            val id = pendingDestination
            if (id != null && ui.navState == NavState.HOME) {
                pendingDestination = null
                if (vm.destinationIsKnown(id)) vm.chooseDestination(id, hasCameraPermission())
            } else if (id != null && ui.navState != NavState.LOADING && ui.navState != NavState.HOME) {
                pendingDestination = null // the person is already doing something else
            }
        }

        // One shortcut per room in map.json, so the phone's assistant or launcher can start guidance.
        LaunchedEffect(ui.destinations) {
            if (ui.destinations.isEmpty()) return@LaunchedEffect
            val shortcuts = ui.destinations.map {
                ShortcutInfoCompat.Builder(this@MainActivity, "go_${it.id}")
                    .setShortLabel("Go to ${it.name}")
                    .setIntent(AndroidIntent(AndroidIntent.ACTION_VIEW, Uri.parse(DeepLink.forDestination(it.id)), this@MainActivity, MainActivity::class.java))
                    .build()
            }
            try { ShortcutManagerCompat.setDynamicShortcuts(this@MainActivity, shortcuts) } catch (_: Exception) { }
        }

        // Messages routed to the accessibility system, so TalkBack reads them in its own turn.
        val view = androidx.compose.ui.platform.LocalView.current
        LaunchedEffect(Unit) {
            vm.announcements.collect {
                @Suppress("DEPRECATION")
                view.announceForAccessibility(it)
            }
        }

        // Start and stop the camera with the machine's wishes; also restart it after Retry.
        val cameraActive by vm.cameraActive.collectAsStateWithLifecycle()
        val generation by vm.cameraGeneration.collectAsStateWithLifecycle()
        val developerMode = ui.settings.developerMode
        LaunchedEffect(cameraActive, generation, developerMode, previewView) {
            if (cameraActive) {
                vm.logCamera("camera_start")
                camera.start(this@MainActivity, if (developerMode) previewView?.surfaceProvider else null)
            } else {
                vm.logCamera("camera_stop")
                camera.stop()
            }
        }

        // Keep the screen on while guiding (FR-17).
        val guiding = ui.navState == NavState.WAITING || ui.navState == NavState.GUIDING
        DisposableEffect(guiding) {
            if (guiding) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }

        BackHandler(enabled = ui.settingsOpen) { vm.closeSettings() }

        when {
            ui.settingsOpen -> SettingsScreen(ui, vm)
            else -> when (ui.navState) {
                NavState.LOADING -> LoadingScreen()
                NavState.HOME, NavState.PERMISSION -> HomeScreen(
                    ui,
                    onChoose = { vm.chooseDestination(it, hasCameraPermission()) },
                    onSettings = vm::openSettings,
                    onOpenAppSettings = ::openAppSettings,
                    onVoice = ::startVoice,
                )
                NavState.WAITING, NavState.GUIDING -> GuidanceScreen(ui, vm, ::exportLog, ::startVoice) { previewView = it }
                NavState.ARRIVED -> ArrivalScreen(ui, vm::chooseAnother)
                NavState.ERROR -> ErrorScreen(ui, vm::retry, vm::back)
            }
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }

    /** Shares the log file through the system share sheet (FR-19). */
    private fun exportLog() {
        val file = vm.logFile()
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(this, "$packageName.logs", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "Export NOVA log"))
    }
}
