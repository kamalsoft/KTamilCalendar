package com.kamalsoft.tamilcalendar.tv.ui.components

import com.kamalsoft.tamilcalendar.tv.domain.model.TamilTime
import com.kamalsoft.tamilcalendar.tv.domain.model.TimingWindow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Tamil-calendar style time strings: "காலை 7:30 – 9:00", "இரவு 10:42 வரை". */
object Fmt {
    val gregorianMonthsTa = listOf(
        "ஜனவரி", "பிப்ரவரி", "மார்ச்", "ஏப்ரல்", "மே", "ஜூன்",
        "ஜூலை", "ஆகஸ்ட்", "செப்டம்பர்", "அக்டோபர்", "நவம்பர்", "டிசம்பர்",
    )
    val weekdayShortTa = listOf("ஞா", "தி", "செ", "பு", "வி", "வெ", "ச")

    fun time(t: LocalTime): String = TamilTime.time(t)

    /** For narrow hourly columns: ("இரவு", "8"). */
    fun hourParts(t: LocalTime): Pair<String, String> = TamilTime.period(t) to TamilTime.clock(t).substringBefore(':')

    fun range(w: TimingWindow): String = range(w.start, w.end)

    /** Period word on the start only, as printed calendars do: "காலை 10:30 – 12:00". */
    fun range(start: LocalTime, end: LocalTime): String = TamilTime.range(start, end)

    fun until(at: LocalDateTime?, day: LocalDate): String {
        at ?: return "முழு நாள்"
        val d = at.toLocalDate()
        val t = at.toLocalTime()
        return when {
            d == day -> "${time(t)} வரை"
            d == day.plusDays(1) && t.hour < 4 -> "${time(t)} வரை"
            d == day.plusDays(1) -> "நாளை ${time(t)} வரை"
            else -> "${d.dayOfMonth} ${gregorianMonthsTa[d.monthValue - 1]} ${time(t)} வரை"
        }
    }

    private val clockEn = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    fun timeEn(t: LocalTime): String = t.plusSeconds(30).withSecond(0).withNano(0).format(clockEn)

    /** English counterpart of [until]: "until 8:32 PM", "until tomorrow 5:09 AM". */
    fun untilEn(at: LocalDateTime?, day: LocalDate): String {
        at ?: return "all day"
        val d = at.toLocalDate()
        return when {
            d == day -> "until ${timeEn(at.toLocalTime())}"
            d == day.plusDays(1) -> "until tomorrow ${timeEn(at.toLocalTime())}"
            else -> "until ${d.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))} ${timeEn(at.toLocalTime())}"
        }
    }

    /** "26 Oct 2026" */
    fun gregorianEn(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))

    fun weekdayShortEn(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)

    fun gregorian(d: LocalDate): String = "${d.dayOfMonth} ${gregorianMonthsTa[d.monthValue - 1]} ${d.year}"
}
