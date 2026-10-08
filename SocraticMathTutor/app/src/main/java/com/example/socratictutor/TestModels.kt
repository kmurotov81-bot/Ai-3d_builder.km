package com.example.socratictutor

import org.json.JSONException
import org.json.JSONObject

data class TestQuestion(
    val question: String,
    val choices: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val commonMistake: String?,
    val topic: Topic,
    val difficulty: Difficulty,
)

object TestParser {
    /** Returns only questions that are structurally valid (4 choices, valid answer index). */
    fun parse(raw: String): List<TestQuestion> {
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        if (s < 0 || e <= s) return emptyList()
        val arr = try {
            JSONObject(raw.substring(s, e + 1)).optJSONArray("questions")
        } catch (ex: JSONException) {
            null
        } ?: return emptyList()
        val out = mutableListOf<TestQuestion>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val q = o.str("question")?.let { LatexRepair.fix(it) } ?: continue
            val choices = o.strings("choices").map { LatexRepair.fix(it) }
            val idx = o.optInt("correctIndex", -1)
            val expl = o.str("explanation")?.let { LatexRepair.fix(it) } ?: continue
            if (choices.size != 4 || idx !in 0..3 || choices.toSet().size != 4) continue
            out += TestQuestion(
                question = q,
                choices = choices,
                correctIndex = idx,
                explanation = expl,
                commonMistake = o.str("commonMistake")?.let { LatexRepair.fix(it) },
                topic = Topic.fromKey(o.str("topic")),
                difficulty = Difficulty.fromKey(o.str("difficulty")),
            )
        }
        return out
    }
}

data class QuestionResult(
    val question: TestQuestion,
    val selected: Int?,
) {
    val isCorrect: Boolean get() = selected == question.correctIndex
}

data class TestResult(
    val results: List<QuestionResult>,
    val secondsUsed: Int,
) {
    val total: Int get() = results.size
    val correct: Int get() = results.count { it.isCorrect }
    val accuracy: Double get() = if (total == 0) 0.0 else correct.toDouble() / total

    /** Per topic: correct / total. */
    val byTopic: Map<Topic, Pair<Int, Int>>
        get() = results.groupBy { it.question.topic }
            .mapValues { (_, v) -> v.count { it.isCorrect } to v.size }

    /** Topics under 60% in this test, weakest first. */
    val weakTopics: List<Topic>
        get() = byTopic.filter { (_, v) -> v.first.toDouble() / v.second < 0.6 }
            .entries.sortedBy { it.value.first.toDouble() / it.value.second }
            .map { it.key }
}
