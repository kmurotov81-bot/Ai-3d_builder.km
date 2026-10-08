package com.example.socratictutor

import org.json.JSONArray
import org.json.JSONObject

data class TopicStat(
    val correct: Int = 0,
    val partial: Int = 0,
    val incorrect: Int = 0,
    val solved: Int = 0,
    val scoreSum: Double = 0.0,
) {
    val answered: Int get() = correct + partial + incorrect
    val accuracy: Double? get() = if (answered == 0) null else correct.toDouble() / answered
    val avgScore: Double? get() = if (solved == 0) null else scoreSum / solved
}

data class RecentProblem(
    val topic: String, val difficulty: String, val text: String,
    val hints: Int, val mistakes: Int, val score: Double, val at: Long,
)

data class MistakeNote(val topic: String, val note: String, val at: Long)

data class TestRecord(val at: Long, val total: Int, val correct: Int, val seconds: Int)

data class ProgressData(
    val solved: Int = 0,
    val hintsTotal: Int = 0,
    val mistakesTotal: Int = 0,
    val perfectSolves: Int = 0,
    val topics: Map<String, TopicStat> = emptyMap(),
    val recent: List<RecentProblem> = emptyList(),
    val mistakes: List<MistakeNote> = emptyList(),
    val tests: List<TestRecord> = emptyList(),
    val activeDays: List<Long> = emptyList(),
) {
    val isEmpty: Boolean get() = solved == 0 && tests.isEmpty() && topics.isEmpty()
}

enum class Outcome { CORRECT, PARTIAL, INCORRECT }
enum class RecReason { START, WEAK, IMPROVE, KEEP_GOING }
data class Recommendation(val topic: Topic?, val reason: RecReason)
enum class Badge { FIRST_SOLVE, FIVE_SOLVED, TEN_SOLVED, NO_HINTS, STREAK_3, STREAK_7, MASTERED_TOPIC, FIRST_TEST }

/** All progress rules live here as pure functions so they are easy to test. */
object ProgressEngine {
    private const val MAX_RECENT = 20
    private const val MAX_MISTAKES = 15
    private const val MAX_TESTS = 20
    private const val MAX_DAYS = 90

    fun score(hints: Int, mistakes: Int): Double = (1.0 - hints * 0.15 - mistakes * 0.2).coerceIn(0.0, 1.0)

    private fun touchDay(p: ProgressData, day: Long): ProgressData =
        if (day in p.activeDays) p else p.copy(activeDays = (p.activeDays + day).takeLast(MAX_DAYS))

    private fun updateTopic(p: ProgressData, topic: Topic, f: (TopicStat) -> TopicStat): ProgressData {
        if (topic == Topic.UNKNOWN) return p
        val cur = p.topics[topic.key] ?: TopicStat()
        return p.copy(topics = p.topics + (topic.key to f(cur)))
    }

    fun recordEvaluation(p: ProgressData, topic: Topic, outcome: Outcome, note: String?, at: Long, day: Long): ProgressData {
        var r = updateTopic(p, topic) {
            when (outcome) {
                Outcome.CORRECT -> it.copy(correct = it.correct + 1)
                Outcome.PARTIAL -> it.copy(partial = it.partial + 1)
                Outcome.INCORRECT -> it.copy(incorrect = it.incorrect + 1)
            }
        }
        if (outcome == Outcome.INCORRECT) {
            r = r.copy(
                mistakesTotal = r.mistakesTotal + 1,
                mistakes = (r.mistakes + MistakeNote(topic.key, note.orEmpty().take(140), at)).takeLast(MAX_MISTAKES),
            )
        }
        return touchDay(r, day)
    }

    fun recordSolved(p: ProgressData, s: SolvedSummary, at: Long, day: Long): ProgressData {
        val sc = score(s.hintsUsed, s.mistakes)
        var r = updateTopic(p, s.topic) { it.copy(solved = it.solved + 1, scoreSum = it.scoreSum + sc) }
        r = r.copy(
            solved = r.solved + 1,
            hintsTotal = r.hintsTotal + s.hintsUsed,
            perfectSolves = r.perfectSolves + if (s.hintsUsed == 0 && s.mistakes == 0) 1 else 0,
            recent = (r.recent + RecentProblem(
                s.topic.key, s.difficulty.key, s.problem.orEmpty().take(140), s.hintsUsed, s.mistakes, sc, at,
            )).takeLast(MAX_RECENT),
        )
        return touchDay(r, day)
    }

