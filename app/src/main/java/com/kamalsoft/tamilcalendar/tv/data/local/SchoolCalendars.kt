package com.kamalsoft.tamilcalendar.tv.data.local

import com.google.gson.Gson
import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.EventSource
import com.kamalsoft.tamilcalendar.tv.domain.model.HolidayRegion
import java.time.LocalDate

/**
 * Parses `assets/school_calendars.json`. School calendars are board-adopted each year and can't be
 * computed, so they ship as data. Multi-day entries are expanded to one event per day so the month
 * grid can tag every day; lists collapse them back into ranges.
 */
object SchoolCalendars {

    private class File(val districts: List<District>?)
    private class District(val region: String?, val days: List<Day>?)
    private class Day(val from: String?, val to: String?, val en: String?, val ta: String?, val noSchool: Boolean?)

    fun parse(json: String, regions: Set<HolidayRegion>): List<CalendarEvent> {
        val file = Gson().fromJson(json, File::class.java)
        return file.districts.orEmpty().flatMap { district ->
            val region = district.region?.let { r -> HolidayRegion.entries.firstOrNull { it.name == r } }
            if (region == null || region !in regions) return@flatMap emptyList()
            district.days.orEmpty().flatMap { day ->
                val from = LocalDate.parse(day.from ?: return@flatMap emptyList())
                val to = day.to?.let(LocalDate::parse) ?: from
                generateSequence(from) { it.plusDays(1) }.takeWhile { it <= to }.map { d ->
                    CalendarEvent(
                        date = d, kind = EventKind.SCHOOL,
                        titleTa = day.ta ?: day.en.orEmpty(), titleEn = day.en ?: day.ta.orEmpty(),
                        isHoliday = day.noSchool ?: false, source = EventSource.BUNDLED, region = region,
                    )
                }.toList()
            }
        }
    }
}
