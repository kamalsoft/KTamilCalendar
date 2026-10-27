package com.kamalsoft.tamilcalendar.tv.core.holidays

import com.kamalsoft.tamilcalendar.tv.core.astro.TamilCalendarEngine
import com.kamalsoft.tamilcalendar.tv.data.notify.bannerFor
import com.kamalsoft.tamilcalendar.tv.data.notify.remindersFor
import com.kamalsoft.tamilcalendar.tv.domain.content.Slokas
import com.kamalsoft.tamilcalendar.tv.domain.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RemindersTest {
    private val day = TamilCalendarEngine().compute(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 26)).single() // Monday, Chennai

    @Test fun rahuReminderTenMinutesBefore() {
        val r = remindersFor(day, AppSettings(rahuReminder = true)).single()
        assertEquals(day.date.atTime(day.timings.rahuKalam.start).minusMinutes(10), r.at)
    }

    @Test fun nallaNeramOneReminderPerWindow() {
        val rs = remindersFor(day, AppSettings(nallaReminder = true))
        assertEquals(day.timings.nallaNeram.size, rs.size)
        assertTrue(remindersFor(day, AppSettings()).isEmpty())
    }

    @Test fun bannerShowsForTwoMinutes() {
        val s = AppSettings(rahuReminder = true)
        val at = day.date.atTime(day.timings.rahuKalam.start).minusMinutes(10)
        assertNotNull(bannerFor(day, s, at.plusMinutes(1)))
        assertNull(bannerFor(day, s, at.plusMinutes(3)))
    }

    @Test fun slokaFollowsTheDay() {
        assertEquals(Slokas.SIVAPURANAM, Slokas.forDay(day.copy(tags = emptySet()))) // Monday → Shiva
    }
}
