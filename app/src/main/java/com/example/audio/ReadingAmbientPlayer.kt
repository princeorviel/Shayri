package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import com.example.data.model.ShayariCategory
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AMBIENT_TAG = "ReadingAmbientPlayer"
private const val AMBIENT_CACHE_DIR = "reading_ambient_v1"
private const val AMBIENT_SAMPLE_RATE = 24_000
private const val AMBIENT_TWO_PI = 2.0 * PI

enum class ReadingAmbientTrack(
    val id: String,
    val labelHi: String,
    val subtitleHi: String,
    val moodHintHi: String,
    val emoji: String
) {
    SOFT_PIANO(
        id = "soft_piano",
        labelHi = "मखमली पियानो",
        subtitleHi = "धीमा इमोशनल फ़ेल्ट पियानो",
        moodHintHi = "दर्द और जज़्बात",
        emoji = "🎹"
    ),
    MIDNIGHT_RAIN(
        id = "midnight_rain",
        labelHi = "बारिश और रात",
        subtitleHi = "हल्की रिमझिम बारिश व सुकून धुन",
        moodHintHi = "तन्हाई और यादें",
        emoji = "🌧️"
    ),
    MEHFIL_BANSURI(
        id = "mehfil_bansuri",
        labelHi = "सूफ़ी बाँसुरी",
        subtitleHi = "सॉफ्ट बाँसुरी व हल्का तानपूरा",
        moodHintHi = "रूहानी व महफ़िल",
        emoji = "🪈"
    ),
    ACOUSTIC_GUITAR(
        id = "acoustic_guitar",
        labelHi = "सॉफ्ट गिटार",
        subtitleHi = "धीमे अकॉस्टिक तारों की मिठास",
        moodHintHi = "दोस्ती और सफ़र",
        emoji = "🎸"
    ),
    SILK_STRINGS(
        id = "silk_strings",
        labelHi = "मखमली वायलिन",
        subtitleHi = "रोमांटिक सॉफ्ट स्ट्रिंग्स पैड",
        moodHintHi = "इश्क़ और मोहब्बत",
        emoji = "🎻"
    ),
    NIGHT_STILLNESS(
        id = "night_stillness",
        labelHi = "ख़ामोश चाँदनी",
        subtitleHi = "मेडिटेटिव एम्बिएंट ड्रोन व चाइम्स",
        moodHintHi = "गहरा सुकून",
        emoji = "🌙"
    );

    companion object {
        fun fromId(id: String?): ReadingAmbientTrack {
            return entries.firstOrNull { it.id == id } ?: SOFT_PIANO
        }

        fun recommendedForCategory(categoryId: String?): ReadingAmbientTrack {
            if (categoryId.isNullOrBlank()) return SOFT_PIANO
            return when (ShayariCategory.fromId(categoryId)) {
                ShayariCategory.DARD,
                ShayariCategory.BEWAFAI,
                ShayariCategory.AANSU -> SOFT_PIANO

                ShayariCategory.TANHAI,
                ShayariCategory.MAUT_JANAZA,
                ShayariCategory.PRAKRITI -> MIDNIGHT_RAIN

                ShayariCategory.MOHABBAT,
                ShayariCategory.ROMANTIC,
                ShayariCategory.SECRET_LOVE,
                ShayariCategory.TAREEF -> SILK_STRINGS

                ShayariCategory.KHUDA_RAB,
                ShayariCategory.ZINDAGI,
                ShayariCategory.WAQT,
                ShayariCategory.AAINA -> MEHFIL_BANSURI

                ShayariCategory.DOSTI,
                ShayariCategory.MUSAFIR,
                ShayariCategory.SHARABI,
                ShayariCategory.NEW_YEAR,
                ShayariCategory.CHRISTMAS -> ACOUSTIC_GUITAR

                else -> NIGHT_STILLNESS
            }
        }
    }
}

enum class ReadingSleepTimerOption(
    val minutes: Int,
    val labelHi: String
) {
    CONTINUOUS(0, "लगातार"),
    TEN_MIN(10, "10 मि."),
    TWENTY_MIN(20, "20 मि."),
    THIRTY_MIN(30, "30 मि.")
}

/**
 * Subtle, artifact-free stereo ambient & soft instrumental music player designed
 * specifically for poetry reading.
 *
 * Pre-renders seamless circular-tail stereo WAV loops offline on a background thread and
 * plays them through hardware-mixed [MediaPlayer] with gentle fade-in/fade-out transitions,
 * polite audio-focus handling, and real-time subtle volume scaling.
 */
