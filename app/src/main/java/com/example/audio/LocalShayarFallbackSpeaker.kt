package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.ai.CoupletEmotion
import com.example.ai.ShayarVoiceEngine
import com.example.ai.ShayarVoicePersona
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "LocalShayarSpeaker"
private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
private const val MASTERED_CACHE_DIR = "local_shayar_master_v8"
private const val NETWORK_VOICE_COOLDOWN_MS = 90_000L

/**
 * Reliable, Natural-Prosody Hindi Shayar Voice Speaker.
 *
 * Designed to eliminate BOTH silent shayaris AND robotic TTS artifacts:
 * 1. Single unified UtteranceProgressListener (never clobbered between synthesis and live speech).
 * 2. Instant generation-based cancellation inside `stop()` so swiping Reels NEVER deadlocks
 *    `synthMutex` waiting on stale timeouts.
 * 3. Artifact-free natural speaking speed (0.92f–0.99f) and pitch (~1.0f) — avoids WSOLA
 *    slow-motion robotic warble while keeping Mushaira feel through clean poetic pauses and
 *    rising-falling comma/purna-viram intonation.
 * 4. Smart network-voice cooldown: prioritizes Google HD Neural `-network` voices when healthy,
 *    and instantly skips broken/blocked network voices for 90s instead of stalling every Reel.
 * 5. Automatic offline error-recovery in live `speak(...)` so NO shayari can ever stay silent!
 */
