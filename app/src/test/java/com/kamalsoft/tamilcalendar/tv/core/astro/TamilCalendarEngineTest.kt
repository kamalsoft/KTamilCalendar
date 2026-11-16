package com.kamalsoft.tamilcalendar.tv.core.astro

import com.kamalsoft.tamilcalendar.tv.domain.model.DayTag
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Reference values are publicly observed dates (Tamil Nadu govt. holiday lists, temple
 * calendars). If a convention changes in the engine, these are the regressions that matter.
 */
class TamilCalendarEngineTest {

    companion object {
        private lateinit var days: Map<LocalDate, TamilDayData>

        @BeforeClass
        @JvmStatic
        fun compute() {
            days = TamilCalendarEngine()
                .compute(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 12, 31))
                .associateBy { it.date }
        }

        private fun day(y: Int, m: Int, d: Int) = days.getValue(LocalDate.of(y, m, d))

        private fun firstDateWithEvent(nameEn: String, year: Int): LocalDate =
            days.values.filter { it.date.year == year }.first { d -> d.events.any { it.titleEn == nameEn } }.date
    }

    @Test fun tamilNewYear2024() {
        val d = day(2024, 4, 14)
        assertEquals(0, d.tamilDate.monthIndex)
        assertEquals(1, d.tamilDate.day)
        assertEquals("குரோதி", d.tamilDate.yearTa)
        assertEquals(11, day(2024, 4, 13).tamilDate.monthIndex) // still Panguni: sankranti after sunset
    }

    @Test fun tamilNewYear2025And2026() {
        assertEquals(1, day(2025, 4, 14).tamilDate.day)
        assertEquals("விசுவாவசு", day(2025, 4, 14).tamilDate.yearTa)
        assertEquals("பராபவ", day(2026, 10, 26).tamilDate.yearTa)
    }

    @Test fun thaiPongalAndThiruvalluvarAandu() {
        assertEquals(9, day(2024, 1, 15).tamilDate.monthIndex)
        assertEquals(1, day(2024, 1, 15).tamilDate.day)
        assertEquals(2055, day(2024, 1, 15).tamilDate.thiruvalluvarAandu)
        assertEquals(2054, day(2024, 1, 14).tamilDate.thiruvalluvarAandu)
        assertEquals(1, day(2025, 1, 14).tamilDate.day)
        assertEquals(2057, day(2026, 10, 26).tamilDate.thiruvalluvarAandu)
    }

    @Test fun lunarTags() {
        assertTrue(DayTag.AMAVASAI in day(2024, 11, 1).tags)
        assertTrue(DayTag.POURNAMI in day(2024, 10, 17).tags)
    }

    @Test fun lunarFestivals() {
        assertEquals(LocalDate.of(2024, 9, 7), firstDateWithEvent("Vinayagar Chathurthi", 2024))
        assertEquals(LocalDate.of(2025, 8, 27), firstDateWithEvent("Vinayagar Chathurthi", 2025))
        assertEquals(LocalDate.of(2024, 8, 26), firstDateWithEvent("Krishna Jayanthi", 2024))
        assertEquals(LocalDate.of(2024, 10, 11), firstDateWithEvent("Ayutha Pooja", 2024))
        assertEquals(LocalDate.of(2024, 10, 12), firstDateWithEvent("Vijayadasami", 2024))
        assertEquals(LocalDate.of(2025, 10, 1), firstDateWithEvent("Ayutha Pooja", 2025))
        assertEquals(LocalDate.of(2025, 10, 2), firstDateWithEvent("Vijayadasami", 2025))
        assertEquals(LocalDate.of(2024, 10, 31), firstDateWithEvent("Deepavali", 2024))
        assertEquals(LocalDate.of(2025, 10, 20), firstDateWithEvent("Deepavali", 2025))
        assertEquals(LocalDate.of(2025, 2, 26), firstDateWithEvent("Maha Shivaratri", 2025))
        assertEquals(LocalDate.of(2024, 12, 13), firstDateWithEvent("Karthigai Deepam", 2024))
        assertEquals(LocalDate.of(2025, 12, 3), firstDateWithEvent("Karthigai Deepam", 2025))
    }

    @Test fun templeAndOtherFaithFestivals() {
        // Tamil Nadu observed Soorasamharam on 27 Oct 2025: Sashti began 06:05 IST, so it prevails that evening.
        assertEquals(LocalDate.of(2025, 10, 27), firstDateWithEvent("Skanda Sashti (Soorasamharam)", 2025))
        // Jain 2026: Mahavir Jayanti 31 Mar; Paryushan 8–15 Sep with Samvatsari on 15 Sep.
        assertEquals(LocalDate.of(2026, 3, 31), firstDateWithEvent("Mahavir Jayanti", 2026))
        assertEquals(LocalDate.of(2026, 9, 15), firstDateWithEvent("Samvatsari", 2026))
        assertEquals(LocalDate.of(2026, 9, 8), firstDateWithEvent("Paryushan begins (Shvetambara)", 2026))
    }

    @Test fun monthlyObservancesOccurAtTheExpectedRate() {
        // Every lunar month has one of each; allow for 12–14 lunar months per calendar year.
        val y2025 = days.values.filter { it.date.year == 2025 }
        listOf(DayTag.SASHTI, DayTag.SANKATAHARA, DayTag.MASA_SIVARATRI, DayTag.AMAVASAI, DayTag.POURNAMI).forEach { tag ->
            val n = y2025.count { tag in it.tags }
            assertTrue("$tag occurred $n times", n in 12..14)
        }
        listOf(DayTag.EKADASI, DayTag.PRADOSHAM).forEach { tag ->
            val n = y2025.count { tag in it.tags }
            assertTrue("$tag occurred $n times", n in 23..27)
        }
    }

    /** Eight equal parts of the daytime from sunrise (validator Step 3B.5): Monday Rahu = 2nd, Yamagandam = 4th, Kuligai = 6th. */
    @Test fun mondayAvoidWindowsAreEighthsOfDaytime() {
        val monday = day(2026, 10, 26)
        val part = (monday.sunset.toSecondOfDay() - monday.sunrise.toSecondOfDay()) / 8
        fun at(n: Int) = LocalTime.ofSecondOfDay((monday.sunrise.toSecondOfDay() + (n - 1) * part).toLong())
        assertEquals(at(2), monday.timings.rahuKalam.start)
        assertEquals(at(3), monday.timings.rahuKalam.end)
        assertEquals(at(4), monday.timings.yamagandam.start)
        assertEquals(at(6), monday.timings.kuligai.start)
    }

    @Test fun chennaiSunrise() {
        val sunrise = day(2026, 10, 26).sunrise
        assertTrue("sunrise $sunrise", sunrise.isAfter(LocalTime.of(5, 50)) && sunrise.isBefore(LocalTime.of(6, 10)))
    }

    @Test fun everyDayHasConsistentPanchangam() {
        days.values.forEach { d ->
            assertTrue(d.panchangam.tithi.index in 0..29)
            assertTrue(d.panchangam.nakshatram.index in 0..26)
            assertTrue(d.tamilDate.day in 1..32)
            assertEquals((d.panchangam.moonRasi + 5) % 12, d.chandrashtamam.rasi)
        }
    }

    /** Validator section D: Subha Muhurtham only on Krishna Prathama to Panchami, and never on a malefic yoga. */
    @Test fun muhurthamKrishnaAndYogaRules() {
        val malefic = setOf(0, 5, 8, 9, 12, 14, 16, 18, 26)
        days.values.filter { it.tags.contains(DayTag.SUBHA_MUHURTHAM) }.forEach { d ->
            val t = d.panchangam.tithi.index
            if (t >= 15) assertTrue("${d.date} Krishna tithi ${t - 14} outside 1-5", t - 15 in 0..4)
            assertFalse("${d.date} on a malefic yoga", d.panchangam.yogam.index in malefic)
        }
    }

    /** Validator gate 1: a Kari Naal day never carries the Subha Muhurtham tag. */
    @Test fun noMuhurthamOnKariNaal() {
        days.values.filter { com.kamalsoft.tamilcalendar.tv.domain.model.DayRules.isKariNaal(it.tamilDate.monthIndex, it.tamilDate.day) }
            .forEach { assertFalse("${it.date} is a Kari Naal day", it.tags.contains(DayTag.SUBHA_MUHURTHAM)) }
    }

    /** A good window (Nalla Neram or a Gowri window) never overlaps Rahu Kalam, Yamagandam or Kuligai. */
    @Test fun goodWindowsNeverOverlapAvoidWindows() {
        days.values.forEach { d ->
            val t = d.timings
            val avoid = listOf(t.rahuKalam, t.yamagandam, t.kuligai)
            (t.nallaNeram + t.gowriNallaNeram).forEach { good ->
                avoid.forEach { bad ->
                    assertTrue("${d.date}: ${good.start}-${good.end} overlaps ${bad.type}", !(good.start < bad.end && bad.start < good.end))
                }
            }
        }
    }

    /** Lagna runs from sunrise to the next sunrise, one sign at a time, forward through the zodiac. */
    @Test fun lagnaCoversTheDayInOrder() {
        val all = days.values.sortedBy { it.date }
        all.zipWithNext().forEach { (d, next) ->
            val lagna = d.panchangam.lagnam
            assertEquals(d.sunrise, lagna.first().start.toLocalTime().withNano(0))
            assertTrue(lagna.last().start.isBefore(next.date.atTime(next.sunrise)))
            lagna.zipWithNext().forEach { (a, b) ->
                assertTrue(b.start.isAfter(a.start))
                assertEquals((a.rasi + 1) % 12, b.rasi)
            }
            // A sign rises for about 1 to 3 hours, so a day has roughly 8 to 16 changes.
            assertTrue("${d.date}: ${lagna.size} lagna segments", lagna.size in 8..16)
        }
    }
}
