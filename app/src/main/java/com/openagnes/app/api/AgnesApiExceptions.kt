package com.openagnes.app.api

import java.io.IOException

class ApiHttpException(
    val code: Int,
    val body: String,
    message: String? = null
) : IOException(message ?: "HTTP $code")

class VideoTimeoutException : IOException("Video generation timed out")

class VideoFailedException(val reason: String) : IOException("Video generation failed: $reason")