class LocalShayarFallbackSpeaker(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private val readyDeferred = CompletableDeferred<Boolean>()
    private var isReady = false
    private var isReleased = false
    private var usesHindiLocale = true
    private var triedDefaultFallbackEngine = false
    private var pendingRequest: PendingSpeech? = null

    private val synthMutex = Mutex()
    private val synthGeneration = AtomicLong(0L)
    private val synthCallbacks = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    @Volatile
    private var networkVoiceCooldownUntilMs: Long = 0L

    @Volatile
    private var activeLiveSession: LiveSpeechSession? = null

    private data class PendingSpeech(
        val couplet: String,
        val persona: ShayarVoicePersona,
        val categoryId: String,
        val tags: List<String>,
        val onSpeakingChanged: (Boolean) -> Unit,
        val onCompletedAll: (() -> Unit)?
    )

    private data class LiveSpeechSession(
        val token: String,
        val lastUtteranceId: String,
        val couplet: String,
        val persona: ShayarVoicePersona,
        val categoryId: String,
        val tags: List<String>,
        val onSpeakingChanged: (Boolean) -> Unit,
        val onCompletedAll: (() -> Unit)?,
        @Volatile var retriedOffline: Boolean = false
    )

    init {
        initEngine(preferredEnginePackage = GOOGLE_TTS_PACKAGE)
    }

    private fun initEngine(preferredEnginePackage: String?) {
        if (isReleased) return
        val listener = TextToSpeech.OnInitListener { status ->
            if (isReleased) return@OnInitListener
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@OnInitListener
                configureEngine(engine)
                installUnifiedListener(engine)
                isReady = true
                if (!readyDeferred.isCompleted) readyDeferred.complete(true)
                pendingRequest?.let { req ->
                    pendingRequest = null
                    speak(
                        couplet = req.couplet,
                        persona = req.persona,
                        categoryId = req.categoryId,
                        tags = req.tags,
                        onSpeakingChanged = req.onSpeakingChanged,
                        onCompletedAll = req.onCompletedAll
                    )
                }
            } else if (preferredEnginePackage != null && !triedDefaultFallbackEngine) {
                triedDefaultFallbackEngine = true
                Log.w(TAG, "Preferred TTS ($preferredEnginePackage) init failed ($status); retrying default engine.")
                runCatching { tts?.shutdown() }
                tts = null
                initEngine(preferredEnginePackage = null)
            } else {
                Log.e(TAG, "Android TextToSpeech initialization failed completely (status=$status).")
                if (!readyDeferred.isCompleted) readyDeferred.complete(false)
            }
        }

        tts = if (preferredEnginePackage != null) {
            runCatching { TextToSpeech(appContext, listener, preferredEnginePackage) }
                .getOrElse {
                    triedDefaultFallbackEngine = true
                    TextToSpeech(appContext, listener)
                }
        } else {
            TextToSpeech(appContext, listener)
        }
    }

    private fun configureEngine(engine: TextToSpeech) {
        val hiLocale = Locale.forLanguageTag("hi-IN")
        val hiRes = engine.setLanguage(hiLocale)
        if (hiRes == TextToSpeech.LANG_MISSING_DATA || hiRes == TextToSpeech.LANG_NOT_SUPPORTED) {
            usesHindiLocale = false
            val enInRes = engine.setLanguage(Locale.forLanguageTag("en-IN"))
            if (enInRes == TextToSpeech.LANG_MISSING_DATA || enInRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.US)
            }
            Log.w(TAG, "hi-IN TTS data unavailable; using phonetic Roman fallback.")
        } else {
            usesHindiLocale = true
        }
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
    }

    private fun installUnifiedListener(engine: TextToSpeech) {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId == null) return
                val session = activeLiveSession
                if (session != null && utteranceId.startsWith(session.token)) {
                    mainHandler.post { session.onSpeakingChanged(true) }
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == null) return
                if (utteranceId.startsWith("synth_")) {
                    synthCallbacks.remove(utteranceId)?.complete(true)
                    return
                }
                val session = activeLiveSession
                if (session != null && utteranceId == session.lastUtteranceId) {
                    mainHandler.post {
                        session.onSpeakingChanged(false)
                        if (activeLiveSession?.token == session.token && !isReleased) {
                            session.onCompletedAll?.invoke()
                        }
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                handleUtteranceError(utteranceId, TextToSpeech.ERROR)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                handleUtteranceError(utteranceId, errorCode)
            }
        })
    }

    private fun handleUtteranceError(utteranceId: String?, errorCode: Int) {
        if (utteranceId == null) return
        Log.w(TAG, "TTS utterance error code=$errorCode id=$utteranceId")
        if (utteranceId.startsWith("synth_")) {
            synthCallbacks.remove(utteranceId)?.complete(false)
            return
        }
        val session = activeLiveSession
        if (session != null && utteranceId.startsWith(session.token)) {
            if (!session.retriedOffline && !isReleased) {
                // Automatic zero-silence recovery: retry immediately with guaranteed default offline Hindi voice!
                session.retriedOffline = true
                networkVoiceCooldownUntilMs = System.currentTimeMillis() + NETWORK_VOICE_COOLDOWN_MS
                mainHandler.post {
                    if (activeLiveSession?.token == session.token && !isReleased) {
                        speakLiveGuaranteedOffline(session)
                    }
                }
            } else {
                mainHandler.post { session.onSpeakingChanged(false) }
            }
        }
    }

    /**
     * Immediately aborts any in-progress synthesis so a newly swiped Reel acquires `synthMutex`
     * in 0ms instead of waiting on a stale timeout.
     */
    fun cancelPendingSynthesis() {
        synthGeneration.incrementAndGet()
        val pending = synthCallbacks.keys.toList()
        pending.forEach { key ->
            synthCallbacks.remove(key)?.complete(false)
        }
    }

    /**
     * Synchronous zero-latency lookup for an already synthesized & mastered Google Android voice WAV.
     */
    fun getCachedMasteredVoiceOrNull(
        couplet: String,
        persona: ShayarVoicePersona,
        categoryId: String = "",
        tags: List<String> = emptyList()
    ): File? {
        val cleanLines = sanitizeCoupletLines(couplet)
        if (isReleased || cleanLines.isEmpty()) return null
        val emotion = ShayarVoiceEngine.detectCoupletEmotion(couplet, categoryId, tags)
        val canonicalKey = cleanLines.joinToString("\n")
        val cacheDir = File(appContext.cacheDir, MASTERED_CACHE_DIR)
        val key = sha256Short("${persona.id}|${emotion.name}|$canonicalKey")
        val cachedMaster = File(cacheDir, "master_$key.wav")
        return cachedMaster.takeIf { it.exists() && it.length() > 2048L }
    }

    suspend fun synthesizeMasteredVoice(
        couplet: String,
        persona: ShayarVoicePersona,
        isPrewarm: Boolean = false,
        categoryId: String = "",
        tags: List<String> = emptyList()
    ): File? {
        val cleanLines = sanitizeCoupletLines(couplet)
        if (isReleased || cleanLines.isEmpty()) return null

        val emotion = ShayarVoiceEngine.detectCoupletEmotion(couplet, categoryId, tags)
        val canonicalKey = cleanLines.joinToString("\n")
        val cacheDir = File(appContext.cacheDir, MASTERED_CACHE_DIR).apply { mkdirs() }
        val key = sha256Short("${persona.id}|${emotion.name}|$canonicalKey")
        val cachedMaster = File(cacheDir, "master_$key.wav")
        if (cachedMaster.exists() && cachedMaster.length() > 2048L) {
            return cachedMaster
        }

        val ready = withTimeoutOrNull(3_000L) { readyDeferred.await() } ?: false
        if (!ready || isReleased) return null

        val myGen = if (isPrewarm) synthGeneration.get() else synthGeneration.incrementAndGet()
        if (!isPrewarm) {
            // Wake any old prewarm/stale synthesis immediately so we never wait on synthMutex!
            synthCallbacks.keys.toList().forEach { k ->
                synthCallbacks.remove(k)?.complete(false)
            }
        }

        return synthMutex.withLock {
            if (isReleased || synthGeneration.get() != myGen) return@withLock null
            if (cachedMaster.exists() && cachedMaster.length() > 2048L) {
                return@withLock cachedMaster
            }
            val engine = tts ?: return@withLock null

            val now = System.currentTimeMillis()
            val allowNetwork = now >= networkVoiceCooldownUntilMs
            val candidateVoices = buildRankedVoicesForPersona(engine, persona)
            val bestNetwork = if (allowNetwork) candidateVoices.firstOrNull { it.isNetworkConnectionRequired } else null
            val bestOffline = candidateVoices.firstOrNull { !it.isNetworkConnectionRequired }

            val attemptVoices: List<Voice?> = buildList {
                if (bestNetwork != null) add(bestNetwork)
                if (bestOffline != null) add(bestOffline)
                if (isEmpty()) add(null)
            }.distinctBy { it?.name }

            for (voice in attemptVoices) {
                if (isReleased || synthGeneration.get() != myGen) return@withLock null
                val rendered = trySynthesizeWithVoice(
                    engine = engine,
                    voice = voice,
                    cleanLines = cleanLines,
                    persona = persona,
                    emotion = emotion,
                    outputMasterFile = cachedMaster,
                    expectedGen = myGen
                )
                if (rendered != null) {
                    trimMasterCacheIfNeeded(cacheDir)
                    return@withLock rendered
                }
                if (voice?.isNetworkConnectionRequired == true && synthGeneration.get() == myGen) {
                    // This device/network rejected HD network synthesis; skip network attempts for 90s!
                    networkVoiceCooldownUntilMs = System.currentTimeMillis() + NETWORK_VOICE_COOLDOWN_MS
                    Log.w(TAG, "HD network voice failed (${voice.name}); cooling down network TTS for 90s.")
                }
            }

            // Final attempt with plain default engine configuration (`hi-IN`) if voice selection failed
            if (!isReleased && synthGeneration.get() == myGen && attemptVoices.none { it == null }) {
                val rendered = trySynthesizeWithVoice(
                    engine = engine,
                    voice = null,
                    cleanLines = cleanLines,
                    persona = persona,
                    emotion = emotion,
                    outputMasterFile = cachedMaster,
                    expectedGen = myGen
                )
                if (rendered != null) {
                    trimMasterCacheIfNeeded(cacheDir)
                    return@withLock rendered
                }
            }
            null
        }
    }

    private suspend fun trySynthesizeWithVoice(
        engine: TextToSpeech,
        voice: Voice?,
        cleanLines: List<String>,
        persona: ShayarVoicePersona,
        emotion: CoupletEmotion,
        outputMasterFile: File,
        expectedGen: Long
    ): File? = withContext(Dispatchers.IO) {
        if (voice != null) {
            val res = runCatching { engine.setVoice(voice) }.getOrDefault(TextToSpeech.ERROR)
            if (res == TextToSpeech.SUCCESS) {
                val lang = voice.locale?.language?.lowercase(Locale.ROOT)
                usesHindiLocale = (lang == "hi" || lang == "ur")
            } else {
                configureEngine(engine)
            }
        } else {
            configureEngine(engine)
        }

        val (rate, pitch) = naturalProsodyFor(persona, emotion)
        engine.setSpeechRate(rate)
        engine.setPitch(pitch)

        val tempDir = File(appContext.cacheDir, "local_tts_raw").apply { mkdirs() }
        val lineFiles = mutableListOf<File>()
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        try {
            for ((idx, line) in cleanLines.withIndex()) {
                if (isReleased || synthGeneration.get() != expectedGen) return@withContext null

                // Natural rising/falling Hindi-Urdu poetic intonation:
                // non-final lines end softly with a comma (poetic suspense), final line resolves with '।'
                val isLastLine = idx == cleanLines.lastIndex
                val phrasedLine = applyPoeticLineCadence(line, isLastLine = isLastLine)
                val spoken = if (usesHindiLocale) phrasedLine else devanagariToPhoneticLatin(phrasedLine)
                if (spoken.isBlank()) continue

                val uttId = "synth_${expectedGen}_${System.nanoTime()}_$idx"
                val outLineFile = File(tempDir, "$uttId.wav")
                val doneSignal = CompletableDeferred<Boolean>()
                synthCallbacks[uttId] = doneSignal

                val startRes = engine.synthesizeToFile(spoken, params, outLineFile, uttId)
                if (startRes != TextToSpeech.SUCCESS) {
                    synthCallbacks.remove(uttId)
                    outLineFile.delete()
                    return@withContext null
                }

                // Snappy timeout for network voices so Reels never stall in silence
                val timeoutMs = if (voice?.isNetworkConnectionRequired == true) 2_600L else 4_000L
                val ok = withTimeoutOrNull(timeoutMs) { doneSignal.await() } ?: false
                synthCallbacks.remove(uttId)

                if (synthGeneration.get() != expectedGen) {
                    outLineFile.delete()
                    return@withContext null
                }
                if (!ok || !outLineFile.exists() || outLineFile.length() < 256L) {
                    outLineFile.delete()
                    if (voice?.isNetworkConnectionRequired == true) {
                        // Fail fast on network error so offline voice starts immediately
                        return@withContext null
                    }
                    // On offline voice, skip only a broken/empty single line instead of failing the whole shayari
                    continue
                }
                lineFiles.add(outLineFile)
            }

            if (lineFiles.isEmpty() || synthGeneration.get() != expectedGen) return@withContext null

            val moodProfile = vocalProfileFor(emotion, persona)

            val mastered = StudioVocalMaster.masterPoetVoiceToStereoWav(
                lineFiles = lineFiles,
                outputFile = outputMasterFile,
                isFemale = persona.isFemale,
                profile = moodProfile
            )
            if (mastered) {
                Log.d(TAG, "Natural studio voice ready (${voice?.name ?: "default"}, lines=${lineFiles.size})")
                outputMasterFile
            } else {
                null
            }
        } finally {
            lineFiles.forEach { runCatching { it.delete() } }
        }
    }

    /**
     * Zero-failure direct live TTS backup (auto-recovers to guaranteed offline Hindi voice on error).
     */
    fun speak(
        couplet: String,
        persona: ShayarVoicePersona,
        onSpeakingChanged: (Boolean) -> Unit,
        onCompletedAll: (() -> Unit)? = null,
        categoryId: String = "",
        tags: List<String> = emptyList()
    ) {
        val cleanLines = sanitizeCoupletLines(couplet)
        if (isReleased || cleanLines.isEmpty()) return
        if (!isReady) {
            pendingRequest = PendingSpeech(couplet, persona, categoryId, tags, onSpeakingChanged, onCompletedAll)
            return
        }
        val engine = tts ?: return
        stop()

        val emotion = ShayarVoiceEngine.detectCoupletEmotion(couplet, categoryId, tags)
        val ranked = buildRankedVoicesForPersona(engine, persona)
        // Prefer reliable offline voice in live backup so asynchronous network errors never cause silence!
        val chosenVoice = ranked.firstOrNull { !it.isNetworkConnectionRequired } ?: ranked.firstOrNull()
        if (chosenVoice != null) {
            val voiceResult = runCatching { engine.setVoice(chosenVoice) }.getOrDefault(TextToSpeech.ERROR)
            if (voiceResult == TextToSpeech.SUCCESS) {
                val lang = chosenVoice.locale?.language?.lowercase(Locale.ROOT)
                usesHindiLocale = (lang == "hi" || lang == "ur")
            } else {
                configureEngine(engine)
            }
        } else {
            configureEngine(engine)
        }

        val (rate, pitch) = naturalProsodyFor(persona, emotion)
        engine.setSpeechRate(rate)
        engine.setPitch(pitch)

        val token = "live_${System.nanoTime()}"
        val spokenLines = cleanLines.mapIndexedNotNull { idx, line ->
            val phrased = applyPoeticLineCadence(line, isLastLine = idx == cleanLines.lastIndex)
            val text = if (usesHindiLocale) phrased else devanagariToPhoneticLatin(phrased)
            text.takeIf { it.isNotBlank() }
        }
        if (spokenLines.isEmpty()) return

        val lastUtteranceId = "${token}_line_${spokenLines.lastIndex}"
        val session = LiveSpeechSession(
            token = token,
            lastUtteranceId = lastUtteranceId,
            couplet = couplet,
            persona = persona,
            categoryId = categoryId,
            tags = tags,
            onSpeakingChanged = onSpeakingChanged,
            onCompletedAll = onCompletedAll
        )
        activeLiveSession = session

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val (midPauseMs, finalPauseMs) = interLinePausesFor(emotion)
        var firstResult = TextToSpeech.SUCCESS
        spokenLines.forEachIndexed { idx, text ->
            val queueMode = if (idx == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val res = engine.speak(text, queueMode, params, "${token}_line_$idx")
            if (idx == 0) firstResult = res
            if (idx < spokenLines.lastIndex) {
                val pauseMs = if (idx == spokenLines.lastIndex - 1) finalPauseMs else midPauseMs
                engine.playSilentUtterance(pauseMs, TextToSpeech.QUEUE_ADD, "${token}_pause_$idx")
            }
        }

        if (firstResult == TextToSpeech.ERROR) {
            session.retriedOffline = true
            speakLiveGuaranteedOffline(session)
        }
    }

    private fun speakLiveGuaranteedOffline(session: LiveSpeechSession) {
        val engine = tts ?: return
        val cleanLines = sanitizeCoupletLines(session.couplet)
        if (cleanLines.isEmpty()) return

        configureEngine(engine)
        val emotion = ShayarVoiceEngine.detectCoupletEmotion(session.couplet, session.categoryId, session.tags)
        val (rate, pitch) = naturalProsodyFor(session.persona, emotion)
        engine.setSpeechRate(rate)
        engine.setPitch(pitch)

        val combined = cleanLines.joinToString("। ")
        val text = if (usesHindiLocale) combined else devanagariToPhoneticLatin(combined)
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, session.lastUtteranceId)
    }

    fun stop() {
        pendingRequest = null
        activeLiveSession = null
        cancelPendingSynthesis()
        runCatching { tts?.stop() }
    }

    fun release() {
        isReleased = true
        pendingRequest = null
        activeLiveSession = null
        cancelPendingSynthesis()
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    private fun sanitizeCoupletLines(couplet: String): List<String> {
        return couplet
            .replace("\r\n", "\n")
            .split("\n")
            .map { raw ->
                // Strip emojis, decorative symbols, underscores & hashtags that break or silence TTS
                raw.replace(Regex("[\\p{So}\\p{Cn}_#*~`^|<>\\[\\]{}]"), " ")
                    .replace(Regex("[•·▪▫●○◆◇★☆]+"), ", ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            }
            .filter { line ->
                // Must contain at least one spoken letter or digit (Devanagari, Urdu/Arabic, or Latin)
                line.any { ch -> ch.isLetterOrDigit() }
            }
    }

    private fun applyPoeticLineCadence(line: String, isLastLine: Boolean): String {
        val trimmed = line.trim().trimEnd(' ', '-', '—', '–')
        if (trimmed.isEmpty()) return ""
        val withBreath = insertMidLineBreathComma(trimmed)
        val lastChar = withBreath.last()
        return if (isLastLine) {
            if (lastChar in charArrayOf('।', '.', '!', '?')) withBreath else "$withBreath।"
        } else {
            // Keep gentle comma cadence at end of non-final poetic lines so Hindi neural TTS
            // uses natural rising-sustained intonation instead of flat mechanical full stops!
            when (lastChar) {
                '।', '.' -> withBreath.dropLast(1).trimEnd() + ","
                ',', '!', '?' -> withBreath
                else -> "$withBreath,"
            }
        }
    }

    private fun insertMidLineBreathComma(line: String): String {
        if (line.contains(',') || line.contains(';') || line.contains("...")) return line
        val words = line.split(" ").filter { it.isNotBlank() }
        if (words.size < 6) return line
        val pivotsBefore = setOf(
            "कि", "पर", "मगर", "लेकिन", "और", "फिर", "जब", "वरना", "जैसे",
            "ki", "par", "magar", "lekin", "aur", "phir", "jab"
        )
        val pivotsAfter = setOf("तो", "बस", "हाँ", "सुनो", "यार", "दोस्त", "सनम", "toh", "bas")
        var splitIdx = -1
        for (i in 2..(words.size - 3)) {
            val w = words[i].lowercase().trim(',', '।', '.', '!', '?')
            if (w in pivotsBefore) {
                splitIdx = i
                break
            }
            if (w in pivotsAfter && i + 1 <= words.size - 2) {
                splitIdx = i + 1
                break
            }
        }
        if (splitIdx == -1 && words.size >= 7) {
            splitIdx = words.size / 2
        }
        if (splitIdx !in 2..(words.size - 2)) return line
        return words.subList(0, splitIdx).joinToString(" ") + ", " +
            words.subList(splitIdx, words.size).joinToString(" ")
    }

    private fun vocalProfileFor(
        emotion: CoupletEmotion,
        persona: ShayarVoicePersona
    ): VocalMoodProfile {
        return when (emotion) {
            CoupletEmotion.BEWAFAI_BETRAYAL,
            CoupletEmotion.DARD_TEARFUL,
            CoupletEmotion.TANHAI_LONELY,
            CoupletEmotion.TADAP_YEARNING,
            CoupletEmotion.MAUT_JANAZA_SOLEMN -> VocalMoodProfile.HEARTBREAK_SOFT
            CoupletEmotion.SHARABI_MAIKHANA,
            CoupletEmotion.SOULFUL_SUFI_WISDOM -> VocalMoodProfile.MEHFIL_BARITONE
            CoupletEmotion.ATTITUDE_SWAGGER,
            CoupletEmotion.HAUSLA_DESHPREM_FIRE -> VocalMoodProfile.STAGE_BOLD
            CoupletEmotion.INTIMATE_ROMANTIC,
            CoupletEmotion.BOLD_FLIRTY,
            CoupletEmotion.DOSTI_KHUSHI_FESTIVE,
            CoupletEmotion.CUTE_GENZ_PLAYFUL,
            CoupletEmotion.COMEDY_WITTY -> if (persona.isFemale) {
                VocalMoodProfile.MELODIOUS_ROMANTIC
            } else {
                VocalMoodProfile.MEHFIL_BARITONE
            }
        }
    }

    private fun interLinePausesFor(emotion: CoupletEmotion): Pair<Long, Long> {
        return when (emotion) {
            CoupletEmotion.MAUT_JANAZA_SOLEMN,
            CoupletEmotion.DARD_TEARFUL,
            CoupletEmotion.TANHAI_LONELY -> 960L to 1_260L
            CoupletEmotion.BEWAFAI_BETRAYAL,
            CoupletEmotion.SHARABI_MAIKHANA -> 860L to 1_160L
            CoupletEmotion.TADAP_YEARNING,
            CoupletEmotion.SOULFUL_SUFI_WISDOM,
            CoupletEmotion.INTIMATE_ROMANTIC -> 820L to 1_060L
            CoupletEmotion.ATTITUDE_SWAGGER,
            CoupletEmotion.HAUSLA_DESHPREM_FIRE -> 640L to 880L
            CoupletEmotion.BOLD_FLIRTY,
            CoupletEmotion.DOSTI_KHUSHI_FESTIVE,
            CoupletEmotion.CUTE_GENZ_PLAYFUL,
            CoupletEmotion.COMEDY_WITTY -> 600L to 800L
        }
    }

    private fun naturalProsodyFor(
        persona: ShayarVoicePersona,
        emotion: CoupletEmotion
    ): Pair<Float, Float> {
        // Distinct persona base pitch (Female: 0.98..1.14, Male: 0.80..0.97) so even on phones
        // with only 1–2 installed Hindi voices, every Shayar/Shayara sounds clearly different.
        val personaBasePitch = when (persona) {
            ShayarVoicePersona.DARD_SHAYARA -> 0.99f
            ShayarVoicePersona.SUREELI_SHAYARA -> 1.14f
            ShayarVoicePersona.TADAP_SHAYARA -> 1.06f
            ShayarVoicePersona.ROMANTIC_SHAYARA -> 1.02f
            ShayarVoicePersona.DARD_SHAYAR -> 0.80f
            ShayarVoicePersona.TADAP_AASHIQ -> 0.96f
            ShayarVoicePersona.MEHFIL_SHAYAR -> 0.85f
            ShayarVoicePersona.JOSH_SHAYAR -> 0.90f
        }

        // Category/tag emotion drives speaking cadence and subtle emotional pitch inflection
        val (emotionRate, emotionPitchOffset) = when (emotion) {
            CoupletEmotion.BEWAFAI_BETRAYAL -> 0.90f to -0.02f
            CoupletEmotion.DARD_TEARFUL -> 0.87f to -0.03f
            CoupletEmotion.TANHAI_LONELY -> 0.88f to -0.02f
            CoupletEmotion.TADAP_YEARNING -> 0.92f to 0.01f
            CoupletEmotion.SHARABI_MAIKHANA -> 0.86f to -0.04f
            CoupletEmotion.MAUT_JANAZA_SOLEMN -> 0.85f to -0.05f
            CoupletEmotion.INTIMATE_ROMANTIC -> 0.91f to 0.00f
            CoupletEmotion.BOLD_FLIRTY -> 0.96f to 0.03f
            CoupletEmotion.ATTITUDE_SWAGGER -> 0.98f to -0.03f
            CoupletEmotion.HAUSLA_DESHPREM_FIRE -> 1.01f to 0.02f
            CoupletEmotion.DOSTI_KHUSHI_FESTIVE -> 1.00f to 0.04f
            CoupletEmotion.CUTE_GENZ_PLAYFUL -> 1.02f to 0.05f
            CoupletEmotion.COMEDY_WITTY -> 1.04f to 0.04f
            CoupletEmotion.SOULFUL_SUFI_WISDOM -> 0.90f to -0.02f
        }

        val finalRate = emotionRate.coerceIn(0.84f, 1.06f)
        val finalPitch = (personaBasePitch + emotionPitchOffset).coerceIn(0.76f, 1.18f)
        return finalRate to finalPitch
    }

    private fun buildRankedVoicesForPersona(engine: TextToSpeech, persona: ShayarVoicePersona): List<Voice> {
        val voices = runCatching { engine.voices }.getOrNull()?.filterNotNull() ?: return emptyList()
        val hindiVoices = voices.filter { v ->
            val lang = v.locale?.language?.lowercase(Locale.ROOT)
            val features = v.features ?: emptySet()
            (lang == "hi" || lang == "ur") && !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }
        val basePool = hindiVoices.ifEmpty {
            voices.filter { v ->
                val loc = v.locale
                val features = v.features ?: emptySet()
                loc?.language?.lowercase(Locale.ROOT) == "en" &&
                    (loc.country.equals("IN", ignoreCase = true) || loc.country.equals("US", ignoreCase = true)) &&
                    !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }
        }
        if (basePool.isEmpty()) return emptyList()

        val femaleHints = listOf("hia", "hic", "cfn", "female", "f0", "f1", "sfg", "lfb", "ene", "enc")
        val maleHints = listOf("hid", "hie", "hif", "hig", "male", "m0", "m1", "end", "enf")
        val targetHints = if (persona.isFemale) femaleHints else maleHints

        val genderMatched = basePool.filter { v ->
            val n = v.name.lowercase(Locale.ROOT)
            targetHints.any { it in n }
        }.ifEmpty { basePool }

        val networkVoices = genderMatched.filter { it.isNetworkConnectionRequired }
            .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.name })
        val localVoices = genderMatched.filter { !it.isNetworkConnectionRequired }
            .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.name })

        val ordered = mutableListOf<Voice>()
        if (networkVoices.isNotEmpty()) {
            val slot = persona.ordinal % networkVoices.size
            ordered.add(networkVoices[slot])
            networkVoices.forEachIndexed { idx, v -> if (idx != slot) ordered.add(v) }
        }
        if (localVoices.isNotEmpty()) {
            val slot = persona.ordinal % localVoices.size
            ordered.add(localVoices[slot])
            localVoices.forEachIndexed { idx, v -> if (idx != slot) ordered.add(v) }
        }
        return ordered
    }

    private fun trimMasterCacheIfNeeded(cacheDir: File) {
        val files = cacheDir.listFiles()?.filter { it.isFile } ?: return
        if (files.size <= 36) return
        files.sortedBy { it.lastModified() }
            .take(files.size - 24)
            .forEach { runCatching { it.delete() } }
    }

    private fun sha256Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.take(10).joinToString("") { "%02x".format(it) }
    }

    private fun devanagariToPhoneticLatin(input: String): String {
        val sb = StringBuilder(input.length * 2)
        var i = 0
        val len = input.length
        while (i < len) {
            val ch = input[i]
            val next = if (i + 1 < len) input[i + 1] else '\u0000'
            val hasNukta = next == '\u093C'
            val effectiveNext = if (hasNukta && i + 2 < len) input[i + 2] else next

            val cons = consonantMap(ch, hasNukta)
            if (cons != null) {
                sb.append(cons)
                if (hasNukta) i++
                val matra = matraMap(effectiveNext)
                when {
                    effectiveNext == '\u094D' -> i++
                    matra != null -> {
                        sb.append(matra)
                        i++
                    }
                    else -> {
                        val isEndOfWord = effectiveNext == '\u0000' ||
                            effectiveNext.isWhitespace() ||
                            effectiveNext == '।' ||
                            effectiveNext == ',' ||
                            effectiveNext == '.' ||
                            effectiveNext == '!' ||
                            effectiveNext == '?' ||
                            effectiveNext == '-'
                        if (!isEndOfWord) sb.append('a')
                    }
                }
                i++
                continue
            }

            val vowel = independentVowelMap(ch)
            if (vowel != null) {
                sb.append(vowel)
                i++
                continue
            }

            when (ch) {
                'ं', 'ँ' -> sb.append('n')
                'ः' -> sb.append('h')
                '।' -> sb.append('.')
                '\u093C' -> Unit
                else -> sb.append(ch)
            }
            i++
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun independentVowelMap(ch: Char): String? = when (ch) {
        'अ' -> "a"; 'आ' -> "aa"; 'इ' -> "i"; 'ई' -> "ee"; 'उ' -> "u"; 'ऊ' -> "oo"
        'ऋ' -> "ri"; 'ए' -> "e"; 'ऐ' -> "ai"; 'ओ' -> "o"; 'औ' -> "au"
        else -> null
    }

    private fun matraMap(ch: Char): String? = when (ch) {
        'ा' -> "aa"; 'ि' -> "i"; 'ी' -> "ee"; 'ु' -> "u"; 'ू' -> "oo"
        'ृ' -> "ri"; 'े' -> "e"; 'ै' -> "ai"; 'ो' -> "o"; 'ौ' -> "au"
        else -> null
    }

    private fun consonantMap(ch: Char, nukta: Boolean): String? = when (ch) {
        'क' -> if (nukta) "q" else "k"; 'ख' -> "kh"; 'ग' -> if (nukta) "gh" else "g"; 'घ' -> "gh"; 'ङ' -> "ng"
        'च' -> "ch"; 'छ' -> "chh"; 'ज' -> if (nukta) "z" else "j"; 'झ' -> "jh"; 'ञ' -> "n"
        'ट' -> "t"; 'ठ' -> "th"; 'ड' -> if (nukta) "r" else "d"; 'ढ' -> if (nukta) "rh" else "dh"; 'ण' -> "n"
        'त' -> "t"; 'थ' -> "th"; 'द' -> "d"; 'ध' -> "dh"; 'न' -> "n"
        'प' -> "p"; 'फ' -> if (nukta) "f" else "ph"; 'ब' -> "b"; 'भ' -> "bh"; 'म' -> "m"
        'य' -> "y"; 'र' -> "r"; 'ल' -> "l"; 'व' -> "v"; 'श' -> "sh"; 'ष' -> "sh"; 'स' -> "s"; 'ह' -> "h"
        '\u0958' -> "q"; '\u0959' -> "kh"; '\u095A' -> "gh"; '\u095B' -> "z"; '\u095C' -> "r"; '\u095D' -> "rh"; '\u095E' -> "f"
        else -> null
    }
}
