package com.example.socratictutor

import org.junit.Assert.*
import org.junit.Test

class TutorReplyTest {
    private val valid = """{"type":"step","topic":"linear_equations","difficulty":"medium",
        "message":"Let's remove the 6 first.","question":"What cancels +6?","expectedAction":"subtract_6",
        "showGraph":false,"graph":[],"canUseWhy":true,"canUseHint":true}"""

    @Test fun parsesValidReply() {
        val r = TutorReply.parseOrNull(valid)!!
        assertEquals(ReplyType.STEP, r.type)
        assertEquals(Topic.LINEAR_EQUATIONS, r.topic)
        assertEquals(Difficulty.MEDIUM, r.difficulty)
        assertEquals("What cancels +6?", r.question)
        assertEquals("subtract_6", r.expectedAction)
        assertTrue(r.canUseHint)
    }

    @Test fun toleratesTextAroundJson() {
        assertNotNull(TutorReply.parseOrNull("Sure!\n```json\n$valid\n```"))
    }

    @Test fun invalidJsonIsRejected() {
        assertNull(TutorReply.parseOrNull("not json at all"))
        assertNull(TutorReply.parseOrNull("{\"type\":\"step\", \"message\": "))
        assertNull(TutorReply.parseOrNull(""))
    }

    @Test fun emptyMessageIsRejected() {
        assertNull(TutorReply.parseOrNull("""{"type":"step","message":"  "}"""))
        assertNull(TutorReply.parseOrNull("""{"type":"step"}"""))
    }

    @Test fun nullQuestionBecomesNull() {
        val r = TutorReply.parseOrNull("""{"type":"final","message":"Done","question":null,"concepts":["Inverse operations"]}""")!!
        assertNull(r.question)
        assertEquals(listOf("Inverse operations"), r.concepts)
        assertFalse(r.canUseHint)
    }

    @Test fun unknownTypeFallsBackToStepAndUnclearMapsToClarification() {
        assertEquals(ReplyType.STEP, TutorReply.parseOrNull("""{"type":"banana","message":"x"}""")!!.type)
        assertEquals(ReplyType.CLARIFICATION, TutorReply.parseOrNull("""{"type":"unclear","message":"x"}""")!!.type)
    }

    @Test fun graphOnlyShownWhenExpressionsExist() {
        val a = TutorReply.parseOrNull("""{"type":"step","message":"m","showGraph":true,"graph":[]}""")!!
        assertFalse(a.showGraph)
        val b = TutorReply.parseOrNull("""{"type":"step","message":"m","graph":["y=2x"]}""")!!
        assertTrue(b.showGraph)
    }

    @Test fun repairsLatexBackslashesEatenByJsonEscapes() {
        // "\frac" written with a single backslash is parsed by JSON as form-feed + "rac".
        val r = TutorReply.parseOrNull("{\"type\":\"step\",\"message\":\"\$\\frac{x}{2}\$\"}")!!
        assertEquals("\$\\frac{x}{2}\$", r.message)
    }

    @Test fun topicAliases() {
        assertEquals(Topic.LINEAR_EQUATIONS, Topic.fromKey("linear_equation"))
        assertEquals(Topic.SYSTEMS, Topic.fromKey("Systems of Equations"))
        assertEquals(Topic.UNKNOWN, Topic.fromKey("basket weaving"))
        assertEquals(Topic.UNKNOWN, Topic.fromKey(null))
    }
}
