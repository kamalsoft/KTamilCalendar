package com.kamalsoft.tamilcalendar.tv.domain.model

import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.math.roundToLong

enum class Direction(val ta: String, val en: String) {
    EAST("கிழக்கு", "East"),
    SOUTH("தெற்கு", "South"),
    WEST("மேற்கு", "West"),
    NORTH("வடக்கு", "North"),
}

data class Soolam(val direction: Direction, val remedyTa: String, val remedyEn: String)

/**
 * Day rules for the day view, from the Tamil almanac sources listed in docs/BACKLOG.md.
 * Tables are copied from those sources; the Tamil spellings of the karanams are the common almanac forms.
 */
object DayRules {

    /** Vaara Soolam: the direction to avoid for the day, by weekday. */
    fun soolam(day: DayOfWeek): Soolam = when (day) {
        DayOfWeek.SUNDAY -> Soolam(Direction.WEST, "நெய் (அ) வெற்றிலை", "Ghee or betel leaf")
        DayOfWeek.MONDAY -> Soolam(Direction.EAST, "பால் (அ) தயிர்", "Milk or curd")
        DayOfWeek.TUESDAY -> Soolam(Direction.NORTH, "வெல்லம் (அ) பால்", "Jaggery or milk")
        DayOfWeek.WEDNESDAY -> Soolam(Direction.NORTH, "பால் (அ) கொத்தமல்லி / எள்", "Milk, coriander or sesame")
        DayOfWeek.THURSDAY -> Soolam(Direction.SOUTH, "தயிர் (அ) சீரகம்", "Curd or cumin")
        DayOfWeek.FRIDAY -> Soolam(Direction.WEST, "பார்லி (அ) நெய் / வெல்லம்", "Barley, ghee or jaggery")
        DayOfWeek.SATURDAY -> Soolam(Direction.EAST, "உளுந்து (அ) எள் / இஞ்சி", "Urad dal, sesame or ginger")
    }

    /** Weekday remedy (parigaram) for a day that is less favourable. */
    fun weekdayRemedy(day: DayOfWeek): String = when (day) {
        DayOfWeek.SUNDAY -> "Offer water to the Sun; donate wheat or jaggery"
        DayOfWeek.MONDAY -> "Offer raw rice or milk; wear white"
        DayOfWeek.TUESDAY -> "Light a ghee lamp for Murugan or Durga; jaggery or red lentils"
        DayOfWeek.WEDNESDAY -> "Donate green gram; feed cows and birds"
        DayOfWeek.THURSDAY -> "Offer yellow flowers or chickpeas; sandalwood or turmeric tilak"
        DayOfWeek.FRIDAY -> "Light a ghee lamp at home; offer white flowers, curd or sugar"
        DayOfWeek.SATURDAY -> "Light a sesame oil lamp; donate black sesame or urad dal"
    }

    /** Abhijit Muhurtham (validator): solar noon ± daytime/30 on each side, so about 48 minutes in Chennai. */
    fun abhijit(sunrise: LocalTime, sunset: LocalTime): Pair<LocalTime, LocalTime> {
        val daytime = sunset.toSecondOfDay() - sunrise.toSecondOfDay()
        val noon = sunrise.toSecondOfDay() + daytime / 2
        val half = daytime / 30
        return LocalTime.ofSecondOfDay((noon - half).toLong()) to LocalTime.ofSecondOfDay((noon + half).toLong())
    }

    private val MOVABLE_KARANAS = listOf(
        "பவம்" to "Bava", "பாலவம்" to "Balava", "கௌலவம்" to "Kaulava", "தைதுலை" to "Taitila",
        "கரசை" to "Gara", "வணிசை" to "Vanija", "பத்திரை (விஷ்டி)" to "Vishti",
    )
    private val FIXED_KARANAS = listOf("சகுனி" to "Shakuni", "சதுஷ்பாதம்" to "Chatushpada", "நாகவம்" to "Naga")

    /** Karanam (half a tithi), index 0..59. Index 0 is Kimstughna; 1..56 repeat the seven movable karanams. */
    fun karanaName(index: Int): Pair<String, String> = when {
        index == 0 -> "கிம்ஸ்துக்னம்" to "Kimstughna"
        index <= 56 -> MOVABLE_KARANAS[(index - 1) % 7]
        else -> FIXED_KARANAS[index - 57]
    }

    /** Vishti (Bhadra) karanam: avoided for auspicious starts. */
    fun isVishti(index: Int): Boolean = index in 1..56 && (index - 1) % 7 == 6

