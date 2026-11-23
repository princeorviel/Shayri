package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.model.ShayariCategory
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.generationConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "ShayarVoiceEngine"
private const val VOICE_CACHE_DIR = "shayar_voices_v3"
private const val PRIMARY_TTS_MODEL = "gemini-3.1-flash-tts-preview"
private const val QUOTA_COOLDOWN_MS = 45_000L

enum class ShayarVoicePersona(
    val id: String,
    val labelHi: String,
    val subtitleHi: String,
    val genderBadgeHi: String,
    val isFemale: Boolean,
    val emoji: String,
    val geminiVoiceName: String,
    val voiceDirection: String,
    val emotionalStyleCue: String
) {
    // 4 Expressive Female Shayara (लड़की शायर) Voices
    DARD_SHAYARA(
        id = "dard_shayara",
        labelHi = "दर्द भरी शायरा",
        subtitleHi = "भीगी, टूटे दिल की दर्द भरी आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "💔",
        geminiVoiceName = "Kore",
        voiceDirection = "heartbroken Indian woman reciting sad Hindi-Urdu shayari in a tender, aching, slightly trembling voice with slow pauses",
        emotionalStyleCue = "deep heartbreak, fragile tearful breathiness"
    ),
    SUREELI_SHAYARA(
        id = "sureeli_shayara",
        labelHi = "सुरीली शायरा",
        subtitleHi = "मीठी, सुरीली व दिलकश आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🎶",
        geminiVoiceName = "Aoede",
        voiceDirection = "melodious, sweet-voiced Indian poetess with silky lyrical lilt and graceful Hindi-Urdu pronunciation",
        emotionalStyleCue = "musical poetic cadence, sweet warmth"
    ),
    TADAP_SHAYARA(
        id = "tadap_shayara",
        labelHi = "प्यार में तड़पती आवाज़",
        subtitleHi = "इंतज़ार और तड़प भरी नाज़ुक आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🥺",
        geminiVoiceName = "Leda",
        voiceDirection = "soft, yearning young woman aching with intense love and separation (tadap), whispered intimacy",
        emotionalStyleCue = "desperate romantic longing, tender quivering pauses"
    ),
    ROMANTIC_SHAYARA(
        id = "romantic_shayara",
        labelHi = "रोमांटिक शायरा",
        subtitleHi = "प्यार भरी, ख़्वाब सी रोमांटिक आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🌹",
        geminiVoiceName = "Zephyr",
        voiceDirection = "dreamy, warm romantic young Indian woman whispering love poetry with velvet tenderness",
        emotionalStyleCue = "dreamy romantic intimacy, affectionate softness"
    ),

    // 4 Expressive Male Shayar (शायर) Voices
    DARD_SHAYAR(
        id = "dard_shayar",
        labelHi = "दर्द-ए-दिल शायर",
        subtitleHi = "गहरी दर्द और खामोशी भरी आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🥀",
        geminiVoiceName = "Orus",
        voiceDirection = "deep, melancholic, heartbroken male Indian Urdu ghazal shayar with quiet pain and lingering pauses",
        emotionalStyleCue = "profound sadness, heavy emotional silence"
    ),
    TADAP_AASHIQ(
        id = "tadap_aashiq",
        labelHi = "तड़पता आशिक़",
        subtitleHi = "इश्क़ की तड़प और जुनून भरी आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "❤️‍🔥",
        geminiVoiceName = "Enceladus",
        voiceDirection = "passionate, intensely yearning young Indian lover reciting romantic separation shayari with raw heartfelt ache",
        emotionalStyleCue = "passionate romantic longing, heartfelt confession"
    ),
    MEHFIL_SHAYAR(
        id = "mehfil_shayar",
        labelHi = "महफ़िल शायर",
        subtitleHi = "रेशमी, गहरी मुशायरा शायर आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🎙️",
        geminiVoiceName = "Charon",
        voiceDirection = "classic deep velvety male Indian mushaira poet baritone performing live on stage with soulful warmth",
        emotionalStyleCue = "authentic mushaira elegance, warm poetic rhythm"
    ),
    JOSH_SHAYAR(
        id = "josh_shayar",
        labelHi = "जोशीला मंच शायर",
        subtitleHi = "दमदार, एटीट्यूड और हौसले वाली आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🔥",
        geminiVoiceName = "Fenrir",
        voiceDirection = "bold, charismatic, commanding Indian stage poet baritone with proud conviction and sharp dramatic pauses",
        emotionalStyleCue = "confident bold delivery, dramatic punchlines"
    );

    companion object {
        fun fromId(id: String?): ShayarVoicePersona {
            return when (id) {
                "shayara_soft" -> SUREELI_SHAYARA
                "dard_ustad" -> DARD_SHAYAR
                "josh_stage" -> JOSH_SHAYAR
                else -> entries.firstOrNull { it.id == id } ?: SUREELI_SHAYARA
            }
        }

        fun recommendedForCategory(categoryId: String): ShayarVoicePersona {
            return recommendedForReel(
                categoryId = categoryId,
                couplet = "",
                pageIndex = 0,
                stableKey = categoryId
            )
        }

        fun recommendedForReel(
            categoryId: String,
            couplet: String,
            pageIndex: Int,
            stableKey: String
        ): ShayarVoicePersona {
            val text = couplet.lowercase()
            val cat = ShayariCategory.fromId(categoryId)

            val hasTadapWords = listOf(
                "तड़प", "तडप", "इंतज़ार", "इंतजार", "जुदाई", "बिन तेरे", "लौट आ", "याद", "तरस",
                "tadap", "intezaar", "intezar", "judai", "yaad", "taras", "bin tere"
            ).any { it in text }

            val hasDardWords = listOf(
                "दर्द", "आँसू", "आंसू", "रो", "बेवफ़ा", "बेवफा", "टूटा", "टूट", "तन्हा", "ज़ख्म", "जख्म", "गम", "ग़म",
                "dard", "aansu", "bewafa", "toot", "tanha", "zakhm", "gham", "rona"
            ).any { it in text }

            val hasRomanticWords = listOf(
                "इश्क़", "इश्क", "मोहब्बत", "प्यार", "बाहों", "होंठ", "चाँद", "धड़कन", "सनम", "जाँ", "जान", "इज़हार",
                "ishq", "mohabbat", "pyaar", "pyar", "bahon", "chaand", "dhadkan", "sanam", "romantic"
            ).any { it in text }

            val hasJoshWords = listOf(
                "औक़ात", "औकात", "शेर", "ज़िद", "जिद", "हौसला", "जीत", "ख़ून", "वतन", "दुश्मन",
                "aukat", "attitude", "hausla", "jeet", "zid", "sher"
            ).any { it in text }

            val pool: List<ShayarVoicePersona> = when {
                cat == ShayariCategory.ATTITUDE || cat == ShayariCategory.HAUSLA || cat == ShayariCategory.DESHPREM || hasJoshWords ->
                    listOf(JOSH_SHAYAR, MEHFIL_SHAYAR, SUREELI_SHAYARA)

                cat == ShayariCategory.DARD || cat == ShayariCategory.BEWAFAI || cat == ShayariCategory.AANSU ||
                    cat == ShayariCategory.TANHAI || cat == ShayariCategory.MAUT_JANAZA || (hasDardWords && !hasRomanticWords) ->
                    listOf(DARD_SHAYARA, DARD_SHAYAR, TADAP_SHAYARA, TADAP_AASHIQ)

                hasTadapWords || cat == ShayariCategory.SECRET_LOVE || cat == ShayariCategory.AAINA ->
                    listOf(TADAP_SHAYARA, TADAP_AASHIQ, DARD_SHAYARA, ROMANTIC_SHAYARA)

                cat == ShayariCategory.MOHABBAT || cat == ShayariCategory.ROMANTIC || cat == ShayariCategory.TAREEF || hasRomanticWords ->
                    listOf(ROMANTIC_SHAYARA, SUREELI_SHAYARA, TADAP_AASHIQ, TADAP_SHAYARA, MEHFIL_SHAYAR)

                else ->
                    listOf(
                        SUREELI_SHAYARA,
                        MEHFIL_SHAYAR,
                        ROMANTIC_SHAYARA,
                        DARD_SHAYARA,
                        TADAP_AASHIQ,
                        DARD_SHAYAR,
                        TADAP_SHAYARA
                    )
            }

            val index = abs(pageIndex + (stableKey.hashCode() and 0x3)) % pool.size
            return pool[index]
        }
    }
}

