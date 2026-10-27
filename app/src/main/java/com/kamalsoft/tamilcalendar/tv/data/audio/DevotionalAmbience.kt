package com.kamalsoft.tamilcalendar.tv.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.kamalsoft.tamilcalendar.tv.domain.model.BellStyle
import com.kamalsoft.tamilcalendar.tv.domain.model.MusicStyle
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Devotional ambience synthesiser (pure Kotlin, no Android APIs, so it can be rendered in unit tests):
 * - tanpura: Pa · Sa' · Sa' · Sa plucked every 1.1 s, bright (jawari) overtones, little fundamental;
 * - flute: a slow raga Mohanam (S R G P D) phrase with vibrato and breath, an octave above the drone;
 * - temple bell: brass-like inharmonic partials with a slight beat, every ~7 s;
 * - a light temple-hall reverb and a low-cut so the bass stays gentle on TV speakers.
 * [render] fills a buffer with the next samples.
 */
class AmbienceSynth(private val rate: Int = 22050, private val style: MusicStyle = MusicStyle()) {
    private val sa = style.pitch.hz
    private val table = FloatArray(TABLE) { sin(2 * PI * it / TABLE).toFloat() }
    private fun sine(phase: Double): Float = table[((phase - Math.floor(phase)) * TABLE).toInt() and (TABLE - 1)]

    private class Voice(val freqs: DoubleArray, val amps: FloatArray, val decay: Double, var t: Int = 0) {
        val phases = DoubleArray(freqs.size)
    }

    private val voices = mutableListOf<Voice>()
    private val pluckPattern = doubleArrayOf(1.5, 2.0, 2.0, 1.0) // Pa, Sa', Sa', Sa
    private val pluckEvery = (rate * 1.1).toInt()
    private val bellEvery = rate * maxOf(1, style.bell.everySeconds)
    private var n = 0L

    // Flute phrase as (scale degree, beats): 0–4 = the raga's five swaras, 5 = upper Sa, -1 = rest.
    // The same contour in every raga, so changing the raga changes the mood, not the shape.
    private val contour = listOf(
        0 to 3, 1 to 1, 2 to 2, 3 to 4, -1 to 2,
        4 to 2, 3 to 1, 2 to 1, 3 to 4, -1 to 2,
        5 to 3, 4 to 1, 3 to 2, 2 to 2, 1 to 1, 0 to 5, -1 to 4,
    )
    private val scale = style.raga.scale + 2.0
    private val phrase = contour.map { (deg, beats) -> (if (deg < 0) 0.0 else scale[deg]) to beats }
    private val beat = (rate * style.tempo.beatSeconds).toInt()
    private val noteStarts = phrase.runningFold(0) { acc, p -> acc + p.second * beat }
    private val phraseLen = noteStarts.last()
    private var flutePhase = 0.0
    private var fluteFreq = sa * 2
    private var fluteAmp = 0f
    private var noise = 12345

    // Reverb: three feedback combs (prime-ish delays ≈ 37–53 ms) + one-pole low-cut.
    private val combs = intArrayOf((rate * 0.0371).toInt(), (rate * 0.0437).toInt(), (rate * 0.0529).toInt()).map { FloatArray(it) }
    private val combIdx = IntArray(3)
    private var hpPrevIn = 0f
    private var hpPrevOut = 0f
    private val hpA = (1.0 / (1.0 + 2 * PI * 140.0 / rate)).toFloat() // ~140 Hz high-pass

