package com.openagnes.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openagnes.app.R
import com.openagnes.app.api.ApiHttpException
import com.openagnes.app.api.VideoFailedException
import com.openagnes.app.api.VideoTimeoutException
import com.openagnes.app.model.Skill
import com.openagnes.app.repository.ImageEditRepository
import com.openagnes.app.storage.ApiKeyStore
import com.openagnes.app.util.ImageUtil
import com.openagnes.app.util.VideoUtil
import kotlinx.coroutines.CancellationException
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

    private var stateBeforeEditor: EditUiState? = null

    fun initApiKeyStore() {
        ApiKeyStore.init(appContext)
    }

    fun getStoredApiKey(): String? = ApiKeyStore.getApiKey()

    fun onImagePicked(uri: Uri) {
        stateBeforeEditor = _uiState.value
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val bitmap = ImageUtil.loadBitmap(appContext, uri)
                    val scaled = ImageUtil.scaleDown(bitmap)
                    val ratio = ImageUtil.detectRatio(scaled.width, scaled.height)
                    val videoRatio = ImageUtil.detectVideoRatio(scaled.width, scaled.height)
                    val dataUri = ImageUtil.bitmapToDataUri(scaled)
                    _uiState.update {
                        it.copy(
                            imageUri = uri,
                            previewBitmap = scaled,
                            imageDataUri = dataUri,
                            ratio = ratio,
                            videoRatio = videoRatio,
                            prompt = "",
                            enhancedPrompt = null,
                            originalPrompt = null,
                            skills = emptyList(),
                            resultB64 = null,
                            resultBitmap = null,
                            videoUrl = null,
                            videoProgress = 0,
                            videoSaving = false,
                            submitted = false,
                            loading = false,
                            message = null,
                            actionMessage = null,
                            error = null,
                            showApiKeyDialog = false,
                            editorOpen = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("EditViewModel", "Image load failed", e)
                stateBeforeEditor = null
                _uiState.update {
                    it.copy(error = e.message ?: resString(R.string.error_general, "Unbekannt"))
                }
            }
        }
    }

    private fun openEditor() {
        stateBeforeEditor = _uiState.value
        _uiState.update { it.copy(editorOpen = true) }
    }

    fun reopenEditor() {
        val cleared = _uiState.value.copy(error = null, actionMessage = null)
        stateBeforeEditor = cleared
        _uiState.value = cleared.copy(editorOpen = true)
    }

    fun closeEditor() {
        val snapshot = stateBeforeEditor
        _uiState.value = snapshot?.copy(editorOpen = false, showApiKeyDialog = false)
            ?: EditUiState()
        stateBeforeEditor = null
    }

    fun requestApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = true) }
    }

    fun dismissApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = false) }
    }

    fun onApiKeySaved(key: String) {
        val saved = ApiKeyStore.saveApiKey(key)
        _uiState.update {
            it.copy(
                showApiKeyDialog = false,
                error = if (saved) null else resString(R.string.error_no_key)
            )
        }
    }

    fun applyQuickPrompt(text: String) {
        _uiState.update { it.copy(prompt = text, error = null) }
    }

    fun onModeChange(mode: GenerationMode) {
        val state = _uiState.value
        if (state.loading || state.mode == mode) return
        _uiState.update {
            it.copy(
                mode = mode,
                resultB64 = null,
                resultBitmap = null,
                videoUrl = null,
                videoProgress = 0,
                videoSaving = false,
                enhancedPrompt = null,
                originalPrompt = null,
                skills = emptyList(),
                submitted = false,
                message = null,
                actionMessage = null,
                error = null
            )
        }
    }

    fun submit() {
        val state = _uiState.value
        if (state.loading) return
        if (ApiKeyStore.getApiKey().isNullOrBlank()) {
            _uiState.update { it.copy(showApiKeyDialog = true) }
            return
        }
        if (state.prompt.isBlank()) return
        if (state.imageDataUri.isNullOrBlank()) {
            _uiState.update { it.copy(error = resString(R.string.error_no_image)) }
            return
        }
        stateBeforeEditor = null
        _uiState.update {
            it.copy(
                editorOpen = false,
                showApiKeyDialog = false,
                error = null,
                actionMessage = null,
                enhancedPrompt = null,
                originalPrompt = null,
                skills = emptyList(),
                thoughtSeconds = 0,
                videoUrl = if (it.mode == GenerationMode.VIDEO) null else it.videoUrl,
                videoProgress = if (it.mode == GenerationMode.VIDEO) 0 else it.videoProgress,
                resultB64 = if (it.mode == GenerationMode.IMAGE) null else it.resultB64,
                resultBitmap = if (it.mode == GenerationMode.IMAGE) null else it.resultBitmap
            )
        }
        viewModelScope.launch {
            enhance()
            generate()
        }
    }

    fun useResultAsSource() {
        val state = _uiState.value
        val b64 = state.resultB64 ?: return
        val bitmap = try {
            ImageUtil.dataUriToBitmap(b64)
        } catch (e: Exception) {
            Log.e("EditViewModel", "Result decode failed", e)
            _uiState.update { it.copy(error = e.message ?: resString(R.string.error_general, "Unbekannt")) }
            return
        }
        val ratio = ImageUtil.detectRatio(bitmap.width, bitmap.height)
        val videoRatio = ImageUtil.detectVideoRatio(bitmap.width, bitmap.height)
        val scaled = ImageUtil.scaleDown(bitmap)
        openEditor()
        _uiState.update {
            it.copy(
                previewBitmap = scaled,
                imageDataUri = b64,
                ratio = ratio,
                videoRatio = videoRatio,
                resultB64 = null,
                resultBitmap = null,
                videoUrl = null,
                videoProgress = 0,
                videoSaving = false,
                prompt = "",
                originalPrompt = null,
                enhancedPrompt = null,
                skills = emptyList(),
                loading = false,
                message = null,
                actionMessage = null,
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
            _uiState.update { it.copy(error = resString(R.string.error_no_image)) }
            return
        }
        _uiState.update { it.copy(loading = true, submitted = true, error = null) }
        val start = System.currentTimeMillis()
        try {
            val enhanced = withContext(Dispatchers.IO) {
                if (state.mode == GenerationMode.VIDEO) {
                    repository.enhanceVideo(key, state.imageDataUri!!, state.prompt)
                } else {
                    repository.enhance(key, state.imageDataUri!!, state.prompt)
                }
            }
            val seconds = ((System.currentTimeMillis() - start) / 1000).coerceAtLeast(1).toInt()
            val skills = if (state.mode == GenerationMode.VIDEO) {
                listOf(
                    Skill("video-generation", 8078),
                    Skill("image-to-video / keyframe", 5429),
                    Skill("video-prompt-craft", 15387)
                )
            } else {
                listOf(
                    Skill("image-generation", 8078),
                    Skill("image-generation / reference", 5429),
                    Skill("image-prompt-craft", 15387)
                )
            }
            _uiState.update {
                it.copy(
                    loading = false,
                    enhancedPrompt = enhanced,
                    originalPrompt = state.prompt,
                    thoughtSeconds = seconds,
                    skills = skills,
                    message = resString(R.string.thoughts_complete, seconds)
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _uiState.update { it.copy(loading = false, error = e.message ?: resString(R.string.error_general, "Unbekannt")) }
            Log.e("EditViewModel", "Enhance failed", e)
        }
    }

    suspend fun generate() {
        when (_uiState.value.mode) {
            GenerationMode.IMAGE -> generateImage()
            GenerationMode.VIDEO -> generateVideo()
        }
    }

    private suspend fun generateImage() {
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
            if (e is CancellationException) throw e
            _uiState.update { it.copy(loading = false, error = e.message ?: resString(R.string.error_general, "Unbekannt")) }
            Log.e("EditViewModel", "Edit failed", e)
        }
    }

    private suspend fun generateVideo() {
        val state = _uiState.value
        val key = ApiKeyStore.getApiKey() ?: run {
            _uiState.update { it.copy(error = resString(R.string.error_no_key)) }
            return
        }
        if (state.enhancedPrompt.isNullOrBlank() || state.imageDataUri.isNullOrBlank()) return
        _uiState.update { it.copy(loading = true, error = null, videoProgress = 0, videoUrl = null) }
        try {
            val url = repository.generateVideo(
                apiKey = key,
                prompt = state.enhancedPrompt!!,
                dataUri = state.imageDataUri!!,
                imageRatio = state.ratio,
                videoRatio = state.videoRatio,
                onProgress = { progress -> _uiState.update { it.copy(videoProgress = progress) } }
            )
            _uiState.update {
                it.copy(
                    loading = false,
                    videoUrl = url,
                    videoProgress = 100,
                    resultB64 = null,
                    resultBitmap = null,
                    message = resString(R.string.result_done_video)
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _uiState.update { it.copy(loading = false, error = videoErrorMessage(e)) }
            Log.e("EditViewModel", "Video generation failed", e)
        }
    }

    private fun videoErrorMessage(e: Throwable): String = when {
        e is ApiHttpException && (e.code == 401 || e.code == 403) -> resString(R.string.error_video_auth)
        e is ApiHttpException && e.code == 429 -> resString(R.string.error_video_rate_limit)
        e is VideoTimeoutException -> resString(R.string.error_video_timeout)
        e is VideoFailedException -> e.reason
        else -> e.message ?: resString(R.string.error_general, "Unbekannt")
    }

    fun saveResult(): String? {
        val state = _uiState.value
        val bitmap = state.resultBitmap ?: return null
        return try {
            ImageUtil.saveToMediaStore(appContext, bitmap, "OpenAgnes_${System.currentTimeMillis()}.jpg")
            _uiState.update { it.copy(actionMessage = resString(R.string.saved_media), error = null) }
            resString(R.string.saved_media)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: resString(R.string.error_general, "Speichern fehlgeschlagen")) }
            null
        }
    }

    fun saveVideo() {
        val state = _uiState.value
        val url = state.videoUrl ?: return
        if (state.videoSaving) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _uiState.update { it.copy(error = resString(R.string.video_save_unsupported)) }
            return
        }
        _uiState.update { it.copy(actionMessage = null, videoSaving = true, error = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        VideoUtil.saveVideo(appContext, url, "OpenAgnes_${System.currentTimeMillis()}.mp4")
                    }
                }
                _uiState.update { it.copy(videoSaving = false, actionMessage = resString(R.string.video_saved_media)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(videoSaving = false, error = e.message ?: resString(R.string.error_general, "Speichern fehlgeschlagen")) }
                Log.e("EditViewModel", "Video save failed", e)
            }
        }
    }

    private fun resString(id: Int, vararg args: Any): String =
        appContext.getString(id, *args)
}