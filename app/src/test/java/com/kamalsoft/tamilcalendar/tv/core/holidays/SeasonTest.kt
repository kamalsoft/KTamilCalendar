package com.kamalsoft.tamilcalendar.tv.core.holidays

import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.ui.weather.Season
import com.kamalsoft.tamilcalendar.tv.ui.weather.seasonFor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SeasonTest {
    private val sydney = GeoLocation("Sydney", "Sydney", -33.87, 151.21, "Australia/Sydney", "AU", "New South Wales", "Australia")

    @Test fun seasons() {
        assertEquals(Season.AUTUMN, seasonFor(LocalDate.of(2026, 10, 26), GeoLocation.NAPERVILLE))
        assertEquals(Season.WINTER, seasonFor(LocalDate.of(2027, 1, 10), GeoLocation.NAPERVILLE))
        assertEquals(Season.MONSOON, seasonFor(LocalDate.of(2026, 7, 1), GeoLocation.CHENNAI))
        assertEquals(Season.SUMMER, seasonFor(LocalDate.of(2026, 4, 20), GeoLocation.CHENNAI))
        assertEquals(Season.SPRING, seasonFor(LocalDate.of(2026, 10, 26), sydney))
    }
}