sealed interface ShayarVoiceResult {
    data class Ready(val audioFile: File) : ShayarVoiceResult
    data class QuotaExceeded(val message: String) : ShayarVoiceResult
    data class Error(val message: String) : ShayarVoiceResult
}

/**
 * Generates expressive, human-like Hindi/Urdu Mushaira poet & poetess voice recitations
 * using Gemini Flash TTS via Firebase AI Logic (`ResponseModality.AUDIO` + `SpeechConfig(Voice)`),
 * with durable disk caching in `filesDir`, single-flight request deduplication, and
 * quota-safe rate handling.
 */
object ShayarVoiceEngine {

    @Volatile
    private var quotaCooldownUntilMs: Long = 0L

    private val modelCache = ConcurrentHashMap<String, com.google.firebase.ai.GenerativeModel>()
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<ShayarVoiceResult>>()
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun clearQuotaCooldown() {
        quotaCooldownUntilMs = 0L
    }

    @OptIn(PublicPreviewAPI::class)
    private fun getModel(persona: ShayarVoicePersona): com.google.firebase.ai.GenerativeModel {
        val cacheKey = "$PRIMARY_TTS_MODEL|${persona.geminiVoiceName}"
        return modelCache.getOrPut(cacheKey) {
            Firebase.ai.generativeModel(
                modelName = PRIMARY_TTS_MODEL,
                generationConfig = generationConfig {
                    responseModalities = listOf(ResponseModality.AUDIO)
                    speechConfig = SpeechConfig(
                        voice = Voice(persona.geminiVoiceName)
                    )
                }
            )
        }
    }

