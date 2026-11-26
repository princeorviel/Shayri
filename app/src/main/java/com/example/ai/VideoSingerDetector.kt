package com.example.ai

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "VideoSingerDetector"
private const val SINGER_MODEL_NAME = "gemini-3.8-flash"
private const val MAX_AUDIO_SNIPPET_US = 22_000_000L
private const val MAX_AUDIO_BYTES = 2 * 1024 * 1024
private const val MAX_RAW_VIDEO_FALLBACK_BYTES = 6 * 1024 * 1024

data class DetectedVideoSinger(
    val singerName: String? = null,
    val songTitle: String? = null,
    val source: String = "none"
)

private data class LocalVideoClues(
    val artistMetadata: String? = null,
    val titleMetadata: String? = null,
    val displayFileName: String? = null,
    val knownSingerFromText: String? = null
)

/**
 * Automatically identifies the playback singer / vocalist singing in a video Reel
 * using a fast 3-layer pipeline:
 * 1. Instant local video metadata (MP4/ID3 Artist/Author/Title) & caption/filename singer matching.
 * 2. Compact audio-track extraction (`MediaExtractor` + `MediaMuxer` -> tiny ~300KB M4A snippet).
 * 3. Multimodal Gemini analysis via Firebase AI Logic (`gemini-3.8-flash`) with structured JSON output.
 *
 * Strictly rejects uploader usernames (such as "Prince") or generic audio placeholders so videos
 * sung by artists like Kishore Kumar, Lata Mangeshkar, Arijit Singh, etc. always credit the singer.
 */
object VideoSingerDetector {

    private val resultCache = ConcurrentHashMap<String, DetectedVideoSinger>()

    private val singerSchema = Schema.obj(
        properties = mapOf(
            "hasSinger" to Schema.boolean(
                description = "True only if a singer/vocalist is singing a song in the audio/video."
            ),
            "singerName" to Schema.string(
                description = "Name of the actual playback singer or vocalist who sang the song (prefer Hindi Devanagari for Indian singers, e.g. 'किशोर कुमार', 'मोहम्मद रफ़ी', 'लता मंगेशकर', 'अरिजीत सिंह'). Empty string if no singer or cannot be identified."
            ),
            "songTitle" to Schema.string(
                description = "Name of the song if clearly recognizable, otherwise empty string."
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = SINGER_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.1f
                responseMimeType = "application/json"
                responseSchema = singerSchema
            },
            systemInstruction = content {
                text(
                    """
                    You are an expert Indian and world music audio recognition assistant.
                    Your task is to listen carefully to the provided audio/video clip (and consider any optional filename/metadata/caption clues) and identify the real playback singer(s) singing the song.

                    Rules:
                    1. If a real song is being sung by a recognizable singer (for example: किशोर कुमार / Kishore Kumar, मोहम्मद रफ़ी / Mohammed Rafi, लता मंगेशकर / Lata Mangeshkar, मुकेश / Mukesh, आशा भोसले / Asha Bhosle, कुमार सानू / Kumar Sanu, उदित नारायण / Udit Narayan, अलका याग्निक / Alka Yagnik, सोनू निगम / Sonu Nigam, श्रेया घोषाल / Shreya Ghoshal, अरिजीत सिंह / Arijit Singh, केके / KK, आतिफ़ असलम / Atif Aslam, जुबिन नौटियाल / Jubin Nautiyal, जगजीत सिंह / Jagjit Singh, राहत फ़तेह अली ख़ान, नुसरत फ़तेह अली ख़ान, दिलजीत दोसांझ, सिद्धू मूसेवाला, बी प्राक, विशाल मिश्रा, etc., or any regional/international singer), set hasSinger=true and return their clean name in singerName.
                    2. For Indian/Hindi/Urdu/Punjabi singers, write the singer's well-known name in clean Hindi Devanagari script (e.g. "किशोर कुमार", "अरिजीत सिंह", "लता मंगेशकर", "मोहम्मद रफ़ी"). For international English singers, write their standard name (e.g. "Ed Sheeran").
                    3. If two singers sing a duet, return up to two names separated by " & " (max 50 characters).
                    4. NEVER return the video uploader's name, social media handle, app name, or words like "Prince", "Original Audio", "Unknown", "User", "Shayar Dost", "WhatsApp", or "Instagram".
                    5. If the audio is NOT a song (e.g., comedy clip, cat/animal sound, speech/dialogue, silent video, pure instrumental with no singer, or unidentifiable background noise), set hasSinger=false, singerName="", songTitle="".
                    6. Only return a singerName when you recognize the song or voice with genuine confidence; do not guess randomly.
                    """.trimIndent()
                )
            }
        )
    }

