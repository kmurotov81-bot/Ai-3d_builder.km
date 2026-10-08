package com.example.socratictutor

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Screen { HOME, TUTOR, PRACTICE, PROGRESS, SETTINGS }

/** Pure navigation state so the rules can be unit tested. */
data class NavState(
    val screen: Screen = Screen.HOME,
    val inTest: Boolean = false,
    val practiceTopic: Topic? = null,
) {
    fun go(s: Screen) = copy(screen = s, inTest = false)
    fun openTest() = copy(inTest = true)
    fun closeTest() = copy(inTest = false)
    fun practiceOn(t: Topic) = copy(screen = Screen.PRACTICE, inTest = false, practiceTopic = t)
    fun consumePracticeTopic() = copy(practiceTopic = null)
    /** Returns the state after Back is pressed, or null if Back should leave the app. */
    fun back(): NavState? = when {
        inTest -> closeTest()
        screen != Screen.HOME -> go(Screen.HOME)
        else -> null
    }
}

class NavViewModel : ViewModel() {
    private val _state = MutableStateFlow(NavState())
    val state: StateFlow<NavState> = _state.asStateFlow()
    fun update(f: (NavState) -> NavState) { _state.value = f(_state.value) }
}
