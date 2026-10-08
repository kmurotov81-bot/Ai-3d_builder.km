package com.example.socratictutor

data class AiResult(val reply: TutorReply, val raw: String)

/** Talks to the AI and turns the answer into validated app data. Retries once on a malformed reply. */
class TutorRepository(private val client: AiClient) {

    suspend fun ask(system: String, history: List<ApiTurn>): AiResult {
        repeat(MAX_ATTEMPTS) {
            val raw = client.send(system, history)
            TutorReply.parseOrNull(raw)?.let { return AiResult(it, raw) }
        }
        throw AiException(AiErrorKind.INVALID_RESPONSE, "Reply was not valid tutor JSON")
    }

    suspend fun generateTest(system: String, request: String, wanted: Int): List<TestQuestion> {
        repeat(MAX_ATTEMPTS) {
            val raw = client.send(system, listOf(ApiTurn("user", request)), maxTokens = 6000)
            val qs = TestParser.parse(raw)
            if (qs.isNotEmpty()) return qs.take(wanted)
        }
        throw AiException(AiErrorKind.INVALID_RESPONSE, "No valid test questions")
    }

    private companion object { const val MAX_ATTEMPTS = 2 }
}