    private val EXPLICIT_SINGER_LABEL_REGEX = Regex(
        pattern = """(?i)(?:singer|vocal(?:ist)?|sung\s*by|voice\s*by|गायक|सिंगर|आवाज़|आवाज)\s*[:\-–—]\s*([^\n,|#•]{2,45})"""
    )

    // Curated canonical Indian & Bollywood/Ghazal/Sufi/Punjabi singers (lowercase alias -> Devanagari display name)
    private val KNOWN_SINGER_ALIASES: List<Pair<String, String>> = listOf(
        "kishore kumar" to "किशोर कुमार",
        "किशोर कुमार" to "किशोर कुमार",
        "kishore da" to "किशोर कुमार",
        "किशोर दा" to "किशोर कुमार",
        "mohammed rafi" to "मोहम्मद रफ़ी",
        "mohammad rafi" to "मोहम्मद रफ़ी",
        "mohd rafi" to "मोहम्मद रफ़ी",
        "mohd. rafi" to "मोहम्मद रफ़ी",
        "मोहम्मद रफी" to "मोहम्मद रफ़ी",
        "मोहम्मद रफ़ी" to "मोहम्मद रफ़ी",
        "lata mangeshkar" to "लता मंगेशकर",
        "लता मंगेशकर" to "लता मंगेशकर",
        "asha bhosle" to "आशा भोसले",
        "आशा भोसले" to "आशा भोसले",
        "mukesh" to "मुकेश",
        "मुकेश" to "मुकेश",
        "manna dey" to "मन्ना डे",
        "मन्ना डे" to "मन्ना डे",
        "hemant kumar" to "हेमंत कुमार",
        "हेमंत कुमार" to "हेमंत कुमार",
        "talat mahmood" to "तलत महमूद",
        "तलत महमूद" to "तलत महमूद",
        "arijit singh" to "अरिजीत सिंह",
        "अरिजीत सिंह" to "अरिजीत सिंह",
        "अरिजीत" to "अरिजीत सिंह",
        "kumar sanu" to "कुमार सानू",
        "कुमार सानू" to "कुमार सानू",
        "udit narayan" to "उदित नारायण",
        "उदित नारायण" to "उदित नारायण",
        "alka yagnik" to "अलका याग्निक",
        "अलका याग्निक" to "अलका याग्निक",
        "sonu nigam" to "सोनू निगम",
        "सोनू निगम" to "सोनू निगम",
        "shreya ghoshal" to "श्रेया घोषाल",
        "श्रेया घोषाल" to "श्रेया घोषाल",
        "atif aslam" to "आतिफ़ असलम",
        "आतिफ असलम" to "आतिफ़ असलम",
        "आतिफ़ असलम" to "आतिफ़ असलम",
        "jubin nautiyal" to "जुबिन नौटियाल",
        "जुबिन नौटियाल" to "जुबिन नौटियाल",
        "jagjit singh" to "जगजीत सिंह",
        "जगजीत सिंह" to "जगजीत सिंह",
        "pankaj udhas" to "पंकज उधास",
        "पंकज उधास" to "पंकज उधास",
        "rahat fateh ali khan" to "राहत फ़तेह अली ख़ान",
        "राहत फतेह अली खान" to "राहत फ़तेह अली ख़ान",
        "राहत फ़तेह अली ख़ान" to "राहत फ़तेह अली ख़ान",
        "nusrat fateh ali khan" to "नुसरत फ़तेह अली ख़ान",
        "नुसरत फतेह अली खान" to "नुसरत फ़तेह अली ख़ान",
        "नुसरत फ़तेह अली ख़ान" to "नुसरत फ़तेह अली ख़ान",
        "a. r. rahman" to "ए. आर. रहमान",
        "a.r. rahman" to "ए. आर. रहमान",
        "ar rahman" to "ए. आर. रहमान",
        "ए आर रहमान" to "ए. आर. रहमान",
        "mohit chauhan" to "मोहित चौहान",
        "मोहित चौहान" to "मोहित चौहान",
        "ankit tiwari" to "अंकित तिवारी",
        "अंकित तिवारी" to "अंकित तिवारी",
        "vishal mishra" to "विशाल मिश्रा",
        "विशाल मिश्रा" to "विशाल मिश्रा",
        "darshan raval" to "दर्शन रावल",
        "दर्शन रावल" to "दर्शन रावल",
        "armaan malik" to "अरमान मलिक",
        "अरमान मलिक" to "अरमान मलिक",
        "sunidhi chauhan" to "सुनिधि चौहान",
        "सुनिधि चौहान" to "सुनिधि चौहान",
        "neha kakkar" to "नेहा कक्कड़",
        "नेहा कक्कड़" to "नेहा कक्कड़",
        "kailash kher" to "कैलाश खेर",
        "कैलाश खेर" to "कैलाश खेर",
        "sukhwinder singh" to "सुखविंदर सिंह",
        "सुखविंदर सिंह" to "सुखविंदर सिंह",
        "javed ali" to "जावेद अली",
        "जावेद अली" to "जावेद अली",
        "hariharan" to "हरिहरन",
        "हरिहरन" to "हरिहरन",
        "shankar mahadevan" to "शंकर महादेवन",
        "शंकर महादेवन" to "शंकर महादेवन",
        "kavita krishnamurthy" to "कविता कृष्णमूर्ति",
        "कविता कृष्णमूर्ति" to "कविता कृष्णमूर्ति",
        "anuradha paudwal" to "अनुराधा पौडवाल",
        "अनुराधा पौडवाल" to "अनुराधा पौडवाल",
        "sadhana sargam" to "साधना सरगम",
        "साधना सरगम" to "साधना सरगम",
        "palak muchhal" to "पलक मुच्छल",
        "पलक मुच्छल" to "पलक मुच्छल",
        "tulsi kumar" to "तुलसी कुमार",
        "तुलसी कुमार" to "तुलसी कुमार",
        "monali thakur" to "मोनाली ठाकुर",
        "मोनाली ठाकुर" to "मोनाली ठाकुर",
        "b praak" to "बी प्राक",
        "बी प्राक" to "बी प्राक",
        "sachet tandon" to "सचेत टंडन",
        "सचेत टंडन" to "सचेत टंडन",
        "hansraj raghuwanshi" to "हंसराज रघुवंशी",
        "हंसराज रघुवंशी" to "हंसराज रघुवंशी",
        "diljit dosanjh" to "दिलजीत दोसांझ",
        "दिलजीत दोसांझ" to "दिलजीत दोसांझ",
        "sidhu moose wala" to "सिद्धू मूसेवाला",
        "sidhu moosewala" to "सिद्धू मूसेवाला",
        "सिद्धू मूसेवाला" to "सिद्धू मूसेवाला",
        "yo yo honey singh" to "हनी सिंह",
        "honey singh" to "हनी सिंह",
        "हनी सिंह" to "हनी सिंह",
        "badshah" to "बादशाह",
        "बादशाह" to "बादशाह",
        "ap dhillon" to "एपी ढिल्लों",
        "karan aujla" to "करण औजला",
        "guru randhawa" to "गुरु रंधावा",
        "गुरु रंधावा" to "गुरु रंधावा",
        "harrdy sandhu" to "हार्डी संधू",
        "adnan sami" to "अदनान सामी",
        "अदनान सामी" to "अदनान सामी",
        "lucky ali" to "लकी अली",
        "लकी अली" to "लकी अली",
        "abhijeet bhattacharya" to "अभिजीत भट्टाचार्य",
        "ghulam ali" to "ग़ुलाम अली",
        "ग़ुलाम अली" to "ग़ुलाम अली",
        "गुलाम अली" to "ग़ुलाम अली",
        "mehdi hassan" to "मेहदी हसन",
        "मेहदी हसन" to "मेहदी हसन",
        "sp balasubrahmanyam" to "एस. पी. बालासुब्रमण्यम",
        "s.p. balasubrahmanyam" to "एस. पी. बालासुब्रमण्यम",
        "k.k." to "केके (KK)",
        "singer kk" to "केके (KK)",
        "kk songs" to "केके (KK)",
        "shaan" to "शान",
        "pawan singh" to "पवन सिंह",
        "पवन सिंह" to "पवन सिंह",
        "khesari lal yadav" to "खेसारी लाल यादव",
        "खेसारी लाल यादव" to "खेसारी लाल यादव",
        "maithili thakur" to "मैथिली ठाकुर",
        "मैथिली ठाकुर" to "मैथिली ठाकुर",
        "sharda sinha" to "शारदा सिन्हा",
        "शारदा सिन्हा" to "शारदा सिन्हा"
    )