class ReadingAmbientPlayer(
    context: Context,
    private val onAudioFocusLost: () -> Unit = {}
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaPlayer: MediaPlayer? = null
    private var loadedTrack: ReadingAmbientTrack? = null
    private var desiredTrack: ReadingAmbientTrack = ReadingAmbientTrack.SOFT_PIANO
    private var isPlayingDesired: Boolean = false
    private var userVolume: Float = 0.38f
    private var currentGain: Float = 0f
    private var switchJob: Job? = null
    private var fadeJob: Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                isPlayingDesired = false
                pauseInternalImmediate()
                onAudioFocusLost()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pauseInternalImmediate()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                smoothFadeTo(effectiveGain() * 0.30f, durationMs = 180L)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                if (isPlayingDesired) {
                    resumeOrLoad()
                }
            }
        }
    }

    fun updateState(
        shouldPlay: Boolean,
        track: ReadingAmbientTrack,
        volume: Float
    ) {
        val clampedVol = volume.coerceIn(0.05f, 1.0f)
        val trackChanged = desiredTrack != track
        val playChanged = isPlayingDesired != shouldPlay
        val volChanged = abs(userVolume - clampedVol) > 0.005f

        desiredTrack = track
        userVolume = clampedVol
        isPlayingDesired = shouldPlay

        if (!shouldPlay) {
            switchJob?.cancel()
            if (playChanged && mediaPlayer != null) {
                fadeOutAndPause()
            } else {
                pauseInternalImmediate()
                abandonAudioFocus()
            }
            return
        }

        if (!requestAudioFocus()) {
            return
        }

        if (trackChanged || mediaPlayer == null || loadedTrack != track) {
            loadAndCrossfadeTrack(track)
        } else {
            val mp = mediaPlayer ?: return
            if (!mp.isPlaying) {
                runCatching {
                    currentGain = 0.01f
                    mp.setVolume(currentGain, currentGain)
                    mp.start()
                }
                smoothFadeTo(effectiveGain(), durationMs = 420L)
            } else if (volChanged) {
                val target = effectiveGain()
                fadeJob?.cancel()
                currentGain = target
                runCatching { mp.setVolume(target, target) }
            }
        }
    }

    fun fadeOutAndStop(onCompleted: () -> Unit = {}) {
        isPlayingDesired = false
        switchJob?.cancel()
        val mp = mediaPlayer
        if (mp == null) {
            abandonAudioFocus()
            onCompleted()
            return
        }
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 16
            val start = currentGain
            for (step in 1..steps) {
                if (!isActive) return@launch
                val v = start * (1f - step.toFloat() / steps.toFloat())
                currentGain = v
                runCatching { mediaPlayer?.setVolume(v, v) }
                delay(30L)
            }
            pauseInternalImmediate()
            abandonAudioFocus()
            onCompleted()
        }
    }

    fun release() {
        isPlayingDesired = false
        switchJob?.cancel()
        fadeJob?.cancel()
        stopAndReleasePlayer()
        abandonAudioFocus()
        scope.cancel()
    }

    private fun effectiveGain(): Float {
        // Gentle perceptual curve so 35-45% feels genuinely soft and unobtrusive behind reading
        val shaped = userVolume * userVolume * 0.52f + userVolume * 0.24f
        return shaped.coerceIn(0.02f, 0.72f)
    }

    private fun resumeOrLoad() {
        val mp = mediaPlayer
        if (mp != null && loadedTrack == desiredTrack) {
            runCatching {
                if (!mp.isPlaying) {
                    currentGain = 0.01f
                    mp.setVolume(currentGain, currentGain)
                    mp.start()
                }
            }
            smoothFadeTo(effectiveGain(), durationMs = 380L)
        } else {
            loadAndCrossfadeTrack(desiredTrack)
        }
    }

    private fun fadeOutAndPause() {
        val mp = mediaPlayer ?: return
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 10
            val start = currentGain
            for (step in 1..steps) {
                if (!isActive) return@launch
                val v = start * (1f - step.toFloat() / steps.toFloat())
                currentGain = v
                runCatching { mp.setVolume(v, v) }
                delay(24L)
            }
            runCatching {
                if (mp.isPlaying) mp.pause()
            }
            abandonAudioFocus()
        }
    }

    private fun pauseInternalImmediate() {
        fadeJob?.cancel()
        currentGain = 0f
        runCatching {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.pause()
            }
        }
    }

    private fun smoothFadeTo(target: Float, durationMs: Long = 400L) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 16
            val stepDelay = (durationMs / steps).coerceAtLeast(12L)
            val start = currentGain
            for (step in 1..steps) {
                if (!isActive) return@launch
                val v = start + (target - start) * (step.toFloat() / steps.toFloat())
                currentGain = v
                runCatching { mediaPlayer?.setVolume(v, v) }
                delay(stepDelay)
            }
        }
    }

    private fun loadAndCrossfadeTrack(track: ReadingAmbientTrack) {
        switchJob?.cancel()
        switchJob = scope.launch {
            val wavFile = withContext(Dispatchers.Default) {
                ReadingAmbientRenderer.getOrRenderLoopWav(appContext, track)
            } ?: return@launch

            if (!isActive || !isPlayingDesired || desiredTrack != track) return@launch

            // Gently fade out existing track before swapping
            val existing = mediaPlayer
            if (existing != null && runCatching { existing.isPlaying }.getOrDefault(false)) {
                fadeJob?.cancel()
                val startVol = currentGain
                for (step in 1..8) {
                    if (!isActive) return@launch
                    val v = startVol * (1f - step / 8f)
                    runCatching { existing.setVolume(v, v) }
                    delay(20L)
                }
            }
            stopAndReleasePlayer()

            val mp = MediaPlayer()
            val prepared = withContext(Dispatchers.IO) {
                runCatching {
                    mp.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    mp.setDataSource(wavFile.absolutePath)
                    mp.isLooping = true
                    mp.prepare()
                    true
                }.onFailure {
                    Log.e(AMBIENT_TAG, "Failed to prepare ambient loop ${track.id}", it)
                    runCatching { mp.release() }
                }.getOrDefault(false)
            }

            if (!prepared) return@launch
            if (!isActive || !isPlayingDesired || desiredTrack != track) {
                runCatching { mp.release() }
                return@launch
            }

            mediaPlayer = mp
            loadedTrack = track
            mp.setOnErrorListener { _, what, extra ->
                Log.e(AMBIENT_TAG, "Ambient MediaPlayer error what=$what extra=$extra for ${track.id}")
                stopAndReleasePlayer()
                true
            }
            currentGain = 0.01f
            runCatching {
                mp.setVolume(currentGain, currentGain)
                mp.start()
            }.onFailure {
                Log.e(AMBIENT_TAG, "Failed to start ambient MediaPlayer for ${track.id}", it)
            }
            smoothFadeTo(effectiveGain(), durationMs = 520L)
        }
    }

    private fun stopAndReleasePlayer() {
        val old = mediaPlayer
        mediaPlayer = null
        loadedTrack = null
        currentGain = 0f
        if (old != null) {
            runCatching { if (old.isPlaying) old.stop() }
            runCatching { old.release() }
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val am = audioManager ?: return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(focusChangeListener)
        }
        hasAudioFocus = false
    }
}

