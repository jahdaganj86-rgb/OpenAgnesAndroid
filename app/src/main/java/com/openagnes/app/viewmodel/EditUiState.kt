package com.openagnes.app.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import com.openagnes.app.model.Skill

data class EditUiState(
    val imageUri: Uri? = null,
    val previewBitmap: Bitmap? = null,
    val imageDataUri: String? = null,
    val ratio: String = "1:1",
    val prompt: String = "",
    val enhancedPrompt: String? = null,
    val originalPrompt: String? = null,
    val resultB64: String? = null,
    val resultBitmap: Bitmap? = null,
    val loading: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val thoughtSeconds: Int = 0,
    val skills: List<Skill> = emptyList(),
    val editorOpen: Boolean = false,
    val showApiKeyDialog: Boolean = false
)
