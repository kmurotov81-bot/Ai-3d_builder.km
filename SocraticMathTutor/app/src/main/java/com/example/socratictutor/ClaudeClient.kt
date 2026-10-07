package com.example.socratictutor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ClaudeClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun send(system: String, history: List<ApiTurn>): String = withContext(Dispatchers.IO) {
        val messages = JSONArray()
        history.forEach { t ->
            val content = JSONArray()
            t.imageB64?.let {
                content.put(
                    JSONObject()
                        .put("type", "image")
                        .put(
                            "source",
                            JSONObject()
                                .put("type", "base64")
                                .put("media_type", "image/jpeg")
                                .put("data", it)
                        )
                )
            }
            content.put(JSONObject().put("type", "text").put("text", t.text))
            messages.put(JSONObject().put("role", t.role).put("content", content))
        }

        val body = JSONObject()
            .put("model", BuildConfig.CLAUDE_MODEL)
            .put("max_tokens", 1500)
            .put("system", system)
            .put("messages", messages)

        val req = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
            .header("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(req).execute().use { resp ->
            val txt = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException(errorMessage(txt, resp.code))
            val arr = JSONObject(txt).getJSONArray("content")
            buildString {
                for (i in 0 until arr.length()) {
                    val b = arr.getJSONObject(i)
                    if (b.optString("type") == "text") append(b.getString("text"))
                }
            }
        }
    }

    private fun errorMessage(body: String, code: Int): String = try {
        val m = JSONObject(body).getJSONObject("error").getString("message")
        "The tutor is unavailable ($code): $m"
    } catch (e: Exception) {
        "The tutor is unavailable (HTTP $code). Your message wasn't sent, please try again."
    }
}
