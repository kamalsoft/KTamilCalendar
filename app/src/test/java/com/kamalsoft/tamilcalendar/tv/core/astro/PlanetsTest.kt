package com.kamalsoft.tamilcalendar.tv.core.astro

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/** Reference: Swiss Ephemeris (Moshier, Lahiri, 0h UT), see the verify-panchangam skill. */
class PlanetsTest {
    private fun jd(d: String) = Astro.julianDay(LocalDate.parse(d).atStartOfDay().toInstant(ZoneOffset.UTC))

    @Test fun matchesSwissEphemeris() {
        val ref = mapOf(
            "2024-01-01" to Triple(11.39, 309.05, 356.69),
            "2026-10-27" to Triple(119.45, 345.38, 302.10),
            "2030-03-15" to Triple(213.35, 26.46, 236.66),
        )
        ref.forEach { (d, r) ->
            assertEquals("Jupiter $d", r.first, Planets.jupiterSidereal(jd(d)), 1.0)
            assertEquals("Saturn $d", r.second, Planets.saturnSidereal(jd(d)), 1.0)
            assertEquals("Rahu $d", r.third, Planets.rahuSidereal(jd(d)), 0.1)
        }
    }
}
