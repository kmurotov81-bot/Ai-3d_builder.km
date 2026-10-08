package com.example.socratictutor

import org.junit.Assert.*
import org.junit.Test

class TutorSessionTest {
    private fun reply(type: ReplyType, topic: Topic = Topic.LINEAR_EQUATIONS, problem: String? = null) = TutorReply(
        type, topic, Difficulty.MEDIUM, "m", "q", null, false, emptyList(), true, true, listOf("Inverse operations"), problem,
    )

    @Test fun startsIdleAndConfirms() {
        val s = TutorSession()
        assertEquals(SessionStatus.IDLE, s.status)
        s.apply(reply(ReplyType.PROBLEM_CONFIRMATION, problem = "2x+6=14"))
        assertEquals(SessionStatus.CONFIRMING, s.status)
        assertEquals("2x+6=14", s.problem)
        assertFalse(s.canRequestHint)
        s.apply(reply(ReplyType.STEP))
        assertEquals(SessionStatus.TUTORING, s.status)
        assertTrue(s.canRequestHint)
        assertEquals(Topic.LINEAR_EQUATIONS, s.topic)
    }

    @Test fun hintLadderProgressesAndCapsAtThree() {
        val s = TutorSession()
        s.apply(reply(ReplyType.STEP))
        assertEquals(1, s.nextHintLevel); s.registerHint()
        assertEquals(2, s.nextHintLevel); s.registerHint()
        assertEquals(3, s.nextHintLevel); s.registerHint()
        assertEquals(3, s.nextHintLevel); s.registerHint()
        assertEquals(4, s.hintsUsed)
    }

    @Test fun correctAnswerResetsHintLadder() {
        val s = TutorSession()
        s.apply(reply(ReplyType.STEP))
        s.registerHint(); s.registerHint()
        s.apply(reply(ReplyType.CORRECT))
        assertEquals(1, s.nextHintLevel)
        assertEquals(1, s.correctSteps)
        assertEquals(2, s.hintsUsed)
    }

    @Test fun incorrectAndPartialAreCounted() {
        val s = TutorSession()
        s.apply(reply(ReplyType.STEP))
        s.apply(reply(ReplyType.INCORRECT))
        s.apply(reply(ReplyType.INCORRECT))
        s.apply(reply(ReplyType.PARTIAL))
        assertEquals(2, s.mistakes)
        assertEquals(1, s.partialSteps)
        assertEquals(SessionStatus.TUTORING, s.status)
    }

    @Test fun clarificationAndWhyDoNotChangeStatus() {
        val s = TutorSession()
        s.apply(reply(ReplyType.CLARIFICATION, Topic.UNKNOWN))
        assertEquals(SessionStatus.IDLE, s.status)
        s.apply(reply(ReplyType.STEP))
        s.apply(reply(ReplyType.WHY))
        assertEquals(SessionStatus.TUTORING, s.status)
        assertEquals(0, s.hintsUsed)
    }

    @Test fun finalSolvesAndSummarises() {
        var now = 1000L
        val s = TutorSession { now }
        s.noteProblemText("2x + 6 = 14")
        s.apply(reply(ReplyType.STEP))
        s.registerHint()
        s.apply(reply(ReplyType.INCORRECT))
        now = 61_000L
        s.apply(reply(ReplyType.FINAL))
        assertEquals(SessionStatus.SOLVED, s.status)
        val sum = s.summary(listOf("Inverse operations"))
        assertEquals(1, sum.hintsUsed)
        assertEquals(1, sum.mistakes)
        assertEquals(60_000L, sum.durationMs)
        assertEquals("2x + 6 = 14", sum.problem)
        // Later replies cannot reopen a solved problem.
        s.apply(reply(ReplyType.INCORRECT))
        assertEquals(1, s.mistakes)
    }

    @Test fun resetClearsEverything() {
        val s = TutorSession()
        s.apply(reply(ReplyType.STEP)); s.registerHint()
        s.reset()
        assertEquals(SessionStatus.IDLE, s.status)
        assertEquals(0, s.hintsUsed)
        assertNull(s.problem)
        assertEquals(Topic.UNKNOWN, s.topic)
    }
}
