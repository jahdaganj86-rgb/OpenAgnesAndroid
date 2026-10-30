package com.openagnes.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object VideoUtil {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveVideo(context: Context, url: String, title: String): Uri {
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Video download failed: ${response.code}")
        }
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, title)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/OpenAgnes")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values)
            ?: throw IOException("Video MediaStore insert returned null")
        try {
            val body = response.body ?: throw IOException("Empty video response")
            resolver.openOutputStream(uri, "w")?.use { out ->
                body.byteStream().use { input -> input.copyTo(out) }
            } ?: throw IOException("Cannot open video output stream")
            val pending = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(uri, pending, null, null)
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        } finally {
            response.close()
        }
    }
}