    private val BLOCKED_GENERIC_NAMES = setOf(
        "unknown",
        "unknown artist",
        "none",
        "null",
        "n/a",
        "na",
        "original audio",
        "original sound",
        "audio",
        "video",
        "reels",
        "reel",
        "whatsapp",
        "whatsapp video",
        "instagram",
        "camera",
        "android",
        "user",
        "creator",
        "shayar",
        "shayar dost",
        "शायर दोस्त",
        "शायरी महफ़िल",
        "prince",
        "prince orviel",
        "princeorviel",
        "prince_shayar"
    )

    fun extractKnownSingerFromText(text: String?, uploaderName: String? = null): String? {
        if (text.isNullOrBlank()) return null

        // 1. Explicit label like "Singer: Kishore Kumar" or "गायक: किशोर कुमार"
        EXPLICIT_SINGER_LABEL_REGEX.find(text)?.groupValues?.getOrNull(1)?.let { explicit ->
            val sanitized = sanitizeSingerName(explicit, uploaderName)
            if (sanitized != null) return sanitized
        }

        // 2. Known playback singer alias match (case-insensitive word/phrase boundary safe)
        val normalized = text.lowercase()
            .replace('_', ' ')
            .replace('-', ' ')
            .replace('#', ' ')
        for ((alias, canonical) in KNOWN_SINGER_ALIASES) {
            if (containsAliasBoundarySafe(normalized, alias)) {
                return canonical
            }
        }
        return null
    }

