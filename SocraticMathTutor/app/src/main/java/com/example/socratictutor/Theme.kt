package com.example.socratictutor

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Calm = lightColorScheme(
    primary = Color(0xFF2F6F73),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE8E5),
    onPrimaryContainer = Color(0xFF0B2F31),
    secondaryContainer = Color(0xFFFFEBC8),
    onSecondaryContainer = Color(0xFF3A2A00),
    surfaceVariant = Color(0xFFEEF3F2),
    background = Color(0xFFFAFCFB),
    surface = Color(0xFFFAFCFB),
)

@Composable
fun TutorTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Calm, content = content)
