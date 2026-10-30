package com.openagnes.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openagnes.app.R
import com.openagnes.app.model.Skill
import com.openagnes.app.repository.ImageEditRepository
import com.openagnes.app.storage.ApiKeyStore
import com.openagnes.app.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditViewModel(application: Application) : ViewModel() {
    private val repository = ImageEditRepository.get()
    private val appContext: Context = application

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    fun initApiKeyStore() {
        ApiKeyStore.init(appContext)
    }

    fun saveApiKey(key: String): Boolean = ApiKeyStore.saveApiKey(key)

    fun getStoredApiKey(): String? = ApiKeyStore.getApiKey()

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val bitmap = ImageUtil.loadBitmap(appContext, uri)
                val scaled = ImageUtil.scaleDown(bitmap)
                val ratio = ImageUtil.detectRatio(scaled.width, scaled.height)
                val dataUri = ImageUtil.bitmapToDataUri(scaled)
                _uiState.update {
                    it.copy(
                        imageUri = uri,
                        previewBitmap = scaled,
                        imageDataUri = dataUri,
                        ratio = ratio,
                        error = null,
                        resultB64 = null,
                        resultBitmap = null
                    )
                }
            }
        }
    }

    fun useResultAsSource() {
        val state = _uiState.value
        val b64 = state.resultB64 ?: return
        val bitmap = ImageUtil.dataUriToBitmap(b64)
        val ratio = ImageUtil.detectRatio(bitmap.width, bitmap.height)
        val scaled = ImageUtil.scaleDown(bitmap)
        _uiState.update {
            it.copy(
                previewBitmap = scaled,
                imageDataUri = b64,
                ratio = ratio,
                resultB64 = null,
                resultBitmap = null,
                error = null
            )
        }
    }

    fun onPromptChange(text: String) {
        _uiState.update { it.copy(prompt = text, error = null) }
    }

    suspend fun enhance() {
        val state = _uiState.value
        val key = ApiKeyStore.getApiKey() ?: run {
            _uiState.update { it.copy(error = resString(R.string.error_no_key)) }
            return
        }
        if (state.imageDataUri.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Bitte wählen Sie zuerst ein Bild aus.") }
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }
        val start = System.currentTimeMillis()
        try {
            val enhanced = withContext(Dispatchers.IO) {
                repository.enhance(key, state.imageDataUri!!, state.prompt)
            }
            val seconds = ((System.currentTimeMillis() - start) / 1000).coerceAtLeast(1).toInt()
            _uiState.update {
                it.copy(
                    loading = false,
                    enhancedPrompt = enhanced,
                    originalPrompt = state.prompt,
                    thoughtSeconds = seconds,
                    skills = listOf(
                        Skill("Original prompt", state.prompt.length),
                        Skill("Enhanced prompt", enhanced.length)
                    ),
                    message = resString(R.string.thoughts_complete, seconds)
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(loading = false, error = e.message ?: resString(R.string.error_general, "Unbekannt")) }
            Log.e("EditViewModel", "Enhance failed", e)
        }
    }

    suspend fun generate() {
        val state = _uiState.value
        val key = ApiKeyStore.getApiKey() ?: run {
            _uiState.update { it.copy(error = resString(R.string.error_no_key)) }
            return
        }
        if (state.enhancedPrompt.isNullOrBlank() || state.imageDataUri.isNullOrBlank()) return
        _uiState.update { it.copy(loading = true, error = null) }
        try {
            val b64 = withContext(Dispatchers.IO) {
                repository.editImage(key, state.enhancedPrompt!!, state.imageDataUri!!, state.ratio)
            }
            val bitmap = ImageUtil.dataUriToBitmap(b64)
            _uiState.update {
                it.copy(loading = false, resultB64 = b64, resultBitmap = bitmap, message = resString(R.string.result_done))
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(loading = false, error = e.message ?: resString(R.string.error_general, "Unbekannt")) }
            Log.e("EditViewModel", "Edit failed", e)
        }
    }

    fun saveResult(): String? {
        val state = _uiState.value
        val bitmap = state.resultBitmap ?: return null
        return try {
            ImageUtil.saveToMediaStore(appContext, bitmap, "OpenAgnes_${System.currentTimeMillis()}.jpg")
            _uiState.update { it.copy(message = resString(R.string.saved_media)) }
            resString(R.string.saved_media)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: resString(R.string.error_general, "Speichern fehlgeschlagen")) }
            null
        }
    }

    private fun resString(id: Int, vararg args: Any): String =
        appContext.getString(id, *args)
}
