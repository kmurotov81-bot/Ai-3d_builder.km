package com.example.socratictutor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TutorViewModel : ViewModel() {

    private val client = ClaudeClient()
    private val history = mutableListOf<ApiTurn>()
    private var pendingB64: String? = null
    private var nextId = 0L
    private var job: Job? = null

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private fun newId() = nextId++

    // ---------- image handling ----------

    fun attachImage(context: Context, uri: Uri) {
        val app = context.applicationContext
        viewModelScope.launch {
            try {
                val (bmp, b64) = withContext(Dispatchers.Default) {
                    val b = ImageUtils.decodeScaled(app, uri)
                    b to ImageUtils.toBase64Jpeg(b)
                }
                pendingB64 = b64
                _state.update { it.copy(pendingImage = bmp, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Couldn't read that photo. Please try again.") }
            }
        }
    }

    fun clearImage() {
        pendingB64 = null
        _state.update { it.copy(pendingImage = null) }
    }

    // ---------- student actions ----------

    /** Typed problem, typed answer, or photo (of a problem or of the student's own work). */
    fun submit(text: String) {
        val s = _state.value
        if (s.isLoading) return
        val trimmed = text.trim()
        val img = s.pendingImage
        if (trimmed.isEmpty() && img == null) return

        val apiText = buildString {
            if (history.isEmpty()) append("[NEW PROBLEM] ")
            append(if (trimmed.isEmpty()) "Here is my problem (see the photo)." else trimmed)
        }
        val b64 = pendingB64
        pendingB64 = null
        dispatch(
            ChatMessage(newId(), Role.STUDENT, trimmed, image = img),
            ApiTurn("user", apiText, b64),
        )
    }

    /** "Why did we do that?" explains only the concept behind one specific step. */
    fun askWhy(step: ChatMessage) {
        if (_state.value.isLoading) return
        dispatch(
            ChatMessage(newId(), Role.STUDENT, "Why did we do that?"),
            ApiTurn("user", "[WHY] The step I want explained is: \"${step.text}\""),
        )
    }

    /** "I'm stuck": reveal the current step, then guide to the next. */
    fun imStuck() {
        if (_state.value.isLoading) return
        dispatch(
            ChatMessage(newId(), Role.STUDENT, "I'm stuck. Can you show me?"),
            ApiTurn("user", "[STUCK] I'm stuck on your last question. Please show me how to do this step."),
        )
    }

    fun newProblem() {
        job?.cancel()
        history.clear()
        pendingB64 = null
        _state.value = UiState()
    }

    fun toggleGraph() = _state.update { it.copy(showGraph = !it.showGraph) }
    fun openGraph(expressions: List<String>) =
        _state.update { it.copy(graph = expressions, showGraph = true) }

    fun dismissError() = _state.update { it.copy(error = null) }

    // ---------- plumbing ----------

    private fun dispatch(display: ChatMessage, turn: ApiTurn) {
        if (BuildConfig.ANTHROPIC_API_KEY.isBlank()) {
            _state.update { it.copy(error = "Add ANTHROPIC_API_KEY to local.properties and rebuild.") }
            return
        }
        history += turn
        _state.update {
            it.copy(messages = it.messages + display, pendingImage = null, isLoading = true, error = null)
        }
        job = viewModelScope.launch {
            try {
                val raw = client.send(TutorPrompt.SYSTEM, history)
                val reply = TutorReply.parse(raw)
                history += ApiTurn("assistant", raw)
                val msg = ChatMessage(
                    id = newId(),
                    role = Role.TUTOR,
                    text = reply.message,
                    question = reply.question,
                    graph = reply.graph,
                    kind = reply.kind,
                )
                _state.update {
                    it.copy(
                        messages = it.messages + msg,
                        isLoading = false,
                        graph = if (reply.graph.isNotEmpty()) reply.graph else it.graph,
                        showGraph = it.showGraph || reply.graph.isNotEmpty(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                history.removeLastOrNull() // let the student retry cleanly
                _state.update {
                    it.copy(
                        messages = it.messages - display,
                        isLoading = false,
                        error = e.message ?: "Something went wrong. Please try again.",
                    )
                }
            }
        }
    }
}
