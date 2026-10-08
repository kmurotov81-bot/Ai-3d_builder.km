package com.example.socratictutor

import org.junit.Assert.*
import org.junit.Test

class ProgressEngineTest {
    private val day = 20_000L
    private fun solved(topic: Topic, hints: Int = 0, mistakes: Int = 0) =
        SolvedSummary(topic, Difficulty.MEDIUM, hints, mistakes, emptyList(), "p", 0)

    @Test fun emptyProgressHasNoAccuracy() {
        val p = ProgressData()
        assertNull(ProgressEngine.accuracy(p))
        assertTrue(p.isEmpty)
        assertEquals(RecReason.START, ProgressEngine.recommendations(p).single().reason)
    }

    @Test fun accuracyFromEvaluations() {
        var p = ProgressData()
        p = ProgressEngine.recordEvaluation(p, Topic.QUADRATICS, Outcome.CORRECT, null, 1, day)
        p = ProgressEngine.recordEvaluation(p, Topic.QUADRATICS, Outcome.CORRECT, null, 1, day)
        p = ProgressEngine.recordEvaluation(p, Topic.QUADRATICS, Outcome.INCORRECT, "forgot sign", 1, day)
        p = ProgressEngine.recordEvaluation(p, Topic.QUADRATICS, Outcome.PARTIAL, null, 1, day)
        assertEquals(0.5, ProgressEngine.accuracy(p)!!, 1e-9)
        assertEquals(1, p.mistakesTotal)
        assertEquals("forgot sign", p.mistakes.single().note)
    }

    @Test fun unknownTopicIsNotTracked() {
        val p = ProgressEngine.recordEvaluation(ProgressData(), Topic.UNKNOWN, Outcome.CORRECT, null, 1, day)
        assertTrue(p.topics.isEmpty())
    }

    @Test fun solvedUpdatesTotalsAndScore() {
        val p = ProgressEngine.recordSolved(ProgressData(), solved(Topic.CALCULUS, hints = 2, mistakes = 1), 5, day)
        assertEquals(1, p.solved)
        assertEquals(2, p.hintsTotal)
        assertEquals(0, p.perfectSolves)
        assertEquals(0.5, p.recent.single().score, 1e-9)
        val perfect = ProgressEngine.recordSolved(p, solved(Topic.CALCULUS), 6, day)
        assertEquals(1, perfect.perfectSolves)
    }

    @Test fun scoreIsClamped() {
        assertEquals(0.0, ProgressEngine.score(10, 10), 1e-9)
        assertEquals(1.0, ProgressEngine.score(0, 0), 1e-9)
    }

    @Test fun levelEvolves() {
        assertEquals(Level.BEGINNER, ProgressEngine.estimateLevel(ProgressData(), null))
        assertEquals(Level.ADVANCED, ProgressEngine.estimateLevel(ProgressData(), Level.ADVANCED))
        var p = ProgressData()
        repeat(10) {
            p = ProgressEngine.recordEvaluation(p, Topic.RATIOS, Outcome.CORRECT, null, 1, day)
            p = ProgressEngine.recordSolved(p, solved(Topic.RATIOS), it.toLong(), day)
        }
        assertEquals(Level.ADVANCED, ProgressEngine.estimateLevel(p, null))
        var weak = ProgressData()
        repeat(5) { weak = ProgressEngine.recordSolved(weak, solved(Topic.RATIOS, hints = 4, mistakes = 3), it.toLong(), day) }
        assertEquals(Level.BEGINNER, ProgressEngine.estimateLevel(weak, null))
    }

    @Test fun weakAndMasteredTopics() {
        var p = ProgressData()
        repeat(3) { p = ProgressEngine.recordEvaluation(p, Topic.FRACTIONS, Outcome.INCORRECT, "x", 1, day) }
        repeat(3) {
            p = ProgressEngine.recordEvaluation(p, Topic.GEOMETRY, Outcome.CORRECT, null, 1, day)
            p = ProgressEngine.recordSolved(p, solved(Topic.GEOMETRY), 1, day)
        }
        assertEquals(listOf(Topic.FRACTIONS), ProgressEngine.weakTopics(p))
        assertEquals(listOf(Topic.GEOMETRY), ProgressEngine.masteredTopics(p))
        val rec = ProgressEngine.recommendations(p).first()
        assertEquals(Topic.FRACTIONS, rec.topic)
        assertEquals(RecReason.WEAK, rec.reason)
    }

    @Test fun streakCountsConsecutiveDays() {
        var p = ProgressData()
        listOf(day - 2, day - 1, day).forEach { p = ProgressEngine.recordEvaluation(p, Topic.RATIOS, Outcome.CORRECT, null, 1, it) }
        assertEquals(3, ProgressEngine.streak(p, day))
        assertEquals(3, ProgressEngine.streak(p, day + 1))   // not yet practised today: streak survives
        assertEquals(0, ProgressEngine.streak(p, day + 2))
        assertTrue(Badge.STREAK_3 in ProgressEngine.badges(p, day))
    }

    @Test fun testsFeedTopicStatsAndHistory() {
        fun q(t: Topic) = TestQuestion("q", listOf("a", "b", "c", "d"), 0, "e", null, t, Difficulty.EASY)
        val result = TestResult(listOf(QuestionResult(q(Topic.RATIOS), 0), QuestionResult(q(Topic.RATIOS), 1)), 120)
        val p = ProgressEngine.recordTest(ProgressData(), result, 1, day)
        assertEquals(1, p.topics["ratios"]!!.correct)
        assertEquals(1, p.topics["ratios"]!!.incorrect)
        assertEquals(1, p.tests.single().correct)
    }

    @Test fun jsonRoundTripAndCorruption() {
        var p = ProgressEngine.recordSolved(ProgressData(), solved(Topic.CALCULUS, 1, 1), 5, day)
        p = ProgressEngine.recordEvaluation(p, Topic.CALCULUS, Outcome.INCORRECT, "chain rule", 6, day)
        val back = ProgressJson.fromJson(ProgressJson.toJson(p))
        assertEquals(p, back)
        assertEquals(ProgressData(), ProgressJson.fromJson("{{{ broken"))
        assertEquals(ProgressData(), ProgressJson.fromJson(null))
    }

    @Test fun recentListIsBounded() {
        var p = ProgressData()
        repeat(40) { p = ProgressEngine.recordSolved(p, solved(Topic.RATIOS), it.toLong(), day) }
        assertEquals(20, p.recent.size)
        assertEquals(40, p.solved)
    }
}