/**
 * Pure, click-free offline stereo WAV loop synthesizer tailored for serene poetry reading.
 * Every track uses gentle low-passed harmonics, lush circular-wrapped hall reverb,
 * and exact peak normalization so it sounds warm, spacious, and calming at low volumes.
 */
private object ReadingAmbientRenderer {

    private val renderLock = Any()

    fun getOrRenderLoopWav(context: Context, track: ReadingAmbientTrack): File? {
        val dir = File(context.cacheDir, AMBIENT_CACHE_DIR).apply { mkdirs() }
        val target = File(dir, "ambient_${track.id}_24k.wav")
        if (target.exists() && target.length() > 4096L) {
            return target
        }

        synchronized(renderLock) {
            if (target.exists() && target.length() > 4096L) {
                return target
            }
            return runCatching {
                val (left, right) = renderTrackBuffers(track)
                val tmp = File(dir, "ambient_${track.id}_24k.wav.tmp")
                writeNormalizedStereoWav(tmp, left, right, targetPeak = 0.78f)
                if (!tmp.renameTo(target)) {
                    target.writeBytes(tmp.readBytes())
                    tmp.delete()
                }
                target
            }.onFailure {
                Log.e(AMBIENT_TAG, "Failed rendering ambient loop ${track.id}", it)
            }.getOrNull()
        }
    }

