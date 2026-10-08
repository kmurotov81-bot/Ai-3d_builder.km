package com.example.socratictutor

import org.json.JSONException
import org.json.JSONObject

/** Structured reply the model must produce. The app logic only trusts this validated form. */
data class TutorReply(
    val type: ReplyType,
    val topic: Topic,
    val difficulty: Difficulty,
    val message: String,
    val question: String?,
    val expectedAction: String?,
    val showGraph: Boolean,
    val graph: List<String>,
    val canUseWhy: Boolean,
    val canUseHint: Boolean,
    val concepts: List<String>,
    val problem: String?,
) {
    companion object {
        private val NO_WHY = setOf(
            ReplyType.WHY, ReplyType.CLARIFICATION, ReplyType.PROBLEM_CONFIRMATION, ReplyType.PRACTICE,
        )

        /**
         * Returns null when the text is not a valid reply (no JSON object, malformed JSON,
         * or an empty "message"). Callers decide whether to retry or show an error.
         */
        fun parseOrNull(raw: String): TutorReply? {
            val s = raw.indexOf('{')
            val e = raw.lastIndexOf('}')
            if (s < 0 || e <= s) return null
            val o = try {
                JSONObject(raw.substring(s, e + 1))
            } catch (ex: JSONException) {
                return null
            }
            val message = LatexRepair.fix(o.str("message") ?: return null)
            val type = ReplyType.fromKey(o.str("type") ?: o.str("kind")) ?: ReplyType.STEP
            val question = o.str("question")?.let { LatexRepair.fix(it) }
            val graph = o.strings("graph").map { LatexRepair.fix(it) }
            return TutorReply(
                type = type,
                topic = Topic.fromKey(o.str("topic")),
                difficulty = Difficulty.fromKey(o.str("difficulty")),
                message = message,
                question = question,
                expectedAction = o.str("expectedAction"),
                showGraph = o.optBoolean("showGraph", graph.isNotEmpty()) && graph.isNotEmpty(),
                graph = graph,
                canUseWhy = o.optBoolean("canUseWhy", type !in NO_WHY),
                canUseHint = o.optBoolean("canUseHint", question != null && type != ReplyType.FINAL),
                concepts = o.strings("concepts").map { LatexRepair.fix(it) },
                problem = o.str("problem")?.let { LatexRepair.fix(it) },
            )
        }
    }
}

internal fun JSONObject.str(name: String): String? =
    if (isNull(name)) null else optString(name).trim().ifBlank { null }

internal fun JSONObject.strings(name: String): List<String> {
    val a = optJSONArray(name) ?: return emptyList()
    return (0 until a.length()).mapNotNull { i ->
        if (a.isNull(i)) null else a.optString(i).trim().ifBlank { null }
    }
}

/**
 * Models sometimes write LaTeX with a single backslash inside JSON ("\frac" becomes a form feed,
 * "\text" a tab, "\beta" a backspace, "\right" a carriage return). Put those backslashes back.
 */
object LatexRepair {
    fun fix(s: String): String =
        s.replace("\u000c", "\\f").replace("\t", "\\t").replace("\b", "\\b").replace("\r", "\\r")
}
