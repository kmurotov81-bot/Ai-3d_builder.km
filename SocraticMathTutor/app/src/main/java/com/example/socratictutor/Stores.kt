package com.example.socratictutor

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
    enumValues<T>().firstOrNull { it.name == this }

/** Settings persisted in SharedPreferences (device only). */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _data = MutableStateFlow(load())
    val data: StateFlow<SettingsData> = _data.asStateFlow()

    private fun load() = SettingsData(
        level = prefs.getString("level", null).toEnumOrNull<Level>(),
        difficulty = prefs.getString("difficulty", null).toEnumOrNull<Difficulty>() ?: Difficulty.MEDIUM,
        style = prefs.getString("style", null).toEnumOrNull<TutorStyle>() ?: TutorStyle.GENTLE,
        theme = prefs.getString("theme", null).toEnumOrNull<ThemeMode>() ?: ThemeMode.SYSTEM,
        language = prefs.getString("language", "system") ?: "system",
        satCalculator = prefs.getBoolean("satCalculator", true),
        satTimeTips = prefs.getBoolean("satTimeTips", true),
    )

    fun update(f: (SettingsData) -> SettingsData) {
        val n = f(_data.value)
        _data.value = n
        prefs.edit()
            .putString("level", n.level?.name)
            .putString("difficulty", n.difficulty.name)
            .putString("style", n.style.name)
            .putString("theme", n.theme.name)
            .putString("language", n.language)
            .putBoolean("satCalculator", n.satCalculator)
            .putBoolean("satTimeTips", n.satTimeTips)
            .apply()
    }

    companion object {
        /** Read without creating the store (used before the app object exists, for locale). */
        fun language(context: Context): String =
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("language", "system") ?: "system"
    }
}

/** Learning progress persisted on the device only. Contains no images and no account data. */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
    private val _data = MutableStateFlow(ProgressJson.fromJson(prefs.getString("data", null)))
    val data: StateFlow<ProgressData> = _data.asStateFlow()

    private fun today(): Long = LocalDate.now().toEpochDay()

    private fun mutate(f: (ProgressData) -> ProgressData) {
        _data.update(f)
        prefs.edit().putString("data", ProgressJson.toJson(_data.value)).apply()
    }

    fun recordEvaluation(topic: Topic, outcome: Outcome, note: String?) =
        mutate { ProgressEngine.recordEvaluation(it, topic, outcome, note, System.currentTimeMillis(), today()) }

    fun recordSolved(s: SolvedSummary) =
        mutate { ProgressEngine.recordSolved(it, s, System.currentTimeMillis(), today()) }

    fun recordTest(t: TestResult) =
        mutate { ProgressEngine.recordTest(it, t, System.currentTimeMillis(), today()) }

    fun todayEpochDay(): Long = today()

    fun clear() {
        _data.value = ProgressData()
        prefs.edit().clear().apply()
    }
}
