package com.example.socratictutor

import org.junit.Assert.*
import org.junit.Test

class TestModeTest {
    private fun q(topic: Topic, correct: Int = 0) =
        TestQuestion("q", listOf("a", "b", "c", "d"), correct, "why", "typical", topic, Difficulty.MEDIUM)

    @Test fun parserKeepsOnlyValidQuestions() {
        val raw = """{"questions":[
          {"question":"1+1?","choices":["1","2","3","4"],"correctIndex":1,"explanation":"sum","topic":"arithmetic","difficulty":"easy"},
          {"question":"bad index","choices":["1","2","3","4"],"correctIndex":9,"explanation":"x"},
          {"question":"three choices","choices":["1","2","3"],"correctIndex":0,"explanation":"x"},
          {"question":"dup choices","choices":["1","1","3","4"],"correctIndex":0,"explanation":"x"},
          {"question":"no explanation","choices":["1","2","3","4"],"correctIndex":0}]}"""
        val qs = TestParser.parse(raw)
        assertEquals(1, qs.size)
        assertEquals(Topic.ARITHMETIC, qs[0].topic)
        assertEquals(1, qs[0].correctIndex)
        assertTrue(TestParser.parse("garbage").isEmpty())
    }

    @Test fun scoringTreatsUnansweredAsIncorrect() {
        val r = TestResult(
            listOf(
                QuestionResult(q(Topic.RATIOS), 0),
                QuestionResult(q(Topic.RATIOS), 2),
                QuestionResult(q(Topic.GEOMETRY), null),
                QuestionResult(q(Topic.GEOMETRY), 0),
            ),
            secondsUsed = 90,
        )
        assertEquals(2, r.correct)
        assertEquals(4, r.total)
        assertEquals(0.5, r.accuracy, 1e-9)
        assertEquals(1 to 2, r.byTopic[Topic.RATIOS])
        assertEquals(listOf(Topic.RATIOS, Topic.GEOMETRY).toSet(), r.weakTopics.toSet())
    }

    @Test fun noWeakTopicsWhenAllGood() {
        val r = TestResult(listOf(QuestionResult(q(Topic.RATIOS), 0)), 10)
        assertTrue(r.weakTopics.isEmpty())
    }
}
