package com.kamalsoft.tamilcalendar.tv.core.holidays

import com.kamalsoft.tamilcalendar.tv.domain.model.AppSettings
import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.EventSource
import com.kamalsoft.tamilcalendar.tv.domain.model.HolidayRegion
import com.kamalsoft.tamilcalendar.tv.domain.model.Religion
import com.kamalsoft.tamilcalendar.tv.domain.model.isVisible
import com.kamalsoft.tamilcalendar.tv.ui.components.localize
import com.kamalsoft.tamilcalendar.tv.ui.theme.DisplayPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SettingsFilterTest {
    private val day = LocalDate.of(2026, 12, 25)
    private fun ev(kind: EventKind, religion: Religion? = null, region: HolidayRegion? = null, local: Boolean = false) =
        CalendarEvent(day, kind, "t", "t", isHoliday = local || region != null, source = EventSource.COMPUTED, region = region, religion = religion, isLocalHoliday = local)

    @Test fun faithToggleHidesObservancesButNotLocalDaysOff() {
        val s = AppSettings(hiddenFaiths = setOf(Religion.CHRISTIAN))
        assertFalse(ev(EventKind.RELIGIOUS, Religion.CHRISTIAN).isVisible(s))
        assertTrue(ev(EventKind.GOVT_HOLIDAY, Religion.CHRISTIAN, HolidayRegion.US_FEDERAL, local = true).isVisible(s))
        assertTrue(ev(EventKind.FESTIVAL, Religion.HINDU).isVisible(AppSettings(hiddenFaiths = Religion.entries.toSet())))
    }

    @Test fun holidayToggles() {
        assertFalse(ev(EventKind.GOVT_HOLIDAY, region = HolidayRegion.ILLINOIS).isVisible(AppSettings(usHolidays = false)))
        assertFalse(ev(EventKind.SCHOOL, region = HolidayRegion.SCHOOL_D203).isVisible(AppSettings(schoolCalendars = false)))
        val noTn = AppSettings(tamilNaduHolidays = false)
        assertFalse(ev(EventKind.GOVT_HOLIDAY, region = HolidayRegion.TAMIL_NADU).isVisible(noTn))
        assertTrue(ev(EventKind.FESTIVAL, Religion.HINDU, HolidayRegion.TAMIL_NADU).isVisible(noTn)) // Deepavali stays
    }

    @Test fun tamilOnlyDropsEnglishParts() {
        DisplayPrefs.tamilOnly = true
        try {
            assertEquals("திதி · தேய்பிறை", localize("திதி · Tithi · தேய்பிறை"))
            assertEquals("Weather · Naperville", localize("Weather · Naperville"))
            assertEquals("இன்று · 2057", localize("இன்று · Today · 2057"))
        } finally {
            DisplayPrefs.tamilOnly = false
        }
    }
}
