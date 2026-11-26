package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import kotlin.math.round
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

private const val TAG = "ShayariBgmPlayer"
private const val BGM_CACHE_DIR = "bgm_loops_v5"
private const val SAMPLE_RATE = 24_000
private const val TWO_PI = 2.0 * PI

enum class ReelBgmTrack(
    val id: String,
    val labelHi: String,
    val subtitleHi: String,
    val emoji: String
) {
    SUFI_BANSURI_MEHFIL(
        id = "sufi_bansuri_mehfil",
        labelHi = "सूफ़ी बाँसुरी व तबला",
        subtitleHi = "राग यमन बाँसुरी + हल्का तबला ताल",
        emoji = "🪈"
    ),
    TIKTOK_SAD_GUITAR(
        id = "tiktok_sad_guitar",
        labelHi = "दर्द अकॉस्टिक गिटार",
        subtitleHi = "साफ़ स्पैनिश गिटार फिंगरपिकिंग",
        emoji = "🎸"
    ),
    TIKTOK_ROMANTIC_STRINGS(
        id = "tiktok_romantic_strings",
        labelHi = "इश्क़ रोमांटिक वायलिन",
        subtitleHi = "खिलता मेजर स्केल वायलिन व हार्प",
        emoji = "🎻"
    ),
    RAINY_NIGHT_SITAR(
        id = "rainy_night_sitar",
        labelHi = "बारिश और सितार",
        subtitleHi = "रिमझिम बारिश + राग भैरवी सितार",
        emoji = "🌧️"
    ),
    TIKTOK_VIRAL_PIANO(
        id = "tiktok_viral_piano",
        labelHi = "स्लो इमोशनल पियानो",
        subtitleHi = "Slowed + Reverb ग्रैंड पियानो",
        emoji = "🎹"
    ),
    TIKTOK_LOFI_BEAT(
        id = "tiktok_lofi_beat",
        labelHi = "चिल लो-फ़ाई रोड्स",
        subtitleHi = "मॉडर्न रोड्स + सॉफ्ट लो-फ़ाई बीट",
        emoji = "🎧"
    ),
    CINEMATIC_CELLO_DRAMA(
        id = "cinematic_cello_drama",
        labelHi = "शायरी सेलो ऑर्केस्ट्रा",
        subtitleHi = "गहरा सेलो और दिल की धड़कन",
        emoji = "🎼"
    ),
    TIKTOK_ATTITUDE_BEAT(
        id = "tiktok_attitude_beat",
        labelHi = "एटीट्यूड स्वैग ट्रैप",
        subtitleHi = "पंच 808 बेस + हाई-हैट व डार्क रिफ़",
        emoji = "🔥"
    ),
    OFF(
        id = "off",
        labelHi = "म्यूज़िक बंद",
        subtitleHi = "सिर्फ़ शायर की आवाज़",
        emoji = "🔇"
    );

    companion object {
        fun fromId(id: String?): ReelBgmTrack {
            return when (id) {
                "emotional_piano" -> TIKTOK_VIRAL_PIANO
                "dard_strings" -> CINEMATIC_CELLO_DRAMA
                "rain_lofi" -> RAINY_NIGHT_SITAR
                "sufi_flute" -> SUFI_BANSURI_MEHFIL
                else -> entries.firstOrNull { it.id == id } ?: SUFI_BANSURI_MEHFIL
            }
        }

        fun recommendedForCategory(categoryId: String): ReelBgmTrack {
            return recommendedForReel(categoryId = categoryId, pageIndex = 0, stableKey = categoryId)
        }

        fun recommendedForReel(
            categoryId: String,
            pageIndex: Int,
            stableKey: String
        ): ReelBgmTrack {
            // Broad contrasting musical styles per mood, rotated strictly by pageIndex so
            // consecutive Reels ALWAYS change instrument, scale (सुर / राग), and rhythm!
            val cat = ShayariCategory.fromId(categoryId)
            val pool = when {
                cat == ShayariCategory.DARD ||
                    cat == ShayariCategory.BEWAFAI ||
                    cat == ShayariCategory.AANSU ||
                    cat == ShayariCategory.TANHAI ||
                    cat == ShayariCategory.MAUT_JANAZA -> listOf(
                    RAINY_NIGHT_SITAR,
                    TIKTOK_SAD_GUITAR,
                    CINEMATIC_CELLO_DRAMA,
                    TIKTOK_VIRAL_PIANO,
                    SUFI_BANSURI_MEHFIL
                )

                cat == ShayariCategory.MOHABBAT ||
                    cat == ShayariCategory.ROMANTIC ||
                    cat == ShayariCategory.SECRET_LOVE ||
                    cat == ShayariCategory.TAREEF -> listOf(
                    TIKTOK_ROMANTIC_STRINGS,
                    SUFI_BANSURI_MEHFIL,
                    TIKTOK_LOFI_BEAT,
                    TIKTOK_SAD_GUITAR,
                    TIKTOK_VIRAL_PIANO
                )

                cat == ShayariCategory.ATTITUDE ||
                    cat == ShayariCategory.HAUSLA ||
                    cat == ShayariCategory.DESHPREM -> listOf(
                    TIKTOK_ATTITUDE_BEAT,
                    CINEMATIC_CELLO_DRAMA,
                    TIKTOK_LOFI_BEAT,
                    SUFI_BANSURI_MEHFIL
                )

                cat == ShayariCategory.KHUDA_RAB ||
                    cat == ShayariCategory.ZINDAGI ||
                    cat == ShayariCategory.WAQT ||
                    cat == ShayariCategory.MUSAFIR -> listOf(
                    SUFI_BANSURI_MEHFIL,
                    RAINY_NIGHT_SITAR,
                    CINEMATIC_CELLO_DRAMA,
                    TIKTOK_SAD_GUITAR
                )

                cat.isJokeCategory || cat == ShayariCategory.GEN_Z || cat == ShayariCategory.NIBBA_NIBBI -> listOf(
                    TIKTOK_LOFI_BEAT,
                    TIKTOK_ROMANTIC_STRINGS,
                    TIKTOK_ATTITUDE_BEAT,
                    SUFI_BANSURI_MEHFIL
                )

                else -> listOf(
                    SUFI_BANSURI_MEHFIL,
                    TIKTOK_SAD_GUITAR,
                    TIKTOK_ROMANTIC_STRINGS,
                    RAINY_NIGHT_SITAR,
                    TIKTOK_LOFI_BEAT,
                    CINEMATIC_CELLO_DRAMA,
                    TIKTOK_VIRAL_PIANO,
                    TIKTOK_ATTITUDE_BEAT
                )
            }
            val pick = abs(pageIndex) % pool.size
            return pool[pick]
        }
    }
}