    private fun containsAliasBoundarySafe(haystack: String, alias: String): Boolean {
        val idx = haystack.indexOf(alias)
        if (idx < 0) return false
        val beforeOk = idx == 0 || !haystack[idx - 1].isLetterOrDigit()
        val endIdx = idx + alias.length
        val afterOk = endIdx >= haystack.length || !haystack[endIdx].isLetterOrDigit()
        return beforeOk && afterOk
    }

    fun sanitizeSingerName(raw: String?, uploaderName: String? = null): String? {
        val cleaned = raw
            ?.trim()
            ?.trim('"', '\'', '“', '”', '•', '-', '—', ':', '|', ' ')
            ?.replace(Regex("\\s+"), " ")
            ?.take(60)
            ?: return null
        if (cleaned.length < 2) return null

        val lower = cleaned.lowercase()
        if (lower in BLOCKED_GENERIC_NAMES) return null
        if (lower.startsWith("vid_") || lower.startsWith("img_") || lower.startsWith("pxl_") || lower.startsWith("reel_")) {
            return null
        }
        if (lower.contains("whatsapp") || lower.contains("instagram") || lower.contains("screen_record")) {
            return null
        }

        // Normalize known aliases to canonical clean Hindi name if matched
        for ((alias, canonical) in KNOWN_SINGER_ALIASES) {
            if (lower == alias) return canonical
        }

        val cleanUploader = uploaderName?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        if (cleanUploader != null && (lower == cleanUploader || lower.contains("prince"))) {
            return null
        }

        return cleaned
    }

    fun sanitizeSongTitle(raw: String?): String? {
        val cleaned = raw
            ?.trim()
            ?.trim('"', '\'', '“', '”', '•', '-', '—', ' ')
            ?.replace(Regex("\\s+"), " ")
            ?.take(80)
            ?: return null
        if (cleaned.length < 2) return null
        val lower = cleaned.lowercase()
        if (lower in BLOCKED_GENERIC_NAMES) return null
        return cleaned
    }