    private fun renderTrackBuffers(track: ReadingAmbientTrack): Pair<FloatArray, FloatArray> {
        val bpm = when (track) {
            ReadingAmbientTrack.SOFT_PIANO -> 58.0
            ReadingAmbientTrack.MIDNIGHT_RAIN -> 56.0
            ReadingAmbientTrack.MEHFIL_BANSURI -> 54.0
            ReadingAmbientTrack.ACOUSTIC_GUITAR -> 64.0
            ReadingAmbientTrack.SILK_STRINGS -> 58.0
            ReadingAmbientTrack.NIGHT_STILLNESS -> 50.0
        }

        val beatSec = 60.0 / bpm
        val eighthSec = beatSec * 0.5
        val totalEighths = 32 // 4 full slow bars (~15-19s seamless meditative cycle)
        val loopSamples = (totalEighths * eighthSec * AMBIENT_SAMPLE_RATE).toInt()
        val tailSamples = (3.6 * AMBIENT_SAMPLE_RATE).toInt()
        val bufL = FloatArray(loopSamples + tailSamples)
        val bufR = FloatArray(loopSamples + tailSamples)

        // Pre-layer continuous rain or tanpura bed before melodic phrases
        when (track) {
            ReadingAmbientTrack.MIDNIGHT_RAIN -> {
                addSoftRainBed(bufL, bufR, loopSamples, amp = 0.16f)
            }
            ReadingAmbientTrack.MEHFIL_BANSURI -> {
                addSoftTanpuraDrone(bufL, bufR, loopSamples, rootFreq = 130.81, amp = 0.20f) // C3 Sa-Pa drone
            }
            ReadingAmbientTrack.NIGHT_STILLNESS -> {
                addWarmBreathDrone(bufL, bufR, loopSamples, rootFreq = 110.00, amp = 0.24f) // A2 deep calm
            }
            else -> Unit
        }

        for (step in 0 until totalEighths) {
            val startSample = (step * eighthSec * AMBIENT_SAMPLE_RATE).toInt()
            val bar = (step / 8) % 4
            val beat = step % 8

            when (track) {
                ReadingAmbientTrack.SOFT_PIANO -> {
                    // Tender felt piano progression: Dm9 -> Bbmaj7 -> Fmaj7 -> Csus2
                    val chord = when (bar) {
                        0 -> doubleArrayOf(146.83, 220.00, 261.63, 329.63, 349.23)
                        1 -> doubleArrayOf(116.54, 174.61, 220.00, 293.66, 349.23)
                        2 -> doubleArrayOf(174.61, 220.00, 261.63, 329.63, 440.00)
                        else -> doubleArrayOf(130.81, 196.00, 261.63, 293.66, 392.00)
                    }
                    if (beat == 0) {
                        addFeltPianoNote(bufL, bufR, startSample, chord[0] * 0.5, durSec = 4.6, amp = 0.24f, pan = -0.18f)
                        addWarmPadNote(bufL, bufR, startSample, chord[1], durSec = beatSec * 4.2, amp = 0.12f, pan = 0.12f)
                    }
                    if (beat == 0 || beat == 2 || beat == 4 || beat == 6) {
                        val idx = (beat / 2 + 1).coerceAtMost(chord.lastIndex)
                        addFeltPianoNote(
                            bufL, bufR, startSample,
                            freq = chord[idx],
                            durSec = 3.4,
                            amp = if (beat == 0) 0.20f else 0.14f,
                            pan = -0.15f + idx * 0.08f
                        )
                    }
                    val melody: Double? = when {
                        bar == 0 && beat == 1 -> 523.25
                        bar == 0 && beat == 5 -> 587.33
                        bar == 1 && beat == 1 -> 523.25
                        bar == 1 && beat == 4 -> 440.00
                        bar == 2 && beat == 1 -> 659.25
                        bar == 2 && beat == 5 -> 587.33
                        bar == 3 && beat == 1 -> 523.25
                        bar == 3 && beat == 5 -> 440.00
                        else -> null
                    }
                    if (melody != null) {
                        addFeltPianoNote(bufL, bufR, startSample, melody, durSec = 3.8, amp = 0.18f, pan = 0.20f)
                    }
                }

                ReadingAmbientTrack.MIDNIGHT_RAIN -> {
                    // Sparse dreamy piano & warm pad over soft rain ambience: Am7 -> Fmaj7 -> Dm7 -> Em7
                    val chord = when (bar) {
                        0 -> doubleArrayOf(110.00, 164.81, 220.00, 261.63, 329.63)
                        1 -> doubleArrayOf(87.31, 174.61, 220.00, 261.63, 329.63)
                        2 -> doubleArrayOf(146.83, 220.00, 261.63, 349.23, 440.00)
                        else -> doubleArrayOf(164.81, 196.00, 246.94, 293.66, 392.00)
                    }
                    if (beat == 0) {
                        addWarmPadNote(bufL, bufR, startSample, chord[0], durSec = beatSec * 4.2, amp = 0.14f, pan = -0.10f)
                        addFeltPianoNote(bufL, bufR, startSample, chord[2], durSec = 3.6, amp = 0.15f, pan = -0.18f)
                    }
                    if (beat == 3) {
                        addFeltPianoNote(bufL, bufR, startSample, chord[4], durSec = 3.2, amp = 0.13f, pan = 0.22f)
                    }
                    val dropNote: Double? = when {
                        bar == 0 && beat == 6 -> 523.25
                        bar == 1 && beat == 5 -> 493.88
                        bar == 2 && beat == 6 -> 587.33
                        bar == 3 && beat == 5 -> 493.88
                        else -> null
                    }
                    if (dropNote != null) {
                        addCelestaDropNote(bufL, bufR, startSample, dropNote, durSec = 3.0, amp = 0.11f, pan = 0.15f)
                    }
                }

                ReadingAmbientTrack.MEHFIL_BANSURI -> {
                    // Soulful Raag Yaman / Bhupali inspired mellow Bansuri flute over C3 Tanpura
                    val phrase: Pair<Double, Double>? = when {
                        bar == 0 && beat == 0 -> 329.63 to beatSec * 2.2 // Ga (E4)
                        bar == 0 && beat == 5 -> 392.00 to beatSec * 1.6 // Pa (G4)
                        bar == 1 && beat == 0 -> 440.00 to beatSec * 2.2 // Dha (A4)
                        bar == 1 && beat == 5 -> 392.00 to beatSec * 1.6 // Pa (G4)
                        bar == 2 && beat == 0 -> 523.25 to beatSec * 2.0 // Sa (C5)
                        bar == 2 && beat == 4 -> 440.00 to beatSec * 1.8 // Dha (A4)
                        bar == 3 && beat == 0 -> 392.00 to beatSec * 2.0 // Pa (G4)
                        bar == 3 && beat == 4 -> 329.63 to beatSec * 2.0 // Ga (E4)
                        else -> null
                    }
                    if (phrase != null) {
                        addBansuriNote(
                            bufL, bufR, startSample,
                            freq = phrase.first,
                            durSec = phrase.second,
                            amp = 0.24f,
                            pan = if (bar % 2 == 0) 0.10f else -0.10f
                        )
                    }
                    if (beat == 0) {
                        val padFreq = if (bar % 2 == 0) 196.00 else 164.81
                        addWarmPadNote(bufL, bufR, startSample, padFreq, durSec = beatSec * 4.0, amp = 0.10f, pan = 0f)
                    }
                }

                ReadingAmbientTrack.ACOUSTIC_GUITAR -> {
                    // Warm late-night fingerpicked nylon guitar: Em9 -> Cmaj7 -> Gmaj7 -> Dsus2
                    val chord = when (bar) {
                        0 -> doubleArrayOf(164.81, 246.94, 293.66, 369.99, 493.88)
                        1 -> doubleArrayOf(130.81, 196.00, 246.94, 329.63, 392.00)
                        2 -> doubleArrayOf(196.00, 246.94, 293.66, 369.99, 493.88)
                        else -> doubleArrayOf(146.83, 220.00, 293.66, 329.63, 440.00)
                    }
                    if (beat == 0) {
                        addWarmPadNote(bufL, bufR, startSample, chord[0] * 0.5, durSec = beatSec * 4.0, amp = 0.15f, pan = 0f)
                    }
                    val finger = when (beat) {
                        0 -> 0; 1 -> 1; 2 -> 2; 3 -> 4; 4 -> 3; 5 -> 2; 6 -> 3; else -> 1
                    }
                    addSoftNylonGuitarNote(
                        bufL, bufR, startSample,
                        freq = chord[finger],
                        durSec = 2.8,
                        amp = if (beat == 0) 0.26f else 0.18f,
                        pan = -0.20f + finger * 0.10f
                    )
                }

                ReadingAmbientTrack.SILK_STRINGS -> {
                    // Lush romantic velvet strings + warm cello: Fmaj7 -> Dm9 -> Bbmaj7 -> Csus4
                    val chord = when (bar) {
                        0 -> doubleArrayOf(174.61, 261.63, 329.63, 440.00)
                        1 -> doubleArrayOf(146.83, 220.00, 261.63, 349.23)
                        2 -> doubleArrayOf(116.54, 174.61, 220.00, 293.66)
                        else -> doubleArrayOf(130.81, 196.00, 261.63, 349.23)
                    }
                    if (beat == 0) {
                        addWarmPadNote(bufL, bufR, startSample, chord[0] * 0.5, durSec = beatSec * 4.4, amp = 0.22f, pan = -0.22f)
                        addWarmPadNote(bufL, bufR, startSample, chord[1], durSec = beatSec * 4.4, amp = 0.16f, pan = 0.22f)
                        addWarmPadNote(bufL, bufR, startSample, chord[2], durSec = beatSec * 4.2, amp = 0.12f, pan = -0.08f)
                    }
                    val violinNote: Pair<Double, Double>? = when {
                        bar == 0 && beat == 0 -> 523.25 to beatSec * 2.4
                        bar == 0 && beat == 5 -> 587.33 to beatSec * 1.8
                        bar == 1 && beat == 0 -> 523.25 to beatSec * 2.6
                        bar == 1 && beat == 5 -> 440.00 to beatSec * 1.8
                        bar == 2 && beat == 0 -> 466.16 to beatSec * 2.4
                        bar == 2 && beat == 5 -> 440.00 to beatSec * 1.8
                        bar == 3 && beat == 0 -> 392.00 to beatSec * 3.4
                        else -> null
                    }
                    if (violinNote != null) {
                        addVelvetStringNote(
                            bufL, bufR, startSample,
                            freq = violinNote.first,
                            durSec = violinNote.second,
                            amp = 0.20f,
                            pan = 0.12f
                        )
                    }
                }

                ReadingAmbientTrack.NIGHT_STILLNESS -> {
                    // Ultra-calm starry chimes floating over warm breath drone
                    val chime: Double? = when {
                        bar == 0 && beat == 0 -> 440.00
                        bar == 0 && beat == 5 -> 523.25
                        bar == 1 && beat == 2 -> 659.25
                        bar == 2 && beat == 0 -> 587.33
                        bar == 2 && beat == 5 -> 523.25
                        bar == 3 && beat == 2 -> 440.00
                        else -> null
                    }
                    if (chime != null) {
                        addCelestaDropNote(
                            bufL, bufR, startSample,
                            freq = chime,
                            durSec = 4.2,
                            amp = 0.14f,
                            pan = if (bar % 2 == 0) -0.22f else 0.22f
                        )
                    }
                    if (beat == 0) {
                        val fifth = if (bar % 2 == 0) 164.81 else 220.00
                        addWarmPadNote(bufL, bufR, startSample, fifth, durSec = beatSec * 4.4, amp = 0.16f, pan = if (bar % 2 == 0) 0.16f else -0.16f)
                    }
                }
            }
        }

        // Fold reverb/note tail circularly onto loop start for 100% invisible loop seam
        val loopL = FloatArray(loopSamples)
        val loopR = FloatArray(loopSamples)
        for (i in 0 until loopSamples) {
            loopL[i] = bufL[i]
            loopR[i] = bufR[i]
        }
        for (i in 0 until tailSamples) {
            val wrapIdx = i % loopSamples
            loopL[wrapIdx] += bufL[loopSamples + i]
            loopR[wrapIdx] += bufR[loopSamples + i]
        }

        val wet = when (track) {
            ReadingAmbientTrack.NIGHT_STILLNESS -> 0.42f
            ReadingAmbientTrack.SOFT_PIANO,
            ReadingAmbientTrack.MIDNIGHT_RAIN -> 0.36f
            ReadingAmbientTrack.MEHFIL_BANSURI,
            ReadingAmbientTrack.SILK_STRINGS -> 0.34f
            ReadingAmbientTrack.ACOUSTIC_GUITAR -> 0.28f
        }
        applyCircularHallReverb(loopL, loopR, wet = wet)

        // Extra-warm analog low-pass so high frequencies stay silky and never distract while reading
        val lpAlpha = when (track) {
            ReadingAmbientTrack.MIDNIGHT_RAIN -> 0.34f
            ReadingAmbientTrack.NIGHT_STILLNESS -> 0.28f
            else -> 0.36f
        }
        applyCircularLowPass(loopL, alpha = lpAlpha)
        applyCircularLowPass(loopR, alpha = lpAlpha)

        return loopL to loopR
    }