    /** Kari Naal: solar days in each Tamil month (0 = Chithirai) when no Subakariyam is held. */
    private val KARI_NAAL = mapOf(
        0 to listOf(6, 15), 1 to listOf(7, 16, 17), 2 to listOf(1, 6), 3 to listOf(2, 10, 20),
        4 to listOf(2, 9, 28), 5 to listOf(16, 29), 6 to listOf(6, 20), 7 to listOf(1, 10, 17),
        8 to listOf(6, 9, 11), 9 to listOf(1, 2, 3, 11, 17), 10 to listOf(15, 16, 17), 11 to listOf(6, 15, 19),
    )

    fun isKariNaal(monthIndex: Int, day: Int): Boolean = KARI_NAAL[monthIndex]?.contains(day) == true

    /** Thara Balam (validator section F): count inclusively from the birth star to the day's star; mod 9, with 0 read as 9. */
    fun thara(dayStar: Int, janmaStar: Int): Int {
        val t = (Math.floorMod(dayStar - janmaStar, 27) + 1) % 9
        return if (t == 0) 9 else t
    }

    /** Passing Tharas: Sampath 2, Kshema 4, Sadhaka 6, Mitra 8, Parama Mitra 9. */
    fun tharaPasses(t: Int): Boolean = t in setOf(2, 4, 6, 8, 9)

    /**
     * Offset from the Moon's Rasi to the Chandrashtamam Rasi, as used by the engine. The validator's inclusive count is 7;
     * this stays 5 until the open question in docs/BACKLOG.md is answered.
     */
    const val CHANDRASHTAMA_OFFSET = 5

    /** True when the Moon's Rasi is the person's Chandrashtamam Rasi. */
    fun personalChandrashtamam(moonRasi: Int, janmaRasi: Int): Boolean = moonRasi == (janmaRasi + CHANDRASHTAMA_OFFSET) % 12

    /** Planetary lords of the Chaldean sequence, for the Hora. Benefic: Jupiter, Venus, Mercury, Moon. */
    enum class Planet(val ta: String, val en: String, val benefic: Boolean) {
        SUN("சூரியன்", "Sun", false), VENUS("சுக்கிரன்", "Venus", true), MERCURY("புதன்", "Mercury", true),
        MOON("சந்திரன்", "Moon", true), SATURN("சனி", "Saturn", false), JUPITER("குரு", "Jupiter", true), MARS("செவ்வாய்", "Mars", false),
    }

    data class Hora(val lord: Planet, val start: LocalTime, val end: LocalTime, val daytime: Boolean)

    /**
     * The 24 horas of a Tamil day (validator Step 3B.6): 12 equal parts of the daytime from sunrise, then 12 equal parts
     * of the night from sunset to the next sunrise. The first hora of a weekday belongs to that weekday's lord, then the
     * sequence Sun, Venus, Mercury, Moon, Saturn, Jupiter, Mars repeats.
     */
    fun hora(day: DayOfWeek, sunrise: LocalTime, sunset: LocalTime, nextSunrise: LocalTime): List<Hora> {
        val seq = listOf(Planet.SUN, Planet.VENUS, Planet.MERCURY, Planet.MOON, Planet.SATURN, Planet.JUPITER, Planet.MARS)
        val lord = when (day) {
            DayOfWeek.SUNDAY -> Planet.SUN
            DayOfWeek.MONDAY -> Planet.MOON
            DayOfWeek.TUESDAY -> Planet.MARS
            DayOfWeek.WEDNESDAY -> Planet.MERCURY
            DayOfWeek.THURSDAY -> Planet.JUPITER
            DayOfWeek.FRIDAY -> Planet.VENUS
            DayOfWeek.SATURDAY -> Planet.SATURN
        }
        val first = seq.indexOf(lord)
        val rise = sunrise.toSecondOfDay().toDouble()
        val set = sunset.toSecondOfDay().toDouble()
        val nextRise = nextSunrise.toSecondOfDay().let { if (it <= set) it + 86400.0 else it.toDouble() }
        val dayPart = (set - rise) / 12
        val nightPart = (nextRise - set) / 12
        fun at(sec: Double) = LocalTime.ofSecondOfDay(sec.roundToLong() % 86400)
        return (0 until 24).map { k ->
            val daytime = k < 12
            val (from, to) = if (daytime) rise + k * dayPart to rise + (k + 1) * dayPart
                else set + (k - 12) * nightPart to set + (k - 11) * nightPart
            Hora(seq[(first + k) % 7], at(from), at(to), daytime)
        }
    }
}
