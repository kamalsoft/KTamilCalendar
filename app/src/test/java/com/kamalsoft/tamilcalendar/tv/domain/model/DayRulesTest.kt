package com.kamalsoft.tamilcalendar.tv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class DayRulesTest {

    /** Soolam directions from the Tamil almanac table (docs/BACKLOG.md item 2). */
    @Test fun soolamDirectionsByWeekday() {
        assertEquals(Direction.WEST, DayRules.soolam(DayOfWeek.SUNDAY).direction)
        assertEquals(Direction.EAST, DayRules.soolam(DayOfWeek.MONDAY).direction)
        assertEquals(Direction.NORTH, DayRules.soolam(DayOfWeek.TUESDAY).direction)
        assertEquals(Direction.NORTH, DayRules.soolam(DayOfWeek.WEDNESDAY).direction)
        assertEquals(Direction.SOUTH, DayRules.soolam(DayOfWeek.THURSDAY).direction)
        assertEquals(Direction.WEST, DayRules.soolam(DayOfWeek.FRIDAY).direction)
        assertEquals(Direction.EAST, DayRules.soolam(DayOfWeek.SATURDAY).direction)
    }

    /** Karanam 0 is Kimstughna, 1 is Bava, 8 is Bava again, 7 is Vishti, 57 is Shakuni. */
    @Test fun karanaNamesAndVishti() {
        assertEquals("Kimstughna", DayRules.karanaName(0).second)
        assertEquals("Bava", DayRules.karanaName(1).second)
        assertEquals("Bava", DayRules.karanaName(8).second)
        assertEquals("Shakuni", DayRules.karanaName(57).second)
        assertEquals("Naga", DayRules.karanaName(59).second)
        assertTrue(DayRules.isVishti(7))
        assertFalse(DayRules.isVishti(1))
    }

    /** Kari Naal: Aippasi 6 (index 6) is a Kari Naal day; Aippasi 7 is not. */
    @Test fun kariNaalDays() {
        assertTrue(DayRules.isKariNaal(6, 6))
        assertFalse(DayRules.isKariNaal(6, 7))
    }

    /** Abhijit is 48 minutes centred on solar noon: 6:00 to 18:00 gives noon 12:00, window 11:36–12:24. */
    @Test fun abhijitIsCentredOnNoon() {
        val (from, to) = DayRules.abhijit(LocalTime.of(6, 0), LocalTime.of(18, 0))
        assertEquals(LocalTime.of(11, 36), from)
        assertEquals(LocalTime.of(12, 24), to)
    }
}
