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

    override fun onDestroy() {
        camera.shutdown()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Leaving the app pauses the camera; coming back resumes it (FR-17).
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) = vm.appBackgrounded()
            override fun onStart(owner: LifecycleOwner) = vm.appForegrounded()
        })
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
                )
                NavState.WAITING, NavState.GUIDING -> GuidanceScreen(ui, vm, ::exportLog) { previewView = it }
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
