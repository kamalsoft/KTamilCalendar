package com.kamalsoft.tamilcalendar.tv.core.astro

import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import com.kamalsoft.tamilcalendar.tv.domain.model.TimingConvention
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/** Naperville, IL (America/Chicago): local sunrise, DST and sunrise-based Rahu Kalam. */
class NapervilleEngineTest {

    companion object {
        private val engine = TamilCalendarEngine(GeoLocation.NAPERVILLE, TimingConvention.SUNRISE_BASED)
        private lateinit var days: Map<LocalDate, TamilDayData>

        @BeforeClass
        @JvmStatic
        fun compute() {
            days = engine.compute(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31)).associateBy { it.date }
        }

        private fun day(y: Int, m: Int, d: Int) = days.getValue(LocalDate.of(y, m, d))

        private fun assertNear(expected: LocalTime, actual: LocalTime, toleranceMin: Long = 2) =
            assertTrue("expected ≈$expected, was $actual", kotlin.math.abs(Duration.between(expected, actual).toMinutes()) <= toleranceMin)
    }

    @Test fun sunriseInLocalTimeAcrossDst() {
        // Reference values: NOAA solar calculator algorithm for 41.7508N, 88.1535W.
        assertNear(LocalTime.of(5, 18), day(2026, 6, 21).sunrise)   // CDT, summer solstice
        assertNear(LocalTime.of(20, 31), day(2026, 6, 21).sunset)
        assertNear(LocalTime.of(7, 18), day(2026, 10, 26).sunrise)  // CDT
        assertNear(LocalTime.of(17, 55), day(2026, 10, 26).sunset)
        assertNear(LocalTime.of(6, 26), day(2026, 11, 2).sunrise)   // CST, day after DST ends (Nov 1)
        assertNear(LocalTime.of(16, 46), day(2026, 11, 2).sunset)
    }

    @Test fun rahuKalamFollowsLocalSunrise() {
        val monday = day(2026, 10, 26)
        val part = Duration.between(monday.sunrise, monday.sunset).seconds / 8
        // Monday Rahu Kalam = 2nd eighth of the local day.
        assertEquals(monday.sunrise.plusSeconds(part), monday.timings.rahuKalam.start)
    }

    @Test fun tithiEndsInLocalTime() {
        // Full moon is 2026-10-26 04:12 UTC = 2026-10-25 23:12 CDT.
        val pournamiEnd = day(2026, 10, 25).panchangam.tithi.endsAt!!
        assertEquals(LocalDate.of(2026, 10, 25), pournamiEnd.toLocalDate())
        assertNear(LocalTime.of(23, 12), pournamiEnd.toLocalTime())
    }

    @Test fun locationChangeInvalidatesCachedData() {
        assertNotEquals(TamilCalendarEngine().dataVersion, engine.dataVersion)
    }
}
