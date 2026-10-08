package com.example.socratictutor

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private class FakeClient(private val answers: List<Any>) : AiClient {
    var calls = 0
    override suspend fun send(system: String, history: List<ApiTurn>, maxTokens: Int): String {
        val a = answers[minOf(calls++, answers.lastIndex)]
        if (a is Throwable) throw a
        return a as String
    }
}

class RepositoryAndErrorsTest {
    private val good = """{"type":"step","message":"Hi","question":"Q?"}"""

    @Test fun returnsParsedReplyAndRawText() = runBlocking {
        val r = TutorRepository(FakeClient(listOf(good))).ask("sys", emptyList())
        assertEquals("Hi", r.reply.message)
        assertEquals(good, r.raw)
    }

    @Test fun retriesOnceOnInvalidJson() = runBlocking {
        val c = FakeClient(listOf("oops", good))
        val r = TutorRepository(c).ask("sys", emptyList())
        assertEquals(2, c.calls)
        assertEquals(ReplyType.STEP, r.reply.type)
    }

    @Test fun failsWithInvalidResponseAfterTwoBadReplies() = runBlocking {
        val c = FakeClient(listOf("oops", "{broken"))
        try {
            TutorRepository(c).ask("sys", emptyList())
            fail("expected exception")
        } catch (e: AiException) {
            assertEquals(AiErrorKind.INVALID_RESPONSE, e.kind)
            assertEquals(2, c.calls)
        }
    }

    @Test fun networkErrorsPropagateWithoutRetry() = runBlocking {
        val c = FakeClient(listOf(UnknownHostException("x")))
        try {
            TutorRepository(c).ask("sys", emptyList())
            fail("expected exception")
        } catch (e: UnknownHostException) {
            assertEquals(1, c.calls)
        }
    }

    @Test fun generateTestRetriesAndFails() = runBlocking {
        val one = """{"questions":[{"question":"q","choices":["a","b","c","d"],"correctIndex":0,"explanation":"e"}]}"""
        assertEquals(1, TutorRepository(FakeClient(listOf("nope", one))).generateTest("s", "r", 5).size)
        try {
            TutorRepository(FakeClient(listOf("nope"))).generateTest("s", "r", 5)
            fail("expected exception")
        } catch (e: AiException) {
            assertEquals(AiErrorKind.INVALID_RESPONSE, e.kind)
        }
    }

    @Test fun httpStatusMapping() {
        assertEquals(AiErrorKind.RATE_LIMIT, AiErrors.fromHttp(429, ""))
        assertEquals(AiErrorKind.AUTH, AiErrors.fromHttp(401, ""))
        assertEquals(AiErrorKind.AUTH, AiErrors.fromHttp(400, "API key not valid"))
        assertEquals(AiErrorKind.SERVER, AiErrors.fromHttp(503, ""))
        assertEquals(AiErrorKind.TIMEOUT, AiErrors.fromHttp(504, ""))
        assertEquals(AiErrorKind.INVALID_RESPONSE, AiErrors.fromHttp(502, "{\"error\":\"invalid_ai_response\"}"))
        assertEquals(AiErrorKind.UNKNOWN, AiErrors.fromHttp(418, ""))
    }

    @Test fun throwableClassification() {
        assertEquals(AiErrorKind.NO_INTERNET, AiErrors.classify(UnknownHostException()))
        assertEquals(AiErrorKind.TIMEOUT, AiErrors.classify(SocketTimeoutException()))
        assertEquals(AiErrorKind.EMPTY, AiErrors.classify(AiException(AiErrorKind.EMPTY)))
        assertEquals(AiErrorKind.UNKNOWN, AiErrors.classify(IllegalStateException("boom")))
    }
}
