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
private const val VOICE_CACHE_DIR = "shayar_voices_v11"
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
    // 4 Strongly Contrasting Female Shayara Voices
    DARD_SHAYARA(
        id = "dard_shayara",
        labelHi = "दर्द भरी शायरा",
        subtitleHi = "भीगी, टूटे दिल की दर्द भरी आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "💔",
        geminiVoiceName = "Vindemiatrix",
        voiceDirection = "a fragile, tearful young North Indian woman speaking slowly with a quivering heartbroken voice",
        emotionalStyleCue = "slow, misty, aching heartbreak as if holding back tears"
    ),
    SUREELI_SHAYARA(
        id = "sureeli_shayara",
        labelHi = "सुरीली शायरा",
        subtitleHi = "मीठी, खिलखिलाती व सुरीली आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🎶",
        geminiVoiceName = "Leda",
        voiceDirection = "a bright, cheerful, melodious young Indian poetess speaking at a lively warm tempo with a natural smile",
        emotionalStyleCue = "light, sweet, musical charm with playful affectionate sparkle"
    ),
    TADAP_SHAYARA(
        id = "tadap_shayara",
        labelHi = "तेवर व जज़्बात शायरा",
        subtitleHi = "दमदार, तड़प और स्वाभिमान भरी आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🥺",
        geminiVoiceName = "Kore",
        voiceDirection = "a clear, emotionally fierce, expressive Indian woman with dramatic intensity and strong vocal presence",
        emotionalStyleCue = "bold poignant emotion, sharp dramatic tension, powerful closing punch"
    ),
    ROMANTIC_SHAYARA(
        id = "romantic_shayara",
        labelHi = "रोमांटिक शायरा",
        subtitleHi = "मखमली, ख़्वाब सी रोमांटिक आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🌹",
        geminiVoiceName = "Aoede",
        voiceDirection = "a warm, silky, breezy young Indian woman speaking romantically with dreamy velvety charm",
        emotionalStyleCue = "warm affectionate closeness, lyrical flow, dreamy romantic glow"
    ),

    // 4 Strongly Contrasting Male Shayar Voices
    DARD_SHAYAR(
        id = "dard_shayar",
        labelHi = "दर्द-ए-दिल शायर",
        subtitleHi = "गहरी, भारी और दर्द भरी आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🥀",
        geminiVoiceName = "Enceladus",
        voiceDirection = "a deep, husky, breathy North Indian man speaking slowly with heavy late-night grief",
        emotionalStyleCue = "deep gravelly ache, slow lingering emotional weight"
    ),
    TADAP_AASHIQ(
        id = "tadap_aashiq",
        labelHi = "तड़पता आशिक़",
        subtitleHi = "इश्क़ की तड़प और जुनून भरी आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "❤️‍🔥",
        geminiVoiceName = "Puck",
        voiceDirection = "an energetic, passionate young Indian man speaking with intense heartfelt urgency and rising emotion",
        emotionalStyleCue = "lively passionate longing, rising emotional crescendo on the last line"
    ),
    MEHFIL_SHAYAR(
        id = "mehfil_shayar",
        labelHi = "महफ़िल उस्ताद",
        subtitleHi = "रॉयल मुशायरा मंच की भारी आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🎙️",
        geminiVoiceName = "Algenib",
        voiceDirection = "a commanding, seasoned Lucknow Mushaira poet in his 40s with a rich resonant baritone and royal theatrical cadence",
        emotionalStyleCue = "poised regal Mushaira storytelling, dramatic pause before landing the sher punchline"
    ),
    JOSH_SHAYAR(
        id = "josh_shayar",
        labelHi = "जोशीला मंच शायर",
        subtitleHi = "दमदार, एटीट्यूड और हौसले वाली आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🔥",
        geminiVoiceName = "Fenrir",
        voiceDirection = "a bold, fearless, high-energy Indian stage poet with powerful chest voice and sharp North Indian swagger",
        emotionalStyleCue = "confident punchy pace, fiery bold authority, hard-hitting closing line"
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

/**
 * Generates expressive, human-like Hindi/Urdu Mushaira poet & poetess voice recitations
 * using Gemini Flash TTS via Firebase AI Logic (`ResponseModality.AUDIO` + `SpeechConfig(Voice)`),
 * with durable disk caching in `filesDir`, single-flight request deduplication, and
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
     * Quietly pre-generates the single upcoming Reel's voice on disk AFTER the current Reel is
     * already playing, so swiping to the next Reel starts in 0ms with zero loading delay.
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

            when {
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

        // Concise, sharply contrasting "Say ...:" directives so every persona & mood has a
        // distinct tempo, pitch contour, and emotional energy (and generates faster!).
        val styleDirective = when (emotion) {
            CoupletEmotion.DEEP_HEARTBREAK ->
                "Say this Hindi-Urdu shayari slowly and emotionally as ${persona.voiceDirection}, with fragile raw heartbreak and voice trembling softly on the closing line:"
            CoupletEmotion.PASSIONATE_YEARNING ->
                "Say this Hindi-Urdu shayari passionately as ${persona.voiceDirection}, with expressive intensity, rising heartfelt longing, and emotional weight on the last line:"
            CoupletEmotion.PLAYFUL_ROMANCE ->
                "Say this Hindi-Urdu shayari warmly at a lively, smiling conversational pace as ${persona.voiceDirection}, with sweet musical lilt and bright affectionate joy:"
            CoupletEmotion.INTIMATE_ROMANTIC ->
                "Say this Hindi-Urdu shayari tenderly as ${persona.voiceDirection}, with velvety romantic warmth, gentle intimacy, and soft lingering charm:"
            CoupletEmotion.WITTY_LIGHTHEARTED ->
                "Say this Hindi-Urdu piece cheerfully at an upbeat, playful pace as ${persona.voiceDirection}, with a warm smile and amused lighthearted timing:"
            CoupletEmotion.BOLD_ATTITUDE ->
                "Say this Hindi-Urdu shayari boldly at a crisp, energetic stage pace as ${persona.voiceDirection}, with fearless swagger and a sharp punch on the final line:"
            CoupletEmotion.ROYAL_MEHFIL ->
                "Recite this Hindi-Urdu sher with classic Lucknow Mushaira poise as ${persona.voiceDirection}, building regal suspense on line one and landing the punchline with resonant authority:"
            CoupletEmotion.SOULFUL_SUFI ->
                "Say this Hindi-Urdu shayari calmly and deeply as ${persona.voiceDirection}, with grounded Sufi serenity, warm conversational wisdom, and peaceful gravity:"
        }

        val taggedTranscript = buildString {
            lines.forEachIndexed { idx, rawLine ->
                val isFirst = idx == 0
                val isLast = idx == lines.lastIndex
                val clean = rawLine.trimEnd('।', '.', ',', ' ', '—', '-')
                val endingPunct = when {
                    rawLine.endsWith("?") -> "?"
                    rawLine.endsWith("!") -> "!"
                    emotion == CoupletEmotion.BOLD_ATTITUDE && isLast -> "!"
                    isLast -> "।"
                    else -> ","
                }

                if (idx > 0) append("\n")

                val prefixTag = when (emotion) {
                    CoupletEmotion.DEEP_HEARTBREAK -> when {
                        isLast && lines.size >= 2 -> "[medium pause] [sigh] "
                        isFirst -> ""
                        else -> "[short pause] "
                    }
                    CoupletEmotion.PASSIONATE_YEARNING -> when {
                        isLast && lines.size >= 2 -> "[short pause] "
                        else -> ""
                    }
                    CoupletEmotion.PLAYFUL_ROMANCE -> when {
                        isFirst -> ""
                        else -> "[short pause] "
                    }
                    CoupletEmotion.INTIMATE_ROMANTIC -> when {
                        isFirst -> ""
                        else -> "[short pause] "
                    }
                    CoupletEmotion.WITTY_LIGHTHEARTED -> when {
                        isLast && lines.size >= 2 -> "[short pause] [chuckle] "
                        else -> ""
                    }
                    CoupletEmotion.BOLD_ATTITUDE -> when {
                        isLast && lines.size >= 2 -> "[medium pause] "
                        else -> ""
                    }
                    CoupletEmotion.ROYAL_MEHFIL -> when {
                        isLast && lines.size >= 2 -> "[medium pause] "
                        else -> ""
                    }
                    CoupletEmotion.SOULFUL_SUFI -> when {
                        isFirst -> ""
                        else -> "[short pause] "
                    }
                }

                append(prefixTag)
                append(clean)
                append(endingPunct)
            }
        }

        return "$styleDirective\n\n$taggedTranscript"
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