    private fun addSoftRainBed(bufL: FloatArray, bufR: FloatArray, loopSamples: Int, amp: Float) {
        // Deterministic pink-filtered soft rain with circular cross-fade so there is zero seam
        var seed = 0x51A7A31L
        fun nextWhite(): Float {
            seed = (seed * 6364136223846793005L + 1442695040888963407L)
            return ((seed ushr 33).toInt() / 2147483648.0f) - 0.5f
        }
        val rawL = FloatArray(loopSamples)
        val rawR = FloatArray(loopSamples)
        var b0L = 0f; var b1L = 0f; var b2L = 0f
        var b0R = 0f; var b1R = 0f; var b2R = 0f
        for (i in 0 until loopSamples) {
            val wL = nextWhite()
            val wR = nextWhite()
            b0L = 0.996f * b0L + wL * 0.055f
            b1L = 0.975f * b1L + wL * 0.085f
            b2L = 0.880f * b2L + wL * 0.095f
            b0R = 0.996f * b0R + wR * 0.055f
            b1R = 0.975f * b1R + wR * 0.085f
            b2R = 0.880f * b2R + wR * 0.095f
            val gentleBreath = 0.86f + 0.14f * sin(AMBIENT_TWO_PI * i / loopSamples).toFloat()
            rawL[i] = (b0L + b1L + b2L) * gentleBreath * amp
            rawR[i] = (b0R + b1R + b2R) * gentleBreath * amp
        }
        // Crossfade last 0.5s with first 0.5s for completely invisible seam
        val xfade = (0.5 * AMBIENT_SAMPLE_RATE).toInt().coerceAtMost(loopSamples / 4)
        for (i in 0 until xfade) {
            val f = i.toFloat() / xfade.toFloat()
            val tailIdx = loopSamples - xfade + i
            rawL[tailIdx] = rawL[tailIdx] * (1f - f) + rawL[i] * f
            rawR[tailIdx] = rawR[tailIdx] * (1f - f) + rawR[i] * f
        }
        for (i in 0 until loopSamples) {
            bufL[i] += rawL[i]
            bufR[i] += rawR[i]
        }
    }

