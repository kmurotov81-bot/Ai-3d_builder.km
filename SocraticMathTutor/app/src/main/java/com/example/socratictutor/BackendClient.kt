package com.example.socratictutor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Production path: Android -> your backend (holds the AI key) -> AI provider. */
class BackendClient : AiClient {
    override suspend fun send(system: String, history: List<ApiTurn>, maxTokens: Int): String =
        withContext(Dispatchers.IO) {
            val turns = JSONArray()
            history.forEach { t ->
                val o = JSONObject().put("role", t.role).put("text", t.text)
                t.imageB64?.let { o.put("imageB64", it) }
                turns.put(o)
            }
            val body = JSONObject().put("system", system).put("history", turns).put("maxTokens", maxTokens)
            val rb = Request.Builder()
                .url(BuildConfig.BACKEND_URL.trimEnd('/') + "/v1/tutor")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
            if (BuildConfig.APP_TOKEN.isNotBlank()) rb.header("X-App-Token", BuildConfig.APP_TOKEN)

            val txt = Http.call(rb.build())
            val out = try {
                JSONObject(txt).optString("text")
            } catch (e: JSONException) {
                throw AiException(AiErrorKind.INVALID_RESPONSE, "Bad backend envelope", e)
            }
            if (out.isBlank()) throw AiException(AiErrorKind.EMPTY)
            out
        }
}
