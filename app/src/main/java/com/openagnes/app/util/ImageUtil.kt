package com.openagnes.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt

object ImageUtil {
    const val MAX_EDGE = 2048
    const val JPEG_QUALITY = 85

    fun loadBitmap(context: Context, uri: Uri): Bitmap {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open image URI: $uri")
        return input.use { BitmapFactory.decodeStream(it) }
    }

    fun scaleDown(bitmap: Bitmap, maxEdge: Int = MAX_EDGE): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val max = maxOf(width, height).coerceAtLeast(1)
        val scale = maxEdge.toFloat() / max
        return if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (width * scale).roundToInt(),
                (height * scale).roundToInt(),
                true
            )
        } else bitmap
    }

    fun bitmapToDataUri(bitmap: Bitmap, quality: Int = JPEG_QUALITY): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        val base64 = Base64.encodeToString(output.toByteArray(), Base64.DEFAULT)
        return "data:image/jpeg;base64,$base64"
    }

    fun dataUriToBitmap(dataUri: String): Bitmap {
        val base64 = dataUri.substringAfter("base64,")
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    fun detectRatio(width: Int, height: Int): String {
        val ar = width.toDouble() / height.coerceAtLeast(1)
        val ratios = mapOf(
            "1:1" to 1.0,
            "3:4" to 0.75,
            "4:3" to 1.3333,
            "16:9" to 1.7778,
            "9:16" to 0.5625,
            "2:3" to 0.6667,
            "3:2" to 1.5,
            "21:9" to 2.3333
        )
        return ratios.entries.minBy { abs(it.value - ar) }!!.key
    }

    fun saveToMediaStore(context: Context, bitmap: Bitmap, title: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, title)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("MediaStore insert returned null")
        context.contentResolver.openOutputStream(uri, "w")?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return uri
    }
}
