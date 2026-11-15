package com.kamalsoft.tamilcalendar.tv.core.holidays

import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.EventSource
import com.kamalsoft.tamilcalendar.tv.domain.model.HolidayRegion
import com.kamalsoft.tamilcalendar.tv.domain.model.Religion
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

/**
 * Rule-based US holidays, valid for any year.
 *
 * - Federal: 5 U.S.C. § 6103. A Saturday holiday is observed the Friday before, a Sunday one the Monday after.
 * - Illinois (state offices, 5 ILCS 490): the federal list plus Lincoln's Birthday, the day after
 *   Thanksgiving and general-election day (even years). The same weekend shift is applied.
 */
object UsHolidays {

    private class Rule(
        val region: HolidayRegion,
        val titleTa: String,
        val titleEn: String,
        /** Floating holidays (e.g. "4th Thursday") never fall on a weekend, so they're never shifted. */
        val shiftsOnWeekend: Boolean,
        val religion: Religion? = null,
        val dateIn: (year: Int) -> LocalDate?,
    )

    private fun nth(year: Int, month: Month, dow: DayOfWeek, n: Int): LocalDate =
        LocalDate.of(year, month, 1).with(TemporalAdjusters.dayOfWeekInMonth(n, dow))

    private fun last(year: Int, month: Month, dow: DayOfWeek): LocalDate =
        LocalDate.of(year, month, 1).with(TemporalAdjusters.lastInMonth(dow))

    private fun thanksgiving(year: Int) = nth(year, Month.NOVEMBER, DayOfWeek.THURSDAY, 4)

    private val rules = listOf(
        Rule(HolidayRegion.US_FEDERAL, "ஆங்கிலப் புத்தாண்டு", "New Year's Day", true) { LocalDate.of(it, 1, 1) },
        Rule(HolidayRegion.US_FEDERAL, "மார்ட்டின் லூதர் கிங் ஜூனியர் தினம்", "Martin Luther King Jr. Day", false) {
            nth(it, Month.JANUARY, DayOfWeek.MONDAY, 3)
        },
        Rule(HolidayRegion.US_FEDERAL, "ஜனாதிபதிகள் தினம்", "Presidents' Day", false) { nth(it, Month.FEBRUARY, DayOfWeek.MONDAY, 3) },
        Rule(HolidayRegion.US_FEDERAL, "நினைவு தினம்", "Memorial Day", false) { last(it, Month.MAY, DayOfWeek.MONDAY) },
        Rule(HolidayRegion.US_FEDERAL, "ஜூன்டீன்த்", "Juneteenth", true) { if (it >= 2021) LocalDate.of(it, 6, 19) else null },
        Rule(HolidayRegion.US_FEDERAL, "அமெரிக்க சுதந்திர தினம்", "Independence Day (US)", true) { LocalDate.of(it, 7, 4) },
        Rule(HolidayRegion.US_FEDERAL, "தொழிலாளர் தினம்", "Labor Day", false) { nth(it, Month.SEPTEMBER, DayOfWeek.MONDAY, 1) },
        Rule(HolidayRegion.US_FEDERAL, "கொலம்பஸ் / பூர்வகுடி மக்கள் தினம்", "Columbus / Indigenous Peoples' Day", false) {
            nth(it, Month.OCTOBER, DayOfWeek.MONDAY, 2)
        },
        Rule(HolidayRegion.US_FEDERAL, "படைவீரர் தினம்", "Veterans Day", true) { LocalDate.of(it, 11, 11) },
        Rule(HolidayRegion.US_FEDERAL, "நன்றி தெரிவிக்கும் நாள்", "Thanksgiving", false) { thanksgiving(it) },
        Rule(HolidayRegion.US_FEDERAL, "கிறிஸ்துமஸ்", "Christmas", true, Religion.CHRISTIAN) { LocalDate.of(it, 12, 25) },

        Rule(HolidayRegion.ILLINOIS, "லிங்கன் பிறந்த நாள்", "Lincoln's Birthday", true) { LocalDate.of(it, 2, 12) },
        Rule(HolidayRegion.ILLINOIS, "நன்றி நாளுக்கு அடுத்த நாள்", "Day after Thanksgiving", false) { thanksgiving(it).plusDays(1) },
        Rule(HolidayRegion.ILLINOIS, "பொதுத் தேர்தல் நாள்", "General Election Day", false) {
            // First Tuesday after the first Monday of November, even years.
            if (it % 2 == 0) nth(it, Month.NOVEMBER, DayOfWeek.MONDAY, 1).plusDays(1) else null
        },
    )

    fun between(from: LocalDate, to: LocalDate, regions: Set<HolidayRegion>): List<CalendarEvent> {
        val out = mutableListOf<CalendarEvent>()
        // ±1 year so a Jan 1 Saturday observed on Dec 31 of the previous year is included.
        for (year in from.year - 1..to.year + 1) for (rule in rules) {
            if (rule.region !in regions) continue
            val date = rule.dateIn(year) ?: continue
            fun add(d: LocalDate, observed: Boolean) {
                if (d < from || d > to) return
                out += CalendarEvent(
                    date = d, kind = EventKind.GOVT_HOLIDAY,
                    titleTa = if (observed) "${rule.titleTa} (விடுமுறை)" else rule.titleTa,
                    titleEn = if (observed) "${rule.titleEn} (observed)" else rule.titleEn,
                    isHoliday = true, source = EventSource.COMPUTED, region = rule.region, religion = rule.religion,
                )
            }
            add(date, observed = false)
            if (rule.shiftsOnWeekend) when (date.dayOfWeek) {
                DayOfWeek.SATURDAY -> add(date.minusDays(1), observed = true)
                DayOfWeek.SUNDAY -> add(date.plusDays(1), observed = true)
                else -> Unit
            }
        }
        return out.sortedBy { it.date }
    }
}
