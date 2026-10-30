package com.openagnes.app.repository

import com.openagnes.app.api.AgnesApi
import com.openagnes.app.api.ApiClient
import com.openagnes.app.api.ImageEditClient
import com.openagnes.app.model.ChatContent
import com.openagnes.app.model.ChatImageUrl
import com.openagnes.app.model.ChatMessage
import com.openagnes.app.model.ChatRequest
import com.openagnes.app.model.EditRequest
import com.openagnes.app.model.ExtraBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException

class ImageEditRepository private constructor(
    private val api: AgnesApi,
    private val editClient: ImageEditClient
) {
    companion object {
        @Volatile private var instance: ImageEditRepository? = null
        fun get(api: AgnesApi = ApiClient.api, editClient: ImageEditClient = ImageEditClient()): ImageEditRepository {
            return instance ?: synchronized(this) {
                instance ?: ImageEditRepository(api, editClient).also { instance = it }
            }
        }
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

    private val MAX_RETRIES = 3

    suspend fun enhance(apiKey: String, dataUri: String, userPrompt: String): String {
        var lastError: Throwable? = null
        for (attempt in 0 until MAX_RETRIES) {
            try {
                return withContext(Dispatchers.IO) {
                    val body = ChatRequest(
                        model = "agnes-2.5-flash",
                        messages = listOf(
                            ChatMessage("system", listOf(ChatContent("text", SYSTEM_PROMPT))),
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

    private fun isRetryable(e: Throwable): Boolean {
        val msg = (e.message ?: "").lowercase()
        return msg.contains("429") || msg.contains("50") ||
            e is java.net.SocketTimeoutException || e is java.net.UnknownHostException
    }
}
