package com.openagnes.app.model

data class VideoRequest(
    val model: String = VideoConfig.MODEL,
    val prompt: String,
    val seconds: String = VideoConfig.SECONDS,
    val mode: String = "keyframe",
    val size: String = VideoConfig.SIZE,
    val aspect_ratio: String,
    val first_frame: String
)

data class VideoTaskResponse(
    val id: String?,
    val task_id: String?,
    val video_id: String?,
    val status: String?,
    val progress: Int?
)

data class VideoPollResponse(
    val id: String?,
    val video_id: String?,
    val status: String?,
    val progress: Int?,
    val url: String?,
    val error: VideoError?
)

data class VideoError(val message: String?)