/**
 * Multi-Genre Studio-Normalized Reels Background Music Player.
 *
 * Pre-renders each seamless stereo WAV loop ONCE offline with distinct Indian & Western
 * scales, percussion/melodic timbres, circular reverb tail wrap-around (zero loop-seam click),
 * and plays through Android's hardware-mixed MediaPlayer.
 */
class ShayariBgmPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaPlayer: MediaPlayer? = null
    private var loadedTrack: ReelBgmTrack? = null
    private var desiredTrack: ReelBgmTrack = ReelBgmTrack.SUFI_BANSURI_MEHFIL
    private var isPlayingDesired: Boolean = false
    private var isDucked: Boolean = false
    private var currentVolume: Float = 0.68f
    private var switchJob: Job? = null
    private var fadeJob: Job? = null

    fun prewarmTrack(track: ReelBgmTrack) {
        if (track == ReelBgmTrack.OFF) return
        scope.launch(Dispatchers.Default) {
            BgmLoopRenderer.getOrRenderLoopWav(appContext, track)
        }
    }

    fun setTrack(track: ReelBgmTrack) {
        if (desiredTrack == track) return
        desiredTrack = track
        if (track == ReelBgmTrack.OFF) {
            stopAndReleasePlayer()
        } else if (isPlayingDesired) {
            loadAndStartTrack(track)
        }
    }

    fun setPlaying(shouldPlay: Boolean) {
        isPlayingDesired = shouldPlay
        if (!shouldPlay || desiredTrack == ReelBgmTrack.OFF) {
            switchJob?.cancel()
            fadeJob?.cancel()
            runCatching {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) mp.pause()
                }
            }
            return
        }

        val mp = mediaPlayer
        if (mp != null && loadedTrack == desiredTrack) {
            runCatching {
                val target = targetVolume()
                currentVolume = target
                mp.setVolume(target, target)
                if (!mp.isPlaying) mp.start()
            }
        } else {
            loadAndStartTrack(desiredTrack)
        }
    }

    fun setDucked(isVoiceSpeaking: Boolean) {
        if (isDucked == isVoiceSpeaking) return
        isDucked = isVoiceSpeaking
        smoothFadeTo(targetVolume())
    }

    fun release() {
        isPlayingDesired = false
        switchJob?.cancel()
        fadeJob?.cancel()
        stopAndReleasePlayer()
        scope.cancel()
    }

    private fun targetVolume(): Float {
        // Gentle cinematic ducking so every soft human breath, sigh, and vocal nuance shines clearly above the BGM
        return if (isDucked) 0.22f else 0.68f
    }

    private fun smoothFadeTo(target: Float) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 12
            val start = currentVolume
            for (step in 1..steps) {
                if (!isActive) return@launch
                val v = start + (target - start) * (step.toFloat() / steps.toFloat())
                currentVolume = v
                runCatching { mediaPlayer?.setVolume(v, v) }
                delay(20L)
            }
        }
    }

    private fun loadAndStartTrack(track: ReelBgmTrack) {
        switchJob?.cancel()
        switchJob = scope.launch {
            val wavFile = withContext(Dispatchers.Default) {
                BgmLoopRenderer.getOrRenderLoopWav(appContext, track)
            } ?: return@launch

            if (!isActive || !isPlayingDesired || desiredTrack != track) return@launch

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
                    Log.e(TAG, "Failed to prepare BGM loop for ${track.id}", it)
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
                Log.e(TAG, "BGM MediaPlayer runtime error what=$what extra=$extra for ${track.id}")
                stopAndReleasePlayer()
                true
            }
            val vol = targetVolume()
            currentVolume = vol
            runCatching {
                mp.setVolume(vol, vol)
                mp.start()
            }.onFailure {
                Log.e(TAG, "Failed starting BGM MediaPlayer for ${track.id}", it)
            }
        }
    }

    private fun stopAndReleasePlayer() {
        val old = mediaPlayer
        mediaPlayer = null
        loadedTrack = null
        if (old != null) {
            runCatching {
                if (old.isPlaying) old.stop()
            }
            runCatching { old.release() }
        }
    }
}

/**
 * Pure, click-free offline stereo WAV loop synthesizer for 8 genuinely distinct musical genres,
 * scales (Indian Raags + Major + Minor + Jazz + Phrygian Trap), rhythms, and instruments.
 */
private object BgmLoopRenderer {

    private val renderLock = Any()

    fun getOrRenderLoopWav(context: Context, track: ReelBgmTrack): File? {
        if (track == ReelBgmTrack.OFF) return null
        val dir = File(context.cacheDir, BGM_CACHE_DIR).apply { mkdirs() }
        val target = File(dir, "${track.id}_24k.wav")
        if (target.exists() && target.length() > 4096L) {
            return target
        }

        synchronized(renderLock) {
            if (target.exists() && target.length() > 4096L) {
                return target
            }
            return runCatching {
                val (left, right) = renderTrackBuffers(track)
                val tmp = File(dir, "${track.id}_24k.wav.tmp")
                writeNormalizedStereoWav(tmp, left, right, targetPeak = 0.84f)
                if (!tmp.renameTo(target)) {
                    target.writeBytes(tmp.readBytes())
                    tmp.delete()
                }
                target
            }.onFailure {
                Log.e(TAG, "Failed rendering BGM loop ${track.id}", it)
            }.getOrNull()
        }
    }

