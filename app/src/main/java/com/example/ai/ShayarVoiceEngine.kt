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
private const val VOICE_CACHE_DIR = "shayar_voices_v14"
private val LEGACY_VOICE_CACHE_DIRS = listOf("shayar_voices_v12", "shayar_voices_v13")
private const val PRIMARY_TTS_MODEL = "gemini-3.1-flash-tts-preview"
private const val QUOTA_COOLDOWN_MS = 15_000L
private const val APP_CHECK_COOLDOWN_MS = 20_000L

enum class CoupletEmotion(val badgeHi: String) {
    BEWAFAI_BETRAYAL("बेवफ़ाई अंदाज़"),
    DARD_TEARFUL("दर्द भरा अंदाज़"),
    TANHAI_LONELY("तन्हाई व यादें"),
    TADAP_YEARNING("तड़प व जुदाई"),
    SHARABI_MAIKHANA("शराबी मैख़ाना"),
    MAUT_JANAZA_SOLEMN("संजीदा व गहरा"),
    INTIMATE_ROMANTIC("रोमांटिक अंदाज़"),
    BOLD_FLIRTY("नटखट व बोल्ड"),
    ATTITUDE_SWAGGER("तेवर व एटीट्यूड"),
    HAUSLA_DESHPREM_FIRE("जुनून व हौसला"),
    DOSTI_KHUSHI_FESTIVE("दोस्ती व ख़ुशी"),
    CUTE_GENZ_PLAYFUL("क्यूट Gen-Z वाइब"),
    COMEDY_WITTY("हँसी-मज़ाक"),
    SOULFUL_SUFI_WISDOM("सूफ़ी सुकून")
}

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
    // 4 Maximally Contrasting Female Shayara Voices across pitch, age & energy
    DARD_SHAYARA(
        id = "dard_shayara",
        labelHi = "दर्द भरी शायरा",
        subtitleHi = "भीगी, टूटे दिल की दर्द भरी आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "💔",
        geminiVoiceName = "Vindemiatrix",
        voiceDirection = "a fragile, deeply emotional young North Indian woman with a soft misty voice and tearful catch in her throat",
        emotionalStyleCue = "raw aching heartbreak as if holding back tears while speaking to someone she still misses"
    ),
    SUREELI_SHAYARA(
        id = "sureeli_shayara",
        labelHi = "सुरीली शायरा",
        subtitleHi = "मीठी, खिलखिलाती व चंचल आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🎶",
        geminiVoiceName = "Zephyr",
        voiceDirection = "a bright, youthful, cheerful North Indian girl with clear high-spirited energy and an audible warm smile",
        emotionalStyleCue = "lively affectionate charm, playful sparkle, bright expressive Hindi delivery"
    ),
    TADAP_SHAYARA(
        id = "tadap_shayara",
        labelHi = "तेवर व जज़्बात शायरा",
        subtitleHi = "दमदार, बेवफ़ाई व स्वाभिमान भरी आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🥺",
        geminiVoiceName = "Kore",
        voiceDirection = "a bold, strong, intensely expressive North Indian poetess with firm vocal presence, wounded pride, and dramatic fire",
        emotionalStyleCue = "fierce emotional intensity, sharp betrayal bite, commanding closing punch"
    ),
    ROMANTIC_SHAYARA(
        id = "romantic_shayara",
        labelHi = "रोमांटिक शायरा",
        subtitleHi = "मखमली, ख़्वाब सी रोमांटिक आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🌹",
        geminiVoiceName = "Aoede",
        voiceDirection = "a warm, velvety, relaxed North Indian poetess speaking softly close to the ear with dreamy romantic tenderness",
        emotionalStyleCue = "intimate velvety romance, breathy closeness, melting heartfelt warmth"
    ),

    // 4 Maximally Contrasting Male Shayar Voices across bass, baritone, tenor & fire
    DARD_SHAYAR(
        id = "dard_shayar",
        labelHi = "दर्द-ए-दिल शायर",
        subtitleHi = "सबसे गहरी, भारी और दर्द भरी आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🥀",
        geminiVoiceName = "Charon",
        voiceDirection = "a deep, dark, low-baritone North Indian shayar speaking quietly at midnight with raw heavy heartbreak and lingering sighs",
        emotionalStyleCue = "deepest resonant ache, raw wounded gravity, deeply human vulnerability"
    ),
    TADAP_AASHIQ(
        id = "tadap_aashiq",
        labelHi = "तड़पता आशिक़",
        subtitleHi = "जवान, इश्क़ और तड़प भरी आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "❤️‍🔥",
        geminiVoiceName = "Puck",
        voiceDirection = "a youthful, earnest young North Indian lover with an expressive lively voice, raw emotional honesty, and youthful energy",
        emotionalStyleCue = "earnest youthful longing, natural conversational warmth, heartfelt emotional swell"
    ),
    MEHFIL_SHAYAR(
        id = "mehfil_shayar",
        labelHi = "महफ़िल उस्ताद",
        subtitleHi = "मैख़ाना व मुशायरा की खनकती आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🎙️",
        geminiVoiceName = "Algenib",
        voiceDirection = "a seasoned, smoky-voiced Lucknow Mushaira shayar in his late 30s with rich husky warmth and effortless Urdu-Hindi tehzeeb",
        emotionalStyleCue = "smoky late-night Mushaira & maikhana depth, reflective philosophical poise"
    ),
    JOSH_SHAYAR(
        id = "josh_shayar",
        labelHi = "जोशीला मंच शायर",
        subtitleHi = "दमदार, एटीट्यूड और हौसले वाली आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🔥",
        geminiVoiceName = "Fenrir",
        voiceDirection = "a fierce, passionate, gravelly-voiced Indian stage shayar with commanding chest power and fearless attitude",
        emotionalStyleCue = "fearless attitude swagger, electrifying stage fire, hard-hitting closing drop"
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
                tags = emptyList(),
                pageIndex = 0,
                stableKey = categoryId
            )
        }

        /**
         * Selects an emotion-authentic Shayar/Shayara voice for the Reel's category, tags, and text,
         * while STRICTLY alternating Female <-> Male and contrasting voice timbres on consecutive
         * pages (`pageIndex`) so NO two adjacent Reels ever sound like the same Shayar.
         */
        fun recommendedForReel(
            categoryId: String,
            couplet: String,
            pageIndex: Int,
            stableKey: String,
            tags: List<String> = emptyList()
        ): ShayarVoicePersona {
            val emotion = ShayarVoiceEngine.detectCoupletEmotion(
                couplet = couplet,
                categoryId = categoryId,
                tags = tags
            )

            // Emotion-authentic Female and Male voice pairs.
            // Strict parity rule: EVEN pageIndex -> Female pool, ODD pageIndex -> Male pool
            // (so adjacent Reels ALWAYS flip gender & vocal timbre even across different categories!).
            val (femalePool, malePool) = when (emotion) {
                CoupletEmotion.BEWAFAI_BETRAYAL ->
                    listOf(TADAP_SHAYARA, DARD_SHAYARA) to listOf(DARD_SHAYAR, JOSH_SHAYAR, MEHFIL_SHAYAR)

                CoupletEmotion.DARD_TEARFUL ->
                    listOf(DARD_SHAYARA, TADAP_SHAYARA) to listOf(DARD_SHAYAR, MEHFIL_SHAYAR)

                CoupletEmotion.TANHAI_LONELY ->
                    listOf(DARD_SHAYARA, ROMANTIC_SHAYARA) to listOf(MEHFIL_SHAYAR, DARD_SHAYAR)

                CoupletEmotion.TADAP_YEARNING ->
                    listOf(TADAP_SHAYARA, DARD_SHAYARA) to listOf(TADAP_AASHIQ, DARD_SHAYAR)

                CoupletEmotion.SHARABI_MAIKHANA ->
                    listOf(TADAP_SHAYARA, ROMANTIC_SHAYARA) to listOf(MEHFIL_SHAYAR, DARD_SHAYAR)

                CoupletEmotion.MAUT_JANAZA_SOLEMN ->
                    listOf(DARD_SHAYARA, TADAP_SHAYARA) to listOf(DARD_SHAYAR, MEHFIL_SHAYAR)

                CoupletEmotion.INTIMATE_ROMANTIC ->
                    listOf(ROMANTIC_SHAYARA, SUREELI_SHAYARA) to listOf(TADAP_AASHIQ, MEHFIL_SHAYAR)

                CoupletEmotion.BOLD_FLIRTY ->
                    listOf(ROMANTIC_SHAYARA, TADAP_SHAYARA) to listOf(MEHFIL_SHAYAR, TADAP_AASHIQ)

                CoupletEmotion.ATTITUDE_SWAGGER ->
                    listOf(TADAP_SHAYARA, SUREELI_SHAYARA) to listOf(JOSH_SHAYAR, MEHFIL_SHAYAR)

                CoupletEmotion.HAUSLA_DESHPREM_FIRE ->
                    listOf(TADAP_SHAYARA, SUREELI_SHAYARA) to listOf(JOSH_SHAYAR, TADAP_AASHIQ)

                CoupletEmotion.DOSTI_KHUSHI_FESTIVE ->
                    listOf(SUREELI_SHAYARA, ROMANTIC_SHAYARA) to listOf(TADAP_AASHIQ, MEHFIL_SHAYAR)

                CoupletEmotion.CUTE_GENZ_PLAYFUL,
                CoupletEmotion.COMEDY_WITTY ->
                    listOf(SUREELI_SHAYARA, TADAP_SHAYARA) to listOf(TADAP_AASHIQ, JOSH_SHAYAR)

                CoupletEmotion.SOULFUL_SUFI_WISDOM ->
                    listOf(ROMANTIC_SHAYARA, DARD_SHAYARA) to listOf(MEHFIL_SHAYAR, DARD_SHAYAR)
            }

            val absPage = abs(pageIndex)
            val pickMale = (absPage % 2) == 1
            val chosenPool = if (pickMale) malePool else femalePool
            val slot = (absPage / 2) % chosenPool.size
            return chosenPool[slot]
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
            for (legacyName in LEGACY_VOICE_CACHE_DIRS) {
                runCatching {
                    val legacyDir = File(appCtx.filesDir, legacyName)
                    if (legacyDir.exists()) legacyDir.deleteRecursively()
                }
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
        persona: ShayarVoicePersona,
        tags: List<String> = emptyList()
    ): File? {
        val cleanCouplet = couplet.trim()
        if (cleanCouplet.isBlank()) return null
        scheduleLegacyCacheCleanupOnce(context)
        val baseFile = resolveCacheFile(context, cleanCouplet, categoryId, tags, persona)
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
        persona: ShayarVoicePersona,
        tags: List<String> = emptyList()
    ): Boolean {
        val cleanCouplet = couplet.trim()
        if (cleanCouplet.isBlank()) return false
        val baseFile = resolveCacheFile(context, cleanCouplet, categoryId, tags, persona)
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
        tags: List<String> = emptyList(),
        onPcmChunk: suspend (pcmBytes: ByteArray, sampleRate: Int, channels: Int) -> Boolean
    ): ShayarVoiceStreamResult {
        val cleanCouplet = couplet.trim()
        if (cleanCouplet.isBlank()) {
            return ShayarVoiceStreamResult.Error("शायरी खाली है।")
        }

        val appCtx = context.applicationContext
        scheduleLegacyCacheCleanupOnce(appCtx)
        val cacheFile = resolveCacheFile(appCtx, cleanCouplet, categoryId, tags, persona)
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
            categoryId = categoryId,
            tags = tags
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
                when (val unary = getOrGenerateVoice(appCtx, cleanCouplet, categoryId, persona, tags)) {
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
                    when (val unary = getOrGenerateVoice(appCtx, cleanCouplet, categoryId, persona, tags)) {
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
        persona: ShayarVoicePersona,
        tags: List<String> = emptyList()
    ): ShayarVoiceResult {
        val cleanCouplet = couplet.trim()
        if (cleanCouplet.isBlank()) {
            return ShayarVoiceResult.Error("शायरी खाली है।")
        }

        val appCtx = context.applicationContext
        scheduleLegacyCacheCleanupOnce(appCtx)
        val cacheFile = resolveCacheFile(appCtx, cleanCouplet, categoryId, tags, persona)
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
                    generateVoiceInternal(cleanCouplet, categoryId, tags, persona, cacheFile)
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
        persona: ShayarVoicePersona,
        tags: List<String> = emptyList()
    ) {
        val cleanCouplet = couplet.trim()
        if (cleanCouplet.isBlank() || isInFallbackCooldown()) return
        val appCtx = context.applicationContext
        val cacheFile = resolveCacheFile(appCtx, cleanCouplet, categoryId, tags, persona)
        if (findExistingCacheFile(cacheFile) != null) return
        val requestKey = cacheFile.nameWithoutExtension
        if (inFlightRequests.containsKey(requestKey)) return
        runCatching {
            getOrGenerateVoice(appCtx, cleanCouplet, categoryId, persona, tags)
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
        tags: List<String>,
        persona: ShayarVoicePersona,
        cacheFile: File
    ): ShayarVoiceResult {
        findExistingCacheFile(cacheFile)?.let { return ShayarVoiceResult.Ready(it) }

        val prompt = buildPoeticRecitationPrompt(
            couplet = cleanCouplet,
            persona = persona,
            categoryId = categoryId,
            tags = tags
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

    /**
     * Determines the authentic emotional mood strictly from the shayari's tags, category, and words.
     * The voice persona NEVER overrides the shayari's real category/tag emotion.
     */
    fun detectCoupletEmotion(
        couplet: String,
        categoryId: String,
        tags: List<String> = emptyList()
    ): CoupletEmotion {
        val tagText = tags.joinToString(" ").lowercase()
        val coupletLower = couplet.lowercase()
        val combined = "$tagText $coupletLower"
        val hasExplicitCategory = categoryId.isNotBlank()
        val cat = ShayariCategory.fromId(categoryId)

        fun matches(words: List<String>, target: String = combined): Boolean =
            words.any { it in target }

        val bewafaiWords = listOf(
            "बेवफ़ा", "बेवफा", "धोखा", "छल", "झूठे वादे", "वादा तोड़", "बदल गए", "ग़ैर", "गैर", "फरेब", "फ़रेब",
            "मतलबी", "दगा", "दग़ा", "bewafa", "bewafai", "dhoka", "betrayal", "cheat", "fareb", "matlabi"
        )
        val sharabiWords = listOf(
            "शराब", "शराबी", "जाम", "पैमाना", "साक़ी", "साकी", "मयख़ाना", "मयखाना", "मैख़ाना", "मैखाना", "नशा", "मदहोश",
            "sharabi", "sharab", "jaam", "saqi", "maikhana", "nasha"
        )
        val mautWords = listOf(
            "मौत", "जनाज़ा", "जनाजा", "कफ़न", "कफन", "कब्र", "क़ब्र", "श्मशान", "आख़िरी सफ़र",
            "maut", "janaza", "kafan", "qabr", "death"
        )
        val dardWords = listOf(
            "दर्द", "आँसू", "आंसू", "अश्क", "रोया", "रोना", "रोते", "ज़ख्म", "जख्म", "टूटा", "टूट गया", "ग़म", "गम",
            "दर्द-ए-दिल", "दिल टूट", "dard", "aansu", "zakhm", "gham", "sad", "pain", "heartbreak", "crying", "ashk"
        )
        val tanhaiWords = listOf(
            "तन्हाई", "तन्हा", "तनहा", "अकेला", "अकेले", "सन्नाटा", "यादें", "ख़ामोशी", "खामोशी", "आईना", "आइना",
            "tanhai", "tanha", "lonely", "alone", "yaadein", "khamoshi", "aaina"
        )
        val tadapWords = listOf(
            "तड़प", "तडप", "इंतज़ार", "इंतजार", "जुदाई", "बिन तेरे", "लौट आ", "तरस", "बिछड़", "याद आ",
            "tadap", "intezaar", "intezar", "judai", "taras", "bin tere", "missing"
        )
        val attitudeWords = listOf(
            "एटीट्यूड", "तेवर", "औक़ात", "औकात", "रुतबा", "ताज", "दुश्मन", "शेर", "अंदाज़", "अंदाज", "बादशाह", "रॉयल",
            "attitude", "aukat", "tevar", "royal", "swag", "dushman", "king", "queen"
        )
        val hauslaDeshWords = listOf(
            "हौसला", "उम्मीद", "जीत", "ज़िद", "जिद", "संघर्ष", "देशभक्ति", "वतन", "तिरंगा", "शहीद", "भारत", "हिंदुस्तान",
            "hausla", "motivation", "jeet", "zid", "deshprem", "vatan", "patriotic", "bharat"
        )
        val boldFlirtyWords = listOf(
            "हॉट", "बोल्ड", "फ्लर्ट", "शरारत", "नटखट", "होंठ", "करीब", "आगोश", "बेक़ाबू",
            "hot", "bold", "flirty", "secret", "naughty", "adultery"
        )
        val comedyWords = listOf(
            "चुटकुला", "कॉमेडी", "मज़ाक", "मजाक", "ठहाके", "हँसी", "हंसी", "संता", "मुल्ला", "मोटू", "जीजा",
            "comedy", "funny", "joke", "jokes", "meme"
        )
        val cuteGenzWords = listOf(
            "निब्बा", "निब्बी", "बाबू", "सोना", "क्यूट", "सीन-ज़ोन", "क्रश", "वाइब",
            "nibba", "nibbi", "babu", "sona", "gen-z", "genz", "vibe", "crush", "cute"
        )
        val dostiKhushiWords = listOf(
            "दोस्ती", "यारी", "यार", "दोस्त", "ख़ुशी", "खुशी", "परिवार", "माँ", "पापा", "नया साल", "क्रिसमस",
            "dosti", "yaari", "friend", "friends", "khushi", "happy", "family", "parivar", "new year", "christmas"
        )
        val romanticWords = listOf(
            "मोहब्बत", "इश्क़", "इश्क", "रोमांटिक", "प्यार", "तारीफ़", "हुस्न", "चाँद", "बाहों", "धड़कन", "सनम", "दिलबर",
            "mohabbat", "ishq", "romantic", "pyaar", "pyar", "love", "tareef", "sanam", "dilbar"
        )
        val sufiLifeWords = listOf(
            "ख़ुदा", "खुदा", "रब", "दुआ", "इबादत", "सजदा", "ज़िंदगी", "जिंदगी", "वक़्त", "वक्त", "मुसाफ़िर", "सफ़र", "बारिश", "प्रकृति",
            "khuda", "rab", "dua", "sufi", "zindagi", "waqt", "musafir", "safar", "prakriti", "rain"
        )

        // 1. Explicit Tags ALWAYS take highest priority so user-tagged or mood-tagged Reels react authentically
        if (tagText.isNotBlank()) {
            when {
                matches(bewafaiWords, tagText) -> return CoupletEmotion.BEWAFAI_BETRAYAL
                matches(sharabiWords, tagText) -> return CoupletEmotion.SHARABI_MAIKHANA
                matches(mautWords, tagText) -> return CoupletEmotion.MAUT_JANAZA_SOLEMN
                matches(comedyWords, tagText) -> return CoupletEmotion.COMEDY_WITTY
                matches(cuteGenzWords, tagText) -> return CoupletEmotion.CUTE_GENZ_PLAYFUL
                matches(attitudeWords, tagText) -> return CoupletEmotion.ATTITUDE_SWAGGER
                matches(hauslaDeshWords, tagText) -> return CoupletEmotion.HAUSLA_DESHPREM_FIRE
                matches(boldFlirtyWords, tagText) -> return CoupletEmotion.BOLD_FLIRTY
                matches(dardWords, tagText) -> return CoupletEmotion.DARD_TEARFUL
                matches(tadapWords, tagText) -> return CoupletEmotion.TADAP_YEARNING
                matches(tanhaiWords, tagText) -> return CoupletEmotion.TANHAI_LONELY
                matches(dostiKhushiWords, tagText) -> return CoupletEmotion.DOSTI_KHUSHI_FESTIVE
                matches(sufiLifeWords, tagText) -> return CoupletEmotion.SOULFUL_SUFI_WISDOM
                matches(romanticWords, tagText) -> return CoupletEmotion.INTIMATE_ROMANTIC
            }
        }

        // 2. Specific Shayari Category drives the core Shayar reaction directly
        if (hasExplicitCategory) {
            when (cat) {
                ShayariCategory.BEWAFAI -> return CoupletEmotion.BEWAFAI_BETRAYAL
                ShayariCategory.ADULTERY -> return if (matches(bewafaiWords, coupletLower)) {
                    CoupletEmotion.BEWAFAI_BETRAYAL
                } else {
                    CoupletEmotion.BOLD_FLIRTY
                }
                ShayariCategory.SHARABI -> return CoupletEmotion.SHARABI_MAIKHANA
                ShayariCategory.MAUT_JANAZA -> return CoupletEmotion.MAUT_JANAZA_SOLEMN
                ShayariCategory.DARD,
                ShayariCategory.AANSU -> return if (matches(bewafaiWords, coupletLower)) {
                    CoupletEmotion.BEWAFAI_BETRAYAL
                } else {
                    CoupletEmotion.DARD_TEARFUL
                }
                ShayariCategory.TANHAI,
                ShayariCategory.YAADEIN,
                ShayariCategory.AAINA -> return if (matches(tadapWords, coupletLower)) {
                    CoupletEmotion.TADAP_YEARNING
                } else {
                    CoupletEmotion.TANHAI_LONELY
                }
                ShayariCategory.ATTITUDE -> return CoupletEmotion.ATTITUDE_SWAGGER
                ShayariCategory.HAUSLA,
                ShayariCategory.DESHPREM -> return CoupletEmotion.HAUSLA_DESHPREM_FIRE
                ShayariCategory.SECRET_LOVE -> return CoupletEmotion.BOLD_FLIRTY
                ShayariCategory.JIJA_SALI,
                ShayariCategory.MULLA_NASRUDDIN,
                ShayariCategory.SANTA_BANTA,
                ShayariCategory.MOTU_PATLU -> return CoupletEmotion.COMEDY_WITTY
                ShayariCategory.NIBBA_NIBBI,
                ShayariCategory.GEN_Z -> return CoupletEmotion.CUTE_GENZ_PLAYFUL
                ShayariCategory.DOSTI,
                ShayariCategory.KHUSHI,
                ShayariCategory.NEW_YEAR,
                ShayariCategory.CHRISTMAS,
                ShayariCategory.PARIVAR -> return CoupletEmotion.DOSTI_KHUSHI_FESTIVE
                ShayariCategory.KHUDA_RAB,
                ShayariCategory.ZINDAGI,
                ShayariCategory.WAQT,
                ShayariCategory.MUSAFIR,
                ShayariCategory.PRAKRITI -> return CoupletEmotion.SOULFUL_SUFI_WISDOM
                ShayariCategory.MOHABBAT,
                ShayariCategory.ROMANTIC,
                ShayariCategory.TAREEF -> {
                    // Even inside Mohabbat, respect strong betrayal/pain/yearning words in the couplet itself!
                    return when {
                        matches(bewafaiWords, coupletLower) -> CoupletEmotion.BEWAFAI_BETRAYAL
                        matches(dardWords, coupletLower) && !matches(romanticWords, coupletLower) -> CoupletEmotion.DARD_TEARFUL
                        matches(tadapWords, coupletLower) -> CoupletEmotion.TADAP_YEARNING
                        else -> CoupletEmotion.INTIMATE_ROMANTIC
                    }
                }
            }
        }

        // 3. Fallback to couplet text keywords (for custom Reels with empty category)
        return when {
            matches(bewafaiWords, coupletLower) -> CoupletEmotion.BEWAFAI_BETRAYAL
            matches(sharabiWords, coupletLower) -> CoupletEmotion.SHARABI_MAIKHANA
            matches(mautWords, coupletLower) -> CoupletEmotion.MAUT_JANAZA_SOLEMN
            matches(comedyWords, coupletLower) -> CoupletEmotion.COMEDY_WITTY
            matches(cuteGenzWords, coupletLower) -> CoupletEmotion.CUTE_GENZ_PLAYFUL
            matches(attitudeWords, coupletLower) -> CoupletEmotion.ATTITUDE_SWAGGER
            matches(hauslaDeshWords, coupletLower) -> CoupletEmotion.HAUSLA_DESHPREM_FIRE
            matches(boldFlirtyWords, coupletLower) -> CoupletEmotion.BOLD_FLIRTY
            matches(dardWords, coupletLower) -> CoupletEmotion.DARD_TEARFUL
            matches(tadapWords, coupletLower) -> CoupletEmotion.TADAP_YEARNING
            matches(tanhaiWords, coupletLower) -> CoupletEmotion.TANHAI_LONELY
            matches(dostiKhushiWords, coupletLower) -> CoupletEmotion.DOSTI_KHUSHI_FESTIVE
            matches(sufiLifeWords, coupletLower) -> CoupletEmotion.SOULFUL_SUFI_WISDOM
            else -> CoupletEmotion.INTIMATE_ROMANTIC
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
        categoryId: String = "",
        tags: List<String> = emptyList()
    ): String {
        val lines = couplet
            .replace("\r\n", "\n")
            .split("\n")
            .map { sanitizeLineForTranscript(it) }
            .filter { line -> line.any { ch -> ch.isLetterOrDigit() } }

        val emotion = detectCoupletEmotion(couplet, categoryId, tags)

        // Clean plain-text acting cue (no Markdown '#' headers) tailored strictly to the category/tag emotion
        val actingDirection = when (emotion) {
            CoupletEmotion.BEWAFAI_BETRAYAL ->
                "Perform this Hindi-Urdu bewafai shayari as ${persona.voiceDirection}, reacting with raw bitter betrayal, wounded pride, stinging hurt, and an accusing broken edge as if confronting an unfaithful lover:"
            CoupletEmotion.DARD_TEARFUL ->
                "Perform this Hindi-Urdu dard shayari as ${persona.voiceDirection}, letting genuine tearful pain drip from every word with natural trembling breath and quiet heartbreak:"
            CoupletEmotion.TANHAI_LONELY ->
                "Perform this Hindi-Urdu tanhai shayari as ${persona.voiceDirection}, speaking softly in quiet midnight loneliness with hollow nostalgic ache and lingering silence:"
            CoupletEmotion.TADAP_YEARNING ->
                "Perform this Hindi-Urdu longing shayari as ${persona.voiceDirection}, with restless desperate yearning, breathless emotional urgency, and aching desire:"
            CoupletEmotion.SHARABI_MAIKHANA ->
                "Perform this Hindi-Urdu sharabi shayari as ${persona.voiceDirection}, like a late-night maikhana poet — slightly tipsy, husky, wryly smiling through hidden heartbreak:"
            CoupletEmotion.MAUT_JANAZA_SOLEMN ->
                "Perform this Hindi-Urdu shayari as ${persona.voiceDirection}, in a hushed, deeply solemn, hauntingly still voice full of quiet grief and gravity:"
            CoupletEmotion.INTIMATE_ROMANTIC ->
                "Perform this Hindi-Urdu romantic shayari as ${persona.voiceDirection}, close-mic and deeply in love, warm, velvety, and tenderly affectionate:"
            CoupletEmotion.BOLD_FLIRTY ->
                "Perform this Hindi-Urdu flirty shayari as ${persona.voiceDirection}, with a teasing mischievous smile, sultry playful charm, and close-mic whisper:"
            CoupletEmotion.ATTITUDE_SWAGGER ->
                "Perform this Hindi-Urdu attitude shayari as ${persona.voiceDirection}, with ice-cold fearless swagger, calm unbothered smirk, and a fierce commanding closing punch:"
            CoupletEmotion.HAUSLA_DESHPREM_FIRE ->
                "Perform this Hindi-Urdu inspiring shayari as ${persona.voiceDirection}, with electrifying passion, proud chest voice, and goosebump-inducing conviction:"
            CoupletEmotion.DOSTI_KHUSHI_FESTIVE ->
                "Perform this Hindi-Urdu joyful shayari as ${persona.voiceDirection}, with a big genuine smile, bright friendly warmth, and affectionate laughter:"
            CoupletEmotion.CUTE_GENZ_PLAYFUL ->
                "Perform this Hindi-Urdu Gen-Z cute shayari as ${persona.voiceDirection}, with playful bubbly youth energy, teasing warmth, and relatable conversational charm:"
            CoupletEmotion.COMEDY_WITTY ->
                "Perform this Hindi-Urdu funny shayari as ${persona.voiceDirection}, with lively comedic storytelling timing and a playful laugh on the punchline:"
            CoupletEmotion.SOULFUL_SUFI_WISDOM ->
                "Perform this Hindi-Urdu soulful shayari as ${persona.voiceDirection}, with serene Sufi warmth, grounded human wisdom, and peaceful unhurried breaths:"
        }

        val taggedTranscript = buildString {
            lines.forEachIndexed { idx, rawLine ->
                val isFirst = idx == 0
                val isLast = idx == lines.lastIndex
                val clean = applyPoeticCaesura(rawLine.trimEnd('।', '.', ',', ' ', '—', '-'))
                val endingPunct = when {
                    rawLine.endsWith("?") -> "?"
                    rawLine.endsWith("!") -> "!"
                    (emotion == CoupletEmotion.ATTITUDE_SWAGGER || emotion == CoupletEmotion.HAUSLA_DESHPREM_FIRE) && isLast -> "!"
                    isLast -> "।"
                    else -> ","
                }

                if (idx > 0) append("\n")

                val expressiveTag = when (emotion) {
                    CoupletEmotion.BEWAFAI_BETRAYAL -> when {
                        isFirst -> "[bitterly hurt, wounded] "
                        isLast -> "[heavy sigh] [deeply betrayed, voice breaking with hurt] "
                        else -> "[scoffs softly, pained] "
                    }
                    CoupletEmotion.DARD_TEARFUL -> when {
                        isFirst -> "[softly, aching with pain] "
                        isLast -> "[sigh] [crying softly, voice trembling] "
                        else -> "[tearful, voice cracking] "
                    }
                    CoupletEmotion.TANHAI_LONELY -> when {
                        isFirst -> "[quietly, lonely and nostalgic] "
                        isLast -> "[soft sigh] [whispering, hollow ache] "
                        else -> "[softly, empty] "
                    }
                    CoupletEmotion.TADAP_YEARNING -> when {
                        isFirst -> "[longingly, breathless] "
                        isLast -> "[sigh] [pleading, deeply emotional] "
                        else -> "[achingly, earnest] "
                    }
                    CoupletEmotion.SHARABI_MAIKHANA -> when {
                        isFirst -> "[husky, slightly tipsy, wry smile] "
                        isLast -> "[soft laugh] [sigh] [raspy, painfully honest] "
                        else -> "[slurring gently, reflective] "
                    }
                    CoupletEmotion.MAUT_JANAZA_SOLEMN -> when {
                        isFirst -> "[hushed, deeply solemn] "
                        isLast -> "[long pause] [hauntingly quiet, heavy sorrow] "
                        else -> "[slowly, somber] "
                    }
                    CoupletEmotion.INTIMATE_ROMANTIC -> when {
                        isFirst -> "[warmly, deeply in love] "
                        isLast -> "[soft smile] [whispering, adoringly] "
                        else -> "[tenderly, softly] "
                    }
                    CoupletEmotion.BOLD_FLIRTY -> when {
                        isFirst -> "[teasingly, sultry smile] "
                        isLast -> "[whispering, mischievously] "
                        else -> "[playfully, close-mic] "
                    }
                    CoupletEmotion.ATTITUDE_SWAGGER -> when {
                        isFirst -> "[calmly, arrogant smirk] "
                        isLast -> "[short pause] [firm, fierce commanding authority] "
                        else -> "[confidently, sharp] "
                    }
                    CoupletEmotion.HAUSLA_DESHPREM_FIRE -> when {
                        isFirst -> "[proudly, passionate] "
                        isLast -> "[short pause] [energetically, powerful conviction] "
                        else -> "[rising passion] "
                    }
                    CoupletEmotion.DOSTI_KHUSHI_FESTIVE -> when {
                        isFirst -> "[cheerfully, warm smile] "
                        isLast -> "[warm chuckle] [joyfully, heartfelt] "
                        else -> "[brightly, friendly] "
                    }
                    CoupletEmotion.CUTE_GENZ_PLAYFUL -> when {
                        isFirst -> "[playfully, bubbly energy] "
                        isLast -> "[giggles] [cheekily, amused] "
                        else -> "[teasingly] "
                    }
                    CoupletEmotion.COMEDY_WITTY -> when {
                        isFirst -> "[amused, lively storytelling] "
                        isLast -> "[chuckle] [laughing playfully] "
                        else -> "[wryly] "
                    }
                    CoupletEmotion.SOULFUL_SUFI_WISDOM -> when {
                        isFirst -> "[serenely, soulful warmth] "
                        isLast -> "[gentle sigh] [deeply reflective, wise] "
                        else -> "[soothingly] "
                    }
                }

                append(expressiveTag)
                append(clean)
                append(endingPunct)
            }
        }

        return "$actingDirection\n\n$taggedTranscript"
    }

    private fun resolveCacheFile(
        context: Context,
        couplet: String,
        categoryId: String,
        tags: List<String>,
        persona: ShayarVoicePersona
    ): File {
        // Store in filesDir so generated Shayar voices persist across cache sweeps and save quota
        val dir = File(context.applicationContext.filesDir, VOICE_CACHE_DIR).apply { mkdirs() }
        val emotion = detectCoupletEmotion(couplet, categoryId, tags)
        val key = sha256Short("${persona.id}|${persona.geminiVoiceName}|${emotion.name}|$couplet")
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
