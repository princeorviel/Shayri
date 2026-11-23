package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.ai.ShayarVoicePersona
import java.util.Locale

/**
 * Transparent on-device Hindi neural-voice backup speaker.
 * Used ONLY when Gemini Flash TTS returns HTTP 429 / Quota Exceeded so the user
 * still hears the shayari clearly while transparently seeing the AI quota notice
 * with 1-tap Gemini AI retry.
 */
class LocalShayarFallbackSpeaker(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var isReleased = false
    private var pendingRequest: PendingSpeech? = null
    private var activeToken: String = ""

    private data class PendingSpeech(
        val couplet: String,
        val persona: ShayarVoicePersona,
        val onSpeakingChanged: (Boolean) -> Unit
    )

    init {
        tts = TextToSpeech(appContext) { status ->
            if (isReleased) return@TextToSpeech
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                val hiLocale = Locale.forLanguageTag("hi-IN")
                val res = engine.setLanguage(hiLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.forLanguageTag("en-IN"))
                }
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                isReady = true
                pendingRequest?.let { req ->
                    pendingRequest = null
                    speak(req.couplet, req.persona, req.onSpeakingChanged)
                }
            }
        }
    }

    fun speak(
        couplet: String,
        persona: ShayarVoicePersona,
        onSpeakingChanged: (Boolean) -> Unit
    ) {
        if (isReleased || couplet.isBlank()) return
        if (!isReady) {
            pendingRequest = PendingSpeech(couplet, persona, onSpeakingChanged)
            return
        }
        val engine = tts ?: return
        stop()

        val token = "shayar_local_${System.nanoTime()}"
        activeToken = token

        // Pick best matching Female/Male hi-IN voice installed on the device
        selectBestVoiceForPersona(engine, persona)?.let { chosen ->
            runCatching { engine.voice = chosen }
        }

        // Expressive poetic pacing tuned per persona emotion
        val (rate, pitch) = when (persona) {
            ShayarVoicePersona.DARD_SHAYARA -> 0.78f to 1.03f
            ShayarVoicePersona.SUREELI_SHAYARA -> 0.84f to 1.12f
            ShayarVoicePersona.TADAP_SHAYARA -> 0.76f to 1.07f
            ShayarVoicePersona.ROMANTIC_SHAYARA -> 0.82f to 1.08f
            ShayarVoicePersona.DARD_SHAYAR -> 0.76f to 0.82f
            ShayarVoicePersona.TADAP_AASHIQ -> 0.80f to 0.88f
            ShayarVoicePersona.MEHFIL_SHAYAR -> 0.80f to 0.80f
            ShayarVoicePersona.JOSH_SHAYAR -> 0.88f to 0.84f
        }
        engine.setSpeechRate(rate)
        engine.setPitch(pitch)

        val lines = couplet
            .replace("\r\n", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) return

        val lastUtteranceId = "${token}_line_${lines.lastIndex}"

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId?.startsWith(token) == true) {
                    mainHandler.post { onSpeakingChanged(true) }
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == lastUtteranceId) {
                    mainHandler.post { onSpeakingChanged(false) }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId?.startsWith(token) == true) {
                    mainHandler.post { onSpeakingChanged(false) }
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId?.startsWith(token) == true) {
                    mainHandler.post { onSpeakingChanged(false) }
                }
            }
        })

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        // Speak line-by-line with gentle poetic silence between misras (couplet lines)
        lines.forEachIndexed { idx, line ->
            val queueMode = if (idx == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(line, queueMode, params, "${token}_line_$idx")
            if (idx < lines.lastIndex) {
                engine.playSilentUtterance(480L, TextToSpeech.QUEUE_ADD, "${token}_pause_$idx")
            }
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
            lang == "hi" || lang == "ur"
        }
        if (hindiVoices.isEmpty()) return null

        // Prefer high-quality non-network-failing local/installed voices matching gender keywords
        val femaleHints = listOf("hid", "hie", "female", "f0", "f1", "sfg", "lfb", "kfb", "hi-in-x-hia", "hi-in-x-hic")
        val maleHints = listOf("hif", "hig", "male", "m0", "m1", "hi-in-x-hid", "hi-in-x-hie")
        val targetHints = if (persona.isFemale) femaleHints else maleHints

        val genderMatched = hindiVoices.filter { v ->
            val n = v.name.lowercase(Locale.ROOT)
            targetHints.any { it in n }
        }
        val pool = genderMatched.ifEmpty { hindiVoices }
        return pool.sortedWith(
            compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                .thenByDescending { it.quality }
        ).firstOrNull()
    }
}
