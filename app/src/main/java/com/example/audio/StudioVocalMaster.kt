package com.example.audio

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

private const val TAG = "StudioVocalMaster"

/**
 * Natural vocal mastering profiles for Shayari Reels.
 *
 * Tuned to keep speech crisp, organic, and completely free of metallic comb-filter echo
 * or telephone-like muffling while preserving subtle mood differences across personas.
 */
enum class VocalMoodProfile(
    val pauseBetweenLinesMs: Int,
    val reverbWet: Float,
    val warmCutoffHz: Double,
    val warmGain: Float,
    val silkCutoffHz: Double
) {
    HEARTBREAK_SOFT(
        pauseBetweenLinesMs = 680,
        reverbWet = 0.07f,
        warmCutoffHz = 230.0,
        warmGain = 0.16f,
        silkCutoffHz = 7_400.0
    ),
    MELODIOUS_ROMANTIC(
        pauseBetweenLinesMs = 600,
        reverbWet = 0.06f,
        warmCutoffHz = 250.0,
        warmGain = 0.13f,
        silkCutoffHz = 8_200.0
    ),
    MEHFIL_BARITONE(
        pauseBetweenLinesMs = 640,
        reverbWet = 0.06f,
        warmCutoffHz = 195.0,
        warmGain = 0.20f,
        silkCutoffHz = 7_600.0
    ),
    STAGE_BOLD(
        pauseBetweenLinesMs = 500,
        reverbWet = 0.04f,
        warmCutoffHz = 185.0,
        warmGain = 0.17f,
        silkCutoffHz = 8_500.0
    )
}

object StudioVocalMaster {

    private data class DecodedPcm(
        val sampleRate: Int,
        val monoSamples: FloatArray
    )

    /**
     * Ultra-transparent mastering for Gemini's already studio-grade human audio:
     * removes DC offset, verifies the audio is genuinely audible (not silent),
     * and peak-normalizes cleanly with ZERO artificial reverb, ZERO muffling,
     * and ZERO distortion so natural human breath and tone remain 100% pure.
     */
    fun masterGeminiVoiceWav(
        inputWav: File,
        outputWav: File
    ): Boolean {
        return runCatching {
            val decoded = decodePcm16Wav(inputWav) ?: return false
            if (decoded.monoSamples.size < decoded.sampleRate / 12) return false

            // Ultra-gentle edge trim (0.0018f threshold + 65ms breathing pad) so soft human
            // sighs, breath intakes, and whispered line endings are NEVER clipped off.
            val samples = trimSilenceEdges(
                samples = decoded.monoSamples,
                sampleRate = decoded.sampleRate,
                threshold = 0.0018f,
                padSeconds = 0.065
            )
            if (samples.isEmpty()) return false

            // Reject truly silent/corrupt generations so Reels immediately falls back
            var rawPeak = 0f
            for (s in samples) {
                val a = abs(s)
                if (a > rawPeak) rawPeak = a
            }
            if (rawPeak < 0.004f) {
                Log.w(TAG, "Gemini WAV appears silent (peak=$rawPeak); rejecting.")
                return false
            }

            applyHighPass(samples, decoded.sampleRate, cutoffHz = 48.0)

            // Subtle close-mic studio body below 180Hz (keeps 100% of Gemini's natural human breath & vocal timbre pure)
            val warmAlpha = (1.0 - exp(-2.0 * PI * 180.0 / decoded.sampleRate)).toFloat()
            var lowState = 0f
            for (i in samples.indices) {
                val x = samples[i]
                lowState += (x - lowState) * warmAlpha
                samples[i] = x + lowState * 0.10f
            }

            // Smooth 10ms edge fades so playback starts and ends with zero digital clicks
            val fadeN = (0.010 * decoded.sampleRate).toInt().coerceAtMost(samples.size / 6)
            if (fadeN > 0) {
                for (i in 0 until fadeN) {
                    val g = i.toFloat() / fadeN.toFloat()
                    samples[i] *= g
                    samples[samples.lastIndex - i] *= g
                }
            }

            val tmp = File(outputWav.parentFile, "${outputWav.name}.tmp")
            writeNormalizedStereoWav(tmp, samples, samples, decoded.sampleRate, targetPeak = 0.93f)
            if (!tmp.renameTo(outputWav)) {
                outputWav.writeBytes(tmp.readBytes())
                tmp.delete()
            }
            outputWav.exists() && outputWav.length() > 1024L
        }.onFailure {
            Log.e(TAG, "Failed transparent Gemini WAV mastering", it)
        }.getOrDefault(false)
    }

