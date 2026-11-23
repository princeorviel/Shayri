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
private const val VOICE_CACHE_DIR = "shayar_voices_v8"
private const val PRIMARY_TTS_MODEL = "gemini-3.1-flash-tts-preview"
private const val QUOTA_COOLDOWN_MS = 45_000L
private const val APP_CHECK_COOLDOWN_MS = 120_000L

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
    // 4 Expressive Female Shayara (लड़की शायर) Voices — cast from Gemini's warmest, softest storytelling voices
    DARD_SHAYARA(
        id = "dard_shayara",
        labelHi = "दर्द भरी शायरा",
        subtitleHi = "भीगी, टूटे दिल की दर्द भरी आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "💔",
        geminiVoiceName = "Vindemiatrix",
        voiceDirection = "a heartbroken young Indian woman speaking quietly from her own fresh pain at 2 AM, voice fragile and misty with unshed tears",
        emotionalStyleCue = "raw personal heartbreak, tender quiet ache, speaking to someone she still misses"
    ),
    SUREELI_SHAYARA(
        id = "sureeli_shayara",
        labelHi = "सुरीली शायरा",
        subtitleHi = "मीठी, सुरीली व दिलकश आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🎶",
        geminiVoiceName = "Sulafat",
        voiceDirection = "a warm, sweet-voiced Indian poetess sharing a heartfelt sher with a soft smile and soulful natural Hindustani flow",
        emotionalStyleCue = "warm affectionate intimacy, effortless lyrical charm, smiling softly through the words"
    ),
    TADAP_SHAYARA(
        id = "tadap_shayara",
        labelHi = "प्यार में तड़पती आवाज़",
        subtitleHi = "इंतज़ार और तड़प भरी नाज़ुक आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🥺",
        geminiVoiceName = "Achernar",
        voiceDirection = "a soft, deeply yearning young Indian woman confessing unspoken love and longing in an intimate close voice note",
        emotionalStyleCue = "quiet breathless longing, vulnerable confession, gentle ache in the throat"
    ),
    ROMANTIC_SHAYARA(
        id = "romantic_shayara",
        labelHi = "रोमांटिक शायरा",
        subtitleHi = "प्यार भरी, ख़्वाब सी रोमांटिक आवाज़",
        genderBadgeHi = "लड़की शायर",
        isFemale = true,
        emoji = "🌹",
        geminiVoiceName = "Despina",
        voiceDirection = "a dreamy, smooth-voiced Indian woman whispering loving Urdu-Hindi words closely to someone dear",
        emotionalStyleCue = "velvet romantic warmth, gentle affectionate whisper-soft closeness"
    ),

    // 4 Expressive Male Shayar (शायर) Voices — cast from Gemini's breathy, mature & gravelly storytelling voices
    DARD_SHAYAR(
        id = "dard_shayar",
        labelHi = "दर्द-ए-दिल शायर",
        subtitleHi = "गहरी दर्द और खामोशी भरी आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🥀",
        geminiVoiceName = "Enceladus",
        voiceDirection = "a heartbroken Indian man speaking softly and breathily from deep personal grief in late-night silence",
        emotionalStyleCue = "heavy quiet pain, intimate breathy vulnerability, spoken straight from a bruised heart"
    ),
    TADAP_AASHIQ(
        id = "tadap_aashiq",
        labelHi = "तड़पता आशिक़",
        subtitleHi = "इश्क़ की तड़प और जुनून भरी आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "❤️‍🔥",
        geminiVoiceName = "Gacrux",
        voiceDirection = "a deeply emotional, grounded Indian lover speaking earnestly from the heart about missing his beloved",
        emotionalStyleCue = "sincere heartfelt longing, conversational warmth turning deeply emotional on the final line"
    ),
    MEHFIL_SHAYAR(
        id = "mehfil_shayar",
        labelHi = "महफ़िल शायर",
        subtitleHi = "रेशमी, गहरी मुशायरा शायर आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🎙️",
        geminiVoiceName = "Algenib",
        voiceDirection = "a soulful, rich-voiced Indian Urdu Mushaira shayar with a slightly gravelly lived-in baritone sharing wisdom with friends at midnight",
        emotionalStyleCue = "authentic Mehfil soul, effortless lived-in conversational depth, landing the punchline with quiet weight"
    ),
    JOSH_SHAYAR(
        id = "josh_shayar",
        labelHi = "जोशीला मंच शायर",
        subtitleHi = "दमदार, एटीट्यूड और हौसले वाली आवाज़",
        genderBadgeHi = "शायर",
        isFemale = false,
        emoji = "🔥",
        geminiVoiceName = "Alnilam",
        voiceDirection = "a charismatic, self-assured Indian poet speaking with natural fire, quiet pride, and sharp punchline timing",
        emotionalStyleCue = "confident bold charisma, real human conviction, crisp dramatic pause before the closing punchline"
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
            val response = withTimeout(30_000L) {
                model.generateContent(prompt)
            }

            val allParts = response.candidates.flatMap { it.content.parts }
            val audioPart = allParts
                .filterIsInstance<InlineDataPart>()
                .firstOrNull { part ->
                    part.mimeType.startsWith("audio/", ignoreCase = true) &&
                        part.inlineData.size >= 1_600
                }

            if (audioPart != null) {
                clearQuotaCooldown()
                Log.d(
                    TAG,
                    "Shayar voice ready via $PRIMARY_TTS_MODEL (${persona.id}/${persona.geminiVoiceName}): mime=${audioPart.mimeType}, bytes=${audioPart.inlineData.size}"
                )
                val savedFile = withContext(Dispatchers.IO) {
                    val rawFile = writePlayableAudioFile(
                        targetBaseFile = cacheFile,
                        rawBytes = audioPart.inlineData,
                        mimeType = audioPart.mimeType
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
                    ShayarVoiceResult.Error("ऑडियो साफ़ नहीं मिला — लोकल आवाज़ चालू")
                }
            } else {
                val partSummary = allParts.joinToString { it::class.simpleName ?: "Part" }
                Log.e(TAG, "Model $PRIMARY_TTS_MODEL returned no audio InlineDataPart. Parts=[$partSummary]")
                ShayarVoiceResult.Error("शायर आवाज़ लोड नहीं हो सकी — लोकल आवाज़ चालू")
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
                    fallbackCooldownMessage = "AI कोटा सीमा • लोकल हिंदी आवाज़ सक्रिय"
                    ShayarVoiceResult.FallbackRequired(
                        message = fallbackCooldownMessage,
                        isQuotaError = true
                    )
                }
                isAppCheck -> {
                    fallbackCooldownUntilMs = System.currentTimeMillis() + APP_CHECK_COOLDOWN_MS
                    fallbackCooldownIsAppCheck = true
                    fallbackCooldownIsQuota = false
                    fallbackCooldownMessage = "लोकल हिंदी आवाज़ सक्रिय • AI रिट्राई (App Check)"
                    ShayarVoiceResult.FallbackRequired(
                        message = fallbackCooldownMessage,
                        isAppCheckError = true
                    )
                }
                else -> {
                    ShayarVoiceResult.Error(
                        rawMsg.take(72).takeIf { it.isNotBlank() }
                            ?: "लोकल हिंदी आवाज़ सक्रिय • AI रिट्राई"
                    )
                }
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
        val lines = couplet
            .replace("\r\n", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Minimal, organic inline cues — NEVER use [slowly] or [deliberate] which make Hindi sound
        // like a stretched audiobook narrator. Only natural human breath + pause before the heart-hitting final line.
        val (openingCue, closingCue) = when (persona) {
            ShayarVoicePersona.DARD_SHAYARA,
            ShayarVoicePersona.DARD_SHAYAR -> "[sighs]" to "[pauses] [softly]"
            ShayarVoicePersona.TADAP_SHAYARA,
            ShayarVoicePersona.TADAP_AASHIQ -> "[softly]" to "[pauses] [sighs]"
            ShayarVoicePersona.SUREELI_SHAYARA,
            ShayarVoicePersona.ROMANTIC_SHAYARA -> "[softly]" to "[pauses]"
            ShayarVoicePersona.MEHFIL_SHAYAR -> "" to "[pauses]"
            ShayarVoicePersona.JOSH_SHAYAR -> "" to "[pauses]"
        }

        val taggedTranscript = buildString {
            lines.forEachIndexed { idx, rawLine ->
                val clean = rawLine.trimEnd('।', '.', ',', ' ')
                val lineWithPunct = if (idx < lines.lastIndex) "$clean," else "$clean।"
                val cue = when {
                    idx == 0 -> openingCue
                    idx == lines.lastIndex -> closingCue
                    else -> "[pauses]"
                }
                if (idx > 0) append("\n")
                if (cue.isNotEmpty()) {
                    append(cue)
                    append(" ")
                }
                append(lineWithPunct)
            }
        }

        return buildString {
            appendLine("Perform this Hindi-Urdu shayari as a real Indian person speaking spontaneously from the heart — NOT reading written text from a page.")
            appendLine("Speaker: ${persona.voiceDirection}.")
            appendLine("Emotion: ${persona.emotionalStyleCue}.")
            appendLine("Style: Conversational, intimate late-night Hindustani/Urdu tone like a real Shayar on a viral heartfelt Reel — natural flowing everyday Indian pronunciation, organic breath, subtle rise on the first line, heartfelt emotional weight on the last line. Zero robotic narration, zero stiff word-by-word reading.")
            appendLine()
            appendLine("#### TRANSCRIPT")
            append(taggedTranscript)
        }
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
