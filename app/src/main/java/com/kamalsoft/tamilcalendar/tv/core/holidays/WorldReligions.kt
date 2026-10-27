package com.kamalsoft.tamilcalendar.tv.core.holidays

import com.kamalsoft.tamilcalendar.tv.core.astro.Astro
import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.EventSource
import com.kamalsoft.tamilcalendar.tv.domain.model.Religion
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

/**
 * Christian, Islamic and Jewish observances for any year. (Hindu, Jain, Sikh and Buddhist dates follow
 * the panchangam and are produced by the TamilCalendarEngine.)
 */
object WorldReligions {

    fun between(from: LocalDate, to: LocalDate): List<CalendarEvent> =
        (christian(from, to) + jewish(from, to) + islamic(from, to)).filter { it.date in from..to }.sortedBy { it.date }

    private fun event(date: LocalDate, ta: String, en: String, religion: Religion, detail: String? = null) = CalendarEvent(
        date = date, kind = EventKind.RELIGIOUS, titleTa = ta, titleEn = en,
        isHoliday = false, source = EventSource.COMPUTED, detail = detail, religion = religion,
    )

    // ------------------------------------------------------------------ Christian

    /** Gregorian (Western) Easter, anonymous computus. */
    fun westernEaster(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val h = (19 * a + b - b / 4 - (b - (b + 8) / 25 + 1) / 3 + 15) % 30
        val l = (32 + 2 * (b % 4) + 2 * (c / 4) - h - c % 4) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    /** Orthodox Easter: Julian computus, converted to the Gregorian calendar (+13 days for 1900–2099). */
    fun orthodoxEaster(year: Int): LocalDate {
        val a = year % 4
        val b = year % 7
        val c = year % 19
        val d = (19 * c + 15) % 30
        val e = (2 * a + 4 * b - d + 34) % 7
        val month = (d + e + 114) / 31
        val day = (d + e + 114) % 31 + 1
        return LocalDate.of(year, month, day).plusDays(13)
    }

    private fun christian(from: LocalDate, to: LocalDate): List<CalendarEvent> = (from.year..to.year).flatMap { y ->
        val easter = westernEaster(y)
        val christmas = LocalDate.of(y, 12, 25)
        val c = Religion.CHRISTIAN
        listOf(
            event(LocalDate.of(y, 1, 6), "திருக்காட்சி விழா", "Epiphany", c),
            event(easter.minusDays(46), "சாம்பல் புதன் (தவக்காலம் ஆரம்பம்)", "Ash Wednesday (Lent begins)", c),
            event(LocalDate.of(y, 3, 25), "மங்கள வார்த்தை", "Annunciation", c),
            event(easter.minusDays(7), "குருத்தோலை ஞாயிறு", "Palm Sunday", c),
            event(easter.minusDays(3), "பெரிய வியாழன்", "Maundy Thursday", c),
            event(easter.minusDays(2), "புனித வெள்ளி", "Good Friday", c),
            event(easter, "ஈஸ்டர்", "Easter Sunday", c),
            event(orthodoxEaster(y), "ஆர்த்தடாக்ஸ் ஈஸ்டர்", "Orthodox Easter", c),
            event(easter.plusDays(39), "விண்ணேற்றம்", "Ascension Day", c),
            event(easter.plusDays(49), "பெந்தகோஸ்து", "Pentecost", c),
            event(LocalDate.of(y, 8, 15), "மரியாவின் விண்ணேற்பு", "Assumption of Mary", c),
            event(LocalDate.of(y, 11, 1), "அனைத்துப் புனிதர் தினம்", "All Saints' Day", c),
            event(LocalDate.of(y, 11, 2), "அனைத்து ஆன்மாக்கள் தினம்", "All Souls' Day", c),
            event(christmas.minusDays(28).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)), "திருவருகைக் காலம் ஆரம்பம்", "First Sunday of Advent", c),
            event(LocalDate.of(y, 12, 24), "கிறிஸ்துமஸ் ஈவ்", "Christmas Eve", c),
            event(christmas, "கிறிஸ்துமஸ்", "Christmas", c),
        )
    }

    // ------------------------------------------------------------------ Jewish (Hebrew calendar, exact)

    /** R.D. (Rata Die) day numbers, as in Reingold & Dershowitz, "Calendrical Calculations". */
    private const val RD_EPOCH_DAY = 719163L // RD of 1970-01-01
    private const val HEBREW_EPOCH = -1373427L

    private fun rd(d: LocalDate) = d.toEpochDay() + RD_EPOCH_DAY
    private fun date(rd: Long) = LocalDate.ofEpochDay(rd - RD_EPOCH_DAY)

    private fun hebrewLeap(y: Long) = Math.floorMod(7 * y + 1, 19L) < 7

    private fun hebrewElapsedDays(y: Long): Long {
        val months = Math.floorDiv(235 * y - 234, 19L)
        val parts = 12084 + 13753 * months
        val day = 29 * months + Math.floorDiv(parts, 25920L)
        return if (Math.floorMod(3 * (day + 1), 7L) < 3) day + 1 else day
    }

    private fun hebrewNewYear(y: Long): Long {
        val ny0 = hebrewElapsedDays(y - 1)
        val ny1 = hebrewElapsedDays(y)
        val ny2 = hebrewElapsedDays(y + 1)
        val correction = when {
            ny2 - ny1 == 356L -> 2
            ny1 - ny0 == 382L -> 1
            else -> 0
        }
        return HEBREW_EPOCH + ny1 + correction
    }

    private fun hebrewYearDays(y: Long) = hebrewNewYear(y + 1) - hebrewNewYear(y)

    /** Months: 1 = Nisan … 7 = Tishri … 12 = Adar (Adar I in leap years), 13 = Adar II. */
    private fun hebrewMonthDays(y: Long, m: Int): Int {
        val yearDays = hebrewYearDays(y)
        return when {
            m in setOf(2, 4, 6, 10, 13) -> 29
            m == 12 && !hebrewLeap(y) -> 29
            m == 8 && yearDays % 10 != 5L -> 29 // Marheshvan is long only in "complete" years
            m == 9 && yearDays % 10 == 3L -> 29 // Kislev is short in "deficient" years
            else -> 30
        }
    }

    private fun hebrewToRd(y: Long, m: Int, d: Int): Long {
        val lastMonth = if (hebrewLeap(y)) 13 else 12
        var days = hebrewNewYear(y) + d - 1
        if (m < 7) {
            for (mm in 7..lastMonth) days += hebrewMonthDays(y, mm)
            for (mm in 1 until m) days += hebrewMonthDays(y, mm)
        } else {
            for (mm in 7 until m) days += hebrewMonthDays(y, mm)
        }
        return days
    }

    private fun jewish(from: LocalDate, to: LocalDate): List<CalendarEvent> = (from.year..to.year).flatMap { g ->
        val spring = (g + 3760).toLong() // Hebrew year whose Nisan falls in Gregorian year g
        val autumn = spring + 1 // Hebrew year beginning at Rosh Hashanah in g
        val j = Religion.JEWISH
        val note = "சூரியன் மறைவிலிருந்து முந்தைய மாலை ஆரம்பம் · Begins at sundown the evening before"
        fun h(y: Long, m: Int, d: Int) = date(hebrewToRd(y, m, d))
        listOf(
            event(h(spring, if (hebrewLeap(spring)) 13 else 12, 14), "பூரிம்", "Purim", j, note),
            event(h(spring, 1, 15), "பாஸ்கா (பெசாக்)", "Passover (Pesach)", j, note),
            event(h(spring, 3, 6), "ஷாவுவோத்", "Shavuot", j, note),
            event(h(autumn, 7, 1), "ரோஷ் ஹஷானா (யூதப் புத்தாண்டு)", "Rosh Hashanah", j, note),
            event(h(autumn, 7, 10), "யோம் கிப்பூர்", "Yom Kippur", j, note),
            event(h(autumn, 7, 15), "சுக்கோத்", "Sukkot", j, note),
            event(h(autumn, 9, 25), "ஹனுக்கா ஆரம்பம்", "Hanukkah begins", j, note),
        )
    }

    // ------------------------------------------------------------------ Islamic (astronomical, Umm al-Qura style)

    private const val MECCA_LAT = 21.4225
    private const val MECCA_LON = 39.8262
    private const val ISLAMIC_EPOCH = 227015L // RD of 1 Muharram 1 AH (Julian 622-07-16), civil/tabular

    /** Tabular (arithmetical) Hijri calendar; used only to name each astronomically computed month. */
    private fun tabularHijri(rd: Long): Pair<Long, Int> {
        val year = Math.floorDiv(30 * (rd - ISLAMIC_EPOCH) + 10646, 10631L)
        val firstOfYear = ISLAMIC_EPOCH - 1 + 354 * (year - 1) + Math.floorDiv(3 + 11 * year, 30L) + 1
        val month = Math.floorDiv(11 * (rd - firstOfYear) + 330, 325L).toInt().coerceIn(1, 12)
        return year to month
    }

    /**
     * A Hijri month starts the day after the conjunction if the conjunction happens before sunset in
     * Mecca, otherwise one day later. This matches the dates the Fiqh Council of North America and
     * Saudi Umm al-Qura published for 2025–2026; local moon-sighting can be a day later.
     */
    fun islamicMonthStarts(from: LocalDate, to: LocalDate): List<Triple<LocalDate, Long, Int>> {
        val out = mutableListOf<Triple<LocalDate, Long, Int>>()
        var jd = Astro.julianDay(from.minusDays(40).atStartOfDay().toInstant(ZoneOffset.UTC))
        val endJd = Astro.julianDay(to.plusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC))
        while (jd < endJd) {
            val conj = Astro.findCrossing(jd, 35.0, 1.0, 0.0, Astro::elongation) ?: break
            val utcDate = Astro.instantOf(conj).atOffset(ZoneOffset.UTC).toLocalDate()
            val meccaSunset = Astro.sunriseSunset(utcDate, MECCA_LAT, MECCA_LON).second
            val first = utcDate.plusDays(if (conj < meccaSunset) 1 else 2)
            val (year, month) = tabularHijri(rd(first.plusDays(14))) // mid-month is safely inside the tabular month
            out += Triple(first, year, month)
            jd = conj + 1.0
        }
        return out
    }

    private fun islamic(from: LocalDate, to: LocalDate): List<CalendarEvent> {
        val i = Religion.ISLAM
        val note = "கணக்கீடு; பிறை பார்த்தலால் ஒரு நாள் மாறலாம் · Calculated; local moon sighting may differ by a day. Begins at sunset the evening before."
        return islamicMonthStarts(from, to).flatMap { (first, year, month) ->
            fun day(n: Int) = first.plusDays((n - 1).toLong())
            when (month) {
                1 -> listOf(
                    event(day(1), "இஸ்லாமியப் புத்தாண்டு ($year AH)", "Islamic New Year ($year AH)", i, note),
                    event(day(10), "ஆஷூரா", "Ashura", i, note),
                )
                3 -> listOf(event(day(12), "மீலாது நபி", "Mawlid an-Nabi", i, note))
                7 -> listOf(event(day(27), "இஸ்ரா மிஃராஜ்", "Isra and Mi'raj", i, note))
                8 -> listOf(event(day(15), "ஷபே பராஅத்", "Shab-e-Barat", i, note))
                9 -> listOf(
                    event(day(1), "ரமலான் ஆரம்பம்", "Ramadan begins", i, note),
                    event(day(27), "லைலத்துல் கத்ர் (27-ஆம் இரவு)", "Laylat al-Qadr (27th night)", i, note),
                )
                10 -> listOf(event(day(1), "ரமலான் பண்டிகை (ஈதுல் பித்ர்)", "Eid al-Fitr", i, note))
                12 -> listOf(
                    event(day(9), "அரஃபா நாள்", "Day of Arafah", i, note),
                    event(day(10), "பக்ரீத் (ஈதுல் அழ்ஹா)", "Eid al-Adha", i, note),
                )
                else -> emptyList()
            }
        }
    }
}
