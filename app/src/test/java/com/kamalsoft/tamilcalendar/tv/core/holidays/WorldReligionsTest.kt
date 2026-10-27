package com.kamalsoft.tamilcalendar.tv.core.holidays

import com.kamalsoft.tamilcalendar.tv.domain.model.Religion
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Reference dates: published religious calendars (see README "Religious dates"). */
class WorldReligionsTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private val events = WorldReligions.between(d(2025, 1, 1), d(2027, 12, 31))
    private fun dateOf(titleEn: String, year: Int) = events.first { it.titleEn == titleEn && it.date.year == year }.date

    @Test fun christian() {
        assertEquals(d(2025, 4, 20), dateOf("Easter Sunday", 2025))
        assertEquals(d(2026, 4, 5), dateOf("Easter Sunday", 2026))
        assertEquals(d(2026, 2, 18), dateOf("Ash Wednesday (Lent begins)", 2026))
        assertEquals(d(2026, 4, 3), dateOf("Good Friday", 2026))
        assertEquals(d(2026, 4, 12), dateOf("Orthodox Easter", 2026))
        assertEquals(d(2026, 11, 29), dateOf("First Sunday of Advent", 2026))
    }

    @Test fun jewish() {
        assertEquals(d(2025, 4, 13), dateOf("Passover (Pesach)", 2025))
        assertEquals(d(2025, 9, 23), dateOf("Rosh Hashanah", 2025))
        assertEquals(d(2025, 10, 2), dateOf("Yom Kippur", 2025))
        assertEquals(d(2025, 12, 15), dateOf("Hanukkah begins", 2025))
        assertEquals(d(2026, 4, 2), dateOf("Passover (Pesach)", 2026))
        assertEquals(d(2026, 9, 12), dateOf("Rosh Hashanah", 2026))
        assertEquals(d(2026, 9, 21), dateOf("Yom Kippur", 2026))
        assertEquals(d(2026, 12, 5), dateOf("Hanukkah begins", 2026))
    }

    @Test fun islamicMatchesPublishedDates() {
        // FCNA / Umm al-Qura published dates.
        assertEquals(d(2025, 3, 1), dateOf("Ramadan begins", 2025))
        assertEquals(d(2025, 3, 30), dateOf("Eid al-Fitr", 2025))
        assertEquals(d(2025, 6, 6), dateOf("Eid al-Adha", 2025))
        assertEquals(d(2026, 2, 18), dateOf("Ramadan begins", 2026))
        assertEquals(d(2026, 3, 20), dateOf("Eid al-Fitr", 2026))
    }

    @Test fun islamicMonthsAreNamedCorrectly() {
        val ramadan2026 = WorldReligions.islamicMonthStarts(d(2026, 2, 1), d(2026, 3, 1)).first { it.first == d(2026, 2, 18) }
        assertEquals(1447L, ramadan2026.second)
        assertEquals(9, ramadan2026.third)
        assertEquals(Religion.ISLAM, events.first { it.titleEn == "Eid al-Fitr" }.religion)
    }
}