    suspend fun detectSinger(
        context: Context,
        videoUri: Uri?,
        caption: String = "",
        uploaderName: String? = null,
        forceRefresh: Boolean = false
    ): DetectedVideoSinger = withContext(Dispatchers.IO) {
        val cacheKey = "${videoUri?.toString().orEmpty()}|${caption.trim().take(120)}"
        if (!forceRefresh) {
            resultCache[cacheKey]?.let { return@withContext it }
        }

        val appCtx = context.applicationContext
        val captionSinger = extractKnownSingerFromText(caption, uploaderName)
        val localClues = if (videoUri != null) {
            extractLocalMetadataClues(appCtx, videoUri, uploaderName)
        } else {
            LocalVideoClues()
        }

        val instantLocalSinger = captionSinger
            ?: localClues.knownSingerFromText
            ?: sanitizeSingerName(localClues.artistMetadata, uploaderName)

        // If we have a video URI, attempt real Gemini multimodal audio analysis so we accurately
        // identify the singer even when filename/caption/metadata have zero hints!
        val aiDetected = if (videoUri != null) {
            detectSingerWithGeminiAudio(
                context = appCtx,
                videoUri = videoUri,
                caption = caption,
                clues = localClues,
                uploaderName = uploaderName
            )
        } else {
            null
        }

        val finalResult = when {
            aiDetected != null && !aiDetected.singerName.isNullOrBlank() -> aiDetected
            !instantLocalSinger.isNullOrBlank() -> DetectedVideoSinger(
                singerName = instantLocalSinger,
                songTitle = aiDetected?.songTitle ?: sanitizeSongTitle(localClues.titleMetadata),
                source = if (captionSinger != null) "caption" else "metadata"
            )
            else -> DetectedVideoSinger(
                singerName = null,
                songTitle = aiDetected?.songTitle,
                source = "none"
            )
        }

        resultCache[cacheKey] = finalResult
        finalResult
    }