    /**
     * Clean studio mastering for multi-line Android HD Neural Hindi TTS:
     *  1. Proper RIFF chunk decoding + silent-audio rejection
     *  2. Natural Mushaira poetic pauses with micro-fades (zero splice clicks)
     *  3. Subtle low-shelf warmth + transparent >7.4kHz de-sibilance (no muffled telephone tone)
     *  4. Transparent RMS leveling (no harsh tanh waveshaping distortion)
     *  5. Subtle all-pass diffused studio room ambience (zero metallic comb-filter ringing)
     *  6. Exact peak normalization so voice stays crystal clear over TikTok BGM
     */
    fun masterPoetVoiceToStereoWav(
        lineFiles: List<File>,
        outputFile: File,
        pauseBetweenLinesMs: Int = 600,
        reverbWet: Float = 0.06f,
        isFemale: Boolean = false,
        profile: VocalMoodProfile? = null
    ): Boolean {
        val activeProfile = profile ?: if (isFemale) VocalMoodProfile.MELODIOUS_ROMANTIC else VocalMoodProfile.MEHFIL_BARITONE
        val effectivePauseMs = if (profile != null && pauseBetweenLinesMs == 600) profile.pauseBetweenLinesMs else pauseBetweenLinesMs
        val effectiveWet = if (profile != null && reverbWet == 0.06f) profile.reverbWet else reverbWet

        return runCatching {
            val decodedLines = lineFiles.mapNotNull { decodePcm16Wav(it) }
                .filter { it.monoSamples.isNotEmpty() }
            if (decodedLines.isEmpty()) return false

            val sampleRate = decodedLines.first().sampleRate
            val normalizedLines = decodedLines.mapNotNull { line ->
                val trimmed = trimSilenceEdges(line.monoSamples, line.sampleRate)
                var peak = 0f
                for (s in trimmed) {
                    val a = abs(s)
                    if (a > peak) peak = a
                }
                if (peak < 0.004f) {
                    null
                } else if (line.sampleRate == sampleRate) {
                    trimmed
                } else {
                    resampleLinear(trimmed, line.sampleRate, sampleRate)
                }
            }

            if (normalizedLines.isEmpty()) return false

            val pauseSamples = ((effectivePauseMs / 1000.0) * sampleRate).toInt().coerceAtLeast(0)
            val leadInSamples = (0.045 * sampleRate).toInt()
            val tailSamples = (0.35 * sampleRate).toInt()

            var totalSamples = leadInSamples + tailSamples
            normalizedLines.forEachIndexed { idx, arr ->
                totalSamples += arr.size
                if (idx < normalizedLines.lastIndex) totalSamples += pauseSamples
            }
            if (totalSamples <= sampleRate / 12) return false

            val mono = FloatArray(totalSamples)
            var cursor = leadInSamples
            normalizedLines.forEachIndexed { idx, arr ->
                val fadeN = (0.010 * sampleRate).toInt().coerceAtMost(arr.size / 4)
                for (i in arr.indices) {
                    var s = arr[i]
                    if (fadeN > 0) {
                        if (i < fadeN) s *= (i.toFloat() / fadeN)
                        else if (i >= arr.size - fadeN) s *= ((arr.size - 1 - i).toFloat() / fadeN)
                    }
                    mono[cursor + i] = s
                }
                cursor += arr.size
                if (idx < normalizedLines.lastIndex) cursor += pauseSamples
            }

            // Step 1: DC / sub-rumble filter (55 Hz)
            applyHighPass(mono, sampleRate, cutoffHz = 55.0)

            // Step 2: Natural close-mic warmth + gentle upper-hiss silk filter (preserves natural Hindi clarity!)
            applyNaturalVocalEq(mono, sampleRate, profile = activeProfile)

            // Step 3: Transparent RMS leveler (NO harmonic distortion)
            applyTransparentLeveler(mono, sampleRate)

            // Step 4: Very subtle all-pass diffused warm studio room (NO comb-filter metallic echo!)
            val (left, right) = applyDiffusedStudioRoom(mono, sampleRate, wet = effectiveWet)

            // Step 5: Atomic peak-normalized stereo 16-bit WAV output
            val tmp = File(outputFile.parentFile, "${outputFile.name}.tmp")
            writeNormalizedStereoWav(tmp, left, right, sampleRate, targetPeak = 0.91f)
            if (!tmp.renameTo(outputFile)) {
                outputFile.writeBytes(tmp.readBytes())
                tmp.delete()
            }
            outputFile.exists() && outputFile.length() > 1024L
        }.onFailure {
            Log.e(TAG, "Failed mastering vocal WAV", it)
        }.getOrDefault(false)
    }

