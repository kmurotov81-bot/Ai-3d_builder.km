package com.example.socratictutor

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class AiErrorKind {
    NO_INTERNET, TIMEOUT, RATE_LIMIT, NOT_CONFIGURED, AUTH, SERVER, EMPTY,
    INVALID_RESPONSE, IMAGE_FAILED, CAMERA_UNAVAILABLE, GRAPH_FAILED, UNKNOWN,
}

/** The only error type the UI sees. Its message is for logs, never shown to students. */
class AiException(val kind: AiErrorKind, detail: String? = null, cause: Throwable? = null) :
    IOException(detail ?: kind.name, cause)

object AiErrors {
    fun fromHttp(code: Int, body: String): AiErrorKind = when {
        code == 429 -> AiErrorKind.RATE_LIMIT
        code == 401 || code == 403 -> AiErrorKind.AUTH
        code == 400 && body.contains("API key", ignoreCase = true) -> AiErrorKind.AUTH
        code == 408 || code == 504 -> AiErrorKind.TIMEOUT
        code == 502 && body.contains("invalid_ai_response") -> AiErrorKind.INVALID_RESPONSE
        code in 500..599 -> AiErrorKind.SERVER
        else -> AiErrorKind.UNKNOWN
    }

    fun classify(t: Throwable): AiErrorKind = when (t) {
        is AiException -> t.kind
        is UnknownHostException, is ConnectException, is NoRouteToHostException -> AiErrorKind.NO_INTERNET
        is SocketTimeoutException -> AiErrorKind.TIMEOUT
        else -> AiErrorKind.UNKNOWN
    }
}
