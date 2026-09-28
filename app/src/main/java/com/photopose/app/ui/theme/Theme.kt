package com.photopose.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentNeonGreen,
    secondary = AccentCyan,
    tertiary = AccentAmber,
    background = Black,
    surface = DarkSurface,
    onPrimary = Black,
    onSecondary = Black,
    onTertiary = Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun PhotoPoseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
