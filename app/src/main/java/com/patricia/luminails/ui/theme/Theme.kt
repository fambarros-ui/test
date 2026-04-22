package com.patricia.luminails.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFFC56A8B),
    secondary = Color(0xFFE8B7C8),
    background = Color(0xFFFFFBFC),
    surface = Color(0xFFFFFFFF),
    tertiary = Color(0xFFD9B8A0)
)

private val DarkScheme = darkColorScheme(primary = Color(0xFFF0B5C8))

@Composable
fun LumiNailsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        typography = Typography,
        content = content
    )
}
