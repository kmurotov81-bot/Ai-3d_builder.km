package com.example.socratictutor

data class SettingsData(
    val level: Level? = null,            // null = automatic (estimated from progress)
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val style: TutorStyle = TutorStyle.GENTLE,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "system",      // "system", "en", "uz"
    val satCalculator: Boolean = true,
    val satTimeTips: Boolean = true,
)
