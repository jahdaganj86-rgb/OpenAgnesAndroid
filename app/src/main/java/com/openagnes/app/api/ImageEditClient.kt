package com.openagnes.app.api

import com.google.gson.Gson
import com.openagnes.app.model.CreateImageRequest
import com.openagnes.app.model.EditRequest
import com.openagnes.app.model.EditResponse
import com.openagnes.app.model.EditUrlResponse
import com.openagnes.app.model.ExtraBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ImageEditClient private constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    constructor() : this(
        OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .build(),
        Gson()
    )

    suspend fun edit(apiKey: String, request: EditRequest): String {
        val json = gson.toJson(request)
        val body = json.toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url("https://apihub.agnes-ai.com/v1/images/generations")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        val response = client.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            throw IOException("Image edit failed: ${response.code} ${response.body?.string() ?: ""}")
        }
        val responseBody = response.body?.string() ?: throw IOException("Empty image edit response")
        return gson.fromJson(responseBody, EditResponse::class.java)
            .data.firstOrNull()?.b64_json
            ?: throw IOException("No image returned by edit API")
    }

    suspend fun create(apiKey: String, request: CreateImageRequest): String {
        val json = gson.toJson(request)
        val body = json.toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url("https://apihub.agnes-ai.com/v1/images/generations")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        val response = client.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            throw IOException("Image create failed: ${response.code} ${response.body?.string() ?: ""}")
        }
        val responseBody = response.body?.string() ?: throw IOException("Empty image create response")
        return gson.fromJson(responseBody, EditResponse::class.java)
            .data.firstOrNull()?.b64_json
            ?: throw IOException("No image returned by create API")
    }

    suspend fun editForUrl(apiKey: String, dataUri: String, ratio: String): String {
        val request = EditRequest(
            model = "agnes-image-2.5-flash",
            prompt = PRESERVE_PROMPT,
            ratio = ratio,
            size = "2K",
            extra_body = ExtraBody(image = listOf(dataUri), response_format = "url")
        )
        val body = gson.toJson(request).toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url("https://apihub.agnes-ai.com/v1/images/generations")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Image url edit failed: ${response.code}")
            }
            val responseBody = response.body?.string() ?: throw IOException("Empty image url edit response")
            return gson.fromJson(responseBody, EditUrlResponse::class.java)
                .data.firstOrNull()?.url
                ?: throw IOException("No image URL returned by edit API")
        }
    }

    private companion object {
        val PRESERVE_PROMPT = """
            Preserve the original image exactly. Keep the subject, identity, face, pose, clothing,
            composition, framing, background, colors, lighting and fine details unchanged. Do not add,
            remove, or alter any element. Return a faithful, high-quality reproduction of the input image.
        """.trimIndent()
    }
}
