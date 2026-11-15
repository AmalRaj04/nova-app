package com.genvision.nova.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * One fixed high-contrast theme: white on black (21:1) and black on yellow (about 14:1).
 * No dynamic colour. Meaning is never carried by colour alone: every control has a text label.
 */
private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val Yellow = Color(0xFFFFD60A)
private val DarkGrey = Color(0xFF1C1C1C)

private val NovaColors = darkColorScheme(
    primary = Yellow, onPrimary = Black,
    secondary = DarkGrey, onSecondary = White,
    background = Black, onBackground = White,
    surface = Black, onSurface = White,
    error = Color(0xFFFF8A80), onError = Black,
)

@Composable
fun NovaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NovaColors) {
        // Without a Surface, text would use the default dark content colour on the black window.
        Surface(modifier = Modifier.fillMaxSize(), color = Black, contentColor = White, content = content)
    }
}

/** Full-width button, at least 72 dp tall (taller at large font sizes). */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 72.dp,
    primary: Boolean = true,
    textSize: Int = 24,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight),
        contentPadding = PaddingValues(16.dp),
        colors = if (primary) ButtonDefaults.buttonColors()
        else ButtonDefaults.buttonColors(containerColor = DarkGrey, contentColor = White),
        border = if (primary) null else BorderStroke(2.dp, White),
    ) {
        Text(text, fontSize = textSize.sp, textAlign = TextAlign.Center)
    }
}
