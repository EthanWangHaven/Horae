package com.horae.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = AccentBlue,
    background = Color(0xFFF2F3F8),
    surface = Color.White,
    onSurface = Ink,
    onSurfaceVariant = SubText,
    outlineVariant = Hairline,
)

@Composable
fun HoraeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        content = content,
    )
}
