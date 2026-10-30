package com.openagnes.app.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.openagnes.app.model.VideoConfig
import com.openagnes.app.model.VideoPollResponse
import com.openagnes.app.model.VideoRequest
import com.openagnes.app.model.VideoTaskResponse
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class VideoClient private constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    constructor() : this(
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build(),
        Gson()
    )

    suspend fun create(apiKey: String, request: VideoRequest): VideoTaskResponse {
        val body = gson.toJson(request).toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url(VideoConfig.CREATE_URL)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        client.newCall(httpRequest).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ApiHttpException(response.code, raw, errorMessageFrom(raw))
            }
            return gson.fromJson(raw, VideoTaskResponse::class.java)
                ?: throw IOException("Empty video create response")
        }
    }

    suspend fun poll(apiKey: String, videoId: String): VideoPollResponse {
        val url = VideoConfig.POLL_URL.toHttpUrl().newBuilder()
            .addQueryParameter("video_id", videoId)
            .addQueryParameter("model_name", VideoConfig.MODEL)
            .build()
        val httpRequest = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()
        client.newCall(httpRequest).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ApiHttpException(response.code, raw, errorMessageFrom(raw))
            }
            return gson.fromJson(raw, VideoPollResponse::class.java)
                ?: throw IOException("Empty video poll response")
        }
    }

    private fun errorMessageFrom(body: String): String? {
        return try {
            val element = JsonParser.parseString(body)
            if (!element.isJsonObject) return null
            val root = element.asJsonObject
            root.stringOrNull("detail")
                ?: root.getAsJsonObject("error")?.stringOrNull("message")
                ?: root.stringOrNull("message")
        } catch (e: Exception) {
            null
        }
    }

    private fun JsonObject.stringOrNull(name: String): String? {
        val value = get(name) ?: return null
        if (value.isJsonNull) return null
        return if (value.isJsonPrimitive) value.asString else value.toString()
    }
}