    fun recordTest(p: ProgressData, t: TestResult, at: Long, day: Long): ProgressData {
        var r = p
        t.results.forEach { q ->
            r = updateTopic(r, q.question.topic) {
                if (q.isCorrect) it.copy(correct = it.correct + 1) else it.copy(incorrect = it.incorrect + 1)
            }
        }
        r = r.copy(tests = (r.tests + TestRecord(at, t.total, t.correct, t.secondsUsed)).takeLast(MAX_TESTS))
        return touchDay(r, day)
    }

    /** Correct / (correct + partial + incorrect) over every checked answer; null if none yet. */
    fun accuracy(p: ProgressData): Double? {
        val c = p.topics.values.sumOf { it.correct }
        val n = p.topics.values.sumOf { it.answered }
        return if (n == 0) null else c.toDouble() / n
    }

    fun estimateLevel(p: ProgressData, preferred: Level?): Level {
        val recent = p.recent.takeLast(10)
        if (recent.size < 3) return preferred ?: Level.BEGINNER
        val avg = recent.map { it.score }.average()
        val acc = accuracy(p) ?: 0.0
        return when {
            recent.size >= 8 && avg >= 0.75 && acc >= 0.7 -> Level.ADVANCED
            avg >= 0.5 -> Level.INTERMEDIATE
            else -> Level.BEGINNER
        }
    }

    fun isMastered(s: TopicStat): Boolean =
        s.solved >= 3 && (s.avgScore ?: 0.0) >= 0.8 && (s.accuracy ?: 0.0) >= 0.8

    fun isWeak(s: TopicStat): Boolean =
        (s.answered >= 3 && (s.accuracy ?: 1.0) < 0.6) || (s.solved >= 2 && (s.avgScore ?: 1.0) < 0.5)

    fun masteredTopics(p: ProgressData): List<Topic> =
        p.topics.filter { isMastered(it.value) }.keys.map { Topic.fromKey(it) }.filter { it != Topic.UNKNOWN }

    fun weakTopics(p: ProgressData): List<Topic> =
        p.topics.filter { isWeak(it.value) }
            .entries.sortedBy { it.value.accuracy ?: it.value.avgScore ?: 0.0 }
            .map { Topic.fromKey(it.key) }.filter { it != Topic.UNKNOWN }

    /** Consecutive active days ending today (or yesterday, so the streak isn't lost before practising today). */
    fun streak(p: ProgressData, today: Long): Int {
        val days = p.activeDays.toSet()
        var d = if (today in days) today else today - 1
        var n = 0
        while (d in days) { n++; d-- }
        return n
    }

    fun recommendations(p: ProgressData): List<Recommendation> {
        if (p.isEmpty) return listOf(Recommendation(null, RecReason.START))
        val weak = weakTopics(p).take(3)
        if (weak.isNotEmpty()) return weak.map { Recommendation(it, RecReason.WEAK) }
        val improve = p.topics.entries
            .filter { !isMastered(it.value) && it.value.answered > 0 }
            .sortedBy { it.value.accuracy ?: 1.0 }
            .map { Topic.fromKey(it.key) }.filter { it != Topic.UNKNOWN }.take(2)
        if (improve.isNotEmpty()) return improve.map { Recommendation(it, RecReason.IMPROVE) }
        return listOf(Recommendation(null, RecReason.KEEP_GOING))
    }

    fun badges(p: ProgressData, today: Long): List<Badge> = buildList {
        if (p.solved >= 1) add(Badge.FIRST_SOLVE)
        if (p.solved >= 5) add(Badge.FIVE_SOLVED)
        if (p.solved >= 10) add(Badge.TEN_SOLVED)
        if (p.perfectSolves >= 1) add(Badge.NO_HINTS)
        val st = streak(p, today)
        if (st >= 3) add(Badge.STREAK_3)
        if (st >= 7) add(Badge.STREAK_7)
        if (masteredTopics(p).isNotEmpty()) add(Badge.MASTERED_TOPIC)
        if (p.tests.isNotEmpty()) add(Badge.FIRST_TEST)
    }

