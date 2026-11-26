package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.ui.screens.UserCreatedReel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Durable on-device storage for user-created Reels, copied gallery media,
 * Reel likes, and followed poets so user creations survive app restarts and
 * Android Photo Picker temporary URI permission expirations.
 */
object ReelsStorage {
    private const val REELS_FILE_NAME = "user_reels_v1.json"
    private const val MEDIA_DIR_NAME = "reels_media"
    private const val PREFS_NAME = "shayari_reels_state_v1"
    private const val KEY_LIKED_REEL_KEYS = "liked_reel_keys"
    private const val KEY_FOLLOWED_POETS = "followed_poets"
    private const val KEY_SAVED_CUSTOM_REELS = "saved_custom_reels"

    fun createCameraVideoTarget(context: Context): Pair<File, Uri>? {
        return runCatching {
            val appCtx = context.applicationContext
            val mediaDir = File(appCtx.filesDir, MEDIA_DIR_NAME).apply { mkdirs() }
            val file = File(mediaDir, "reel_cam_${System.currentTimeMillis()}.mp4")
            val contentUri = FileProvider.getUriForFile(
                appCtx,
                "${appCtx.packageName}.fileprovider",
                file
            )
            file to contentUri
        }.getOrNull()
    }

    suspend fun loadUserReels(context: Context): List<UserCreatedReel> = withContext(Dispatchers.IO) {
        val file = File(context.applicationContext.filesDir, REELS_FILE_NAME)
        if (!file.exists()) return@withContext emptyList()
        runCatching {
            val raw = file.readText()
            if (raw.isBlank()) return@runCatching emptyList()
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val couplet = obj.optString("couplet")
                    val poet = obj.optString("poet").ifBlank { "शायर दोस्त" }
                    val categoryId = obj.optString("categoryId")
                    val tagsArray = obj.optJSONArray("tags")
                    val tags = buildList {
                        if (tagsArray != null) {
                            for (t in 0 until tagsArray.length()) {
                                val tag = tagsArray.optString(t).trim()
                                if (tag.isNotBlank()) add(tag)
                            }
                        }
                    }
                    val videoUri = obj.optString("videoUri").takeIf { it.isNotBlank() && isAccessibleMediaUri(it) }
                    val photoUri = obj.optString("photoUri").takeIf { it.isNotBlank() && isAccessibleMediaUri(it) }
                    val backgroundDrawableName = obj.optString("backgroundDrawableName").takeIf { it.isNotBlank() }
                    val voicePersonaId = obj.optString("voicePersonaId").takeIf { it.isNotBlank() }
                    val bgmTrackId = obj.optString("bgmTrackId").takeIf { it.isNotBlank() }
                    val ownerUserId = obj.optString("ownerUserId").takeIf { it.isNotBlank() }
                    val mediaType = obj.optString("mediaType").takeIf { it.isNotBlank() }
                        ?: when {
                            videoUri != null -> "video"
                            photoUri != null -> "photo"
                            else -> "text"
                        }
                    val thumbnailBase64 = obj.optString("thumbnailBase64").takeIf { it.isNotBlank() }
                    val singerName = obj.optString("singerName").takeIf { it.isNotBlank() }
                    val songTitle = obj.optString("songTitle").takeIf { it.isNotBlank() }
                    val singerChecked = obj.optBoolean("singerChecked", false)
                    val isOnlineSynced = obj.optBoolean("isOnlineSynced", false)
                    val likeCount = obj.optInt("likeCount", 0)
                    val createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                    add(
                        UserCreatedReel(
                            id = id,
                            couplet = couplet,
                            poet = poet,
                            categoryId = categoryId,
                            tags = tags,
                            videoUri = videoUri,
                            photoUri = photoUri,
                            backgroundDrawableName = backgroundDrawableName,
                            voicePersonaId = voicePersonaId,
                            bgmTrackId = bgmTrackId,
                            ownerUserId = ownerUserId,
                            mediaType = mediaType,
                            thumbnailBase64 = thumbnailBase64,
                            singerName = singerName,
                            songTitle = songTitle,
                            singerChecked = singerChecked,
                            likeCount = likeCount,
                            isOnlineSynced = isOnlineSynced,
                            createdAtMs = createdAtMs
                        )
                    )
                }
            }.sortedByDescending { it.createdAtMs }
        }.getOrDefault(emptyList())
    }

    suspend fun saveUserReels(context: Context, reels: List<UserCreatedReel>) = withContext(Dispatchers.IO) {
        runCatching {
            val array = JSONArray()
            reels.forEach { reel ->
                val tagsJson = JSONArray()
                reel.tags.forEach { tagsJson.put(it) }
                val obj = JSONObject().apply {
                    put("id", reel.id)
                    put("couplet", reel.couplet)
                    put("poet", reel.poet)
                    put("categoryId", reel.categoryId)
                    put("tags", tagsJson)
                    put("videoUri", reel.videoUri.orEmpty())
                    put("photoUri", reel.photoUri.orEmpty())
                    put("backgroundDrawableName", reel.backgroundDrawableName.orEmpty())
                    put("voicePersonaId", reel.voicePersonaId.orEmpty())
                    put("bgmTrackId", reel.bgmTrackId.orEmpty())
                    put("ownerUserId", reel.ownerUserId.orEmpty())
                    put("mediaType", reel.mediaType)
                    put("thumbnailBase64", reel.thumbnailBase64.orEmpty())
                    put("singerName", reel.singerName.orEmpty())
                    put("songTitle", reel.songTitle.orEmpty())
                    put("singerChecked", reel.singerChecked)
                    put("isOnlineSynced", reel.isOnlineSynced)
                    put("likeCount", reel.likeCount)
                    put("createdAtMs", reel.createdAtMs)
                }
                array.put(obj)
            }
            val target = File(context.applicationContext.filesDir, REELS_FILE_NAME)
            val tmp = File(context.applicationContext.filesDir, "$REELS_FILE_NAME.tmp")
            tmp.writeText(array.toString())
            if (!tmp.renameTo(target)) {
                target.writeText(array.toString())
                tmp.delete()
            }
        }
    }

    /**
     * Extracts a lightweight compressed JPEG Base64 poster/photo (<= ~260 KB Base64)
     * so online Reels created from gallery photos or videos render visually across all users' devices
     * while staying comfortably under Firestore's 1 MiB document size limit.
     */
    suspend fun extractCompressedThumbnailBase64(
        context: Context,
        uriString: String?,
        isVideo: Boolean
    ): String? = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) return@withContext null
        runCatching {
            val appCtx = context.applicationContext
            val uri = Uri.parse(uriString)
            val rawBitmap: Bitmap? = if (isVideo) {
                val retriever = MediaMetadataRetriever()
                try {
                    if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                        retriever.setDataSource(uri.path)
                    } else {
                        retriever.setDataSource(appCtx, uri)
                    }
                    retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                } finally {
                    runCatching { retriever.release() }
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                openStreamForUri(appCtx, uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val maxSide = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
                var sampleSize = 1
                while (maxSide / sampleSize > 1080) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                openStreamForUri(appCtx, uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
            }
            val bitmap = rawBitmap ?: return@runCatching null
            val scaled = scaleBitmapDown(bitmap, maxDimPx = 540)
            if (scaled !== bitmap) {
                bitmap.recycle()
            }
            try {
                var quality = 74
                var encoded = ""
                while (quality >= 40) {
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    val candidate = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    encoded = candidate
                    if (candidate.length <= 260_000) break
                    quality -= 12
                }
                encoded.takeIf { it.isNotBlank() && it.length <= 600_000 }
            } finally {
                scaled.recycle()
            }
        }.getOrNull()
    }

    private fun openStreamForUri(context: Context, uri: Uri) =
        if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
            File(uri.path!!).takeIf { it.exists() }?.inputStream()
        } else {
            context.contentResolver.openInputStream(uri)
        }

    private fun scaleBitmapDown(source: Bitmap, maxDimPx: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDimPx && h <= maxDimPx) return source
        val ratio = maxDimPx.toFloat() / maxOf(w, h).toFloat()
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    /**
     * Copies a picked photo or video URI from the Android Photo Picker into private
     * app storage so playback continues working reliably after process death.
     */
    suspend fun persistPickedMedia(
        context: Context,
        sourceUri: Uri,
        isVideo: Boolean
    ): String? = withContext(Dispatchers.IO) {
        if (sourceUri.scheme == "file") {
            return@withContext sourceUri.toString()
        }
        runCatching {
            val appCtx = context.applicationContext
            val mediaDir = File(appCtx.filesDir, MEDIA_DIR_NAME).apply { mkdirs() }
            val mime = appCtx.contentResolver.getType(sourceUri)
            val ext = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mime)
                ?.takeIf { it.isNotBlank() }
                ?: if (isVideo) "mp4" else "jpg"
            val prefix = if (isVideo) "reel_vid_" else "reel_img_"
            val outFile = File(mediaDir, "$prefix${System.currentTimeMillis()}.$ext")
            appCtx.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output, bufferSize = 32 * 1024)
                }
            } ?: return@runCatching sourceUri.toString()
            if (outFile.exists() && outFile.length() > 0L) {
                Uri.fromFile(outFile).toString()
            } else {
                sourceUri.toString()
            }
        }.getOrElse { sourceUri.toString() }
    }

    suspend fun deleteLocalMediaIfOwned(context: Context, uriString: String?) = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) return@withContext
        runCatching {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val path = uri.path ?: return@runCatching
                val mediaDir = File(context.applicationContext.filesDir, MEDIA_DIR_NAME).canonicalPath
                val target = File(path)
                if (target.canonicalPath.startsWith(mediaDir) && target.exists()) {
                    target.delete()
                }
            }
        }
    }

    fun loadLikedReelKeys(context: Context): Set<String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_LIKED_REEL_KEYS, emptySet())?.toSet() ?: emptySet()
    }

    fun saveLikedReelKeys(context: Context, keys: Set<String>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_LIKED_REEL_KEYS, keys.toSet()).apply()
    }

    fun loadSavedCustomReelIds(context: Context): Set<String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SAVED_CUSTOM_REELS, emptySet())?.toSet() ?: emptySet()
    }

    fun saveSavedCustomReelIds(context: Context, ids: Set<String>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_SAVED_CUSTOM_REELS, ids.toSet()).apply()
    }

    private const val KEY_PER_REEL_VOICE_MAP = "per_reel_voice_map_v1"
    private const val KEY_PER_REEL_BGM_MAP = "per_reel_bgm_map_v1"
    private const val KEY_ALLOW_LOCAL_TTS_FALLBACK = "allow_local_tts_fallback_v1"

    fun loadAllowLocalTtsFallback(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ALLOW_LOCAL_TTS_FALLBACK, true)
    }

    fun saveAllowLocalTtsFallback(context: Context, enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ALLOW_LOCAL_TTS_FALLBACK, enabled).apply()
    }

    fun loadFollowedPoets(context: Context): Set<String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_FOLLOWED_POETS, emptySet())?.toSet() ?: emptySet()
    }

    fun saveFollowedPoets(context: Context, poets: Set<String>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_FOLLOWED_POETS, poets.toSet()).apply()
    }

    fun loadPerReelVoiceOverrides(context: Context): Map<String, String> {
        return loadStringMap(context, KEY_PER_REEL_VOICE_MAP)
    }

    fun savePerReelVoiceOverrides(context: Context, map: Map<String, String>) {
        saveStringMap(context, KEY_PER_REEL_VOICE_MAP, map)
    }

    fun loadPerReelBgmOverrides(context: Context): Map<String, String> {
        return loadStringMap(context, KEY_PER_REEL_BGM_MAP)
    }

    fun savePerReelBgmOverrides(context: Context, map: Map<String, String>) {
        saveStringMap(context, KEY_PER_REEL_BGM_MAP, map)
    }

    private fun loadStringMap(context: Context, key: String): Map<String, String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, null)?.takeIf { it.isNotBlank() } ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = json.optString(k)
                    if (k.isNotBlank() && v.isNotBlank()) put(k, v)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun saveStringMap(context: Context, key: String, map: Map<String, String>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString(key, json.toString()).apply()
    }

    private fun isAccessibleMediaUri(uriString: String): Boolean {
        val parsed = runCatching { Uri.parse(uriString) }.getOrNull() ?: return false
        return if (parsed.scheme == "file") {
            val path = parsed.path ?: return false
            File(path).exists()
        } else {
            true
        }
    }
}