    private fun extractLocalMetadataClues(
        context: Context,
        videoUri: Uri,
        uploaderName: String?
    ): LocalVideoClues {
        var artist: String? = null
        var title: String? = null
        var displayName: String? = null

        runCatching {
            if (videoUri.scheme == "file" && !videoUri.path.isNullOrBlank()) {
                displayName = File(videoUri.path!!).nameWithoutExtension
            } else {
                context.contentResolver.query(
                    videoUri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            displayName = cursor.getString(idx)?.substringBeforeLast('.')
                        }
                    }
                }
            }
        }

        val retriever = MediaMetadataRetriever()
        try {
            if (videoUri.scheme == "file" && !videoUri.path.isNullOrBlank()) {
                retriever.setDataSource(videoUri.path)
            } else {
                retriever.setDataSource(context, videoUri)
            }
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    ?.takeIf { it.isNotBlank() }
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                    ?.takeIf { it.isNotBlank() }
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
                    ?.takeIf { it.isNotBlank() }

            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.d(TAG, "Metadata extraction skipped: ${e.message}")
        } finally {
            runCatching { retriever.release() }
        }

        val combinedText = listOfNotNull(artist, title, displayName).joinToString(" ")
        val knownSinger = extractKnownSingerFromText(combinedText, uploaderName)

        return LocalVideoClues(
            artistMetadata = sanitizeSingerName(artist, uploaderName),
            titleMetadata = sanitizeSongTitle(title),
            displayFileName = displayName?.take(120),
            knownSingerFromText = knownSinger
        )
    }

    private suspend fun detectSingerWithGeminiAudio(
        context: Context,
        videoUri: Uri,
        caption: String,
        clues: LocalVideoClues,
        uploaderName: String?
    ): DetectedVideoSinger? {
        val mediaSnippet = extractCompactAudioSnippet(context, videoUri)
            ?: readCompactRawVideoFallback(context, videoUri)
            ?: return null

        val (mediaBytes, mimeType) = mediaSnippet
        if (mediaBytes.size < 2_048) return null

        val hintParts = buildList {
            if (caption.isNotBlank()) add("Video caption: ${caption.trim().take(200)}")
            if (!clues.titleMetadata.isNullOrBlank()) add("Embedded title metadata: ${clues.titleMetadata}")
            if (!clues.artistMetadata.isNullOrBlank()) add("Embedded artist metadata: ${clues.artistMetadata}")
            if (!clues.displayFileName.isNullOrBlank()) add("Filename hint: ${clues.displayFileName}")
        }
        val promptText = buildString {
            append("Listen to this clip and identify the real playback singer who is singing the song (for example Kishore Kumar / किशोर कुमार, Lata Mangeshkar, Mohammed Rafi, Arijit Singh, Kumar Sanu, etc.). ")
            append("If no recognizable singer is singing a song (e.g. funny sound, dialogue, silent, or random noise), set hasSinger=false and singerName=\"\". ")
            if (!uploaderName.isNullOrBlank()) {
                append("Do NOT return the uploader name '$uploaderName' or 'Prince'. ")
            }
            if (hintParts.isNotEmpty()) {
                append("\nOptional hints:\n")
                append(hintParts.joinToString("\n"))
            }
        }

        return try {
            val response = withTimeout(45_000L) {
                model.generateContent(
                    content {
                        inlineData(mediaBytes, mimeType)
                        text(promptText)
                    }
                )
            }
            val rawJson = response.text?.trim() ?: return null
            val json = JSONObject(rawJson)
            val hasSinger = json.optBoolean("hasSinger", false)
            val singer = if (hasSinger) {
                sanitizeSingerName(json.optString("singerName"), uploaderName)
            } else {
                null
            }
            val songTitle = sanitizeSongTitle(json.optString("songTitle"))
            Log.d(TAG, "Gemini singer detection result: hasSinger=$hasSinger singer=$singer song=$songTitle")
            DetectedVideoSinger(
                singerName = singer,
                songTitle = songTitle,
                source = if (singer != null) "gemini_audio" else "gemini_none"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Gemini singer detection timed out", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Gemini singer detection failed: ${e.localizedMessage}", e)
            null
        }
    }

    /**
     * Extracts ONLY the encoded audio track (up to ~22s / <= 2MB) from any video URI into a tiny
     * M4A (`audio/mp4`) byte array using Android's native `MediaExtractor` + `MediaMuxer`.
     */
    private fun extractCompactAudioSnippet(
        context: Context,
        videoUri: Uri
    ): Pair<ByteArray, String>? {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        val tempFile = File.createTempFile("reel_singer_audio_", ".m4a", context.cacheDir)
        return try {
            if (videoUri.scheme == "file" && !videoUri.path.isNullOrBlank()) {
                extractor.setDataSource(videoUri.path!!)
            } else {
                extractor.setDataSource(context, videoUri, null)
            }

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/", ignoreCase = true)) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrackIndex < 0 || audioFormat == null) return null

            val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                runCatching { audioFormat.getLong(MediaFormat.KEY_DURATION) }.getOrDefault(0L)
            } else {
                0L
            }

            extractor.selectTrack(audioTrackIndex)
            // Skip first 2 seconds on longer videos so music intros don't hide vocals
            val startUs = if (durationUs > 14_000_000L) 2_000_000L else 0L
            if (startUs > 0L) {
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val dstTrack = muxer.addTrack(audioFormat)
            muxer.start()
            muxerStarted = true

            val maxInputSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                runCatching { audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) }
                    .getOrDefault(256 * 1024)
                    .coerceIn(64 * 1024, 1024 * 1024)
            } else {
                256 * 1024
            }
            val buffer = ByteBuffer.allocate(maxInputSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var firstSampleTimeUs = -1L
            var totalBytes = 0
            while (true) {
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs < 0L) break
                if (firstSampleTimeUs < 0L) firstSampleTimeUs = sampleTimeUs
                if (sampleTimeUs - firstSampleTimeUs > MAX_AUDIO_SNIPPET_US) break
                if (totalBytes + sampleSize > MAX_AUDIO_BYTES) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = (sampleTimeUs - firstSampleTimeUs).coerceAtLeast(0L)
                @Suppress("DEPRECATION")
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(dstTrack, buffer, bufferInfo)
                totalBytes += sampleSize
                extractor.advance()
            }

            if (muxerStarted) {
                muxer.stop()
                muxerStarted = false
            }
            muxer.release()
            muxer = null

            if (totalBytes > 1_024 && tempFile.exists() && tempFile.length() in 1_024L..MAX_AUDIO_BYTES.toLong()) {
                tempFile.readBytes() to "audio/mp4"
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Audio track extraction fallback triggered: ${e.message}")
            null
        } finally {
            runCatching {
                if (muxerStarted) muxer?.stop()
                muxer?.release()
            }
            runCatching { extractor.release() }
            runCatching { tempFile.delete() }
        }
    }

    private fun readCompactRawVideoFallback(
        context: Context,
        videoUri: Uri
    ): Pair<ByteArray, String>? {
        return runCatching {
            val stream = if (videoUri.scheme == "file" && !videoUri.path.isNullOrBlank()) {
                val f = File(videoUri.path!!)
                if (!f.exists() || f.length() <= 0L || f.length() > MAX_RAW_VIDEO_FALLBACK_BYTES) {
                    return null
                }
                f.inputStream()
            } else {
                context.contentResolver.openInputStream(videoUri) ?: return null
            }
            stream.use { input ->
                val bytes = input.readBytes()
                if (bytes.size in 4_096..MAX_RAW_VIDEO_FALLBACK_BYTES) {
                    val mime = context.contentResolver.getType(videoUri)
                        ?.takeIf { it.startsWith("video/") }
                        ?: "video/mp4"
                    bytes to mime
                } else {
                    null
                }
            }
        }.getOrNull()
    }
}
