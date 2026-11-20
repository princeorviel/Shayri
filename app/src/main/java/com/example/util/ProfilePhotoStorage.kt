package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProfilePhotoStorage {

    private const val FILE_PREFIX = "profile_avatar_"
    private const val FILE_EXT = ".jpg"
    private const val MAX_DIMENSION_PX = 512

    suspend fun savePickedProfilePhoto(
        context: Context,
        sourceUri: Uri
    ): String? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val decoded = decodeDownsampledBitmap(appContext, sourceUri) ?: return@withContext null
        val scaled = scaleBitmapIfNeeded(decoded)
        if (scaled !== decoded) {
            decoded.recycle()
        }

        val targetFile = File(appContext.filesDir, "$FILE_PREFIX${System.currentTimeMillis()}$FILE_EXT")
        val saved = runCatching {
            FileOutputStream(targetFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
        }.getOrDefault(false)
        scaled.recycle()

        if (!saved || !targetFile.exists()) {
            targetFile.delete()
            return@withContext null
        }

        // Remove older avatar files so internal storage stays clean and Coil always loads the fresh image.
        appContext.filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith(FILE_PREFIX) && file.name != targetFile.name) {
                runCatching { file.delete() }
            }
        }

        Uri.fromFile(targetFile).toString()
    }

    suspend fun clearSavedProfilePhotos(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        appContext.filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith(FILE_PREFIX)) {
                runCatching { file.delete() }
            }
        }
    }

    fun resolveDisplayUri(
        storedPhotoUri: String?,
        fallbackRemoteUri: Uri? = null
    ): Uri? {
        val trimmed = storedPhotoUri?.trim().orEmpty()
        if (trimmed.isNotEmpty()) {
            val parsed = runCatching { Uri.parse(trimmed) }.getOrNull()
            if (parsed != null) {
                if (parsed.scheme == "file") {
                    val path = parsed.path.orEmpty()
                    if (path.isNotBlank() && File(path).exists()) {
                        return parsed
                    }
                } else if (parsed.scheme.isNullOrBlank()) {
                    val file = File(trimmed)
                    if (file.exists()) {
                        return Uri.fromFile(file)
                    }
                } else {
                    return parsed
                }
            }
        }
        return fallbackRemoteUri
    }

    private fun decodeDownsampledBitmap(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sampleSize = 1
            val longestEdge = max(bounds.outWidth, bounds.outHeight)
            while (longestEdge / (sampleSize * 2) >= MAX_DIMENSION_PX) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        }.getOrNull()
    }

    private fun scaleBitmapIfNeeded(source: Bitmap): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= MAX_DIMENSION_PX) return source
        val ratio = MAX_DIMENSION_PX.toFloat() / longest.toFloat()
        val targetW = (source.width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (source.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }
}
