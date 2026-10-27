package com.kamalsoft.tamilcalendar.tv.core.holidays

import com.kamalsoft.tamilcalendar.tv.data.local.SchoolCalendars
import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.EventSource
import com.kamalsoft.tamilcalendar.tv.domain.model.HolidayRegion
import com.kamalsoft.tamilcalendar.tv.domain.usecase.forDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class HolidaysTest {

    private val us = setOf(HolidayRegion.US_FEDERAL, HolidayRegion.ILLINOIS)
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private fun on(date: LocalDate) = UsHolidays.between(date, date, us).map { it.titleEn }

    @Test fun federal2026() {
        assertEquals(listOf("Martin Luther King Jr. Day"), on(d(2026, 1, 19)))
        assertEquals(listOf("Presidents' Day"), on(d(2026, 2, 16)))
        assertEquals(listOf("Memorial Day"), on(d(2026, 5, 25)))
        assertEquals(listOf("Labor Day"), on(d(2026, 9, 7)))
        assertEquals(listOf("Columbus / Indigenous Peoples' Day"), on(d(2026, 10, 12)))
        assertTrue("Thanksgiving" in on(d(2026, 11, 26)))
    }

    @Test fun weekendObservance() {
        // July 4 2026 is a Saturday → observed Friday July 3.
        assertEquals(listOf("Independence Day (US) (observed)"), on(d(2026, 7, 3)))
        // Juneteenth 2027 is a Saturday → Friday June 18. Christmas 2027 Saturday → Dec 24.
        assertEquals(listOf("Juneteenth (observed)"), on(d(2027, 6, 18)))
        assertEquals(listOf("Christmas (observed)"), on(d(2027, 12, 24)))
        // Jan 1 2028 is a Saturday → observed Fri Dec 31 2027.
        assertEquals(listOf("New Year's Day (observed)"), on(d(2027, 12, 31)))
    }

    @Test fun illinoisExtras() {
        assertEquals(listOf("General Election Day"), on(d(2026, 11, 3)))
        assertEquals(listOf("Day after Thanksgiving"), on(d(2026, 11, 27)))
        assertEquals(listOf("Lincoln's Birthday"), on(d(2027, 2, 12)))
        assertTrue(on(d(2027, 11, 2)).isEmpty()) // no general election in odd years
    }

    private val schoolJson = File("src/main/assets/school_calendars.json").readText()

    @Test fun schoolCalendarsParseAndExpand() {
        val both = SchoolCalendars.parse(schoolJson, setOf(HolidayRegion.SCHOOL_D203, HolidayRegion.SCHOOL_D204))
        val winter203 = both.filter { it.region == HolidayRegion.SCHOOL_D203 && it.titleEn == "Winter Vacation" }
        assertEquals(12, winter203.size) // Dec 21 .. Jan 1 inclusive
        assertTrue(both.any { it.region == HolidayRegion.SCHOOL_D204 && it.date == d(2027, 4, 6) && it.isHoliday })
        assertFalse(both.first { it.titleEn == "Classes Begin" }.isHoliday)

        val only204 = SchoolCalendars.parse(schoolJson, setOf(HolidayRegion.SCHOOL_D204))
        assertTrue(only204.all { it.region == HolidayRegion.SCHOOL_D204 })
    }

    @Test fun listCollapsesRangesAndMergesDuplicateHolidays() {
        val school = SchoolCalendars.parse(schoolJson, setOf(HolidayRegion.SCHOOL_D203))
        val winter = school.forDisplay().single { it.titleEn == "Winter Vacation" }
        assertEquals(d(2026, 12, 21), winter.date)
        assertEquals(d(2027, 1, 1), winter.endDate)

        val tnChristmas = CalendarEvent(d(2026, 12, 25), EventKind.GOVT_HOLIDAY, "கிறிஸ்துமஸ்", "Christmas", true, EventSource.COMPUTED, region = HolidayRegion.TAMIL_NADU)
        val merged = (UsHolidays.between(d(2026, 12, 25), d(2026, 12, 25), us) + tnChristmas).forDisplay()
        assertEquals(HolidayRegion.US_FEDERAL, merged.single().region)
    }
}
