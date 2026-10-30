package com.openagnes.app.model

object VideoConfig {
    const val MODEL = "agnes-video-2.5-flash"
    const val SECONDS = "5"
    const val SIZE = "720P"
    const val CREATE_URL = "https://apihub.agnes-ai.com/v1/videos"
    const val POLL_URL = "https://apihub.agnes-ai.com/agnesapi"
    const val POLL_INTERVAL_MS = 2_000L
    const val MAX_POLL_MS = 600_000L
}