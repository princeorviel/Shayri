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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "ShayarVoiceEngine"
private const val VOICE_CACHE_DIR = "shayar_voices_v13"
private const val LEGACY_VOICE_CACHE_DIR = "shayar_voices_v12"
private const val PRIMARY_TTS_MODEL = "gemini-3.1-flash-tts-preview"
private const val QUOTA_COOLDOWN_MS = 15_000L
private const val APP_CHECK_COOLDOWN_MS = 20_000L

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
    // 4 Strongly Contrasting Female Shayara Voices — natural native Hindi-Urdu human emotion
    DARD_SHAYARA(
        id = "dard_shayara",
        labelHi = "दर्द भरी शायरा",
        subtitleHi = "भीगी, टूटे दिल की दर्द भरी आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "💔",
        geminiVoiceName = "Vindemiatrix",
        voiceDirection = "a 24-year-old native North Indian woman confessing real heartbreak late at night, soft misty voice with natural breath and a slight emotional catch in her throat",
        emotionalStyleCue = "tender, aching heartbreak as if holding back tears while speaking to someone she still misses"
    ),
    SUREELI_SHAYARA(
        id = "sureeli_shayara",
        labelHi = "सुरीली शायरा",
        subtitleHi = "मीठी, खिलखिलाती व सुरीली आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🎶",
        geminiVoiceName = "Leda",
        voiceDirection = "a warm, expressive young native Hindi-Urdu poetess speaking naturally with an affectionate smile, lively musical inflection, and heartfelt charm",
        emotionalStyleCue = "sweet affectionate warmth, gentle smile audible in the voice, effortless poetic flow"
    ),
    TADAP_SHAYARA(
        id = "tadap_shayara",
        labelHi = "तेवर व जज़्बात शायरा",
        subtitleHi = "दमदार, तड़प और स्वाभिमान भरी आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🥺",
        geminiVoiceName = "Kore",
        voiceDirection = "a deeply expressive native North Indian poetess on an intimate stage, speaking from the gut with raw longing, controlled intensity, and soulful vocal weight",
        emotionalStyleCue = "intense lived-in emotion, heartfelt tension building naturally into a powerful closing line"
    ),
    ROMANTIC_SHAYARA(
        id = "romantic_shayara",
        labelHi = "रोमांटिक शायरा",
        subtitleHi = "मखमली, ख़्वाब सी रोमांटिक आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🌹",
        geminiVoiceName = "Aoede",
        voiceDirection = "an intimate, velvety young native Hindi-Urdu woman speaking softly close to the ear, warm breathy tenderness, genuine shy affection",
        emotionalStyleCue = "close-mic dreamy romance, soft affectionate glow, heartfelt warmth"
    ),

    // 4 Strongly Contrasting Male Shayar Voices — natural native Hindi-Urdu Mushaira soul
    DARD_SHAYAR(
        id = "dard_shayar",
        labelHi = "दर्द-ए-दिल शायर",
        subtitleHi = "गहरी, भारी और दर्द भरी आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🥀",
        geminiVoiceName = "Enceladus",
        voiceDirection = "a 30-year-old native North Indian shayar with a warm husky voice, speaking quietly after midnight with genuine unspoken grief, natural sighs, and lived-in pain",
        emotionalStyleCue = "raw quiet heartbreak, husky intimate ache, deeply human emotional vulnerability"
    ),
    TADAP_AASHIQ(
        id = "tadap_aashiq",
        labelHi = "तड़पता आशिक़",
        subtitleHi = "इश्क़ की तड़प और जुनून भरी आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "❤️‍🔥",
        geminiVoiceName = "Puck",
        voiceDirection = "a passionate young native Hindi-Urdu lover speaking straight from memory with earnest aching longing, natural conversational rhythm, and rising emotion",
        emotionalStyleCue = "earnest heartfelt yearning, intimate honesty, emotional swell on the closing line"
    ),
    MEHFIL_SHAYAR(
        id = "mehfil_shayar",
        labelHi = "महफ़िल उस्ताद",
        subtitleHi = "रॉयल मुशायरा मंच की भारी आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🎙️",
        geminiVoiceName = "Algenib",
        voiceDirection = "a charismatic native Lucknow Mushaira shayar in his late 30s with a rich resonant baritone, speaking with warm conversational tehzeeb, soulful nuance, and captivating poise",
        emotionalStyleCue = "soulful Mushaira storytelling, natural pause before landing the sher with warm depth"
    ),
    JOSH_SHAYAR(
        id = "josh_shayar",
        labelHi = "जोशीला मंच शायर",
        subtitleHi = "दमदार, एटीट्यूड और हौसले वाली आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🔥",
        geminiVoiceName = "Fenrir",
        voiceDirection = "a bold, grounded native Indian stage poet with a strong chest voice, speaking with calm fearless confidence, slight wry edge, and crisp authoritative landing",
        emotionalStyleCue = "calm fearless attitude, commanding presence, sharp unforgettable closing punch"
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

            // Carefully interleaved Female/Male contrasting pools rotated strictly by pageIndex
            // so consecutive Reels NEVER repeat the same voice, gender, or emotional cadence.
            val isComedyOrPlayful = cat.isJokeCategory || cat == ShayariCategory.NIBBA_NIBBI ||
                cat == ShayariCategory.GEN_Z || cat == ShayariCategory.DOSTI

            val pool: List<ShayarVoicePersona> = when {
                isComedyOrPlayful ->
                    listOf(SUREELI_SHAYARA, TADAP_AASHIQ, TADAP_SHAYARA, JOSH_SHAYAR)

                cat == ShayariCategory.ATTITUDE || cat == ShayariCategory.HAUSLA || cat == ShayariCategory.DESHPREM || hasJoshWords ->
                    listOf(JOSH_SHAYAR, TADAP_SHAYARA, MEHFIL_SHAYAR, SUREELI_SHAYARA)

                cat == ShayariCategory.DARD || cat == ShayariCategory.BEWAFAI || cat == ShayariCategory.AANSU ||
                    cat == ShayariCategory.TANHAI || cat == ShayariCategory.MAUT_JANAZA || (hasDardWords && !hasRomanticWords) ->
                    listOf(DARD_SHAYARA, DARD_SHAYAR, TADAP_SHAYARA, MEHFIL_SHAYAR, ROMANTIC_SHAYARA, TADAP_AASHIQ)

                hasTadapWords || cat == ShayariCategory.SECRET_LOVE || cat == ShayariCategory.AAINA ->
                    listOf(TADAP_AASHIQ, TADAP_SHAYARA, DARD_SHAYAR, ROMANTIC_SHAYARA, MEHFIL_SHAYAR, DARD_SHAYARA)

                cat == ShayariCategory.MOHABBAT || cat == ShayariCategory.ROMANTIC || cat == ShayariCategory.TAREEF || hasRomanticWords ->
                    listOf(ROMANTIC_SHAYARA, TADAP_AASHIQ, SUREELI_SHAYARA, MEHFIL_SHAYAR, TADAP_SHAYARA, DARD_SHAYAR)

                else ->
                    listOf(
                        MEHFIL_SHAYAR,
                        SUREELI_SHAYARA,
                        TADAP_AASHIQ,
                        ROMANTIC_SHAYARA,
                        JOSH_SHAYAR,
                        DARD_SHAYARA,
                        DARD_SHAYAR,
                        TADAP_SHAYARA
                    )
            }

            // Strictly advance by pageIndex (no random hash offset) so adjacent pages ALWAYS contrast!
            val index = abs(pageIndex) % pool.size
            return pool[index]
        }
    }
}

