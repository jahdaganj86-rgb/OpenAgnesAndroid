package com.openagnes.app.repository

import android.util.Log
import com.openagnes.app.api.AgnesApi
import com.openagnes.app.api.ApiClient
import com.openagnes.app.api.ApiHttpException
import com.openagnes.app.api.ImageEditClient
import com.openagnes.app.api.VideoClient
import com.openagnes.app.api.VideoFailedException
import com.openagnes.app.api.VideoTimeoutException
import com.openagnes.app.model.ChatContent
import com.openagnes.app.model.ChatImageUrl
import com.openagnes.app.model.ChatMessage
import com.openagnes.app.model.ChatRequest
import com.openagnes.app.model.EditRequest
import com.openagnes.app.model.ExtraBody
import com.openagnes.app.model.VideoConfig
import com.openagnes.app.model.VideoRequest
import com.openagnes.app.model.VideoTaskResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException

class ImageEditRepository private constructor(
    private val api: AgnesApi,
    private val editClient: ImageEditClient,
    private val videoClient: VideoClient
) {
    companion object {
        @Volatile private var instance: ImageEditRepository? = null
        fun get(
            api: AgnesApi = ApiClient.api,
            editClient: ImageEditClient = ImageEditClient(),
            videoClient: VideoClient = VideoClient()
        ): ImageEditRepository {
            return instance ?: synchronized(this) {
                instance ?: ImageEditRepository(api, editClient, videoClient).also { instance = it }
            }
        }

        private const val TAG = "ImageEditRepository"
    }

    private val SYSTEM_PROMPT = """
        You are an image editing prompt engineer. You receive an original image and a user prompt.
        1. Analyze the original image and the prompt.
        2. Write an "Enhanced Prompt" in English for img2img.
        3. Change ONLY what is explicitly mentioned in the user prompt.
        4. EVERYTHING else (person, clothing, face, pose, background, lighting, composition, text, logos) remains 1:1 from the original.
        5. Use explicit "preserve"/"keep unchanged" instructions.
        6. Structure: "[What changes] + [What stays unchanged] + [Style, lighting, composition, quality]".
    """.trimIndent()

    private val VIDEO_SYSTEM_PROMPT = """
        You are an image-to-video prompt engineer. You receive an original image and a user request.
        1. Analyze the original image and the requested motion.
        2. Write an "Enhanced Prompt" in English for image-to-video keyframe animation.
        3. Preserve the subject, face, identity, clothing, and background; change only what the user asked for.
        4. Structure: [subject and setting] -> [action or change] -> [camera language] -> [style and lighting] -> [sound and rhythm] -> [consistency].
        5. Keep it concise and vivid. Do not invent new characters, objects, or scenes.
    """.trimIndent()

    private val MAX_RETRIES = 3

    suspend fun enhance(apiKey: String, dataUri: String, userPrompt: String): String =
        enhanceInternal(SYSTEM_PROMPT, apiKey, dataUri, userPrompt)

    suspend fun enhanceVideo(apiKey: String, dataUri: String, userPrompt: String): String =
        enhanceInternal(VIDEO_SYSTEM_PROMPT, apiKey, dataUri, userPrompt)

    private suspend fun enhanceInternal(
        systemPrompt: String,
        apiKey: String,
        dataUri: String,
        userPrompt: String
    ): String {
        var lastError: Throwable? = null
        for (attempt in 0 until MAX_RETRIES) {
            try {
                return withContext(Dispatchers.IO) {
                    val body = ChatRequest(
                        model = "agnes-2.5-flash",
                        messages = listOf(
                            ChatMessage("system", listOf(ChatContent("text", systemPrompt))),
                            ChatMessage("user", listOf(
                                ChatContent("text", userPrompt),
                                ChatContent("image_url", image_url = ChatImageUrl(dataUri))
                            ))
                        ),
                        temperature = 0.3
                    )
                    val response = api.chatCompletions("Bearer $apiKey", body)
                    when {
                        response.isSuccessful -> {
                            response.body()?.choices?.firstOrNull()?.message?.content?.firstOrNull()?.text
                                ?: throw IOException("Enhance: empty response")
                        }
                        response.code() == 401 -> throw IOException("Enhance: 401 unauthorized")
                        response.code() == 429 || response.code() in 500..599 ->
                            throw IOException("Enhance: retryable ${response.code()}")
                        else -> throw IOException("Enhance: ${response.code()} ${response.errorBody()?.string() ?: ""}")
                    }
                }
            } catch (e: Throwable) {
                lastError = e
                if (attempt < MAX_RETRIES - 1 && isRetryable(e)) {
                    delay((1L shl (attempt + 1)) * 1000)
                } else {
                    throw lastError!!
                }
            }
        }
        throw lastError!!
    }

    suspend fun editImage(apiKey: String, enhancedPrompt: String, dataUri: String, ratio: String): String {
        var lastError: Throwable? = null
        for (attempt in 0 until MAX_RETRIES) {
            try {
                return withContext(Dispatchers.IO) {
                    editClient.edit(
                        apiKey,
                        EditRequest(
                            prompt = enhancedPrompt,
                            ratio = ratio,
                            extra_body = ExtraBody(image = listOf(dataUri))
                        )
                    )
                }
            } catch (e: Throwable) {
                lastError = e
                if (attempt < MAX_RETRIES - 1 && isRetryable(e)) {
                    delay((1L shl (attempt + 1)) * 1000)
                } else {
                    throw lastError!!
                }
            }
        }
        throw lastError!!
    }

    suspend fun generateVideo(
        apiKey: String,
        prompt: String,
        dataUri: String,
        imageRatio: String,
        videoRatio: String,
        onProgress: (Int) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val raw = dataUri.substringAfter("base64,", dataUri)
        try {
            createAndPoll(apiKey, prompt, videoRatio, raw, onProgress)
        } catch (e: ApiHttpException) {
            if (e.code != 400 || !isCreateMediaError(e.body)) throw e
            generateVideoWithUrl(apiKey, prompt, dataUri, imageRatio, videoRatio, onProgress)
        } catch (e: VideoFailedException) {
            if (!isFailedMediaError(e.reason)) throw e
            generateVideoWithUrl(apiKey, prompt, dataUri, imageRatio, videoRatio, onProgress)
        }
    }

    private suspend fun generateVideoWithUrl(
        apiKey: String,
        prompt: String,
        dataUri: String,
        imageRatio: String,
        videoRatio: String,
        onProgress: (Int) -> Unit
    ): String {
        Log.w(TAG, "video create rejected base64; retrying with public image URL")
        val url = editClient.editForUrl(apiKey, dataUri, imageRatio)
        return createAndPoll(apiKey, prompt, videoRatio, url, onProgress)
    }

    private suspend fun createAndPoll(
        apiKey: String,
        prompt: String,
        videoRatio: String,
        firstFrame: String,
        onProgress: (Int) -> Unit
    ): String {
        val task = createTaskWithRetry(apiKey, prompt, videoRatio, firstFrame)
        val videoId = task.video_id ?: task.id
            ?: throw VideoFailedException("missing video id")
        val deadline = System.currentTimeMillis() + VideoConfig.MAX_POLL_MS

        while (true) {
            if (System.currentTimeMillis() >= deadline) throw VideoTimeoutException()
            try {
                val poll = videoClient.poll(apiKey, videoId)
                poll.progress?.let { onProgress(it.coerceIn(0, 100)) }
                when (poll.status?.lowercase()) {
                    "completed", "success", "succeeded", "done" ->
                        return poll.url ?: throw VideoFailedException("completed without url")
                    "failed", "error", "cancelled" ->
                        throw VideoFailedException(poll.error?.message ?: "unknown failure")
                }
            } catch (e: ApiHttpException) {
                when {
                    e.code == 401 || e.code == 403 || e.code == 404 -> throw e
                    e.code == 429 -> delay(10_000L)
                    e.code in 500..599 -> delay(5_000L)
                    else -> throw e
                }
                continue
            }
            delay(VideoConfig.POLL_INTERVAL_MS)
        }
    }

    private suspend fun createTaskWithRetry(
        apiKey: String,
        prompt: String,
        videoRatio: String,
        firstFrame: String
    ): VideoTaskResponse {
        var lastError: Throwable? = null
        for (attempt in 0 until MAX_RETRIES) {
            try {
                return videoClient.create(
                    apiKey,
                    VideoRequest(
                        prompt = prompt,
                        aspect_ratio = videoRatio,
                        first_frame = firstFrame
                    )
                )
            } catch (e: ApiHttpException) {
                lastError = e
                if (attempt < MAX_RETRIES - 1 && e.code in 500..599) {
                    delay(if (attempt == 0) 5_000L else 10_000L)
                } else {
                    throw e
                }
            } catch (e: SocketTimeoutException) {
                lastError = e
                if (attempt < MAX_RETRIES - 1) {
                    delay(if (attempt == 0) 5_000L else 10_000L)
                } else {
                    throw e
                }
            }
        }
        throw lastError!!
    }

    private fun isCreateMediaError(body: String?): Boolean {
        val lower = (body ?: "").lowercase()
        return listOf("first_frame", "frame", "image", "url", "base64", "media", "reference")
            .any { lower.contains(it) }
    }

    private fun isFailedMediaError(reason: String?): Boolean {
        val lower = (reason ?: "").lowercase()
        return listOf("image", "media", "frame", "reference")
            .any { lower.contains(it) }
    }

    private fun isRetryable(e: Throwable): Boolean {
        val msg = (e.message ?: "").lowercase()
        return msg.contains("429") || msg.contains("50") ||
            e is java.net.SocketTimeoutException || e is java.net.UnknownHostException
    }
}