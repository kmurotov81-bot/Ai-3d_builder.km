package com.example.socratictutor

import android.graphics.Bitmap
import org.json.JSONException
import org.json.JSONObject

enum class Role { STUDENT, TUTOR }
enum class Kind { STEP, WHY, CLARIFY, FINAL }

data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String,
    val question: String? = null,
    val image: Bitmap? = null,
    val graph: List<String> = emptyList(),
    val kind: Kind = Kind.STEP,
)

data class UiState(
    val messages: List<ChatMessage> = emptyList(),
    val pendingImage: Bitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val graph: List<String> = emptyList(),
    val showGraph: Boolean = false,
)

/** One turn sent to the Claude API. */
data class ApiTurn(val role: String, val text: String, val imageB64: String? = null)

/** Structured reply we ask the model to produce. */
data class TutorReply(
    val message: String,
    val question: String?,
    val graph: List<String>,
    val kind: Kind,
) {
    companion object {
        fun parse(raw: String): TutorReply {
            val s = raw.indexOf('{')
            val e = raw.lastIndexOf('}')
            if (s < 0 || e <= s) return TutorReply(raw.trim(), null, emptyList(), Kind.STEP)
            return try {
                val o = JSONObject(raw.substring(s, e + 1))
                val g = o.optJSONArray("graph")
                val graph = List(g?.length() ?: 0) { g!!.getString(it) }
                val kind = when (o.optString("kind", "step").lowercase()) {
                    "why" -> Kind.WHY
                    "clarify" -> Kind.CLARIFY
                    "final" -> Kind.FINAL
                    else -> Kind.STEP
                }
                TutorReply(
                    message = o.optString("message", "").trim(),
                    question = if (o.isNull("question")) null else o.optString("question").trim().ifBlank { null },
                    graph = graph,
                    kind = kind,
                )
            } catch (ex: JSONException) {
                TutorReply(raw.trim(), null, emptyList(), Kind.STEP)
            }
        }
    }
}