    private fun renderTrackBuffers(track: ReelBgmTrack): Pair<FloatArray, FloatArray> {
        val bpm = when (track) {
            ReelBgmTrack.SUFI_BANSURI_MEHFIL -> 82.0
            ReelBgmTrack.TIKTOK_SAD_GUITAR -> 72.0
            ReelBgmTrack.TIKTOK_ROMANTIC_STRINGS -> 78.0
            ReelBgmTrack.RAINY_NIGHT_SITAR -> 68.0
            ReelBgmTrack.TIKTOK_VIRAL_PIANO -> 60.0
            ReelBgmTrack.TIKTOK_LOFI_BEAT -> 86.0
            ReelBgmTrack.CINEMATIC_CELLO_DRAMA -> 64.0
            ReelBgmTrack.TIKTOK_ATTITUDE_BEAT -> 98.0
            ReelBgmTrack.OFF -> 72.0
        }
        val beatSec = 60.0 / bpm
        val eighthSec = beatSec * 0.5
        val totalEighths = 32
        val loopSamples = (totalEighths * eighthSec * SAMPLE_RATE).toInt()
        val tailSamples = (2.8 * SAMPLE_RATE).toInt()
        val bufL = FloatArray(loopSamples + tailSamples)
        val bufR = FloatArray(loopSamples + tailSamples)

        // Pre-layer authentic ambient/drone beds for distinct Indian classical tracks
        when (track) {
            ReelBgmTrack.SUFI_BANSURI_MEHFIL -> {
                addTanpuraDrone(bufL, bufR, loopSamples, rootFreq = 146.83, amp = 0.16f) // D3 Sa-Pa Tanpura
            }
            ReelBgmTrack.RAINY_NIGHT_SITAR -> {
                addSoftRainBed(bufL, bufR, loopSamples, amp = 0.14f)
                addTanpuraDrone(bufL, bufR, loopSamples, rootFreq = 130.81, amp = 0.11f) // C3 Bhairavi drone
            }
            else -> Unit
        }

        for (step in 0 until totalEighths) {
            val startSample = (step * eighthSec * SAMPLE_RATE).toInt()
            val bar = (step / 8) % 4
            val beat = step % 8

            when (track) {
                ReelBgmTrack.SUFI_BANSURI_MEHFIL -> {
                    // Authentic Raag Yaman (D E F# G# A B C# D) Bansuri + Soft Keherwa Tabla Groove
                    // Tabla Bayan (low Dha/Ghe) + Dayan (crisp Na/Tin)
                    when (beat) {
                        0 -> {
                            addTablaBayan(bufL, bufR, startSample, startFreq = 132.0, endFreq = 86.0, amp = 0.34f)
                            addTablaDayan(bufL, bufR, startSample, freq = 293.66, amp = 0.22f)
                        }
                        2 -> addTablaDayan(bufL, bufR, startSample, freq = 329.63, amp = 0.16f)
                        3 -> addTablaBayan(bufL, bufR, startSample, startFreq = 115.0, endFreq = 92.0, amp = 0.24f)
                        4 -> addTablaDayan(bufL, bufR, startSample, freq = 293.66, amp = 0.20f)
                        6 -> {
                            addTablaBayan(bufL, bufR, startSample, startFreq = 128.0, endFreq = 88.0, amp = 0.28f)
                            addTablaDayan(bufL, bufR, startSample, freq = 369.99, amp = 0.14f)
                        }
                    }
                    // Expressive Raag Yaman Bansuri phrases with authentic Indian meend glides
                    val bansuriPhrase: Pair<Double, Double>? = when {
                        bar == 0 && beat == 0 -> 369.99 to beatSec * 1.6 // Ga (F#4)
                        bar == 0 && beat == 3 -> 415.30 to beatSec * 1.1 // Teevra Ma (G#4)
                        bar == 0 && beat == 5 -> 440.00 to beatSec * 1.7 // Pa (A4)
                        bar == 1 && beat == 1 -> 493.88 to beatSec * 1.5 // Dha (B4)
                        bar == 1 && beat == 4 -> 554.37 to beatSec * 1.8 // Ni (C#5)
                        bar == 2 && beat == 0 -> 587.33 to beatSec * 1.6 // Upper Sa (D5)
                        bar == 2 && beat == 4 -> 493.88 to beatSec * 1.6 // Dha (B4)
                        bar == 3 && beat == 0 -> 440.00 to beatSec * 1.5 // Pa (A4)
                        bar == 3 && beat == 3 -> 369.99 to beatSec * 1.3 // Ga (F#4)
                        bar == 3 && beat == 6 -> 329.63 to beatSec * 1.2 // Re (E4)
                        else -> null
                    }
                    if (bansuriPhrase != null) {
                        addBansuriNote(
                            bufL, bufR, startSample,
                            freq = bansuriPhrase.first,
                            durSec = bansuriPhrase.second,
                            amp = 0.30f,
                            pan = if (bar % 2 == 0) 0.12f else -0.10f
                        )
                    }
                }

                ReelBgmTrack.TIKTOK_SAD_GUITAR -> {
                    // Clean Spanish/Bollywood Acoustic Guitar Fingerpicking (Em -> C -> Am -> B7) — bright, crisp, zero muddy pad
                    val chord = when (bar) {
                        0 -> doubleArrayOf(82.41, 164.81, 246.94, 329.63, 392.00)  // Em
                        1 -> doubleArrayOf(130.81, 196.00, 261.63, 329.63, 392.00) // C
                        2 -> doubleArrayOf(110.00, 164.81, 220.00, 261.63, 329.63) // Am
                        else -> doubleArrayOf(123.47, 185.00, 246.94, 311.13, 369.99) // B7
                    }
                    val fingerIdx = when (beat) {
                        0 -> 0; 1 -> 2; 2 -> 3; 3 -> 4; 4 -> 2; 5 -> 3; 6 -> 4; else -> 1
                    }
                    addCrispAcousticGuitarNote(
                        bufL, bufR, startSample,
                        freq = chord[fingerIdx],
                        durSec = 2.1,
                        amp = if (beat == 0) 0.36f else 0.24f,
                        pan = -0.24f + fingerIdx * 0.12f
                    )
                    // Expressive soulful high Spanish guitar hook
                    val lead: Double? = when {
                        bar == 0 && beat == 2 -> 659.25
                        bar == 0 && beat == 5 -> 587.33
                        bar == 1 && beat == 0 -> 523.25
                        bar == 1 && beat == 4 -> 493.88
                        bar == 2 && beat == 2 -> 523.25
                        bar == 2 && beat == 5 -> 493.88
                        bar == 3 && beat == 0 -> 493.88
                        bar == 3 && beat == 4 -> 466.16
                        else -> null
                    }
                    if (lead != null) {
                        addCrispAcousticGuitarNote(bufL, bufR, startSample, lead, durSec = 2.4, amp = 0.25f, pan = 0.24f)
                    }
                }

                ReelBgmTrack.TIKTOK_ROMANTIC_STRINGS -> {
                    // BRIGHT, UPLIFTING G MAJOR Bollywood Romance (G -> D -> Em7 -> Cmaj7) + Sparkling Harp
                    val chord = when (bar) {
                        0 -> doubleArrayOf(196.00, 246.94, 293.66, 392.00, 493.88) // G maj
                        1 -> doubleArrayOf(146.83, 220.00, 293.66, 369.99, 440.00) // D maj
                        2 -> doubleArrayOf(164.81, 246.94, 293.66, 392.00, 493.88) // Em7
                        else -> doubleArrayOf(130.81, 196.00, 246.94, 329.63, 392.00) // Cmaj7
                    }
                    if (beat == 0) {
                        addWarmPadNote(bufL, bufR, startSample, chord[0] * 0.5, durSec = beatSec * 3.8, amp = 0.16f, pan = -0.16f)
                    }
                    // Sparkling rising major harp shimmer
                    val harpIdx = when (beat) {
                        0 -> 0; 1 -> 1; 2 -> 2; 3 -> 3; 4 -> 4; 5 -> 3; 6 -> 2; else -> 4
                    }
                    addHarpPluckNote(
                        bufL, bufR, startSample,
                        freq = chord[harpIdx],
                        durSec = 2.0,
                        amp = 0.18f,
                        pan = -0.28f + harpIdx * 0.14f
                    )
                    // Soaring joyful G-major romantic violin theme
                    val violin: Pair<Double, Double>? = when {
                        bar == 0 && beat == 0 -> 587.33 to beatSec * 1.8 // D5
                        bar == 0 && beat == 4 -> 659.25 to beatSec * 1.0 // E5
                        bar == 0 && beat == 6 -> 783.99 to beatSec * 1.2 // G5
                        bar == 1 && beat == 0 -> 739.99 to beatSec * 2.0 // F#5
                        bar == 1 && beat == 4 -> 587.33 to beatSec * 1.8 // D5
                        bar == 2 && beat == 0 -> 659.25 to beatSec * 1.8 // E5
                        bar == 2 && beat == 4 -> 783.99 to beatSec * 1.8 // G5
                        bar == 3 && beat == 0 -> 739.99 to beatSec * 1.4 // F#5
                        bar == 3 && beat == 3 -> 659.25 to beatSec * 1.4 // E5
                        bar == 3 && beat == 6 -> 587.33 to beatSec * 1.2 // D5
                        else -> null
                    }
                    if (violin != null) {
                        addSilkyViolinNote(bufL, bufR, startSample, violin.first, violin.second, amp = 0.28f, pan = 0.14f)
                    }
                }

                ReelBgmTrack.RAINY_NIGHT_SITAR -> {
                    // Melancholic Raag Bhairavi (C Db Eb F G Ab Bb C) Sitar & Santoor in evening rain
                    val sitarNote: Double? = when {
                        bar == 0 && beat == 0 -> 311.13 // Komal Ga (Eb4)
                        bar == 0 && beat == 2 -> 277.18 // Komal Re (Db4)
                        bar == 0 && beat == 4 -> 261.63 // Sa (C4)
                        bar == 0 && beat == 6 -> 349.23 // Ma (F4)
                        bar == 1 && beat == 0 -> 392.00 // Pa (G4)
                        bar == 1 && beat == 3 -> 415.30 // Komal Dha (Ab4)
                        bar == 1 && beat == 5 -> 392.00 // Pa (G4)
                        bar == 2 && beat == 0 -> 466.16 // Komal Ni (Bb4)
                        bar == 2 && beat == 2 -> 523.25 // Sa (C5)
                        bar == 2 && beat == 5 -> 415.30 // Komal Dha (Ab4)
                        bar == 3 && beat == 0 -> 392.00 // Pa (G4)
                        bar == 3 && beat == 2 -> 349.23 // Ma (F4)
                        bar == 3 && beat == 4 -> 311.13 // Komal Ga (Eb4)
                        bar == 3 && beat == 6 -> 277.18 // Komal Re (Db4)
                        else -> null
                    }
                    if (sitarNote != null) {
                        addSitarPluckNote(
                            bufL, bufR, startSample,
                            freq = sitarNote,
                            durSec = 2.6,
                            amp = 0.30f,
                            pan = if (beat % 2 == 0) -0.16f else 0.18f
                        )
                    }
                    // Subtle shimmering Santoor response on off-beats
                    if (beat == 1 || beat == 7) {
                        val santoor = if (bar % 2 == 0) 523.25 else 392.00
                        addHarpPluckNote(bufL, bufR, startSample, santoor, durSec = 1.6, amp = 0.13f, pan = 0.25f)
                    }
                }

                ReelBgmTrack.TIKTOK_VIRAL_PIANO -> {
                    // Slowed + Reverb Felt Grand Piano (Dm -> Bb -> F -> C) — spacious and ethereal
                    val chord = when (bar) {
                        0 -> doubleArrayOf(146.83, 220.00, 293.66, 349.23, 440.00)
                        1 -> doubleArrayOf(116.54, 174.61, 233.08, 293.66, 349.23)
                        2 -> doubleArrayOf(174.61, 261.63, 349.23, 440.00, 523.25)
                        else -> doubleArrayOf(130.81, 196.00, 261.63, 329.63, 392.00)
                    }
                    if (beat == 0) {
                        addFeltPianoNote(bufL, bufR, startSample, chord[0] * 0.5, durSec = 4.2, amp = 0.30f, pan = -0.20f)
                        addFeltPianoNote(bufL, bufR, startSample, chord[0], durSec = 3.8, amp = 0.22f, pan = -0.10f)
                    }
                    if (beat == 0 || beat == 2 || beat == 4 || beat == 6) {
                        val idx = (beat / 2 + 1).coerceAtMost(chord.lastIndex)
                        addFeltPianoNote(bufL, bufR, startSample, chord[idx], durSec = 3.2, amp = 0.18f, pan = -0.15f + idx * 0.08f)
                    }
                    val melody: Double? = when {
                        bar == 0 && beat == 1 -> 587.33
                        bar == 0 && beat == 5 -> 698.46
                        bar == 1 && beat == 1 -> 659.25
                        bar == 1 && beat == 5 -> 587.33
                        bar == 2 && beat == 1 -> 698.46
                        bar == 2 && beat == 5 -> 523.25
                        bar == 3 && beat == 1 -> 659.25
                        bar == 3 && beat == 5 -> 587.33
                        else -> null
                    }
                    if (melody != null) {
                        addFeltPianoNote(bufL, bufR, startSample, melody, durSec = 3.4, amp = 0.26f, pan = 0.22f)
                    }
                }

                ReelBgmTrack.TIKTOK_LOFI_BEAT -> {
                    // Modern Jazzy Neo-Soul Lo-Fi (Fmaj7 -> Em7 -> Dm9 -> Cmaj7) + Crisp Snare & Hi-Hat Groove
                    val chord = when (bar) {
                        0 -> doubleArrayOf(174.61, 220.00, 261.63, 329.63) // Fmaj7
                        1 -> doubleArrayOf(164.81, 196.00, 246.94, 293.66) // Em7
                        2 -> doubleArrayOf(146.83, 220.00, 261.63, 329.63) // Dm9
                        else -> doubleArrayOf(130.81, 196.00, 246.94, 329.63) // Cmaj7
                    }
                    if (beat == 0) {
                        for (k in chord.indices) {
                            addRhodesNote(bufL, bufR, startSample, chord[k], durSec = beatSec * 3.0, amp = 0.15f, pan = -0.24f + k * 0.16f)
                        }
                        addCleanSub808(bufL, bufR, startSample, chord[0] * 0.5, chord[0] * 0.5, durSec = beatSec * 1.8, amp = 0.26f)
                    } else if (beat == 5) {
                        for (k in 1..3) {
                            addRhodesNote(bufL, bufR, startSample, chord[k], durSec = beatSec * 1.2, amp = 0.11f, pan = 0.12f)
                        }
                    }
                    // Authentic chill lo-fi drum pattern: kick on 0, 5; soft brush snare on 2, 6; ticks on every eighth
                    if (beat == 0 || beat == 5) {
                        addCleanSub808(bufL, bufR, startSample, startFreq = 112.0, endFreq = 48.0, durSec = 0.22, amp = 0.30f)
                    }
                    if (beat == 2 || beat == 6) {
                        addCrispSnareClap(bufL, bufR, startSample, amp = 0.17f, bright = false)
                    }
                    addSoftHiHat(bufL, bufR, startSample, amp = if (beat % 2 == 0) 0.08f else 0.045f, pan = 0.18f)
                }

                ReelBgmTrack.CINEMATIC_CELLO_DRAMA -> {
                    // Deep Dramatic Solo Cello + Sweeping Orchestral Swells + Timpani Heartbeat (Cm -> Ab -> Fm -> G)
                    val root = when (bar) {
                        0 -> 130.81 // C3
                        1 -> 103.83 // Ab2
                        2 -> 87.31  // F2
                        else -> 98.00 // G2
                    }
                    val fifth = root * 1.5
                    if (beat == 0) {
                        addWarmPadNote(bufL, bufR, startSample, root, durSec = beatSec * 4.2, amp = 0.24f, pan = -0.20f)
                        addWarmPadNote(bufL, bufR, startSample, fifth, durSec = beatSec * 4.2, amp = 0.18f, pan = 0.20f)
                    }
                    // Dramatic double heartbeat timpani pulse (lub-dub)
                    if (beat == 0 || beat == 1 || beat == 4) {
                        addCleanSub808(
                            bufL, bufR, startSample,
                            startFreq = if (beat == 1) 74.0 else 92.0,
                            endFreq = 46.0,
                            durSec = 0.38,
                            amp = if (beat == 0) 0.30f else 0.18f
                        )
                    }
                    // Deep expressive solo Cello melody
                    val cello: Pair<Double, Double>? = when {
                        bar == 0 && beat == 0 -> 196.00 to beatSec * 2.2 // G3
                        bar == 0 && beat == 5 -> 233.08 to beatSec * 1.6 // Bb3
                        bar == 1 && beat == 0 -> 207.65 to beatSec * 2.4 // Ab3
                        bar == 1 && beat == 5 -> 196.00 to beatSec * 1.6 // G3
                        bar == 2 && beat == 0 -> 174.61 to beatSec * 2.2 // F3
                        bar == 2 && beat == 4 -> 207.65 to beatSec * 1.8 // Ab3
                        bar == 3 && beat == 0 -> 196.00 to beatSec * 2.4 // G3
                        bar == 3 && beat == 5 -> 146.83 to beatSec * 1.5 // D3
                        else -> null
                    }
                    if (cello != null) {
                        addRichCelloNote(bufL, bufR, startSample, cello.first, cello.second, amp = 0.32f, pan = 0f)
                    }
                }

                ReelBgmTrack.TIKTOK_ATTITUDE_BEAT -> {
                    // Hard-Hitting 98 BPM Dark Trap Attitude (F Phrygian: F Gb Ab Bb C Db Eb)
                    val bassRoot = when (bar) {
                        0 -> 87.31  // F2
                        1 -> 92.50  // Gb2 (Phrygian tension!)
                        2 -> 69.30  // Db2
                        else -> 65.41 // C2
                    }
                    val darkRiff = when (bar) {
                        0 -> doubleArrayOf(349.23, 369.99, 415.30, 369.99) // F - Gb - Ab - Gb
                        1 -> doubleArrayOf(369.99, 349.23, 311.13, 349.23) // Gb - F - Eb - F
                        2 -> doubleArrayOf(277.18, 349.23, 415.30, 369.99) // Db - F - Ab - Gb
                        else -> doubleArrayOf(261.63, 311.13, 349.23, 523.25) // C - Eb - F - C5
                    }
                    // Heavy trap 808 pattern
                    if (beat == 0 || beat == 3 || beat == 6) {
                        addCleanSub808(
                            bufL, bufR, startSample,
                            startFreq = bassRoot * 1.75,
                            endFreq = bassRoot,
                            durSec = 0.45,
                            amp = if (beat == 0) 0.42f else 0.30f
                        )
                    }
                    // Snappy trap clap on beat 2 and 6
                    if (beat == 2 || beat == 6) {
                        addCrispSnareClap(bufL, bufR, startSample, amp = 0.24f, bright = true)
                    }
                    // Fast trap hi-hat ticks
                    addSoftHiHat(bufL, bufR, startSample, amp = if (beat % 2 == 0) 0.11f else 0.07f, pan = if (beat % 2 == 0) -0.15f else 0.15f)
                    // Exotic plucked Eastern attitude riff
                    if (beat % 2 == 0) {
                        val note = darkRiff[(beat / 2) % darkRiff.size]
                        addSitarPluckNote(bufL, bufR, startSample, note, durSec = 0.85, amp = 0.24f, pan = if (beat % 4 == 0) -0.20f else 0.20f)
                    }
                }

                ReelBgmTrack.OFF -> Unit
            }
        }

        // Fold tail seamlessly onto start of loop for 100% click-free circular looping
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

        val wetAmount = when (track) {
            ReelBgmTrack.TIKTOK_VIRAL_PIANO -> 0.36f
            ReelBgmTrack.CINEMATIC_CELLO_DRAMA -> 0.34f
            ReelBgmTrack.TIKTOK_ROMANTIC_STRINGS -> 0.30f
            ReelBgmTrack.RAINY_NIGHT_SITAR,
            ReelBgmTrack.SUFI_BANSURI_MEHFIL -> 0.28f
            ReelBgmTrack.TIKTOK_ATTITUDE_BEAT -> 0.14f
            else -> 0.22f
        }
        applyCleanStereoHallReverb(loopL, loopR, wet = wetAmount)

        // Per-genre tonal shaping so every track keeps its own authentic acoustic color!
        val lpAlpha = when (track) {
            ReelBgmTrack.TIKTOK_SAD_GUITAR,
            ReelBgmTrack.RAINY_NIGHT_SITAR,
            ReelBgmTrack.TIKTOK_ATTITUDE_BEAT -> 0.72f
            ReelBgmTrack.TIKTOK_ROMANTIC_STRINGS,
            ReelBgmTrack.SUFI_BANSURI_MEHFIL -> 0.62f
            ReelBgmTrack.CINEMATIC_CELLO_DRAMA -> 0.56f
            else -> 0.48f
        }
        applyGentleLowPass(loopL, alpha = lpAlpha)
        applyGentleLowPass(loopR, alpha = lpAlpha)

        return loopL to loopR
    }

