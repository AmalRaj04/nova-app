package com.genvision.nova.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.genvision.nova.camera.MarkerGeometry
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import com.genvision.nova.NovaViewModel
import com.genvision.nova.UiState
import com.genvision.nova.settings.Settings

/** Standard page: scrolls, so it stays usable at 200 percent font size. First item is the title. */
@Composable
private fun Page(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            // Accessibility requirement 6: a pane title tells TalkBack that a new screen has
            // appeared, so it starts again from the top (the title) instead of keeping its
            // place from the previous screen.
            .semantics { paneTitle = title }
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
fun LoadingScreen() = Page("NOVA") {
    Text("Loading the map.", fontSize = 24.sp)
}

@Composable
fun HomeScreen(ui: UiState, onChoose: (String) -> Unit, onSettings: () -> Unit, onOpenAppSettings: () -> Unit, onVoice: () -> Unit) =
    Page("NOVA") {
        Text("Where do you want to go?", fontSize = 28.sp)
        ui.destinations.forEach { d ->
            BigButton("Go to ${d.name}", onClick = { onChoose(d.id) }, minHeight = 96.dp, textSize = 32)
        }
        if (ui.voiceReady) {
            BigButton("Speak a command", onClick = onVoice, primary = false)
        }
        if (ui.showSettingsButton) {
            Text("NOVA needs the camera to read markers.", fontSize = 22.sp)
            BigButton("Open camera settings", onClick = onOpenAppSettings, primary = false)
        }
        BigButton("Settings", onClick = onSettings, primary = false)
    }

@Composable
fun GuidanceScreen(ui: UiState, vm: NovaViewModel, onExportLog: () -> Unit, onVoice: () -> Unit, onPreviewView: (PreviewView?) -> Unit) =
    Page("Going to ${ui.destinationName}") {
        Text(ui.guidanceText, fontSize = 30.sp, lineHeight = 38.sp)
        BigButton("Repeat", onClick = vm::repeat, minHeight = 160.dp, textSize = 36)
        BigButton("Where am I", onClick = vm::whereAmI, minHeight = 96.dp, primary = false)
        BigButton("Stop", onClick = vm::stop, minHeight = 96.dp, primary = false)
        if (ui.voiceReady) BigButton("Speak a command", onClick = onVoice, minHeight = 96.dp, primary = false)
        if (ui.settings.developerMode) DevPanel(ui, vm::simulateSighting, vm::typedCommand, onExportLog, onPreviewView)
    }

@Composable
fun ArrivalScreen(ui: UiState, onChooseAnother: () -> Unit) = Page("You have arrived") {
    Text("You have arrived at ${ui.destinationName}.", fontSize = 30.sp, lineHeight = 38.sp)
    BigButton("Choose another destination", onClick = onChooseAnother, minHeight = 120.dp)
}

@Composable
fun ErrorScreen(ui: UiState, onRetry: () -> Unit, onBack: () -> Unit) = Page("Something went wrong") {
    Text(ui.errorMessage, fontSize = 26.sp, lineHeight = 34.sp)
    BigButton("Retry", onClick = onRetry, minHeight = 96.dp)
    BigButton("Back", onClick = onBack, primary = false)
}

@Composable
fun SettingsScreen(ui: UiState, vm: NovaViewModel) = Page("Settings") {
    val s = ui.settings
    Stepper(
        label = "Walking step length",
        value = "%.2f metres".format(s.stepLengthM),
        decreaseLabel = "Shorter step",
        increaseLabel = "Longer step",
        onDecrease = { vm.changeSettings { it.withStepLength(it.stepLengthM - Settings.STEP_DELTA) } },
        onIncrease = { vm.changeSettings { it.withStepLength(it.stepLengthM + Settings.STEP_DELTA) } },
    )
    Stepper(
        label = "Speech rate",
        value = "%.1f times normal".format(s.speechRate),
        decreaseLabel = "Slower speech",
        increaseLabel = "Faster speech",
        onDecrease = { vm.changeSettings { it.withSpeechRate(it.speechRate - Settings.RATE_DELTA) } },
        onIncrease = { vm.changeSettings { it.withSpeechRate(it.speechRate + Settings.RATE_DELTA) } },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(
                value = s.developerMode,
                role = Role.Switch,
                onValueChange = { on -> vm.changeSettings { it.copy(developerMode = on) } },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("For the team", fontSize = 24.sp)
        Switch(checked = s.developerMode, onCheckedChange = null)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(
                value = s.voiceCommands,
                enabled = ui.voiceAvailable,
                role = Role.Switch,
                onValueChange = { on -> vm.changeSettings { it.copy(voiceCommands = on) } },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Voice commands", fontSize = 24.sp)
        Switch(checked = s.voiceCommands && ui.voiceAvailable, onCheckedChange = null, enabled = ui.voiceAvailable)
    }
    Text(
        if (ui.voiceAvailable) "When on, a Speak a command button appears, and the Volume up key starts listening " +
            "(so Volume up no longer raises the volume; use Volume down and the phone's volume panel). " +
            "Say go to D13, repeat, where am I, or stop. It works offline."
        else "Voice commands are not available on this phone. They need Android 13 or newer with offline " +
            "speech recognition installed. The buttons always work.",
        fontSize = 18.sp,
    )
    Text("About NOVA", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    Text(ABOUT_TEXT, fontSize = 18.sp)
    BigButton("Back", onClick = vm::closeSettings, primary = false)
}

@Composable
private fun Stepper(
    label: String,
    value: String,
    decreaseLabel: String,
    increaseLabel: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$label: $value", fontSize = 24.sp)
        BigButton(decreaseLabel, onClick = onDecrease, primary = false)
        BigButton(increaseLabel, onClick = onIncrease, primary = false)
    }
}

/** Honest limits, as required by the brief. Do not soften this text. */
private const val ABOUT_TEXT =
    "NOVA gives spoken wayfinding only. It has not been tested in the building. " +
        "Marker positions come from an approximate drawing, distances are estimates, and the " +
        "walking step length has not been measured. NOVA speaks only at printed markers, so you " +
        "can drift off course between two markers without being told. Marker reading can fail in " +
        "glare, low light or at a sharp angle. NOVA does not warn about people, open doors, glass, " +
        "steps or other obstacles. It does not replace a white cane or a guide dog. A sighted " +
        "person must be beside anyone testing it."

/** Developer panel (FR-18, FR-19): simulate sightings, see state and the log. */
@Composable
fun DevPanel(ui: UiState, onSimulate: (Int) -> Unit, onTypedCommand: (String) -> Unit, onExportLog: () -> Unit, onPreviewView: (PreviewView?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Developer panel", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("State: ${ui.navState}. Current place: ${ui.currentPlace}.", fontSize = 18.sp)
        Text("Analysis rate: %.1f frames per second".format(ui.analysisRate), fontSize = 18.sp)
        val seen = ui.overlay?.markers?.joinToString { it.id.toString() } ?: "none"
        Text("Markers in view: $seen", fontSize = 18.sp)
        Box(modifier = Modifier.fillMaxWidth().height(240.dp).clipToBounds()) {
        AndroidView(
            factory = {
                // COMPATIBLE uses a TextureView, which respects Compose layout and clipping.
                PreviewView(it).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                }.also(onPreviewView)
            },
            onRelease = { onPreviewView(null) },
            modifier = Modifier.matchParentSize(),
        )
        // The IDs are also given as text above, so the drawing is hidden from TalkBack.
        MarkerOverlay(ui.overlay, Modifier.clearAndSetSemantics { })
        }
        ui.markerIds.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { id ->
                    BigButton("Marker $id", onClick = { onSimulate(id) }, modifier = Modifier.weight(1f), minHeight = 56.dp, primary = false, textSize = 18)
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        var typed by remember { mutableStateOf("") }
        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            label = { Text("Type a voice command, for example go to D13") },
            modifier = Modifier.fillMaxWidth(),
        )
        BigButton("Run typed command", onClick = { onTypedCommand(typed) }, minHeight = 56.dp, primary = false, textSize = 18)
        BigButton("Export log", onClick = onExportLog, minHeight = 56.dp, primary = false, textSize = 18)
        Text("Last ${ui.logLines.size} log lines", fontSize = 16.sp)
        ui.logLines.reversed().forEach {
            Text(it, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Boxes and IDs around the markers found in the latest frame (developer panel only). */
@Composable
private fun MarkerOverlay(result: com.genvision.nova.camera.FrameResult?, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val boxColor = Color(Palette.GREEN)
    Canvas(modifier = modifier.fillMaxSize()) {
        val r = result ?: return@Canvas
        for (marker in r.markers) {
            val pts = marker.corners.map {
                val (x, y) = MarkerGeometry.toView(it.first, it.second, r.width, r.height, r.rotationDegrees, size.width, size.height)
                Offset(x, y)
            }
            if (pts.size != 4) continue
            for (i in 0 until 4) drawLine(boxColor, pts[i], pts[(i + 1) % 4], strokeWidth = 6f)
            drawText(
                measurer,
                text = "ID ${marker.id}",
                topLeft = pts[0],
                style = TextStyle(color = boxColor, fontSize = 22.sp, background = Color.Black),
            )
        }
    }
}