    fun render(out: FloatArray) {
        for (i in out.indices) {
            if (n % pluckEvery == 0L) {
                val f = sa * pluckPattern[((n / pluckEvery) % 4).toInt()]
                voices += Voice(
                    DoubleArray(9) { h -> f * (h + 1) * (1 + 0.0012 * h) },
                    // Weak fundamental, rich 3rd–7th overtones: the tanpura's buzzing shimmer, not bass.
                    FloatArray(9) { h -> style.mix.tanpura * when (h) { 0 -> 0.10f; 1 -> 0.16f; in 2..6 -> 0.22f / (1 + 0.25f * h); else -> 0.05f } },
                    decay = 2.8,
                )
            }
            if (style.bell != BellStyle.OFF && n % bellEvery == (rate * 1.5).toLong()) {
                // 1.8 kHz: clear of the tanpura's overtones (Sa' × 4 = 1.4 kHz would mask it).
                // Partials stay below Nyquist (11 kHz) to avoid aliasing.
                val f = 1800.0
                voices += Voice(
                    doubleArrayOf(f, f + 1.8, f * 2.0, f * 2.76, f * 2.76 + 2.4, f * 5.40),
                    floatArrayOf(0.85f, 0.65f, 0.40f, 0.45f, 0.32f, 0.15f).map { it * style.bell.level }.toFloatArray(),
                    decay = 4.0,
                )
            }
            var s = 0f
            val it = voices.iterator()
            while (it.hasNext()) {
                val v = it.next()
                val env = exp(-v.t / (rate * v.decay)).toFloat() * minOf(1f, v.t / ATTACK.toFloat())
                // Only after the attack: at t = 0 the envelope is 0 and the note would be dropped unheard.
                if (v.t > ATTACK && env < 0.002f) { it.remove(); continue }
                for (k in v.freqs.indices) {
                    v.phases[k] += v.freqs[k] / rate
                    s += sine(v.phases[k]) * v.amps[k] * env
                }
                v.t++
            }

            // Flute.
            val pos = (n % phraseLen).toInt()
            val idx = noteStarts.indexOfLast { it <= pos }.coerceAtMost(phrase.size - 1)
            val (ratio, beats) = phrase[idx]
            val intoNote = pos - noteStarts[idx]
            val noteLen = beats * beat
            val target = if (ratio > 0) sa * 2 * ratio else fluteFreq
            fluteFreq += (target - fluteFreq) * 0.0025 // ~20 ms glide (gamaka-like slide)
            val wantAmp = if (ratio > 0) minOf(1f, intoNote / (rate * 0.35f), (noteLen - intoNote) / (rate * 0.45f)).coerceAtLeast(0f) else 0f
            fluteAmp += (wantAmp - fluteAmp) * 0.002f
            val vib = 1 + 0.006 * sine(n * 5.2 / rate) * minOf(1f, intoNote / (rate * 0.6f))
            flutePhase += fluteFreq * vib / rate
            noise = noise * 1103515245 + 12345
            val breath = ((noise shr 16) and 0x7fff) / 32768f - 0.5f
            s += (sine(flutePhase) + 0.22f * sine(flutePhase * 2) + 0.06f * sine(flutePhase * 3) + 0.05f * breath) * 0.32f * fluteAmp * style.mix.flute

            // Low-cut, then reverb.
            val hp = hpA * (hpPrevOut + s - hpPrevIn)
            hpPrevIn = s
            hpPrevOut = hp
            var wet = 0f
            for (c in combs.indices) {
                val buf = combs[c]
                val j = combIdx[c]
                val y = buf[j]
                buf[j] = hp + y * 0.72f
                combIdx[c] = (j + 1) % buf.size
                wet += y
            }
            out[i] = hp * 0.75f + wet * 0.12f
            n++
        }
    }

    private companion object {
        const val TABLE = 4096
        /** Note attack, samples (~2 ms): avoids clicks. */
        const val ATTACK = 40
    }
}

/**
 * Plays [AmbienceSynth] on an AudioTrack with fade in/out. [loud] = music on its own (no voice);
 * otherwise it is mixed lower so a voice can sit on top.
 */
class DevotionalAmbience {
    private val rate = 22050
    @Volatile private var running = false
    @Volatile private var fadingOut = false
    @Volatile var loud = true
    @Volatile var style = MusicStyle()

    fun start() {
        if (running) { fadingOut = false; return }
        running = true
        fadingOut = false
        Thread({ runCatching { play() }.onFailure { Log.w("Ambience", "Playback failed", it) } }, "sloka-ambience").start()
    }

    /** Fades out over ~1.5 s and releases the audio track. */
    fun stop() {
        if (running) fadingOut = true
    }

    private fun play() {
        val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
            AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
            // Bytes, and a whole number of 16-bit frames (an odd size is rejected): ~0.5 s of audio.
            maxOf(minBuf, rate) and 1.inv(), AudioTrack.MODE_STREAM, android.media.AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
        track.play()
        val synth = AmbienceSynth(rate, style)
        val buf = FloatArray(1024)
        val pcm = ShortArray(buf.size)
        var gain = 0f
        while (running) {
            synth.render(buf)
            val volume = if (loud) LOUD else UNDER_VOICE
            for (i in buf.indices) {
                gain = if (fadingOut) (gain - 1f / (rate * 1.5f)).coerceAtLeast(0f) else (gain + 1f / (rate * 2f)).coerceAtMost(1f)
                pcm[i] = (master(buf[i], volume) * gain * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
            }
            track.write(pcm, 0, pcm.size)
            if (fadingOut && gain <= 0f) running = false
        }
        track.stop()
        track.release()
    }

    companion object {
        /** Music alone (levels checked in AmbienceSynthTest). */
        const val LOUD = 0.6f
        /** Under a voice. */
        const val UNDER_VOICE = 0.25f

        /** Gain + soft limiter: pluck/bell transients round off smoothly instead of clipping. */
        fun master(x: Float, volume: Float): Float = kotlin.math.tanh(x * volume)
    }
}
