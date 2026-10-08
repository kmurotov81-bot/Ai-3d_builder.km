package com.example.socratictutor

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Drives both Tutor mode and Practice mode (same Socratic flow, different starting point). */
class TutorViewModel(app: Application) : AndroidViewModel(app) {
    private val socratic = app as SocraticApp
    private val repo = TutorRepository(AiClientFactory.create())
    private val session = TutorSession()
    private val history = mutableListOf<ApiTurn>()
    private var pendingB64: String? = null
    private var nextId = 0L
    private var job: Job? = null
    private var sat = false

    private val _state = MutableStateFlow(TutorUiState())
    val state: StateFlow<TutorUiState> = _state.asStateFlow()

    /** Topic of the finished/active problem, used by "Practice this topic". */
    val currentTopic: Topic get() = session.topic

    private fun str(id: Int): String = getApplication<Application>().getString(id)

    // ---- Student actions -------------------------------------------------------------------

    fun setCheckWork(on: Boolean) = _state.update { it.copy(checkWork = on) }

    fun submit(text: String) {
        val s = _state.value
        if (s.loading != null) return
        val clean = text.trim()
        val image = s.pendingImage
        if (clean.isEmpty() && image == null) return
        if (session.status == SessionStatus.SOLVED) resetConversation()

        val mistake = s.checkWork && image != null
        val isNew = session.status == SessionStatus.IDLE && !mistake
        val tag = when {
            mistake -> "[MISTAKE] "
            isNew -> "[NEW PROBLEM] "
            else -> ""
        }
        if (isNew) session.noteProblemText(clean)
        val loading = when {
            mistake -> LoadingKind.CHECKING_WORK
            isNew && image != null -> LoadingKind.READING_IMAGE
            isNew -> LoadingKind.READING_PROBLEM
            else -> LoadingKind.CHECKING_ANSWER
        }
        val b64 = pendingB64
        pendingB64 = null
        dispatch(
            display = ChatMessage(nextId++, Role.STUDENT, clean, image = image),
            turn = ApiTurn("user", tag + clean.ifBlank { "(see photo)" }, b64),
            loading = loading,
        )
        _state.update { it.copy(checkWork = false) }
    }

    fun confirmProblem() = submit(str(R.string.confirm_reply))

    fun requestHint() {
        if (_state.value.loading != null || !session.canRequestHint) return
        val level = session.nextHintLevel
        dispatch(
            display = ChatMessage(nextId++, Role.STUDENT, str(R.string.im_stuck)),
            turn = ApiTurn("user", "[HINT $level] I'm stuck."),
            loading = LoadingKind.PREPARING_HINT,
            hint = true,
        )
    }

    fun askWhy(step: ChatMessage) {
        if (_state.value.loading != null) return
        val quoted = listOfNotNull(step.text, step.question).joinToString(" ")
        dispatch(
            display = ChatMessage(nextId++, Role.STUDENT, str(R.string.why_did_we_do_that)),
            turn = ApiTurn("user", "[WHY] Explain only the reason for this step: $quoted"),
            loading = LoadingKind.EXPLAINING_WHY,
        )
    }

    fun startPractice(topic: Topic?, difficulty: Difficulty) {
        if (_state.value.loading != null) return
        resetConversation()
        sat = topic == Topic.SAT_MATH
        dispatch(
            display = null,
            turn = ApiTurn("user", "[PRACTICE topic=${topic?.key ?: "auto"} difficulty=${difficulty.key}]"),
            loading = LoadingKind.GENERATING_PROBLEM,
        )
    }

    fun similarProblem() {
        if (_state.value.loading != null) return
        val topic = session.topic
        val original = session.problem.orEmpty()
        resetConversation()
        sat = topic == Topic.SAT_MATH
        dispatch(
            display = null,
            turn = ApiTurn("user", "[SIMILAR] topic=${topic.key}. Original problem: $original"),
            loading = LoadingKind.GENERATING_PROBLEM,
        )
    }

