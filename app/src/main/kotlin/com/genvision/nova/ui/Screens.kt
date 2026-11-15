package com.genvision.nova.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.role
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
fun HomeScreen(ui: UiState, onChoose: (String) -> Unit, onSettings: () -> Unit, onOpenAppSettings: () -> Unit) =
    Page("NOVA") {
        Text("Where do you want to go?", fontSize = 28.sp)
        ui.destinations.forEach { d ->
            BigButton("Go to ${d.name}", onClick = { onChoose(d.id) }, minHeight = 96.dp, textSize = 32)
        }
        if (ui.showSettingsButton) {
            Text("NOVA needs the camera to read markers.", fontSize = 22.sp)
            BigButton("Open camera settings", onClick = onOpenAppSettings, primary = false)
        }
        BigButton("Settings", onClick = onSettings, primary = false)
    }

@Composable
fun GuidanceScreen(ui: UiState, vm: NovaViewModel, onExportLog: () -> Unit) =
    Page("Going to ${ui.destinationName}") {
        Text(ui.guidanceText, fontSize = 30.sp, lineHeight = 38.sp)
        BigButton("Repeat", onClick = vm::repeat, minHeight = 160.dp, textSize = 36)
        BigButton("Where am I", onClick = vm::whereAmI, minHeight = 96.dp, primary = false)
        BigButton("Stop", onClick = vm::stop, minHeight = 96.dp, primary = false)
        if (ui.settings.developerMode) DevPanel(ui, vm::simulateSighting, onExportLog)
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
fun DevPanel(ui: UiState, onSimulate: (Int) -> Unit, onExportLog: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Developer panel", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("State: ${ui.navState}. Current place: ${ui.currentPlace}.", fontSize = 18.sp)
        Text("Camera preview and marker boxes arrive with the camera steps.", fontSize = 16.sp)
        ui.markerIds.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { id ->
                    BigButton("Marker $id", onClick = { onSimulate(id) }, modifier = Modifier.weight(1f), minHeight = 56.dp, primary = false, textSize = 18)
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        BigButton("Export log", onClick = onExportLog, minHeight = 56.dp, primary = false, textSize = 18)
        Text("Last ${ui.logLines.size} log lines", fontSize = 16.sp)
        ui.logLines.reversed().forEach {
            Text(it, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(24.dp))
    }
}