    // -------------------------------------------------------------------------
    // Distinct Indian Classical & Modern Studio Instrument Voices
    // -------------------------------------------------------------------------

    private fun addTanpuraDrone(bufL: FloatArray, bufR: FloatArray, loopSamples: Int, rootFreq: Double, amp: Float) {
        val freqs = doubleArrayOf(rootFreq, rootFreq * 1.5, rootFreq * 2.0)
        val gains = floatArrayOf(0.55f, 0.28f, 0.17f)
        for (i in 0 until loopSamples) {
            val frac = i.toDouble() / loopSamples.toDouble()
            val swell = 0.84 + 0.16 * sin(TWO_PI * frac)
            var sL = 0.0
            var sR = 0.0
            for (k in freqs.indices) {
                val cycles = round(freqs[k] * loopSamples / SAMPLE_RATE).coerceAtLeast(1.0)
                val p = TWO_PI * cycles * frac
                val h = sin(p) * 0.76 + sin(2.0 * p) * 0.18 + sin(3.0 * p) * 0.06
                sL += h * gains[k] * (0.85 - k * 0.1)
                sR += h * gains[k] * (0.65 + k * 0.1)
            }
            bufL[i] += (sL * swell * amp).toFloat()
            bufR[i] += (sR * swell * amp).toFloat()
        }
    }

