package com.example.socratictutor

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Common interface so the tutor can use Gemini, Claude, or your own backend. */
interface AiClient {
    suspend fun send(system: String, history: List<ApiTurn>, maxTokens: Int = 2048): String
}

/** Shared HTTP plumbing: one OkHttp client, and HTTP failures mapped to [AiException]. */
internal object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    /** Blocking; call from Dispatchers.IO. Returns the body or throws [AiException]. */
    fun call(req: Request): String =
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw AiException(AiErrors.fromHttp(resp.code, body), "HTTP ${resp.code}")
            body
        }
}

object AiClientFactory {
    private val useBackend get() = BuildConfig.BACKEND_URL.isNotBlank()
    private val useClaude get() = BuildConfig.AI_PROVIDER.equals("claude", ignoreCase = true)

    fun create(): AiClient = when {
        useBackend -> BackendClient()
        useClaude -> ClaudeClient()
        else -> GeminiClient()
    }

    /** Name of the missing setting, or null when the app is ready to call an AI. */
    fun missingConfig(): String? = when {
        useBackend -> null
        useClaude -> if (BuildConfig.ANTHROPIC_API_KEY.isBlank()) "ANTHROPIC_API_KEY" else null
        else -> if (BuildConfig.GEMINI_API_KEY.isBlank()) "GEMINI_API_KEY" else null
    }
}
