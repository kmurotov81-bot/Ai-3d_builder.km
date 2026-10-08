package com.example.socratictutor

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = SettingsStore.language(newBase)
        if (lang == "system") {
            super.attachBaseContext(newBase)
        } else {
            val cfg = Configuration(newBase.resources.configuration)
            cfg.setLocale(Locale(lang))
            super.attachBaseContext(newBase.createConfigurationContext(cfg))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SocraticApp
        setContent {
            val settings by app.settings.data.collectAsState()
            TutorTheme(settings.theme) { AppRoot(app, onLanguageChanged = { recreate() }) }
        }
    }
}

private data class NavItem(val screen: Screen, val label: Int, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem(Screen.HOME, R.string.nav_home, Icons.Filled.Home),
    NavItem(Screen.TUTOR, R.string.nav_tutor, Icons.Filled.Face),
    NavItem(Screen.PRACTICE, R.string.nav_practice, Icons.Filled.PlayArrow),
    NavItem(Screen.PROGRESS, R.string.nav_progress, Icons.Filled.Star),
    NavItem(Screen.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
private fun AppRoot(app: SocraticApp, onLanguageChanged: () -> Unit) {
    val nav: NavViewModel = viewModel()
    val tutor: TutorViewModel = viewModel(key = "tutor")
    val practice: TutorViewModel = viewModel(key = "practice")
    val test: TestViewModel = viewModel()

    val navState by nav.state.collectAsState()
    val settings by app.settings.data.collectAsState()
    val progress by app.progress.data.collectAsState()
    val today = app.progress.todayEpochDay()

    BackHandler(enabled = navState.back() != null) {
        if (navState.inTest) test.reset()
        navState.back()?.let { s -> nav.update { s } }
    }

    if (navState.inTest) {
        Box(Modifier.systemBarsPadding()) { TestScreen(test, onExit = { nav.update { it.closeTest() } }) }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NAV_ITEMS.forEach { item ->
                    val label = stringResource(item.label)
                    NavigationBarItem(
                        selected = navState.screen == item.screen,
                        onClick = { nav.update { it.go(item.screen) } },
                        icon = { Icon(item.icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        val isChat = navState.screen == Screen.TUTOR || navState.screen == Screen.PRACTICE
        val m = Modifier.padding(padding).then(if (isChat) Modifier else Modifier.statusBarsPadding())
        Box(m) {
            when (navState.screen) {
                Screen.HOME -> HomeScreen(
                    tutor = tutor,
                    streak = ProgressEngine.streak(progress, today),
                    onOpenTutor = { nav.update { it.go(Screen.TUTOR) } },
                    onOpenPractice = { nav.update { it.go(Screen.PRACTICE) } },
                    onOpenTest = { nav.update { it.openTest() } },
                )
                Screen.TUTOR -> TutorScreen(tutor) { t -> nav.update { it.practiceOn(t) } }
                Screen.PRACTICE -> PracticeScreen(
                    vm = practice,
                    settings = settings,
                    pendingTopic = navState.practiceTopic,
                    onTopicConsumed = { nav.update { it.consumePracticeTopic() } },
                    onPracticeTopic = { t -> nav.update { it.practiceOn(t) } },
                )
                Screen.PROGRESS -> ProgressScreen(progress, settings, today) { t -> nav.update { it.practiceOn(t) } }
                Screen.SETTINGS -> SettingsScreen(
                    s = settings,
                    onChange = { f -> app.settings.update(f) },
                    onDeleteProgress = { app.progress.clear() },
                    onLanguageChanged = onLanguageChanged,
                )
            }
        }
    }
}