    private fun addSoftRainBed(bufL: FloatArray, bufR: FloatArray, loopSamples: Int, amp: Float) {
        var seed = 0x4B1D9A7L
        fun nextWhite(): Float {
            seed = seed * 6364136223846793005L + 1442695040888963407L
            return ((seed ushr 33).toInt() / 2147483648.0f) - 0.5f
        }
        val rawL = FloatArray(loopSamples)
        val rawR = FloatArray(loopSamples)
        var b0L = 0f; var b1L = 0f
        var b0R = 0f; var b1R = 0f
        for (i in 0 until loopSamples) {
            val wL = nextWhite()
            val wR = nextWhite()
            b0L = 0.992f * b0L + wL * 0.06f
            b1L = 0.940f * b1L + wL * 0.10f
            b0R = 0.992f * b0R + wR * 0.06f
            b1R = 0.940f * b1R + wR * 0.10f
            rawL[i] = (b0L + b1L) * amp
            rawR[i] = (b0R + b1R) * amp
        }
        val xfade = (0.4 * SAMPLE_RATE).toInt().coerceAtMost(loopSamples / 4)
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

    private fun addBansuriNote(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        durSec: Double,
        amp: Float,
        pan: Float
    ) {
        val len = (durSec * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        var phase = 0.0
        for (i in start until end) {
            val n = i - start
            val frac = n.toDouble() / len.coerceAtLeast(1).toDouble()
            val env = sin(PI * frac).let { it * it }
            val t = n.toDouble() / SAMPLE_RATE
            val meend = 1.0 - 0.016 * exp(-t * 8.5)
            val vib = 1.0 + 0.005 * sin(TWO_PI * 4.8 * t) * env
            val instFreq = freq * meend * vib
            phase = (phase + TWO_PI * instFreq / SAMPLE_RATE) % TWO_PI
            val wave = sin(phase) * 0.82 + sin(phase * 2.0) * 0.14 + sin(phase * 3.0) * 0.04
            val s = (wave * env).toFloat()
            bufL[i] += s * gL
            bufR[i] += s * gR
        }
    }

    private fun addSitarPluckNote(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        durSec: Double,
        amp: Float,
        pan: Float
    ) {
        val len = (durSec * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        var phase = 0.0
        for (i in start until end) {
            val n = i - start
            val t = n.toDouble() / SAMPLE_RATE
            val attack = 1.0 - exp(-t * 220.0)
            val decay = exp(-t * 1.45)
            val tailFade = if (n > len - 1600) ((len - n).toDouble() / 1600.0).coerceIn(0.0, 1.0) else 1.0
            // Subtle jawari pitch bend + rich metallic Indian sitar overtones
            val bend = 1.0 + 0.009 * exp(-t * 11.0)
            phase = (phase + TWO_PI * freq * bend / SAMPLE_RATE) % TWO_PI
            val shimmer = 0.85 + 0.15 * sin(TWO_PI * 5.2 * t)
            val wave = (
                sin(phase) * 0.48 +
                    sin(phase * 2.0) * 0.25 * exp(-t * 1.1) +
                    sin(phase * 3.0) * 0.16 * exp(-t * 1.6) +
                    sin(phase * 4.0) * 0.08 * exp(-t * 2.4) +
                    sin(phase * 5.0) * 0.04 * exp(-t * 3.2)
                ) * shimmer
            val s = (wave * attack * decay * tailFade).toFloat()
            bufL[i] += s * gL
            bufR[i] += s * gR
        }
    }

    private fun addTablaBayan(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        startFreq: Double,
        endFreq: Double,
        amp: Float
    ) {
        val len = (0.34 * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        var phase = 0.0
        for (i in start until end) {
            val n = i - start
            val t = n.toDouble() / SAMPLE_RATE
            val curFreq = endFreq + (startFreq - endFreq) * exp(-t * 14.0)
            phase = (phase + TWO_PI * curFreq / SAMPLE_RATE) % TWO_PI
            val attack = 1.0 - exp(-t * 180.0)
            val decay = exp(-t * 9.5)
            val s = ((sin(phase) * 0.82 + sin(phase * 1.98) * 0.18) * attack * decay * amp).toFloat()
            bufL[i] += s * 0.62f
            bufR[i] += s * 0.45f
        }
    }

    private fun addTablaDayan(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        amp: Float
    ) {
        val len = (0.18 * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val w = TWO_PI * freq / SAMPLE_RATE
        for (i in start until end) {
            val n = i - start
            val t = n.toDouble() / SAMPLE_RATE
            val attack = 1.0 - exp(-t * 420.0)
            val decay = exp(-t * 20.0)
            val wave = sin(w * n) * 0.62 + sin(w * 2.02 * n) * 0.28 + sin(w * 3.05 * n) * 0.10
            val s = (wave * attack * decay * amp).toFloat()
            bufL[i] += s * 0.42f
            bufR[i] += s * 0.65f
        }
    }

    private fun addCrispAcousticGuitarNote(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        durSec: Double,
        amp: Float,
        pan: Float
    ) {
        val len = (durSec * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        val w = TWO_PI * freq / SAMPLE_RATE
        for (i in start until end) {
            val n = i - start
            val t = n.toDouble() / SAMPLE_RATE
            val attack = 1.0 - exp(-t * 220.0)
            val decay = exp(-t * 1.55)
            val tailFade = if (n > len - 1400) ((len - n).toDouble() / 1400.0).coerceIn(0.0, 1.0) else 1.0
            val p = w * n
            val wave = sin(p) * 0.58 +
                sin(2.0 * p) * 0.26 * exp(-t * 1.6) +
                sin(3.0 * p) * 0.11 * exp(-t * 2.8) +
                sin(4.0 * p) * 0.05 * exp(-t * 4.2)
            val s = (wave * attack * decay * tailFade).toFloat()
            bufL[i] += s * gL
            bufR[i] += s * gR
        }
    }

    private fun addHarpPluckNote(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        durSec: Double,
        amp: Float,
        pan: Float
    ) {
        val len = (durSec * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        val w = TWO_PI * freq / SAMPLE_RATE
        for (i in start until end) {
            val n = i - start
            val t = n.toDouble() / SAMPLE_RATE
            val attack = 1.0 - exp(-t * 260.0)
            val decay = exp(-t * 1.85)
            val p = w * n
            val wave = sin(p) * 0.78 + sin(2.0 * p) * 0.17 * exp(-t * 2.4) + sin(3.0 * p) * 0.05 * exp(-t * 4.5)
            val s = (wave * attack * decay).toFloat()
            bufL[i] += s * gL
            bufR[i] += s * gR
        }
    }

    private fun addRichCelloNote(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        durSec: Double,
        amp: Float,
        pan: Float
    ) {
        val len = (durSec * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        var phase = 0.0
        for (i in start until end) {
            val n = i - start
            val frac = n.toDouble() / len.coerceAtLeast(1).toDouble()
            val env = sin(PI * frac).let { it * it }
            val t = n.toDouble() / SAMPLE_RATE
            val instFreq = freq * (1.0 + 0.004 * sin(TWO_PI * 4.2 * t) * env)
            phase = (phase + TWO_PI * instFreq / SAMPLE_RATE) % TWO_PI
            // Warm resonant woody cello overtones
            val wave = sin(phase) * 0.56 + sin(phase * 2.0) * 0.28 + sin(phase * 3.0) * 0.12 + sin(phase * 4.0) * 0.04
            val s = (wave * env).toFloat()
            bufL[i] += s * gL
            bufR[i] += s * gR
        }
    }

    private fun addCrispSnareClap(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        amp: Float,
        bright: Boolean
    ) {
        val len = (0.14 * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        var seed = (start.toLong() * 0x5DEECE66DL) xor 0x13579BDFL
        var prev = 0f
        for (i in start until end) {
            val n = i - start
            val t = n.toDouble() / SAMPLE_RATE
            seed = seed * 6364136223846793005L + 1442695040888963407L
            val white = ((seed ushr 33).toInt() / 2147483648.0f) - 0.5f
            val filtered = if (bright) white - prev * 0.55f else prev * 0.5f + white * 0.5f
            prev = white
            val env = exp(-t * (if (bright) 28.0 else 22.0)).toFloat()
            val body = (sin(TWO_PI * 195.0 * t) * exp(-t * 36.0)).toFloat() * 0.45f
            val s = (filtered * env + body) * amp
            bufL[i] += s
            bufR[i] += s
        }
    }

    private fun addSoftHiHat(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        amp: Float,
        pan: Float
    ) {
        val len = (0.045 * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        var seed = (start.toLong() * 0x517CC1B727220A95L) xor 0x2468ACEL
        var prev = 0f
        for (i in start until end) {
            val n = i - start
            val t = n.toDouble() / SAMPLE_RATE
            seed = seed * 6364136223846793005L + 1442695040888963407L
            val white = ((seed ushr 33).toInt() / 2147483648.0f) - 0.5f
            val hp = white - prev
            prev = white
            val env = exp(-t * 75.0).toFloat()
            val s = hp * env
            bufL[i] += s * gL
            bufR[i] += s * gR
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
        val len = (durSec * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        val w1 = TWO_PI * freq / SAMPLE_RATE
        val wDet = TWO_PI * (freq * 1.0014) / SAMPLE_RATE
        for (i in start until end) {
            val n = i - start
            val t = n.toDouble() / SAMPLE_RATE
            val attack = 1.0 - exp(-t * 85.0)
            val decay = exp(-t * 0.88)
            val tailFade = if (n > len - 2200) ((len - n).toDouble() / 2200.0).coerceIn(0.0, 1.0) else 1.0
            val p = w1 * n
            val wave = sin(p) * 0.62 + sin(wDet * n) * 0.22 + sin(2.0 * p) * 0.14 * exp(-t * 1.9) + sin(3.0 * p) * 0.04 * exp(-t * 3.8)
            val s = (wave * attack * decay * tailFade).toFloat()
            bufL[i] += s * gL
            bufR[i] += s * gR
        }
    }

    private fun addRhodesNote(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        durSec: Double,
        amp: Float,
        pan: Float
    ) {
        val len = (durSec * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        val w = TWO_PI * freq / SAMPLE_RATE
        for (i in start until end) {
            val n = i - start
            val t = n.toDouble() / SAMPLE_RATE
            val attack = 1.0 - exp(-t * 110.0)
            val decay = exp(-t * 0.95)
            val tailFade = if (n > len - 1800) ((len - n).toDouble() / 1800.0).coerceIn(0.0, 1.0) else 1.0
            val p = w * n
            val trem = 0.90 + 0.10 * sin(TWO_PI * 2.6 * t)
            val wave = (sin(p) * 0.80 + sin(2.0 * p) * 0.14 * exp(-t * 3.5) + sin(3.0 * p) * 0.04 * exp(-t * 6.0)) * trem
            val s = (wave * attack * decay * tailFade).toFloat()
            bufL[i] += s * gL
            bufR[i] += s * gR
        }
    }

    private fun addSilkyViolinNote(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        freq: Double,
        durSec: Double,
        amp: Float,
        pan: Float
    ) {
        val len = (durSec * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        var phase = 0.0
        for (i in start until end) {
            val n = i - start
            val frac = n.toDouble() / len.coerceAtLeast(1).toDouble()
            val env = sin(PI * frac).let { it * it }
            val t = n.toDouble() / SAMPLE_RATE
            val instFreq = freq * (1.0 + 0.004 * sin(TWO_PI * 5.0 * t) * env)
            phase = (phase + TWO_PI * instFreq / SAMPLE_RATE) % TWO_PI
            val wave = sin(phase) * 0.72 + sin(phase * 2.0) * 0.21 + sin(phase * 3.0) * 0.06
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
        val len = (durSec * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        val gL = amp * (0.5f - pan * 0.4f)
        val gR = amp * (0.5f + pan * 0.4f)
        val wL = TWO_PI * freq / SAMPLE_RATE
        val wR = TWO_PI * (freq * 1.0018) / SAMPLE_RATE
        for (i in start until end) {
            val n = i - start
            val frac = n.toDouble() / len.coerceAtLeast(1).toDouble()
            val env = 0.5 - 0.5 * cos(TWO_PI * frac)
            val sL = (sin(wL * n) * 0.85 + sin(2.0 * wL * n) * 0.15) * env
            val sR = (sin(wR * n) * 0.85 + sin(2.0 * wR * n) * 0.15) * env
            bufL[i] += (sL * gL).toFloat()
            bufR[i] += (sR * gR).toFloat()
        }
    }

    private fun addCleanSub808(
        bufL: FloatArray,
        bufR: FloatArray,
        start: Int,
        startFreq: Double,
        endFreq: Double,
        durSec: Double,
        amp: Float
    ) {
        val len = (durSec * SAMPLE_RATE).toInt()
        val end = (start + len).coerceAtMost(bufL.size)
        var phase = 0.0
        for (i in start until end) {
            val n = i - start
            val t = n.toDouble() / SAMPLE_RATE
            val curFreq = endFreq + (startFreq - endFreq) * exp(-t * 24.0)
            phase = (phase + TWO_PI * curFreq / SAMPLE_RATE) % TWO_PI
            val attack = 1.0 - exp(-t * 130.0)
            val decay = exp(-t * (2.4 / durSec.coerceAtLeast(0.2)))
            val tailFade = if (n > len - 1000) ((len - n).toDouble() / 1000.0).coerceIn(0.0, 1.0) else 1.0
            val s = ((sin(phase) * 0.86 + sin(phase * 2.0) * 0.14) * attack * decay * tailFade * amp).toFloat()
            bufL[i] += s
            bufR[i] += s
        }
    }

    private fun applyCleanStereoHallReverb(bufL: FloatArray, bufR: FloatArray, wet: Float) {
        val len = bufL.size
        val tapsL = intArrayOf((SAMPLE_RATE * 0.11).toInt(), (SAMPLE_RATE * 0.23).toInt(), (SAMPLE_RATE * 0.37).toInt())
        val tapsR = intArrayOf((SAMPLE_RATE * 0.14).toInt(), (SAMPLE_RATE * 0.28).toInt(), (SAMPLE_RATE * 0.43).toInt())
        val gains = floatArrayOf(0.45f, 0.28f, 0.16f)

        val srcL = bufL.copyOf()
        val srcR = bufR.copyOf()
        val dry = 1f - wet * 0.25f

        for (i in 0 until len) {
            var rL = 0f
            var rR = 0f
            for (t in 0 until 3) {
                val idxL = (i - tapsL[t] + len) % len
                val idxR = (i - tapsR[t] + len) % len
                rL += (srcL[idxL] * 0.72f + srcR[idxL] * 0.28f) * gains[t]
                rR += (srcR[idxR] * 0.72f + srcL[idxR] * 0.28f) * gains[t]
            }
            bufL[i] = srcL[i] * dry + rL * wet
            bufR[i] = srcR[i] * dry + rR * wet
        }
    }

    private fun applyGentleLowPass(buf: FloatArray, alpha: Float) {
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
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 4)
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