sealed interface ShayarVoiceResult {
    data class Ready(val audioFile: File) : ShayarVoiceResult
    data class FallbackRequired(
        val message: String,
        val isAppCheckError: Boolean = false,
        val isQuotaError: Boolean = false
    ) : ShayarVoiceResult
    data class Error(val message: String) : ShayarVoiceResult
}

sealed interface ShayarVoiceStreamResult {
    /** 0ms cache hit or buffered container file ready to play via MediaPlayer. */
    data class CachedReady(val audioFile: File) : ShayarVoiceStreamResult
    /** Live PCM chunks were already streamed into AudioTrack; audioFile is saved for instant replay. */
    data class StreamedComplete(val audioFile: File?) : ShayarVoiceStreamResult
    data class FallbackRequired(
        val message: String,
        val isAppCheckError: Boolean = false,
        val isQuotaError: Boolean = false
    ) : ShayarVoiceStreamResult
    data class Error(val message: String) : ShayarVoiceStreamResult
}

/**
 * Generates expressive, human-like Hindi/Urdu Mushaira poet & poetess voice recitations
 * using Gemini Flash TTS via Firebase AI Logic (`ResponseModality.AUDIO` + `SpeechConfig(Voice)`),
 * supporting live chunked audio streaming (`generateContentStream`) for <1s start latency,
 * durable disk caching in `filesDir`, single-flight request deduplication, and
 * quota/App-Check-safe rate handling.
 */
object ShayarVoiceEngine {

    @Volatile
    private var fallbackCooldownUntilMs: Long = 0L
    @Volatile
    private var fallbackCooldownMessage: String = ""
    @Volatile
    private var fallbackCooldownIsAppCheck: Boolean = false
    @Volatile
    private var fallbackCooldownIsQuota: Boolean = false
    @Volatile
    private var legacyCleanupScheduled: Boolean = false

    private val modelCache = ConcurrentHashMap<String, com.google.firebase.ai.GenerativeModel>()
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<ShayarVoiceResult>>()
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isInFallbackCooldown(): Boolean {
        return System.currentTimeMillis() < fallbackCooldownUntilMs
    }

    fun clearQuotaCooldown() {
        fallbackCooldownUntilMs = 0L
        fallbackCooldownMessage = ""
        fallbackCooldownIsAppCheck = false
        fallbackCooldownIsQuota = false
    }

    private fun scheduleLegacyCacheCleanupOnce(context: Context) {
        if (legacyCleanupScheduled) return
        legacyCleanupScheduled = true
        val appCtx = context.applicationContext
        engineScope.launch {
            runCatching {
                val legacyDir = File(appCtx.filesDir, LEGACY_VOICE_CACHE_DIR)
                if (legacyDir.exists()) legacyDir.deleteRecursively()
            }
        }
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
        scheduleLegacyCacheCleanupOnce(context)
        val baseFile = resolveCacheFile(context, cleanCouplet, categoryId, persona)
        return findExistingCacheFile(baseFile)
    }

    /**
     * Returns true if this voice is already cached on disk or currently pre-warming in-flight,
     * allowing Reels swipe transitions to start with zero debounce delay.
     */
    fun isVoiceInFlightOrCached(
        context: Context,
        couplet: String,
        categoryId: String,
        persona: ShayarVoicePersona
    ): Boolean {
        val cleanCouplet = couplet.trim()
        if (cleanCouplet.isBlank()) return false
        val baseFile = resolveCacheFile(context, cleanCouplet, categoryId, persona)
        if (findExistingCacheFile(baseFile) != null) return true
        return inFlightRequests.containsKey(baseFile.nameWithoutExtension)
    }

