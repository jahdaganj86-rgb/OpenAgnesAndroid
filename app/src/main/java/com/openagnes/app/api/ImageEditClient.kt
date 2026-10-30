package com.openagnes.app.api

import com.google.gson.Gson
import com.openagnes.app.model.EditRequest
import com.openagnes.app.model.EditResponse
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
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
        val body = RequestBody.create(json, MediaType.Companion.parse("application/json"))
        val request = Request.Builder()
            .url("https://apihub.agnes-ai.com/v1/images/generations")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Image edit failed: ${response.code} ${response.body?.string() ?: ""}")
        }
        val responseBody = response.body?.string() ?: throw IOException("Empty image edit response")
        return gson.fromJson(responseBody, EditResponse::class.java)
            .data.firstOrNull()?.b64_json
            ?: throw IOException("No image returned by edit API")
    }
}
