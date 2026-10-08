package com.example.socratictutor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Anthropic Messages API. Development use only: the key is inside the APK (use a backend for release). */
class ClaudeClient : AiClient {
    override suspend fun send(system: String, history: List<ApiTurn>, maxTokens: Int): String =
        withContext(Dispatchers.IO) {
            val messages = JSONArray()
            history.forEach { t ->
                val content = JSONArray()
                t.imageB64?.let {
                    content.put(
                        JSONObject().put("type", "image").put(
                            "source",
                            JSONObject().put("type", "base64").put("media_type", "image/jpeg").put("data", it),
                        ),
                    )
                }
                content.put(JSONObject().put("type", "text").put("text", t.text))
                messages.put(JSONObject().put("role", t.role).put("content", content))
            }
            val body = JSONObject()
                .put("model", BuildConfig.CLAUDE_MODEL)
                .put("max_tokens", maxTokens)
                .put("system", system)
                .put("messages", messages)

            val req = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
                .header("anthropic-version", "2023-06-01")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val txt = Http.call(req)
            val out = try {
                val arr = JSONObject(txt).getJSONArray("content")
                buildString {
                    for (i in 0 until arr.length()) {
                        val b = arr.getJSONObject(i)
                        if (b.optString("type") == "text") append(b.optString("text"))
                    }
                }
            } catch (e: JSONException) {
                throw AiException(AiErrorKind.INVALID_RESPONSE, "Bad Claude envelope", e)
            }
            if (out.isBlank()) throw AiException(AiErrorKind.EMPTY)
            out
        }
}