    /** Short text for the AI prompt, built only from real stored data. */
    fun promptSummary(p: ProgressData, level: Level): String {
        if (p.isEmpty) return "New student, no history yet. Estimated level: ${level.name.lowercase()}."
        val acc = accuracy(p)?.let { "${(it * 100).toInt()}%" } ?: "n/a"
        val weak = weakTopics(p).joinToString { it.key }.ifBlank { "none identified" }
        val strong = masteredTopics(p).joinToString { it.key }.ifBlank { "none yet" }
        val lastMistakes = p.mistakes.takeLast(3).joinToString("; ") { "${it.topic}: ${it.note}" }.ifBlank { "none" }
        return "Estimated level: ${level.name.lowercase()}. Problems solved: ${p.solved}. Answer accuracy: $acc. " +
            "Weak topics: $weak. Mastered topics: $strong. Recent mistakes: $lastMistakes."
    }
}

object ProgressJson {
    fun toJson(p: ProgressData): String {
        val o = JSONObject()
        o.put("solved", p.solved).put("hintsTotal", p.hintsTotal).put("mistakesTotal", p.mistakesTotal)
        o.put("perfectSolves", p.perfectSolves)
        val topics = JSONObject()
        p.topics.forEach { (k, s) ->
            topics.put(k, JSONObject().put("c", s.correct).put("p", s.partial).put("i", s.incorrect)
                .put("s", s.solved).put("sum", s.scoreSum))
        }
        o.put("topics", topics)
        o.put("recent", JSONArray().also { a ->
            p.recent.forEach { r ->
                a.put(JSONObject().put("t", r.topic).put("d", r.difficulty).put("x", r.text)
                    .put("h", r.hints).put("m", r.mistakes).put("sc", r.score).put("at", r.at))
            }
        })
        o.put("mistakes", JSONArray().also { a ->
            p.mistakes.forEach { m -> a.put(JSONObject().put("t", m.topic).put("n", m.note).put("at", m.at)) }
        })
        o.put("tests", JSONArray().also { a ->
            p.tests.forEach { t ->
                a.put(JSONObject().put("at", t.at).put("n", t.total).put("c", t.correct).put("s", t.seconds))
            }
        })
        o.put("days", JSONArray().also { a -> p.activeDays.forEach { a.put(it) } })
        return o.toString()
    }

    /** Never throws: corrupt data simply means an empty profile. */
    fun fromJson(s: String?): ProgressData = try {
        if (s.isNullOrBlank()) ProgressData() else parse(JSONObject(s))
    } catch (e: Exception) {
        ProgressData()
    }

    private fun parse(o: JSONObject): ProgressData {
        val topics = mutableMapOf<String, TopicStat>()
        o.optJSONObject("topics")?.let { t ->
            t.keys().forEach { k ->
                val x = t.getJSONObject(k)
                topics[k] = TopicStat(x.optInt("c"), x.optInt("p"), x.optInt("i"), x.optInt("s"), x.optDouble("sum", 0.0))
            }
        }
        fun <T> arr(name: String, f: (JSONObject) -> T): List<T> {
            val a = o.optJSONArray(name) ?: return emptyList()
            return (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(f) }
        }
        val days = o.optJSONArray("days")?.let { a -> (0 until a.length()).map { a.getLong(it) } } ?: emptyList()
        return ProgressData(
            solved = o.optInt("solved"),
            hintsTotal = o.optInt("hintsTotal"),
            mistakesTotal = o.optInt("mistakesTotal"),
            perfectSolves = o.optInt("perfectSolves"),
            topics = topics,
            recent = arr("recent") {
                RecentProblem(it.optString("t"), it.optString("d"), it.optString("x"), it.optInt("h"),
                    it.optInt("m"), it.optDouble("sc", 0.0), it.optLong("at"))
            },
            mistakes = arr("mistakes") { MistakeNote(it.optString("t"), it.optString("n"), it.optLong("at")) },
            tests = arr("tests") { TestRecord(it.optLong("at"), it.optInt("n"), it.optInt("c"), it.optInt("s")) },
            activeDays = days,
        )
    }
}