    private fun addSoftTanpuraDrone(bufL: FloatArray, bufR: FloatArray, loopSamples: Int, rootFreq: Double, amp: Float) {
        // Gentle Sa (root) + Pa (fifth) + upper Sa harmonic swell
        val freqs = doubleArrayOf(rootFreq, rootFreq * 1.5, rootFreq * 2.0)
        val gains = floatArrayOf(0.55f, 0.28f, 0.17f)
        for (i in 0 until loopSamples) {
            val t = i.toDouble() / AMBIENT_SAMPLE_RATE
            val cycleFrac = i.toDouble() / loopSamples.toDouble()
            val slowSwell = 0.82 + 0.18 * sin(AMBIENT_TWO_PI * cycleFrac)
            var sL = 0.0
            var sR = 0.0
            for (k in freqs.indices) {
                val f = freqs[k]
                // Integer cycles over loopSamples so phase wraps cleanly
                val cycles = kotlin.math.round(f * loopSamples / AMBIENT_SAMPLE_RATE).coerceAtLeast(1.0)
                val exactPhase = AMBIENT_TWO_PI * cycles * cycleFrac
                val harmonic = sin(exactPhase) * 0.78 + sin(2.0 * exactPhase) * 0.18 + sin(3.0 * exactPhase) * 0.04
                val panMod = 0.5 + 0.15 * sin(AMBIENT_TWO_PI * (cycleFrac * (k + 1)))
                sL += harmonic * gains[k] * (1.0 - panMod * 0.4)
                sR += harmonic * gains[k] * (0.6 + panMod * 0.4)
            }
            bufL[i] += (sL * slowSwell * amp).toFloat()
            bufR[i] += (sR * slowSwell * amp).toFloat()
        }
    }

