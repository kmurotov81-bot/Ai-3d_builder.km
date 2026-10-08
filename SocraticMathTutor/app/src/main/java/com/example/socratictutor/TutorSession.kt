package com.example.socratictutor

enum class SessionStatus { IDLE, CONFIRMING, TUTORING, SOLVED }

data class SolvedSummary(
    val topic: Topic,
    val difficulty: Difficulty,
    val hintsUsed: Int,
    val mistakes: Int,
    val concepts: List<String>,
    val problem: String?,
    val durationMs: Long,
)

/** Pure tutoring state machine (no Android, no network) so it can be unit tested. */
class TutorSession(private val clock: () -> Long = { System.currentTimeMillis() }) {
    var status: SessionStatus = SessionStatus.IDLE; private set
    var topic: Topic = Topic.UNKNOWN; private set
    var difficulty: Difficulty = Difficulty.MEDIUM; private set
    var problem: String? = null; private set
    var hintLevel: Int = 0; private set
    var hintsUsed: Int = 0; private set
    var mistakes: Int = 0; private set
    var correctSteps: Int = 0; private set
    var partialSteps: Int = 0; private set
    private var startedAt: Long = 0L

    /** The hint level the next "I'm stuck" tap should produce (1..3, capped). */
    val nextHintLevel: Int get() = minOf(hintLevel + 1, MAX_HINT)

    val canRequestHint: Boolean get() = status == SessionStatus.TUTORING

    /** Remember what the student typed as the problem, until the AI restates it. */
    fun noteProblemText(text: String) {
        if (status == SessionStatus.IDLE && problem == null && text.isNotBlank()) problem = text.trim().take(160)
    }

    /** Call only after the hint reply arrived successfully. */
    fun registerHint() {
        hintLevel = nextHintLevel
        hintsUsed++
    }

    fun apply(reply: TutorReply) {
        if (status == SessionStatus.SOLVED) return
        if (startedAt == 0L) startedAt = clock()
        if (reply.topic != Topic.UNKNOWN) topic = reply.topic
        difficulty = reply.difficulty
        reply.problem?.let { problem = it }
        when (reply.type) {
            ReplyType.PROBLEM_CONFIRMATION -> if (status == SessionStatus.IDLE) status = SessionStatus.CONFIRMING
            ReplyType.STEP, ReplyType.PRACTICE -> { status = SessionStatus.TUTORING; hintLevel = 0 }
            ReplyType.GRAPH, ReplyType.MISTAKE_ANALYSIS -> status = SessionStatus.TUTORING
            ReplyType.CORRECT -> { correctSteps++; hintLevel = 0; status = SessionStatus.TUTORING }
            ReplyType.PARTIAL -> { partialSteps++; status = SessionStatus.TUTORING }
            ReplyType.INCORRECT -> { mistakes++; status = SessionStatus.TUTORING }
            ReplyType.FINAL -> status = SessionStatus.SOLVED
            ReplyType.HINT, ReplyType.WHY, ReplyType.CLARIFICATION -> Unit
        }
    }

    fun summary(concepts: List<String>): SolvedSummary = SolvedSummary(
        topic = topic,
        difficulty = difficulty,
        hintsUsed = hintsUsed,
        mistakes = mistakes,
        concepts = concepts,
        problem = problem,
        durationMs = if (startedAt == 0L) 0L else clock() - startedAt,
    )

    fun reset() {
        status = SessionStatus.IDLE
        topic = Topic.UNKNOWN
        difficulty = Difficulty.MEDIUM
        problem = null
        hintLevel = 0; hintsUsed = 0; mistakes = 0; correctSteps = 0; partialSteps = 0
        startedAt = 0L
    }

    companion object { const val MAX_HINT = 3 }
}
