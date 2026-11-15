package com.kamalsoft.tamilcalendar.tv.domain.model

import java.time.LocalTime

/** Tamil-calendar style clock text ("காலை 7:30"), shared by the engine (stored text) and the UI. */
object TamilTime {
    fun period(t: LocalTime): String = when (t.hour) {
        in 0..3 -> "பின்னிரவு"
        in 4..11 -> "காலை"
        in 12..15 -> "மதியம்"
        in 16..18 -> "மாலை"
        else -> "இரவு"
    }

    /** 12-hour h:mm, rounded to the nearest minute (as published panchangams do). */
    fun clock(time: LocalTime): String {
        val t = time.plusSeconds(30).withSecond(0).withNano(0)
        val h = if (t.hour % 12 == 0) 12 else t.hour % 12
        return "%d:%02d".format(h, t.minute)
    }

    fun time(t: LocalTime): String = "${period(t)} ${clock(t)}"

    /** Period word on the start only, as printed calendars do: "காலை 10:30 – 12:00". */
    fun range(start: LocalTime, end: LocalTime): String = "${period(start)} ${clock(start)} – ${clock(end)}"
}