    fun newProblem() {
        job?.cancel()
        resetConversation()
    }

    // ---- Images / graph / errors -----------------------------------------------------------

    fun attachImage(context: Context, uri: Uri) {
        try {
            val bmp = ImageUtils.decodeScaled(context, uri)
            pendingB64 = ImageUtils.toBase64Jpeg(bmp)
            _state.update { it.copy(pendingImage = bmp, error = null) }
        } catch (e: Exception) {
            Log.w(TAG, "Image load failed", e)
            _state.update { it.copy(error = UiError(AiErrorKind.IMAGE_FAILED)) }
        } finally {
            ImageUtils.clearTempPhotos(context)
        }
    }

    fun clearImage() {
        pendingB64 = null
        _state.update { it.copy(pendingImage = null) }
    }

    fun openGraph(expr: List<String>) = _state.update { it.copy(graph = expr, graphOpen = true) }
    fun closeGraph() = _state.update { it.copy(graphOpen = false) }
    fun reportError(kind: AiErrorKind) = _state.update { it.copy(error = UiError(kind)) }
    fun dismissError() = _state.update { it.copy(error = null) }

    // ---- Internals -------------------------------------------------------------------------

    private fun resetConversation() {
        history.clear()
        session.reset()
        pendingB64 = null
        _state.value = TutorUiState()
    }

    private fun dispatch(display: ChatMessage?, turn: ApiTurn, loading: LoadingKind, hint: Boolean = false) {
        AiClientFactory.missingConfig()?.let { name ->
            _state.update { it.copy(error = UiError(AiErrorKind.NOT_CONFIGURED, name)) }
            return
        }
        history += turn
        _state.update {
            it.copy(
                messages = if (display != null) it.messages + display else it.messages,
                pendingImage = null, loading = loading, error = null, solved = null,
            )
        }
        job = viewModelScope.launch {
            try {
                val settings = socratic.settings.data.value
                val progress = socratic.progress.data.value
                val level = settings.level ?: ProgressEngine.estimateLevel(progress, null)
                val system = TutorPrompt.system(
                    PromptContext(settings, level, ProgressEngine.promptSummary(progress, level), sat),
                )
                val result = repo.ask(system, history)
                history += ApiTurn("assistant", result.raw)
                if (hint) session.registerHint()
                handleReply(result.reply)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "AI request failed", e)
                history.removeLastOrNull()
                _state.update {
                    it.copy(
                        messages = if (display != null) it.messages.filter { m -> m.id != display.id } else it.messages,
                        loading = null,
                        error = UiError(AiErrors.classify(e)),
                    )
                }
            }
        }
    }

    private fun handleReply(reply: TutorReply) {
        val before = session.status
        session.apply(reply)
        val outcome = when (reply.type) {
            ReplyType.CORRECT -> Outcome.CORRECT
            ReplyType.PARTIAL -> Outcome.PARTIAL
            ReplyType.INCORRECT -> Outcome.INCORRECT
            else -> null
        }
        if (outcome != null) socratic.progress.recordEvaluation(session.topic, outcome, reply.message)

        var solved: SolvedSummary? = null
        if (session.status == SessionStatus.SOLVED && before != SessionStatus.SOLVED) {
            solved = session.summary(reply.concepts)
            socratic.progress.recordSolved(solved)
        }
        val msg = ChatMessage(
            id = nextId++, role = Role.TUTOR, text = reply.message, type = reply.type,
            question = reply.question, problem = if (reply.type == ReplyType.PRACTICE) reply.problem else null,
            graph = reply.graph, canUseWhy = reply.canUseWhy,
        )
        _state.update {
            it.copy(
                messages = it.messages + msg, loading = null, solved = solved,
                canHint = session.canRequestHint && reply.canUseHint,
            )
        }
    }

    private companion object { const val TAG = "TutorVM" }
}