    private fun decodePcm16Wav(file: File): DecodedPcm? {
        if (!file.exists() || file.length() < 44L) return null
        val bytes = file.readBytes()
        if (bytes.size < 44) return null

        if (bytes[0] != 'R'.code.toByte() || bytes[1] != 'I'.code.toByte() ||
            bytes[2] != 'F'.code.toByte() || bytes[3] != 'F'.code.toByte() ||
            bytes[8] != 'W'.code.toByte() || bytes[9] != 'A'.code.toByte() ||
            bytes[10] != 'V'.code.toByte() || bytes[11] != 'E'.code.toByte()
        ) {
            return null
        }

        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var offset = 12
        var audioFormat = 1
        var channels = 1
        var sampleRate = 24_000
        var bitsPerSample = 16
        var dataOffset = -1
        var dataSize = 0

        while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
            val chunkSize = buf.getInt(offset + 4)
            val payloadStart = offset + 8
            if (chunkSize < 0 || payloadStart > bytes.size) break

            when (chunkId) {
                "fmt " -> {
                    if (payloadStart + 16 <= bytes.size) {
                        audioFormat = buf.getShort(payloadStart).toInt() and 0xFFFF
                        channels = buf.getShort(payloadStart + 2).toInt() and 0xFFFF
                        sampleRate = buf.getInt(payloadStart + 4)
                        bitsPerSample = buf.getShort(payloadStart + 14).toInt() and 0xFFFF
                    }
                }
                "data" -> {
                    dataOffset = payloadStart
                    dataSize = minOf(chunkSize, bytes.size - payloadStart)
                    break
                }
            }
            val paddedSize = chunkSize + (chunkSize and 1)
            offset = payloadStart + paddedSize
        }

        if (dataOffset < 0 || dataSize < 4 || audioFormat != 1 || bitsPerSample != 16 || channels !in 1..2) {
            return null
        }
        val safeRate = sampleRate.coerceIn(8_000, 96_000)
        val bytesPerFrame = channels * 2
        val frameCount = dataSize / bytesPerFrame
        if (frameCount <= 0) return null

