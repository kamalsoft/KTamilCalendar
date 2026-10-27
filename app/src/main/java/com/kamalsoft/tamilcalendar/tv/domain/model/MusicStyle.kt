package com.kamalsoft.tamilcalendar.tv.domain.model

/** Swara ratios (just intonation) of the five notes of each pentatonic raga, upper Sa added by the player. */
enum class Raga(val ta: String, val en: String, val scale: DoubleArray) {
    MOHANAM("மோகனம்", "Mohanam", doubleArrayOf(1.0, 9.0 / 8, 5.0 / 4, 3.0 / 2, 5.0 / 3)),        // S R2 G3 P D2
    HINDOLAM("ஹிந்தோளம்", "Hindolam", doubleArrayOf(1.0, 6.0 / 5, 4.0 / 3, 8.0 / 5, 9.0 / 5)),     // S G2 M1 D1 N2
    REVATHI("ரேவதி", "Revathi", doubleArrayOf(1.0, 16.0 / 15, 4.0 / 3, 3.0 / 2, 9.0 / 5)),         // S R1 M1 P N2
}

enum class MusicTempo(val ta: String, val en: String, val beatSeconds: Double) {
    SLOW("மெதுவாக", "Slow", 1.2),
    MEDIUM("நடுத்தரம்", "Medium", 0.8),
}

enum class BellStyle(val ta: String, val en: String, val everySeconds: Int, val level: Float) {
    OFF("இல்லை", "Off", 0, 0f),
    SOFT("மென்மை", "Soft · every 12 s", 12, 0.5f),
    NORMAL("இயல்பு", "Normal · every 7 s", 7, 1f),
    PROMINENT("அதிகம்", "Prominent · every 5 s", 5, 1.15f),
}

/** Tonic (Sa) of the tanpura; the flute plays an octave above. */
enum class MusicPitch(val label: String, val hz: Double) {
    C("C", 130.81), D("D", 146.83), E("E", 164.81), F("F", 174.61), G("G", 196.00),
}

enum class MusicMix(val ta: String, val en: String, val tanpura: Float, val flute: Float) {
    TANPURA("தம்புரா அதிகம்", "More tanpura", 1.3f, 0.5f),
    BALANCED("சமநிலை", "Balanced", 1f, 1f),
    FLUTE("புல்லாங்குழல் அதிகம்", "More flute", 0.75f, 1.25f),
}

data class MusicStyle(
    val raga: Raga = Raga.MOHANAM,
    val tempo: MusicTempo = MusicTempo.SLOW,
    val bell: BellStyle = BellStyle.NORMAL,
    val pitch: MusicPitch = MusicPitch.F,
    val mix: MusicMix = MusicMix.BALANCED,
)
