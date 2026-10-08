package com.example.socratictutor

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TestPhase { SETUP, LOADING, RUNNING, REVIEW }

data class TestUiState(
    val phase: TestPhase = TestPhase.SETUP,
    val topic: Topic? = null,             // null = mixed
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val count: Int = 5,
    val questions: List<TestQuestion> = emptyList(),
    val answers: List<Int?> = emptyList(),
    val index: Int = 0,
    val secondsLeft: Int = 0,
    val secondsTotal: Int = 0,
    val result: TestResult? = null,
    val error: UiError? = null,
)

/** Test mode: no hints, no step help, no answers revealed until the test is submitted. */
class TestViewModel(app: Application) : AndroidViewModel(app) {
    private val socratic = app as SocraticApp
    private val repo = TutorRepository(AiClientFactory.create())
    private var timer: Job? = null
    private var gen: Job? = null

    private val _state = MutableStateFlow(TestUiState())
    val state: StateFlow<TestUiState> = _state.asStateFlow()

    fun setTopic(t: Topic?) = _state.update { it.copy(topic = t) }
    fun setDifficulty(d: Difficulty) = _state.update { it.copy(difficulty = d) }
    fun setCount(n: Int) = _state.update { it.copy(count = n) }
    fun dismissError() = _state.update { it.copy(error = null) }

    fun start() {
        if (_state.value.phase == TestPhase.LOADING) return
        AiClientFactory.missingConfig()?.let { name ->
            _state.update { it.copy(error = UiError(AiErrorKind.NOT_CONFIGURED, name)) }
            return
        }
        val s = _state.value
        _state.update { it.copy(phase = TestPhase.LOADING, error = null) }
        gen = viewModelScope.launch {
            try {
                val settings = socratic.settings.data.value
                val progress = socratic.progress.data.value
                val level = settings.level ?: ProgressEngine.estimateLevel(progress, null)
                val qs = repo.generateTest(
                    TestPrompt.system(settings, s.topic == Topic.SAT_MATH),
                    TestPrompt.request(s.topic, s.difficulty, s.count, ProgressEngine.promptSummary(progress, level)),
                    s.count,
                )
                val total = qs.size * SECONDS_PER_QUESTION
                _state.update {
                    it.copy(
                        phase = TestPhase.RUNNING, questions = qs, answers = List(qs.size) { null },
                        index = 0, secondsLeft = total, secondsTotal = total, result = null,
                    )
                }
                startTimer()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Test generation failed", e)
                _state.update { it.copy(phase = TestPhase.SETUP, error = UiError(AiErrors.classify(e))) }
            }
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = viewModelScope.launch {
            while (_state.value.secondsLeft > 0 && _state.value.phase == TestPhase.RUNNING) {
                delay(1000)
                _state.update { it.copy(secondsLeft = (it.secondsLeft - 1).coerceAtLeast(0)) }
            }
            if (_state.value.phase == TestPhase.RUNNING) finish()
        }
    }

    fun select(choice: Int) = _state.update { st ->
        if (st.phase != TestPhase.RUNNING) st
        else st.copy(answers = st.answers.mapIndexed { i, a -> if (i == st.index) choice else a })
    }

    fun next() {
        val st = _state.value
        if (st.phase != TestPhase.RUNNING) return
        if (st.index >= st.questions.lastIndex) finish() else _state.update { it.copy(index = it.index + 1) }
    }

    fun previous() = _state.update { if (it.index > 0) it.copy(index = it.index - 1) else it }

    fun finish() {
        val st = _state.value
        if (st.phase != TestPhase.RUNNING) return
        timer?.cancel()
        val used = st.secondsTotal - st.secondsLeft
        val result = TestResult(st.questions.mapIndexed { i, q -> QuestionResult(q, st.answers.getOrNull(i)) }, used)
        socratic.progress.recordTest(result)
        _state.update { it.copy(phase = TestPhase.REVIEW, result = result) }
    }

    fun reset() {
        timer?.cancel(); gen?.cancel()
        _state.value = TestUiState()
    }

    override fun onCleared() {
        timer?.cancel(); gen?.cancel()
    }

    private companion object {
        const val TAG = "TestVM"
        const val SECONDS_PER_QUESTION = 90
    }
}