    /**
     * Ultra-low-latency streaming voice retrieval:
     * 1. Returns `CachedReady` in 0ms if already on disk.
     * 2. Reuses an already-running prewarm request if in-flight (zero duplicate calls).
     * 3. Otherwise streams live PCM chunks from `generateContentStream(...)` straight into
     *    `onPcmChunk` so the Shayar begins speaking in ~0.7–1.2s while persisting the finished
     *    WAV to disk cache for instant 0ms loops & revisits.
     */
    suspend fun streamOrGetVoice(
        context: Context,
        couplet: String,
        categoryId: String,
        persona: ShayarVoicePersona,
        onPcmChunk: suspend (pcmBytes: ByteArray, sampleRate: Int, channels: Int) -> Boolean
    ): ShayarVoiceStreamResult {
        val cleanCouplet = couplet.trim()
        if (cleanCouplet.isBlank()) {
            return ShayarVoiceStreamResult.Error("शायरी खाली है।")
        }

        val appCtx = context.applicationContext
        scheduleLegacyCacheCleanupOnce(appCtx)
        val cacheFile = resolveCacheFile(appCtx, cleanCouplet, categoryId, persona)
        findExistingCacheFile(cacheFile)?.let { existing ->
            return ShayarVoiceStreamResult.CachedReady(existing)
        }

        // If a background prewarm for this exact Reel is already halfway done, join it!
        val requestKey = cacheFile.nameWithoutExtension
        inFlightRequests[requestKey]?.let { existingDeferred ->
            return when (val joined = existingDeferred.await()) {
                is ShayarVoiceResult.Ready -> ShayarVoiceStreamResult.CachedReady(joined.audioFile)
                is ShayarVoiceResult.FallbackRequired -> ShayarVoiceStreamResult.FallbackRequired(
                    message = joined.message,
                    isAppCheckError = joined.isAppCheckError,
                    isQuotaError = joined.isQuotaError
                )
                is ShayarVoiceResult.Error -> ShayarVoiceStreamResult.Error(joined.message)
            }
        }

        val now = System.currentTimeMillis()
        if (now < fallbackCooldownUntilMs) {
            return ShayarVoiceStreamResult.FallbackRequired(
                message = fallbackCooldownMessage.ifBlank { "AI आवाज़ उपलब्ध नहीं — लोकल शायर आवाज़ चालू" },
                isAppCheckError = fallbackCooldownIsAppCheck,
                isQuotaError = fallbackCooldownIsQuota
            )
        }

        val prompt = buildPoeticRecitationPrompt(
            couplet = cleanCouplet,
            persona = persona,
            categoryId = categoryId
        )

        var emittedAnyPcm = false
        var detectedContainerFormat = false
        var containerMimeType = "audio/L16;rate=24000"
        var streamSampleRate = 24_000
        var streamChannels = 1
        var checkedFirstChunkHeader = false
        var carryBytes = ByteArray(0)
        val pcmAccumulator = ByteArrayOutputStream(64 * 1024)
        val containerChunks = mutableListOf<InlineDataPart>()

        return try {
            val model = getModel(persona)
            withTimeout(40_000L) {
                model.generateContentStream(prompt).collect { chunk ->
                    val audioParts = chunk.candidates
                        .flatMap { it.content.parts }
                        .filterIsInstance<InlineDataPart>()
                        .filter { part ->
                            part.mimeType.startsWith("audio/", ignoreCase = true) &&
                                part.inlineData.isNotEmpty()
                        }

                    for (part in audioParts) {
                        val raw = part.inlineData
                        val lowerMime = part.mimeType.lowercase()

                        if (!checkedFirstChunkHeader) {
                            checkedFirstChunkHeader = true
                            containerMimeType = part.mimeType
                            val isCompressed = lowerMime.contains("mp3") ||
                                lowerMime.contains("mpeg") ||
                                lowerMime.contains("ogg") ||
                                lowerMime.contains("mp4") ||
                                lowerMime.contains("aac")
                            if (isCompressed) {
                                detectedContainerFormat = true
                            } else {
                                streamSampleRate = parseSampleRate(lowerMime) ?: 24_000
                                streamChannels = parseChannelCount(lowerMime) ?: 1
                            }
                        }

                        if (detectedContainerFormat) {
                            containerChunks.add(part)
                            continue
                        }

                        // Strip RIFF/WAV header if Gemini sent a WAV-wrapped first chunk
                        val payload: ByteArray = if (!emittedAnyPcm && hasRiffWaveHeader(raw)) {
                            val wavInfo = parseRiffWaveHeader(raw)
                            if (wavInfo != null) {
                                streamSampleRate = wavInfo.sampleRate
                                streamChannels = wavInfo.channels
                                if (wavInfo.dataOffset < raw.size) {
                                    raw.copyOfRange(wavInfo.dataOffset, raw.size)
                                } else {
                                    ByteArray(0)
                                }
                            } else {
                                raw
                            }
                        } else {
                            raw
                        }

                        if (payload.isEmpty()) continue

                        // Merge any leftover partial-frame bytes from the previous network chunk
                        val combined = if (carryBytes.isNotEmpty()) {
                            ByteArray(carryBytes.size + payload.size).also { dst ->
                                System.arraycopy(carryBytes, 0, dst, 0, carryBytes.size)
                                System.arraycopy(payload, 0, dst, carryBytes.size, payload.size)
                            }
                        } else {
                            payload
                        }

                        val frameAlign = streamChannels * 2
                        val usableLen = (combined.size / frameAlign) * frameAlign
                        carryBytes = if (usableLen < combined.size) {
                            combined.copyOfRange(usableLen, combined.size)
                        } else {
                            ByteArray(0)
                        }
                        if (usableLen <= 0) continue

                        val alignedSlice = if (usableLen == combined.size) {
                            combined
                        } else {
                            combined.copyOf(usableLen)
                        }

                        pcmAccumulator.write(alignedSlice)
                        emittedAnyPcm = true
                        clearQuotaCooldown()

                        val keepStreaming = onPcmChunk(alignedSlice, streamSampleRate, streamChannels)
                        if (!keepStreaming) {
                            throw CancellationException("Audio playback stopped by caller")
                        }
                    }
                }
            }

            if (detectedContainerFormat && containerChunks.isNotEmpty()) {
                val combined = combineAudioParts(containerChunks)
                if (combined != null && combined.first.size >= 1_600) {
                    clearQuotaCooldown()
                    val savedFile = withContext(Dispatchers.IO) {
                        writePlayableAudioFile(
                            targetBaseFile = cacheFile,
                            rawBytes = combined.first,
                            mimeType = combined.second
                        )
                    }
                    return ShayarVoiceStreamResult.CachedReady(savedFile)
                }
            }

            val allPcm = pcmAccumulator.toByteArray()
            if (emittedAnyPcm && allPcm.size >= 1_600) {
                Log.d(
                    TAG,
                    "Streamed Shayar voice via $PRIMARY_TTS_MODEL (${persona.id}/${persona.geminiVoiceName}): rate=$streamSampleRate, ch=$streamChannels, pcmBytes=${allPcm.size}"
                )
                // Persist cleanly in background for instant 0ms replay & future visits
                val savedFile = withContext(Dispatchers.IO) {
                    runCatching {
                        val rawWav = writePlayableAudioFile(
                            targetBaseFile = cacheFile,
                            rawBytes = allPcm,
                            mimeType = "audio/L16;rate=$streamSampleRate;channels=$streamChannels"
                        )
                        val masteredFile = File(rawWav.parentFile, "${rawWav.nameWithoutExtension}_m.wav")
                        val ok = com.example.audio.StudioVocalMaster.masterGeminiVoiceWav(
                            inputWav = rawWav,
                            outputWav = masteredFile
                        )
                        if (ok && masteredFile.exists() && masteredFile.length() > 1024L) {
                            if (!masteredFile.renameTo(rawWav)) {
                                rawWav.writeBytes(masteredFile.readBytes())
                                masteredFile.delete()
                            }
                            rawWav
                        } else {
                            masteredFile.delete()
                            rawWav.takeIf { it.exists() && it.length() > 1024L }
                        }
                    }.getOrNull()
                }
                ShayarVoiceStreamResult.StreamedComplete(savedFile)
            } else {
                // Stream finished without PCM chunks; fall back once to unary generation
                Log.w(TAG, "Stream returned empty audio; retrying unary generation for ${persona.id}.")
                when (val unary = getOrGenerateVoice(appCtx, cleanCouplet, categoryId, persona)) {
                    is ShayarVoiceResult.Ready -> ShayarVoiceStreamResult.CachedReady(unary.audioFile)
                    is ShayarVoiceResult.FallbackRequired -> ShayarVoiceStreamResult.FallbackRequired(
                        message = unary.message,
                        isAppCheckError = unary.isAppCheckError,
                        isQuotaError = unary.isQuotaError
                    )
                    is ShayarVoiceResult.Error -> ShayarVoiceStreamResult.Error(unary.message)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Shayar TTS stream timed out on $PRIMARY_TTS_MODEL", e)
            if (emittedAnyPcm) {
                ShayarVoiceStreamResult.StreamedComplete(null)
            } else {
                ShayarVoiceStreamResult.Error("आवाज़ बनने में समय लग रहा है — दोबारा टैप करें")
            }
        } catch (e: Exception) {
            if (emittedAnyPcm) {
                Log.w(TAG, "Stream ended early after emitting audio: ${e.localizedMessage}")
                return ShayarVoiceStreamResult.StreamedComplete(null)
            }
            val classified = classifyVoiceException(e)
            when (classified) {
                is ShayarVoiceResult.FallbackRequired -> ShayarVoiceStreamResult.FallbackRequired(
                    message = classified.message,
                    isAppCheckError = classified.isAppCheckError,
                    isQuotaError = classified.isQuotaError
                )
                is ShayarVoiceResult.Error -> {
                    // Retry once with unary generateContent in case streaming transport had a hiccup
                    Log.w(TAG, "Streaming failed before first chunk (${e.localizedMessage}); retrying unary.")
                    when (val unary = getOrGenerateVoice(appCtx, cleanCouplet, categoryId, persona)) {
                        is ShayarVoiceResult.Ready -> ShayarVoiceStreamResult.CachedReady(unary.audioFile)
                        is ShayarVoiceResult.FallbackRequired -> ShayarVoiceStreamResult.FallbackRequired(
                            message = unary.message,
                            isAppCheckError = unary.isAppCheckError,
                            isQuotaError = unary.isQuotaError
                        )
                        is ShayarVoiceResult.Error -> ShayarVoiceStreamResult.Error(unary.message)
                    }
                }
                is ShayarVoiceResult.Ready -> ShayarVoiceStreamResult.CachedReady(classified.audioFile)
            }
        }
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
        scheduleLegacyCacheCleanupOnce(appCtx)
        val cacheFile = resolveCacheFile(appCtx, cleanCouplet, categoryId, persona)
        findExistingCacheFile(cacheFile)?.let { existing ->
            return ShayarVoiceResult.Ready(existing)
        }

        // If Gemini TTS recently returned Quota Exceeded or App Check Invalid, switch to local voice immediately
        val now = System.currentTimeMillis()
        if (now < fallbackCooldownUntilMs) {
            return ShayarVoiceResult.FallbackRequired(
                message = fallbackCooldownMessage.ifBlank { "AI आवाज़ उपलब्ध नहीं — लोकल शायर आवाज़ चालू" },
                isAppCheckError = fallbackCooldownIsAppCheck,
                isQuotaError = fallbackCooldownIsQuota
            )
        }

        val requestKey = cacheFile.nameWithoutExtension
        val deferred = inFlightRequests.computeIfAbsent(requestKey) {
            engineScope.async {
                try {
                    generateVoiceInternal(cleanCouplet, categoryId, persona, cacheFile)
                } finally {
                    inFlightRequests.remove(requestKey)
                }
            }
        }
        return deferred.await()
    }

    /**
     * Quietly pre-generates upcoming Reels' voices on disk in the background while the current
     * Reel is playing, so swiping to the next Reels starts in 0ms with zero loading delay.
     * Skips immediately if already cached, already in-flight, or in quota/App-Check cooldown.
     */
    suspend fun prewarmVoiceIfSafe(
        context: Context,
        couplet: String,
        categoryId: String,
        persona: ShayarVoicePersona
    ) {
        val cleanCouplet = couplet.trim()
        if (cleanCouplet.isBlank() || isInFallbackCooldown()) return
        val appCtx = context.applicationContext
        val cacheFile = resolveCacheFile(appCtx, cleanCouplet, categoryId, persona)
        if (findExistingCacheFile(cacheFile) != null) return
        val requestKey = cacheFile.nameWithoutExtension
        if (inFlightRequests.containsKey(requestKey)) return
        runCatching {
            getOrGenerateVoice(appCtx, cleanCouplet, categoryId, persona)
        }
    }

    private fun classifyVoiceException(e: Exception): ShayarVoiceResult {
        val rawMsg = e.localizedMessage.orEmpty()
        Log.e(TAG, "Shayar TTS failed on $PRIMARY_TTS_MODEL: $rawMsg", e)
        val isQuota = rawMsg.contains("quota", ignoreCase = true) ||
            rawMsg.contains("429", ignoreCase = true) ||
            rawMsg.contains("resource_exhausted", ignoreCase = true) ||
            rawMsg.contains("billing", ignoreCase = true) ||
            rawMsg.contains("rate limit", ignoreCase = true)

        val isAppCheck = rawMsg.contains("app check", ignoreCase = true) ||
            rawMsg.contains("attestation", ignoreCase = true) ||
            rawMsg.contains("token is invalid", ignoreCase = true) ||
            rawMsg.contains("403", ignoreCase = true) ||
            rawMsg.contains("unauthenticated", ignoreCase = true)

        return when {
            isQuota -> {
                fallbackCooldownUntilMs = System.currentTimeMillis() + QUOTA_COOLDOWN_MS
                fallbackCooldownIsQuota = true
                fallbackCooldownIsAppCheck = false
                fallbackCooldownMessage = "अभी व्यस्त है • ऑफ़लाइन आवाज़ चालू (रिट्राई करें)"
                ShayarVoiceResult.FallbackRequired(
                    message = fallbackCooldownMessage,
                    isQuotaError = true
                )
            }
            isAppCheck -> {
                fallbackCooldownUntilMs = System.currentTimeMillis() + APP_CHECK_COOLDOWN_MS
                fallbackCooldownIsAppCheck = true
                fallbackCooldownIsQuota = false
                fallbackCooldownMessage = "ऑफ़लाइन आवाज़ सक्रिय • रिट्राई करें"
                ShayarVoiceResult.FallbackRequired(
                    message = fallbackCooldownMessage,
                    isAppCheckError = true
                )
            }
            else -> {
                ShayarVoiceResult.Error("इंटरनेट या आवाज़ सेवा उपलब्ध नहीं — रिट्राई करें")
            }
        }
    }

    private data class RiffHeaderInfo(
        val sampleRate: Int,
        val channels: Int,
        val dataOffset: Int
    )

    private fun hasRiffWaveHeader(bytes: ByteArray): Boolean {
        return bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() &&
            bytes[9] == 'A'.code.toByte() &&
            bytes[10] == 'V'.code.toByte() &&
            bytes[11] == 'E'.code.toByte()
    }

    private fun parseRiffWaveHeader(bytes: ByteArray): RiffHeaderInfo? {
        if (!hasRiffWaveHeader(bytes) || bytes.size < 44) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var offset = 12
        var channels = 1
        var sampleRate = 24_000
        while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
            val chunkSize = buf.getInt(offset + 4)
            val payloadStart = offset + 8
            if (chunkSize < 0 || payloadStart > bytes.size) break
            when (chunkId) {
                "fmt " -> {
                    if (payloadStart + 16 <= bytes.size) {
                        channels = (buf.getShort(payloadStart + 2).toInt() and 0xFFFF).coerceIn(1, 2)
                        sampleRate = buf.getInt(payloadStart + 4).coerceIn(8_000, 96_000)
                    }
                }
                "data" -> {
                    return RiffHeaderInfo(sampleRate, channels, payloadStart)
                }
            }
            val padded = chunkSize + (chunkSize and 1)
            offset = payloadStart + padded
        }
        return RiffHeaderInfo(sampleRate, channels, 44)
    }

    private suspend fun generateVoiceInternal(
        cleanCouplet: String,
        categoryId: String,
        persona: ShayarVoicePersona,
        cacheFile: File
    ): ShayarVoiceResult {
        findExistingCacheFile(cacheFile)?.let { return ShayarVoiceResult.Ready(it) }

        val prompt = buildPoeticRecitationPrompt(
            couplet = cleanCouplet,
            persona = persona,
            categoryId = categoryId
        )
        return try {
            val model = getModel(persona)
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }

            val allParts = response.candidates.flatMap { it.content.parts }
            val audioParts = allParts
                .filterIsInstance<InlineDataPart>()
                .filter { part ->
                    part.mimeType.startsWith("audio/", ignoreCase = true) &&
                        part.inlineData.isNotEmpty()
                }

            val combinedAudio = combineAudioParts(audioParts)
            if (combinedAudio != null && combinedAudio.first.size >= 1_600) {
                val (audioBytes, audioMimeType) = combinedAudio
                clearQuotaCooldown()
                Log.d(
                    TAG,
                    "Shayar voice ready via $PRIMARY_TTS_MODEL (${persona.id}/${persona.geminiVoiceName}): mime=$audioMimeType, parts=${audioParts.size}, bytes=${audioBytes.size}"
                )
                val savedFile = withContext(Dispatchers.IO) {
                    val rawFile = writePlayableAudioFile(
                        targetBaseFile = cacheFile,
                        rawBytes = audioBytes,
                        mimeType = audioMimeType
                    )
                    if (rawFile.extension.equals("wav", ignoreCase = true)) {
                        val masteredFile = File(rawFile.parentFile, "${rawFile.nameWithoutExtension}_m.wav")
                        val ok = com.example.audio.StudioVocalMaster.masterGeminiVoiceWav(
                            inputWav = rawFile,
                            outputWav = masteredFile
                        )
                        if (ok && masteredFile.exists() && masteredFile.length() > 1024L) {
                            if (!masteredFile.renameTo(rawFile)) {
                                rawFile.writeBytes(masteredFile.readBytes())
                                masteredFile.delete()
                            }
                            rawFile
                        } else {
                            masteredFile.delete()
                            rawFile.delete()
                            null
                        }
                    } else {
                        rawFile
                    }
                }
                if (savedFile != null) {
                    ShayarVoiceResult.Ready(savedFile)
                } else {
                    Log.w(TAG, "Gemini returned silent/unplayable WAV; triggering fallback.")
                    ShayarVoiceResult.Error("ऑडियो साफ़ नहीं मिला — दोबारा प्रयास करें")
                }
            } else {
                val partSummary = allParts.joinToString { it::class.simpleName ?: "Part" }
                Log.e(TAG, "Model $PRIMARY_TTS_MODEL returned no usable audio InlineDataPart. Parts=[$partSummary]")
                ShayarVoiceResult.Error("शायर आवाज़ लोड नहीं हो सकी — दोबारा प्रयास करें")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Shayar TTS timed out on $PRIMARY_TTS_MODEL", e)
            ShayarVoiceResult.Error("आवाज़ बनने में समय लग रहा है — दोबारा टैप करें")
        } catch (e: Exception) {
            classifyVoiceException(e)
        }
    }

    private fun combineAudioParts(parts: List<InlineDataPart>): Pair<ByteArray, String>? {
        if (parts.isEmpty()) return null
        if (parts.size == 1) return parts.first().inlineData to parts.first().mimeType
        val firstMime = parts.first().mimeType
        val lowerMime = firstMime.lowercase()
        val isContainerFormat = lowerMime.contains("mp3") || lowerMime.contains("mpeg") ||
            lowerMime.contains("ogg") || lowerMime.contains("mp4") ||
            (parts.first().inlineData.size >= 4 &&
                parts.first().inlineData[0] == 'R'.code.toByte() &&
                parts.first().inlineData[1] == 'I'.code.toByte() &&
                parts.first().inlineData[2] == 'F'.code.toByte() &&
                parts.first().inlineData[3] == 'F'.code.toByte())
        if (isContainerFormat) {
            val largest = parts.maxByOrNull { it.inlineData.size } ?: return null
            return largest.inlineData to largest.mimeType
        }
        val merged = ByteArrayOutputStream()
        parts.forEach { part -> merged.write(part.inlineData) }
        return merged.toByteArray() to firstMime
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

    private enum class CoupletEmotion {
        DEEP_HEARTBREAK,
        PASSIONATE_YEARNING,
        PLAYFUL_ROMANCE,
        INTIMATE_ROMANTIC,
        WITTY_LIGHTHEARTED,
        BOLD_ATTITUDE,
        ROYAL_MEHFIL,
        SOULFUL_SUFI
    }

    private fun detectCoupletEmotion(
        couplet: String,
        categoryId: String,
        persona: ShayarVoicePersona
    ): CoupletEmotion {
        val lower = couplet.lowercase()
        val cat = ShayariCategory.fromId(categoryId)

        if (cat.isJokeCategory || cat == ShayariCategory.NIBBA_NIBBI || cat == ShayariCategory.GEN_Z) {
            return CoupletEmotion.WITTY_LIGHTHEARTED
        }

        val hasHeartbreak = listOf(
            "दर्द", "आँसू", "आंसू", "बेवफ़ा", "बेवफा", "टूटा", "टूट", "ज़ख्म", "जख्म", "तन्हा", "बिछड़", "रो", "ग़म", "गम",
            "dard", "aansu", "bewafa", "toota", "zakhm", "tanha", "gham", "rona"
        ).any { it in lower }

        val hasYearning = listOf(
            "तड़प", "तडप", "इंतज़ार", "इंतजार", "जुदाई", "याद", "तरस", "बिन तेरे", "लौट आ", "ख़ामोश", "खामोश",
            "tadap", "intezaar", "intezar", "judai", "yaad", "taras", "bin tere"
        ).any { it in lower }

        val hasAttitude = listOf(
            "औक़ात", "औकात", "शेर", "ज़िद", "जिद", "हौसला", "जीत", "दुश्मन", "वतन", "अंदाज़", "अंदाज",
            "aukat", "attitude", "hausla", "jeet", "zid", "dushman"
        ).any { it in lower }

        val hasRomantic = listOf(
            "इश्क़", "इश्क", "मोहब्बत", "प्यार", "चाँद", "होंठ", "बाहों", "धड़कन", "सनम", "जाँ", "जान", "इज़हार", "ख़ूबसूरत",
            "ishq", "mohabbat", "pyaar", "pyar", "chaand", "dhadkan", "sanam"
        ).any { it in lower }

        val hasSufiOrLife = cat == ShayariCategory.KHUDA_RAB || cat == ShayariCategory.ZINDAGI ||
            cat == ShayariCategory.WAQT || cat == ShayariCategory.MUSAFIR || cat == ShayariCategory.AAINA

        return when {
            persona == ShayarVoicePersona.JOSH_SHAYAR || cat == ShayariCategory.ATTITUDE ||
                cat == ShayariCategory.HAUSLA || cat == ShayariCategory.DESHPREM || hasAttitude ->
                CoupletEmotion.BOLD_ATTITUDE

            persona == ShayarVoicePersona.MEHFIL_SHAYAR ->
                if (hasSufiOrLife) CoupletEmotion.SOULFUL_SUFI else CoupletEmotion.ROYAL_MEHFIL

            persona == ShayarVoicePersona.SUREELI_SHAYARA ->
                if (cat == ShayariCategory.DOSTI || cat == ShayariCategory.NEW_YEAR || cat == ShayariCategory.CHRISTMAS) {
                    CoupletEmotion.WITTY_LIGHTHEARTED
                } else {
                    CoupletEmotion.PLAYFUL_ROMANCE
                }

            persona == ShayarVoicePersona.TADAP_AASHIQ || persona == ShayarVoicePersona.TADAP_SHAYARA ->
                CoupletEmotion.PASSIONATE_YEARNING

            persona == ShayarVoicePersona.DARD_SHAYARA || persona == ShayarVoicePersona.DARD_SHAYAR ||
                cat == ShayariCategory.DARD || cat == ShayariCategory.BEWAFAI ||
                cat == ShayariCategory.AANSU || cat == ShayariCategory.TANHAI ||
                cat == ShayariCategory.MAUT_JANAZA || (hasHeartbreak && !hasRomantic) ->
                CoupletEmotion.DEEP_HEARTBREAK

            hasYearning || cat == ShayariCategory.SECRET_LOVE ->
                CoupletEmotion.PASSIONATE_YEARNING

            persona == ShayarVoicePersona.ROMANTIC_SHAYARA || cat == ShayariCategory.MOHABBAT ||
                cat == ShayariCategory.ROMANTIC || cat == ShayariCategory.TAREEF || hasRomantic ->
                CoupletEmotion.INTIMATE_ROMANTIC

            hasSufiOrLife -> CoupletEmotion.SOULFUL_SUFI
            else -> CoupletEmotion.ROYAL_MEHFIL
        }
    }

    private fun sanitizeLineForTranscript(rawLine: String): String {
        return rawLine
            .replace(Regex("[\\p{So}\\p{Cn}_#*~`^|<>\\[\\]{}]"), " ")
            .replace(Regex("[•·▪▫●○◆◇★☆]+"), ", ")
            .replace(Regex("\\.{2,}"), ",")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Adds a gentle natural breath comma ONLY into genuinely long unpunctuated lines (9+ words)
     * so short/medium shayari misras flow smoothly in one organic human breath without robotic breaks.
     */
    private fun applyPoeticCaesura(cleanLine: String): String {
        if (cleanLine.contains(',') || cleanLine.contains("...") || cleanLine.contains(';') || cleanLine.contains('—')) {
            return cleanLine
        }
        val words = cleanLine.split(" ").filter { it.isNotBlank() }
        if (words.size < 9) return cleanLine

        val beforeBreakWords = setOf(
            "कि", "पर", "मगर", "लेकिन", "और", "फिर", "जब", "वरना", "क्योंकि", "जैसे",
            "ki", "par", "magar", "lekin", "aur", "phir", "jab", "varna", "jaise"
        )
        val afterBreakWords = setOf(
            "तो", "बस", "हाँ", "सुनो", "जान", "यार", "दोस्त", "सनम",
            "toh", "to", "bas", "haan", "suno", "yaar", "dost", "sanam"
        )

        val minIdx = 3
        val maxIdx = words.size - 4
        var splitIndex = -1

        for (i in minIdx..maxIdx) {
            val w = words[i].lowercase().trim(',', '।', '.', '!', '?')
            if (w in beforeBreakWords) {
                splitIndex = i
                break
            }
            if (w in afterBreakWords && i + 1 <= maxIdx + 1) {
                splitIndex = i + 1
                break
            }
        }
        if (splitIndex !in 3..(words.size - 3)) return cleanLine

        val firstHalf = words.subList(0, splitIndex).joinToString(" ")
        val secondHalf = words.subList(splitIndex, words.size).joinToString(" ")
        return "$firstHalf, $secondHalf"
    }

    internal fun buildPoeticRecitationPrompt(
        couplet: String,
        persona: ShayarVoicePersona,
        categoryId: String = ""
    ): String {
        val lines = couplet
            .replace("\r\n", "\n")
            .split("\n")
            .map { sanitizeLineForTranscript(it) }
            .filter { line -> line.any { ch -> ch.isLetterOrDigit() } }

        val emotion = detectCoupletEmotion(couplet, categoryId, persona)

        val sceneAndArc = when (emotion) {
            CoupletEmotion.DEEP_HEARTBREAK ->
                "Late-night heartbreak confession. Speak naturally and softly with real pain in your voice, natural micro-breaths, and let your voice soften and catch with genuine emotion on the final line."
            CoupletEmotion.PASSIONATE_YEARNING ->
                "Intimate aching longing for someone deeply missed. Start softly and warmly, then let genuine heartfelt longing swell naturally into the closing line."
            CoupletEmotion.PLAYFUL_ROMANCE ->
                "Warm, sweet affectionate moment. Speak with a genuine gentle smile in your voice, playful Hindi-Urdu lilt, and tender closeness."
            CoupletEmotion.INTIMATE_ROMANTIC ->
                "Close-mic late-night romantic whisper. Soft, velvety, breathing naturally as if speaking straight to your beloved's heart."
            CoupletEmotion.WITTY_LIGHTHEARTED ->
                "Relaxed friendly mehfil storytelling. Natural conversational Indian warmth, amused smile, and crisp comic timing on the final line."
            CoupletEmotion.BOLD_ATTITUDE ->
                "Grounded, fearless stage shayar. Calm, unhurried self-assurance with crisp vocal presence and a sharp, unforgettable closing drop."
            CoupletEmotion.ROYAL_MEHFIL ->
                "Intimate Lucknow Mushaira evening. Conversational warmth and authentic Urdu-Hindi tehzeeb; set up the opening thought smoothly and land the closing sher with soulful depth."
            CoupletEmotion.SOULFUL_SUFI ->
                "Peaceful midnight reflection. Warm, soothing, deeply grounded human wisdom with calm unhurried breaths."
        }

        val directorNotes = buildString {
            append("# AUDIO PROFILE\n")
            append("Native North Indian Hindi-Urdu speaker: ${persona.voiceDirection}.\n")
            append("# DIRECTOR'S NOTES\n")
            append("$sceneAndArc ")
            append("Sound 100% human and lived-in — speak directly to one person from memory, NEVER flat, robotic, or like reading written text. Use authentic Hindi-Urdu pronunciation, natural pitch contour, and real emotional feeling.\n")
            append("# TRANSCRIPT\n")
        }

        val taggedTranscript = buildString {
            lines.forEachIndexed { idx, rawLine ->
                val isFirst = idx == 0
                val isLast = idx == lines.lastIndex
                val clean = applyPoeticCaesura(rawLine.trimEnd('।', '.', ',', ' ', '—', '-'))
                val endingPunct = when {
                    rawLine.endsWith("?") -> "?"
                    rawLine.endsWith("!") -> "!"
                    emotion == CoupletEmotion.BOLD_ATTITUDE && isLast -> "!"
                    isLast -> "।"
                    else -> ","
                }

                if (idx > 0) append("\n")

                val expressiveTag = when (emotion) {
                    CoupletEmotion.DEEP_HEARTBREAK -> when {
                        isFirst && lines.size == 1 -> "[softly, heartbroken] "
                        isFirst -> "[softly, heartbroken] "
                        isLast -> "[sigh] [sadly, voice trembling] "
                        else -> "[softly, aching] "
                    }
                    CoupletEmotion.PASSIONATE_YEARNING -> when {
                        isFirst -> "[softly, longing] "
                        isLast -> "[sigh] [deeply emotional] "
                        else -> "[warmly, earnest] "
                    }
                    CoupletEmotion.PLAYFUL_ROMANCE -> when {
                        isFirst -> "[warmly, smiling] "
                        isLast -> "[softly, affectionate smile] "
                        else -> "[warmly] "
                    }
                    CoupletEmotion.INTIMATE_ROMANTIC -> when {
                        isFirst -> "[warmly, intimately] "
                        isLast -> "[whispering, tenderly] "
                        else -> "[softly] "
                    }
                    CoupletEmotion.WITTY_LIGHTHEARTED -> when {
                        isFirst -> "[warmly, conversational] "
                        isLast -> "[chuckle] [playfully] "
                        else -> ""
                    }
                    CoupletEmotion.BOLD_ATTITUDE -> when {
                        isFirst -> "[calmly, confident] "
                        isLast -> "[short pause] [firm, commanding] "
                        else -> "[confidently] "
                    }
                    CoupletEmotion.ROYAL_MEHFIL -> when {
                        isFirst -> "[warmly, soulful] "
                        isLast -> "[short pause] [slowly, with deep emotion] "
                        else -> "[thoughtfully] "
                    }
                    CoupletEmotion.SOULFUL_SUFI -> when {
                        isFirst -> "[soothing, calm] "
                        isLast -> "[sigh] [warmly, reflective] "
                        else -> "[peacefully] "
                    }
                }

                append(expressiveTag)
                append(clean)
                append(endingPunct)
            }
        }

        return "$directorNotes$taggedTranscript"
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
                val frameAlign = channels * 2
                val alignedLen = (rawBytes.size / frameAlign) * frameAlign
                val alignedPcm = if (alignedLen == rawBytes.size) rawBytes else rawBytes.copyOf(alignedLen)
                wrapPcmAsWav(alignedPcm, sampleRate = sampleRate, channels = channels, bitsPerSample = 16)
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
        if (files.size <= 90) return
        files.sortedBy { it.lastModified() }
            .take(files.size - 70)
            .forEach { runCatching { it.delete() } }
    }

    private fun sha256Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.take(10).joinToString("") { "%02x".format(it) }
    }
}