    /**
     * Synchronous zero-latency check for an already cached voice audio file on disk.
     */
    fun getCachedVoiceOrNull(
        context: Context,
        couplet: String,
        categoryId: String,
        persona: ShayarVoicePersona
    ): File? {
        val cleanCouplet = couplet.trim()
        if (cleanCouplet.isBlank()) return null
        val baseFile = resolveCacheFile(context, cleanCouplet, categoryId, persona)
        return findExistingCacheFile(baseFile)
    }

    suspend fun getOrGenerateVoice(
        context: Context,
        couplet: String,
        categoryId: String,
        persona: ShayarVoicePersona
    ): ShayarVoiceResult {
        val cleanCouplet = couplet.trim()
        if (cleanCouplet.isBlank()) {
            return ShayarVoiceResult.Error("शायरी खाली है।")
        }

        val appCtx = context.applicationContext
        val cacheFile = resolveCacheFile(appCtx, cleanCouplet, categoryId, persona)
        findExistingCacheFile(cacheFile)?.let { existing ->
            return ShayarVoiceResult.Ready(existing)
        }

        // If Gemini TTS recently returned HTTP 429 Quota Exceeded, avoid spamming the backend
        val now = System.currentTimeMillis()
        if (now < quotaCooldownUntilMs) {
            return ShayarVoiceResult.QuotaExceeded("Gemini AI कोटा सीमा पूरी — लोकल आवाज़ चालू")
        }

        val requestKey = cacheFile.nameWithoutExtension
        val deferred = inFlightRequests.computeIfAbsent(requestKey) {
            engineScope.async {
                try {
                    generateVoiceInternal(cleanCouplet, persona, cacheFile)
                } finally {
                    inFlightRequests.remove(requestKey)
                }
            }
        }
        return deferred.await()
    }

    private suspend fun generateVoiceInternal(
        cleanCouplet: String,
        persona: ShayarVoicePersona,
        cacheFile: File
    ): ShayarVoiceResult {
        findExistingCacheFile(cacheFile)?.let { return ShayarVoiceResult.Ready(it) }

        val prompt = buildPoeticRecitationPrompt(cleanCouplet, persona)
        return try {
            val model = getModel(persona)
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }

            val allParts = response.candidates.flatMap { it.content.parts }
            val audioPart = allParts
                .filterIsInstance<InlineDataPart>()
                .firstOrNull { part ->
                    part.mimeType.startsWith("audio/", ignoreCase = true) &&
                        part.inlineData.isNotEmpty()
                }

            if (audioPart != null) {
                quotaCooldownUntilMs = 0L
                Log.d(
                    TAG,
                    "Shayar voice ready via $PRIMARY_TTS_MODEL (${persona.id}/${persona.geminiVoiceName}): mime=${audioPart.mimeType}, bytes=${audioPart.inlineData.size}"
                )
                val savedFile = withContext(Dispatchers.IO) {
                    writePlayableAudioFile(
                        targetBaseFile = cacheFile,
                        rawBytes = audioPart.inlineData,
                        mimeType = audioPart.mimeType
                    )
                }
                ShayarVoiceResult.Ready(savedFile)
            } else {
                val partSummary = allParts.joinToString { it::class.simpleName ?: "Part" }
                Log.e(TAG, "Model $PRIMARY_TTS_MODEL returned no audio InlineDataPart. Parts=[$partSummary]")
                ShayarVoiceResult.Error("शायर आवाज़ लोड नहीं हो सकी — दोबारा टैप करें")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Shayar TTS timed out on $PRIMARY_TTS_MODEL", e)
            ShayarVoiceResult.Error("आवाज़ बनने में समय लग रहा है — दोबारा टैप करें")
        } catch (e: Exception) {
            val rawMsg = e.localizedMessage.orEmpty()
            Log.e(TAG, "Shayar TTS failed on $PRIMARY_TTS_MODEL: $rawMsg", e)
            val isQuota = rawMsg.contains("quota", ignoreCase = true) ||
                rawMsg.contains("429", ignoreCase = true) ||
                rawMsg.contains("resource_exhausted", ignoreCase = true) ||
                rawMsg.contains("billing", ignoreCase = true) ||
                rawMsg.contains("rate limit", ignoreCase = true)

            if (isQuota) {
                quotaCooldownUntilMs = System.currentTimeMillis() + QUOTA_COOLDOWN_MS
                ShayarVoiceResult.QuotaExceeded(rawMsg.take(72).ifBlank { "Quota exceeded" })
            } else {
                ShayarVoiceResult.Error(
                    rawMsg.take(72).takeIf { it.isNotBlank() }
                        ?: "शायर आवाज़ लोड नहीं हो सकी — दोबारा टैप करें"
                )
            }
        }
    }

