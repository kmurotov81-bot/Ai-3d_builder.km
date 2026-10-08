package com.example.socratictutor

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1F6F6B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBE7),
    onPrimaryContainer = Color(0xFF062F2D),
    secondaryContainer = Color(0xFFFFE9BF),
    onSecondaryContainer = Color(0xFF3A2A00),
    surfaceVariant = Color(0xFFEAF1F0),
    background = Color(0xFFF9FBFA),
    surface = Color(0xFFF9FBFA),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7FD6CF),
    onPrimary = Color(0xFF00201E),
    primaryContainer = Color(0xFF1B4F4B),
    onPrimaryContainer = Color(0xFFCDEBE7),
    secondaryContainer = Color(0xFF5A4300),
    onSecondaryContainer = Color(0xFFFFE9BF),
    surfaceVariant = Color(0xFF26302F),
    background = Color(0xFF101414),
    surface = Color(0xFF101414),
)

@Composable
fun TutorTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
