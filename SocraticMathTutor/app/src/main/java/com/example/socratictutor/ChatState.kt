package com.example.socratictutor

import android.graphics.Bitmap

enum class Role { STUDENT, TUTOR }

/** What the app is actually doing right now; the loading text mirrors this (no fake delays). */
enum class LoadingKind {
    READING_IMAGE, READING_PROBLEM, CHECKING_ANSWER, PREPARING_HINT,
    EXPLAINING_WHY, CHECKING_WORK, GENERATING_PROBLEM,
}

data class UiError(val kind: AiErrorKind, val detail: String? = null)

data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String,
    val type: ReplyType? = null,
    val question: String? = null,
    val problem: String? = null,
    val image: Bitmap? = null,
    val graph: List<String> = emptyList(),
    val canUseWhy: Boolean = false,
)

data class TutorUiState(
    val messages: List<ChatMessage> = emptyList(),
    val pendingImage: Bitmap? = null,
    val checkWork: Boolean = false,
    val loading: LoadingKind? = null,
    val error: UiError? = null,
    val graph: List<String> = emptyList(),
    val graphOpen: Boolean = false,
    val solved: SolvedSummary? = null,
    val canHint: Boolean = false,
)
