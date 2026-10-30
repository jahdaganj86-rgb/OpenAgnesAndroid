package com.openagnes.app.api

import com.openagnes.app.model.ChatRequest
import com.openagnes.app.model.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AgnesApi {
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