    private fun findExistingCacheFile(baseWavFile: File): File? {
        val parent = baseWavFile.parentFile ?: return null
        val stem = baseWavFile.nameWithoutExtension
        val candidates = listOf(
            baseWavFile,
            File(parent, "$stem.mp3"),
            File(parent, "$stem.ogg")
        )
        return candidates.firstOrNull { it.exists() && it.length() > 128L }
    }

    private fun buildPoeticRecitationPrompt(
        couplet: String,
        persona: ShayarVoicePersona
    ): String {
        return "Recite this Hindi/Urdu shayari aloud like ${persona.voiceDirection} (${persona.emotionalStyleCue}). Natural pauses, speak ONLY these lines:\n$couplet"
    }

    private fun resolveCacheFile(
        context: Context,
        couplet: String,
        categoryId: String,
        persona: ShayarVoicePersona
    ): File {
        // Store in filesDir so generated Shayar voices persist across cache sweeps and save quota
        val dir = File(context.applicationContext.filesDir, VOICE_CACHE_DIR).apply { mkdirs() }
        val key = sha256Short("${persona.id}|${persona.geminiVoiceName}|$categoryId|$couplet")
        return File(dir, "voice_$key.wav")
    }

    private fun writePlayableAudioFile(
        targetBaseFile: File,
        rawBytes: ByteArray,
        mimeType: String
    ): File {
        val lowerMime = mimeType.lowercase()
        val hasRiffHeader = rawBytes.size >= 4 &&
            rawBytes[0] == 'R'.code.toByte() &&
            rawBytes[1] == 'I'.code.toByte() &&
            rawBytes[2] == 'F'.code.toByte() &&
            rawBytes[3] == 'F'.code.toByte()

        val bytesToWrite = when {
            hasRiffHeader || lowerMime.contains("mp3") || lowerMime.contains("mpeg") || lowerMime.contains("ogg") || lowerMime.contains("mp4") -> {
                rawBytes
            }
            else -> {
                val sampleRate = parseSampleRate(lowerMime) ?: 24_000
                val channels = parseChannelCount(lowerMime) ?: 1
                wrapPcmAsWav(rawBytes, sampleRate = sampleRate, channels = channels, bitsPerSample = 16)
            }
        }

        val finalFile = when {
            lowerMime.contains("mp3") || lowerMime.contains("mpeg") ->
                File(targetBaseFile.parentFile, targetBaseFile.nameWithoutExtension + ".mp3")
            lowerMime.contains("ogg") ->
                File(targetBaseFile.parentFile, targetBaseFile.nameWithoutExtension + ".ogg")
            else -> targetBaseFile
        }

        val tmp = File(finalFile.parentFile, "${finalFile.name}.tmp")
        tmp.writeBytes(bytesToWrite)
        if (!tmp.renameTo(finalFile)) {
            finalFile.writeBytes(bytesToWrite)
            tmp.delete()
        }
        trimCacheIfNeeded(finalFile.parentFile)
        return finalFile
    }

    private fun parseSampleRate(mimeType: String): Int? {
        val match = Regex("""rate=(\d+)""").find(mimeType) ?: return null
        return match.groupValues[1].toIntOrNull()?.takeIf { it in 8_000..96_000 }
    }

    private fun parseChannelCount(mimeType: String): Int? {
        val match = Regex("""channels=(\d+)""").find(mimeType) ?: return null
        return match.groupValues[1].toIntOrNull()?.takeIf { it in 1..2 }
    }

    private fun wrapPcmAsWav(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val dataSize = pcmData.size
        val chunkSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(chunkSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign)
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }.array()

        return ByteArrayOutputStream(44 + dataSize).apply {
            write(header)
            write(pcmData)
        }.toByteArray()
    }

    private fun trimCacheIfNeeded(cacheDir: File?) {
        val files = cacheDir?.listFiles()?.filter { it.isFile } ?: return
        if (files.size <= 45) return
        files.sortedBy { it.lastModified() }
            .take(files.size - 32)
            .forEach { runCatching { it.delete() } }
    }

    private fun sha256Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.take(10).joinToString("") { "%02x".format(it) }
    }
}
