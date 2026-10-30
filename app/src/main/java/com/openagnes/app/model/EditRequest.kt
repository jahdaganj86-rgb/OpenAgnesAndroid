package com.openagnes.app.model

data class ChatContent(
    val type: String,
    val text: String? = null,
    val image_url: ChatImageUrl? = null
)

data class ChatImageUrl(val url: String)

data class ChatMessage(val role: String, val content: List<ChatContent>)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.3
)

data class ChatChoice(val message: ChatMessage)

data class ChatResponse(val choices: List<ChatChoice>)

data class ExtraBody(
    val image: List<String>,
    val response_format: String = "b64_json"
)

data class EditRequest(
    val model: String = "agnes-image-2.1-flash",
    val prompt: String,
    val size: String = "2K",
    val ratio: String,
    val extra_body: ExtraBody
)

data class EditImage(val b64_json: String)

data class EditResponse(val data: List<EditImage>)

data class EditUrl(val url: String?)

data class EditUrlResponse(val data: List<EditUrl>)

data class Skill(val name: String, val chars: Int)
