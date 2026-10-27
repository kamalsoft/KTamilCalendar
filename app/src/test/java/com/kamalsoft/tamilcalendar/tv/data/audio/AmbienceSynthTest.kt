package com.kamalsoft.tamilcalendar.tv.data.audio

import com.kamalsoft.tamilcalendar.tv.domain.model.BellStyle
import com.kamalsoft.tamilcalendar.tv.domain.model.MusicMix
import com.kamalsoft.tamilcalendar.tv.domain.model.MusicPitch
import com.kamalsoft.tamilcalendar.tv.domain.model.MusicStyle
import com.kamalsoft.tamilcalendar.tv.domain.model.MusicTempo
import com.kamalsoft.tamilcalendar.tv.domain.model.Raga
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/** Loudness sanity check; with -DwavPath=… also writes 30 s of the ambience as a WAV to listen to. */
class AmbienceSynthTest {
    private val rate = 22050

    private fun render(seconds: Int): FloatArray {
        val synth = AmbienceSynth(rate)
        val out = FloatArray(rate * seconds)
        synth.render(out)
        return out
    }

    @Test fun loudEnoughButNotClipping() {
        val x = render(30).map { DevotionalAmbience.master(it, DevotionalAmbience.LOUD) }
        val peak = x.maxOf { abs(it) }
        val rms = sqrt(x.sumOf { (it * it).toDouble() } / x.size)
        println("AMBIENCE peak=%.2f (%.1f dBFS) rms=%.3f (%.1f dBFS)".format(peak, 20 * log10(peak), rms, 20 * log10(rms)))
        assertTrue("peak $peak must not clip", peak < 0.97f)
        assertTrue("rms $rms too quiet", rms > 0.08)
    }

    @Test fun bassStaysGentle() {
        val x = render(30)
        var lp = 0f
        val a = (2 * Math.PI * 150 / rate / (1 + 2 * Math.PI * 150 / rate)).toFloat()
        var low = 0.0
        for (v in x) { lp += a * (v - lp); low += (lp * lp).toDouble() }
        val share = sqrt(low / x.size) / sqrt(x.sumOf { (it * it).toDouble() } / x.size)
        assertTrue("energy below 150 Hz is ${"%.2f".format(share)} of total", share < 0.4)
    }

    @Test fun bellIsAudible() {
        // Goertzel energy at the bell's pitch must jump at the first strike (1.5 s) versus just before.
        val x = render(4)
        fun tone(from: Int, to: Int, f: Double): Double {
            val w = 2 * Math.PI * f / rate
            var s1 = 0.0; var s2 = 0.0
            for (i in from until to) { val s0 = x[i] + 2 * Math.cos(w) * s1 - s2; s2 = s1; s1 = s0 }
            return sqrt(s1 * s1 + s2 * s2 - 2 * Math.cos(w) * s1 * s2) / (to - from)
        }
        val before = tone(rate / 2, rate, 1800.0)
        val during = tone((rate * 1.55).toInt(), (rate * 2.05).toInt(), 1800.0)
        assertTrue("bell tone before=$before during=$during", during > before * 5)
    }

    @Test fun everyStyleStaysInRange() {
        val styles = listOf(
            MusicStyle(mix = MusicMix.FLUTE, bell = BellStyle.PROMINENT, pitch = MusicPitch.G, tempo = MusicTempo.MEDIUM),
            MusicStyle(mix = MusicMix.TANPURA, bell = BellStyle.OFF, pitch = MusicPitch.C),
        ) + Raga.entries.map { MusicStyle(raga = it) }
        styles.forEach { st ->
            val out = FloatArray(rate * 20)
            AmbienceSynth(rate, st).render(out)
            val y = out.map { DevotionalAmbience.master(it, DevotionalAmbience.LOUD) }
            val peak = y.maxOf { abs(it) }
            val rms = sqrt(y.sumOf { (it * it).toDouble() } / y.size)
            assertTrue("$st peak $peak", peak < 0.97f)
            assertTrue("$st rms $rms too quiet", rms > 0.06)
        }
    }

    /** -DragaWav=… writes a 60 s audition: Mohanam, Hindolam, Revathi (20 s each, slow, normal bell). */
    @Test fun writeRagaAudition() {
        val path = System.getProperty("ragaWav") ?: return
        val all = Raga.entries.flatMap { r ->
            val out = FloatArray(rate * 20)
            AmbienceSynth(rate, MusicStyle(raga = r)).render(out)
            out.mapIndexed { i, v -> DevotionalAmbience.master(v, DevotionalAmbience.LOUD) * minOf(1f, i / (rate * 1f), (out.size - i) / (rate * 1.5f)) } + List(rate) { 0f }
        }
        writeWavFile(path, all.toFloatArray())
    }

    private fun writeWavFile(path: String, y: FloatArray) {
        val pcm = ShortArray(y.size) { (y[it] * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort() }
        DataOutputStream(FileOutputStream(File(path))).use { o ->
            fun i32(v: Int) = o.write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))
            fun i16(v: Int) = o.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
            o.writeBytes("RIFF"); i32(36 + pcm.size * 2); o.writeBytes("WAVEfmt "); i32(16); i16(1); i16(1); i32(rate); i32(rate * 2); i16(2); i16(16)
            o.writeBytes("data"); i32(pcm.size * 2)
            pcm.forEach { i16(it.toInt()) }
        }
    }

    @Test fun writeWav() {
        val path = System.getProperty("wavPath") ?: return
        val x = render(30)
        val pcm = ShortArray(x.size) { i ->
            val fade = minOf(1f, i / (rate * 2f), (x.size - i) / (rate * 1.5f))
            (DevotionalAmbience.master(x[i], DevotionalAmbience.LOUD) * fade * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        DataOutputStream(FileOutputStream(File(path))).use { o ->
            fun i32(v: Int) = o.write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))
            fun i16(v: Int) = o.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
            o.writeBytes("RIFF"); i32(36 + pcm.size * 2); o.writeBytes("WAVEfmt "); i32(16); i16(1); i16(1); i32(rate); i32(rate * 2); i16(2); i16(16)
            o.writeBytes("data"); i32(pcm.size * 2)
            pcm.forEach { i16(it.toInt()) }
        }
    }
}
