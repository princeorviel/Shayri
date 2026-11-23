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
import com.example.ai.ShayarVoicePersona
import java.util.Locale

private const val TAG = "LocalShayarSpeaker"
private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"

/**
 * High-quality on-device Neural Hindi voice speaker for Shayari Reels.
 *
 * Prioritizes Google Speech Services (`com.google.android.tts`) high-quality neural
 * `hi-IN` Male/Female voices (requires `TTS_SERVICE` in AndroidManifest `<queries>` on API 30+),
 * adds natural Mushaira breath pauses at commas and line breaks, keeps pitch natural
 * (no robotic distortion), plays on `STREAM_MUSIC` at full clarity, and falls back to
 * phonetic Roman Hindi only when a device lacks `hi-IN` voice data.
 */
class LocalShayarFallbackSpeaker(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var isReleased = false
    private var usesHindiLocale = true
    private var triedDefaultFallbackEngine = false
    private var pendingRequest: PendingSpeech? = null
    private var activeToken: String = ""

    private data class PendingSpeech(
        val couplet: String,
        val persona: ShayarVoicePersona,
        val onSpeakingChanged: (Boolean) -> Unit,
        val onCompletedAll: (() -> Unit)?
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
                isReady = true
                pendingRequest?.let { req ->
                    pendingRequest = null
                    speak(req.couplet, req.persona, req.onSpeakingChanged, req.onCompletedAll)
                }
            } else if (preferredEnginePackage != null && !triedDefaultFallbackEngine) {
                triedDefaultFallbackEngine = true
                Log.w(TAG, "Preferred TTS engine ($preferredEnginePackage) failed (status=$status); retrying default engine.")
                runCatching { tts?.shutdown() }
                tts = null
                initEngine(preferredEnginePackage = null)
            } else {
                Log.e(TAG, "Android TextToSpeech initialization failed completely (status=$status).")
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
            Log.w(TAG, "hi-IN TTS data unavailable; using phonetic Roman fallback on Indian/US English voice.")
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

    fun speak(
        couplet: String,
        persona: ShayarVoicePersona,
        onSpeakingChanged: (Boolean) -> Unit,
        onCompletedAll: (() -> Unit)? = null
    ) {
        if (isReleased || couplet.isBlank()) return
        if (!isReady) {
            pendingRequest = PendingSpeech(couplet, persona, onSpeakingChanged, onCompletedAll)
            return
        }
        val engine = tts ?: return
        stop()

        val token = "shayar_local_${System.nanoTime()}"
        activeToken = token

        // Pick distinct, highest-quality installed Female/Male Hindi neural voice
        val chosenVoice = selectBestVoiceForPersona(engine, persona)
        if (chosenVoice != null) {
            val voiceResult = runCatching { engine.setVoice(chosenVoice) }.getOrDefault(TextToSpeech.ERROR)
            if (voiceResult == TextToSpeech.SUCCESS) {
                val lang = chosenVoice.locale?.language?.lowercase(Locale.ROOT)
                usesHindiLocale = (lang == "hi" || lang == "ur")
            }
        }

        // Keep pitch strictly near natural 1.0f (extreme pitch shifts cause robotic artifacts!)
        // and use authentic Mushaira pacing per persona mood
        val (rate, pitch) = when (persona) {
            ShayarVoicePersona.DARD_SHAYARA -> 0.83f to 0.98f
            ShayarVoicePersona.SUREELI_SHAYARA -> 0.88f to 1.04f
            ShayarVoicePersona.TADAP_SHAYARA -> 0.81f to 1.01f
            ShayarVoicePersona.ROMANTIC_SHAYARA -> 0.85f to 1.02f
            ShayarVoicePersona.DARD_SHAYAR -> 0.81f to 0.93f
            ShayarVoicePersona.TADAP_AASHIQ -> 0.85f to 0.96f
            ShayarVoicePersona.MEHFIL_SHAYAR -> 0.84f to 0.92f
            ShayarVoicePersona.JOSH_SHAYAR -> 0.92f to 0.96f
        }
        engine.setSpeechRate(rate)
        engine.setPitch(pitch)

        val rawLines = couplet
            .replace("\r\n", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (rawLines.isEmpty()) return

        // Break into natural poetic clauses (soft 240ms breath on commas, 660ms silence between lines)
        data class SpokenSegment(val text: String, val pauseAfterMs: Long)
        val segments = buildList {
            rawLines.forEachIndexed { lineIdx, rawLine ->
                val clauses = rawLine
                    .split(Regex("[,;—–]+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                clauses.forEachIndexed { clauseIdx, clause ->
                    val spoken = if (usesHindiLocale) clause else devanagariToPhoneticLatin(clause)
                    if (spoken.isNotBlank()) {
                        val isLastClauseOfLine = clauseIdx == clauses.lastIndex
                        val isLastLine = lineIdx == rawLines.lastIndex
                        val pauseMs = when {
                            isLastClauseOfLine && isLastLine -> 0L
                            isLastClauseOfLine -> 660L
                            else -> 240L
                        }
                        add(SpokenSegment(spoken, pauseMs))
                    }
                }
            }
        }

        if (segments.isEmpty()) return

        val lastUtteranceId = "${token}_seg_${segments.lastIndex}"

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId?.startsWith(token) == true) {
                    mainHandler.post { onSpeakingChanged(true) }
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == lastUtteranceId) {
                    mainHandler.post {
                        onSpeakingChanged(false)
                        if (activeToken == token && !isReleased) {
                            onCompletedAll?.invoke()
                        }
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId?.startsWith(token) == true) {
                    mainHandler.post { onSpeakingChanged(false) }
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.w(TAG, "Local TTS utterance error code=$errorCode id=$utteranceId")
                if (utteranceId?.startsWith(token) == true) {
                    mainHandler.post { onSpeakingChanged(false) }
                }
            }
        })

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        var firstResult = TextToSpeech.SUCCESS
        segments.forEachIndexed { idx, seg ->
            val queueMode = if (idx == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val res = engine.speak(seg.text, queueMode, params, "${token}_seg_$idx")
            if (idx == 0) firstResult = res
            if (seg.pauseAfterMs > 0L) {
                engine.playSilentUtterance(seg.pauseAfterMs, TextToSpeech.QUEUE_ADD, "${token}_pause_$idx")
            }
        }

        // Auto-recover if selected voice returned ERROR: reset to default language & speak once cleanly
        if (firstResult == TextToSpeech.ERROR) {
            Log.w(TAG, "TTS speak returned ERROR with selected voice; retrying with default locale voice.")
            configureEngine(engine)
            val fallbackText = if (usesHindiLocale) {
                rawLines.joinToString(". ")
            } else {
                devanagariToPhoneticLatin(rawLines.joinToString(". "))
            }
            engine.speak(fallbackText, TextToSpeech.QUEUE_FLUSH, params, lastUtteranceId)
        }
    }

    fun stop() {
        pendingRequest = null
        activeToken = ""
        runCatching { tts?.stop() }
    }

    fun release() {
        isReleased = true
        pendingRequest = null
        activeToken = ""
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    private fun selectBestVoiceForPersona(engine: TextToSpeech, persona: ShayarVoicePersona): Voice? {
        val voices = runCatching { engine.voices }.getOrNull()?.filterNotNull() ?: return null
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
        if (basePool.isEmpty()) return null

        // Prefer offline-ready installed voices so playback NEVER stalls or errors on flaky mobile data
        val offlineReady = basePool.filter { !it.isNetworkConnectionRequired }.ifEmpty { basePool }

        // Google Android TTS Hindi neural voice IDs:
        // Female: hi-in-x-hia, hi-in-x-hic, hi-in-x-cfn
        // Male:   hi-in-x-hid, hi-in-x-hie, hi-in-x-hif, hi-in-x-hig
        val femaleHints = listOf("hia", "hic", "cfn", "female", "f0", "f1", "sfg", "lfb", "ene", "enc")
        val maleHints = listOf("hid", "hie", "hif", "hig", "male", "m0", "m1", "end", "enf")
        val targetHints = if (persona.isFemale) femaleHints else maleHints

        val genderMatched = offlineReady.filter { v ->
            val n = v.name.lowercase(Locale.ROOT)
            targetHints.any { it in n }
        }
        val sorted = (genderMatched.ifEmpty { offlineReady }).sortedWith(
            compareByDescending<Voice> { it.quality }
                .thenBy { it.latency }
                .thenBy { it.name }
        )
        // Spread the 4 Female / 4 Male personas across distinct installed neural voices when available
        val personaSlot = persona.ordinal % sorted.size
        return sorted[personaSlot]
    }

    /**
     * Compact Devanagari -> readable phonetic Roman Hindi converter used ONLY when the
     * host device/emulator lacks a `hi-IN` TTS voice pack (preventing silent `en-IN` drops).
     */
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
                        if (!isEndOfWord) {
                            sb.append('a')
                        }
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
        return sb.toString()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun independentVowelMap(ch: Char): String? = when (ch) {
        'अ' -> "a"
        'आ' -> "aa"
        'इ' -> "i"
        'ई' -> "ee"
        'उ' -> "u"
        'ऊ' -> "oo"
        'ऋ' -> "ri"
        'ए' -> "e"
        'ऐ' -> "ai"
        'ओ' -> "o"
        'औ' -> "au"
        else -> null
    }

    private fun matraMap(ch: Char): String? = when (ch) {
        'ा' -> "aa"
        'ि' -> "i"
        'ी' -> "ee"
        'ु' -> "u"
        'ू' -> "oo"
        'ृ' -> "ri"
        'े' -> "e"
        'ै' -> "ai"
        'ो' -> "o"
        'ौ' -> "au"
        else -> null
    }

    private fun consonantMap(ch: Char, nukta: Boolean): String? = when (ch) {
        'क' -> if (nukta) "q" else "k"
        'ख' -> "kh"
        'ग' -> if (nukta) "gh" else "g"
        'घ' -> "gh"
        'ङ' -> "ng"
        'च' -> "ch"
        'छ' -> "chh"
        'ज' -> if (nukta) "z" else "j"
        'झ' -> "jh"
        'ञ' -> "n"
        'ट' -> "t"
        'ठ' -> "th"
        'ड' -> if (nukta) "r" else "d"
        'ढ' -> if (nukta) "rh" else "dh"
        'ण' -> "n"
        'त' -> "t"
        'थ' -> "th"
        'द' -> "d"
        'ध' -> "dh"
        'न' -> "n"
        'प' -> "p"
        'फ' -> if (nukta) "f" else "ph"
        'ब' -> "b"
        'भ' -> "bh"
        'म' -> "m"
        'य' -> "y"
        'र' -> "r"
        'ल' -> "l"
        'व' -> "v"
        'श' -> "sh"
        'ष' -> "sh"
        'स' -> "s"
        'ह' -> "h"
        '\u0958' -> "q"
        '\u0959' -> "kh"
        '\u095A' -> "gh"
        '\u095B' -> "z"
        '\u095C' -> "r"
        '\u095D' -> "rh"
        '\u095E' -> "f"
        else -> null
    }
}