    private fun addWarmBreathDrone(bufL: FloatArray, bufR: FloatArray, loopSamples: Int, rootFreq: Double, amp: Float) {
        val freqs = doubleArrayOf(rootFreq, rootFreq * 1.5, rootFreq * 2.0)
        val gains = floatArrayOf(0.60f, 0.25f, 0.15f)
        for (i in 0 until loopSamples) {
            val frac = i.toDouble() / loopSamples.toDouble()
            val swell = 0.78 + 0.22 * cos(AMBIENT_TWO_PI * frac)
            var sum = 0.0
            for (k in freqs.indices) {
                val cycles = kotlin.math.round(freqs[k] * loopSamples / AMBIENT_SAMPLE_RATE).coerceAtLeast(1.0)
                sum += sin(AMBIENT_TWO_PI * cycles * frac) * gains[k]
            }
            val valF = (sum * swell * amp).toFloat()
            bufL[i] += valF
            bufR[i] += valF
        }
    }

    private fun addFeltPianoNote(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        durSec: Double,
        amp: Float,
        pan: Float
    ) {
        val len = (durSec * AMBIENT_SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        val w1 = AMBIENT_TWO_PI * freq / AMBIENT_SAMPLE_RATE
        val wDet = AMBIENT_TWO_PI * (freq * 1.0012) / AMBIENT_SAMPLE_RATE
        for (i in start until end) {
            val n = i - start
            val t = n.toDouble() / AMBIENT_SAMPLE_RATE
            val attack = 1.0 - exp(-t * 55.0)
            val decay = exp(-t * 0.78)
            val tailFade = if (n > len - 2800) ((len - n).toDouble() / 2800.0).coerceIn(0.0, 1.0) else 1.0
            val p = w1 * n
            val wave = sin(p) * 0.68 + sin(wDet * n) * 0.20 + sin(2.0 * p) * 0.10 * exp(-t * 2.1)
            val s = (wave * attack * decay * tailFade).toFloat()
            bufL[i] += s * gL
            bufR[i] += s * gR
        }
    }

    private fun addCelestaDropNote(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        durSec: Double,
        amp: Float,
        pan: Float
    ) {
        val len = (durSec * AMBIENT_SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        val w = AMBIENT_TWO_PI * freq / AMBIENT_SAMPLE_RATE
        for (i in start until end) {
            val n = i - start
            val t = n.toDouble() / AMBIENT_SAMPLE_RATE
            val attack = 1.0 - exp(-t * 70.0)
            val decay = exp(-t * 1.05)
            val tailFade = if (n > len - 2400) ((len - n).toDouble() / 2400.0).coerceIn(0.0, 1.0) else 1.0
            val wave = sin(w * n) * 0.84 + sin(2.0 * w * n) * 0.12 * exp(-t * 3.0) + sin(3.0 * w * n) * 0.04 * exp(-t * 5.0)
            val s = (wave * attack * decay * tailFade).toFloat()
            bufL[i] += s * gL
            bufR[i] += s * gR
        }
    }

    private fun addBansuriNote(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        durSec: Double,
        amp: Float,
        pan: Float
    ) {
        val len = (durSec * AMBIENT_SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        var phase = 0.0
        for (i in start until end) {
            val n = i - start
            val frac = n.toDouble() / len.coerceAtLeast(1).toDouble()
            // Expressive breathy bamboo flute swell
            val env = sin(PI * frac).let { it * it }
            val t = n.toDouble() / AMBIENT_SAMPLE_RATE
            // Gentle pitch glide-in + warm Indian meend vibrato
            val glide = 1.0 - 0.012 * exp(-t * 7.5)
            val vibrato = 1.0 + 0.0045 * sin(AMBIENT_TWO_PI * 4.4 * t) * env
            val instFreq = freq * glide * vibrato
            phase = (phase + AMBIENT_TWO_PI * instFreq / AMBIENT_SAMPLE_RATE) % AMBIENT_TWO_PI
            val wave = sin(phase) * 0.84 + sin(phase * 2.0) * 0.13 + sin(phase * 3.0) * 0.03
            val s = (wave * env).toFloat()
            bufL[i] += s * gL
            bufR[i] += s * gR
        }
    }

    private fun addSoftNylonGuitarNote(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        durSec: Double,
        amp: Float,
        pan: Float
    ) {
        val len = (durSec * AMBIENT_SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        val w = AMBIENT_TWO_PI * freq / AMBIENT_SAMPLE_RATE
        for (i in start until end) {
            val n = i - start
            val t = n.toDouble() / AMBIENT_SAMPLE_RATE
            val attack = 1.0 - exp(-t * 110.0)
            val decay = exp(-t * 1.35)
            val tailFade = if (n > len - 1800) ((len - n).toDouble() / 1800.0).coerceIn(0.0, 1.0) else 1.0
            val p = w * n
            val wave = sin(p) * 0.72 + sin(2.0 * p) * 0.21 * exp(-t * 2.0) + sin(3.0 * p) * 0.06 * exp(-t * 3.8)
            val s = (wave * attack * decay * tailFade).toFloat()
            bufL[i] += s * gL
            bufR[i] += s * gR
        }
    }

    private fun addVelvetStringNote(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        durSec: Double,
        amp: Float,
        pan: Float
    ) {
        val len = (durSec * AMBIENT_SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        var phase = 0.0
        for (i in start until end) {
            val n = i - start
            val frac = n.toDouble() / len.coerceAtLeast(1).toDouble()
            val env = sin(PI * frac).let { it * it }
            val t = n.toDouble() / AMBIENT_SAMPLE_RATE
            val instFreq = freq * (1.0 + 0.0032 * sin(AMBIENT_TWO_PI * 4.5 * t) * env)
            phase = (phase + AMBIENT_TWO_PI * instFreq / AMBIENT_SAMPLE_RATE) % AMBIENT_TWO_PI
            val wave = sin(phase) * 0.78 + sin(phase * 2.0) * 0.16 + sin(phase * 3.0) * 0.05
            val s = (wave * env).toFloat()
            bufL[i] += s * gL
            bufR[i] += s * gR
        }
    }

    private fun addWarmPadNote(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        durSec: Double,
        amp: Float,
        pan: Float
    ) {
        val len = (durSec * AMBIENT_SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        val wL = AMBIENT_TWO_PI * freq / AMBIENT_SAMPLE_RATE
        val wR = AMBIENT_TWO_PI * (freq * 1.0016) / AMBIENT_SAMPLE_RATE
        for (i in start until end) {
            val n = i - start
            val frac = n.toDouble() / len.coerceAtLeast(1).toDouble()
            val env = 0.5 - 0.5 * cos(AMBIENT_TWO_PI * frac)
            val sL = (sin(wL * n) * 0.88 + sin(2.0 * wL * n) * 0.12) * env
            val sR = (sin(wR * n) * 0.88 + sin(2.0 * wR * n) * 0.12) * env
            bufL[i] += (sL * gL).toFloat()
            bufR[i] += (sR * gR).toFloat()
        }
    }

    private fun applyCircularHallReverb(bufL: FloatArray, bufR: FloatArray, wet: Float) {
        val len = bufL.size
        val tapsL = intArrayOf((AMBIENT_SAMPLE_RATE * 0.13).toInt(), (AMBIENT_SAMPLE_RATE * 0.27).toInt(), (AMBIENT_SAMPLE_RATE * 0.43).toInt())
        val tapsR = intArrayOf((AMBIENT_SAMPLE_RATE * 0.17).toInt(), (AMBIENT_SAMPLE_RATE * 0.31).toInt(), (AMBIENT_SAMPLE_RATE * 0.49).toInt())
        val gains = floatArrayOf(0.46f, 0.30f, 0.18f)

        val srcL = bufL.copyOf()
        val srcR = bufR.copyOf()
        val dry = 1f - wet * 0.28f

        for (i in 0 until len) {
            var rL = 0f
            var rR = 0f
            for (t in 0 until 3) {
                val idxL = (i - tapsL[t] + len) % len
                val idxR = (i - tapsR[t] + len) % len
                rL += (srcL[idxL] * 0.70f + srcR[idxL] * 0.30f) * gains[t]
                rR += (srcR[idxR] * 0.70f + srcL[idxR] * 0.30f) * gains[t]
            }
            bufL[i] = srcL[i] * dry + rL * wet
            bufR[i] = srcR[i] * dry + rR * wet
        }
    }

    private fun applyCircularLowPass(buf: FloatArray, alpha: Float) {
        if (buf.isEmpty()) return
        var state = buf[buf.size - 1]
        for (i in buf.indices) {
            state += (buf[i] - state) * alpha
            buf[i] = state
        }
    }

    private fun writeNormalizedStereoWav(
        outFile: File,
        left: FloatArray,
        right: FloatArray,
        targetPeak: Float
    ) {
        val frames = left.size
        var maxPeak = 0.0001f
        for (i in 0 until frames) {
            maxPeak = max(maxPeak, max(abs(left[i]), abs(right[i])))
        }
        val scale = (targetPeak / maxPeak) * Short.MAX_VALUE

        val dataSize = frames * 4
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(2)
            putInt(AMBIENT_SAMPLE_RATE)
            putInt(AMBIENT_SAMPLE_RATE * 4)
            putShort(4)
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }.array()

        FileOutputStream(outFile).use { fos ->
            fos.write(header)
            val chunkFrames = 4096
            val byteBuf = ByteArray(chunkFrames * 4)
            var offset = 0
            while (offset < frames) {
                val count = minOf(chunkFrames, frames - offset)
                var b = 0
                for (i in 0 until count) {
                    val sL = (left[offset + i] * scale).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    val sR = (right[offset + i] * scale).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    byteBuf[b++] = (sL and 0xFF).toByte()
                    byteBuf[b++] = ((sL shr 8) and 0xFF).toByte()
                    byteBuf[b++] = (sR and 0xFF).toByte()
                    byteBuf[b++] = ((sR shr 8) and 0xFF).toByte()
                }
                fos.write(byteBuf, 0, count * 4)
                offset += count
            }
        }
    }
}
