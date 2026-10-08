package com.example.socratictutor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Google Gemini generateContent API. Development use only: the key is inside the APK. */
class GeminiClient : AiClient {
    override suspend fun send(system: String, history: List<ApiTurn>, maxTokens: Int): String =
        withContext(Dispatchers.IO) {
            val contents = JSONArray()
            history.forEach { t ->
                val parts = JSONArray()
                t.imageB64?.let {
                    parts.put(
                        JSONObject().put(
                            "inline_data",
                            JSONObject().put("mime_type", "image/jpeg").put("data", it),
                        ),
                    )
                }
                parts.put(JSONObject().put("text", t.text))
                val role = if (t.role == "assistant") "model" else "user"
                contents.put(JSONObject().put("role", role).put("parts", parts))
            }

            val model = BuildConfig.GEMINI_MODEL
            val flash = model.contains("flash")
            val gen = JSONObject()
                // Thinking tokens count against the output limit, so leave room for them.
                .put("maxOutputTokens", maxTokens + if (flash) 1024 else 4096)
                .put("responseMimeType", "application/json")
            if (flash) gen.put("thinkingConfig", JSONObject().put("thinkingBudget", 1024))

            val body = JSONObject()
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
                .put("contents", contents)
                .put("generationConfig", gen)

            val req = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                .header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val txt = Http.call(req)
            val out = try {
                val root = JSONObject(txt)
                val cands = root.optJSONArray("candidates")
                if (cands == null || cands.length() == 0) {
                    throw AiException(AiErrorKind.EMPTY, "blocked: " + root.optJSONObject("promptFeedback")?.optString("blockReason"))
                }
                val parts = cands.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                buildString {
                    if (parts != null) for (i in 0 until parts.length()) append(parts.getJSONObject(i).optString("text", ""))
                }
            } catch (e: JSONException) {
                throw AiException(AiErrorKind.INVALID_RESPONSE, "Bad Gemini envelope", e)
            }
            if (out.isBlank()) throw AiException(AiErrorKind.EMPTY)
            out
        }
}