        val mono = FloatArray(frameCount)
        var p = dataOffset
        val invScale = 1f / 32768f
        if (channels == 1) {
            for (i in 0 until frameCount) {
                val lo = bytes[p].toInt() and 0xFF
                val hi = bytes[p + 1].toInt()
                mono[i] = ((hi shl 8) or lo).toShort().toFloat() * invScale
                p += 2
            }
        } else {
            for (i in 0 until frameCount) {
                val lLo = bytes[p].toInt() and 0xFF
                val lHi = bytes[p + 1].toInt()
                val rLo = bytes[p + 2].toInt() and 0xFF
                val rHi = bytes[p + 3].toInt()
                val sL = ((lHi shl 8) or lLo).toShort().toFloat()
                val sR = ((rHi shl 8) or rLo).toShort().toFloat()
                mono[i] = ((sL + sR) * 0.5f) * invScale
                p += 4
            }
        }
        return DecodedPcm(safeRate, mono)
    }

    private fun trimSilenceEdges(
        samples: FloatArray,
        sampleRate: Int,
        threshold: Float = 0.006f,
        padSeconds: Double = 0.030
    ): FloatArray {
        var start = 0
        while (start < samples.size && abs(samples[start]) < threshold) start++
        var end = samples.size - 1
        while (end > start && abs(samples[end]) < threshold) end--
        if (end <= start) return samples
        val pad = (padSeconds * sampleRate).toInt()
        val safeStart = (start - pad).coerceAtLeast(0)
        val safeEnd = (end + pad).coerceAtMost(samples.size - 1)
        return samples.copyOfRange(safeStart, safeEnd + 1)
    }

    private fun resampleLinear(input: FloatArray, srcRate: Int, dstRate: Int): FloatArray {
        if (srcRate == dstRate || input.isEmpty()) return input
        val outLen = ((input.size.toLong() * dstRate) / srcRate).toInt().coerceAtLeast(1)
        val out = FloatArray(outLen)
        val ratio = srcRate.toDouble() / dstRate.toDouble()
        for (i in 0 until outLen) {
            val pos = i * ratio
            val idx = pos.toInt().coerceIn(0, input.lastIndex)
            val next = (idx + 1).coerceAtMost(input.lastIndex)
            val frac = (pos - idx).toFloat()
            out[i] = input[idx] + (input[next] - input[idx]) * frac
        }
        return out
    }

    private fun applyHighPass(buf: FloatArray, sampleRate: Int, cutoffHz: Double) {
        val rc = 1.0 / (2.0 * PI * cutoffHz)
        val dt = 1.0 / sampleRate
        val alpha = (rc / (rc + dt)).toFloat()
        var prevIn = 0f
        var prevOut = 0f
        for (i in buf.indices) {
            val x = buf[i]
            val y = alpha * (prevOut + x - prevIn)
            buf[i] = y
            prevIn = x
            prevOut = y
        }
    }

    private fun applyNaturalVocalEq(buf: FloatArray, sampleRate: Int, profile: VocalMoodProfile) {
        val warmAlpha = (1.0 - exp(-2.0 * PI * profile.warmCutoffHz / sampleRate)).toFloat()
        val warmGain = profile.warmGain
        // Keep silk cutoff well below Nyquist (only tames >7.4kHz harsh digital edge)
        val safeSilkHz = profile.silkCutoffHz.coerceAtMost(sampleRate * 0.44)
        val silkAlpha = (1.0 - exp(-2.0 * PI * safeSilkHz / sampleRate)).toFloat()

        var lowState = 0f
        var silkState = 0f
        for (i in buf.indices) {
            val x = buf[i]
            lowState += (x - lowState) * warmAlpha
            val warmed = x + lowState * warmGain
            silkState += (warmed - silkState) * silkAlpha
            buf[i] = silkState
        }
    }

    private fun applyTransparentLeveler(buf: FloatArray, sampleRate: Int) {
        val attackCoeff = exp(-1.0 / (0.018 * sampleRate)).toFloat()
        val releaseCoeff = exp(-1.0 / (0.180 * sampleRate)).toFloat()
        var env = 0.06f
        val threshold = 0.28f

        for (i in buf.indices) {
            val x = buf[i]
            val level = abs(x)
            env = if (level > env) {
                attackCoeff * env + (1f - attackCoeff) * level
            } else {
                releaseCoeff * env + (1f - releaseCoeff) * level
            }
            val gain = if (env > threshold) {
                (threshold + (env - threshold) * 0.62f) / env.coerceAtLeast(0.0001f)
            } else {
                1.0f
            }
            buf[i] = x * gain
        }
    }

    /**
     * Two-stage Schroeder all-pass diffuser with warm 2.6 kHz low-pass damping.
     * Adds imperceptible natural studio room depth with ZERO metallic comb-filter echo!
     */
    private fun applyDiffusedStudioRoom(
        mono: FloatArray,
        sampleRate: Int,
        wet: Float
    ): Pair<FloatArray, FloatArray> {
        val len = mono.size
        if (wet <= 0.001f) return mono.copyOf() to mono.copyOf()

        // Warm low-pass send so reverb carries only velvety room warmth, never metallic sibilance
        val sendAlpha = (1.0 - exp(-2.0 * PI * 2_600.0 / sampleRate)).toFloat()
        val warmSend = FloatArray(len)
        var lp = 0f
        for (i in 0 until len) {
            lp += (mono[i] - lp) * sendAlpha
            warmSend[i] = lp
        }

        val diffL = runAllPass(
            runAllPass(warmSend, (sampleRate * 0.017).toInt().coerceAtLeast(16), 0.45f),
            (sampleRate * 0.031).toInt().coerceAtLeast(24),
            0.38f
        )
        val diffR = runAllPass(
            runAllPass(warmSend, (sampleRate * 0.021).toInt().coerceAtLeast(16), 0.45f),
            (sampleRate * 0.037).toInt().coerceAtLeast(24),
            0.38f
        )

        val outL = FloatArray(len)
        val outR = FloatArray(len)
        val dry = 1f - wet * 0.15f
        for (i in 0 until len) {
            outL[i] = mono[i] * dry + diffL[i] * wet
            outR[i] = mono[i] * dry + diffR[i] * wet
        }
        return outL to outR
    }

    private fun runAllPass(input: FloatArray, delaySamples: Int, gain: Float): FloatArray {
        val out = FloatArray(input.size)
        val delayBuf = FloatArray(delaySamples)
        var idx = 0
        for (i in input.indices) {
            val delayed = delayBuf[idx]
            val x = input[i]
            val y = -gain * x + delayed
            delayBuf[idx] = x + gain * y
            out[i] = y
            idx++
            if (idx >= delaySamples) idx = 0
        }
        return out
    }

    private fun writeNormalizedStereoWav(
        outFile: File,
        left: FloatArray,
        right: FloatArray,
        sampleRate: Int,
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
            putInt(sampleRate)
            putInt(sampleRate * 4)
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
