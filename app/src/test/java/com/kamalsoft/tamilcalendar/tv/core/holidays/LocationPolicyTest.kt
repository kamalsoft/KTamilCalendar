package com.kamalsoft.tamilcalendar.tv.core.holidays

import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.domain.model.HolidayRegion
import com.kamalsoft.tamilcalendar.tv.domain.model.LocationPolicy
import com.kamalsoft.tamilcalendar.tv.domain.model.TimingConvention
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPolicyTest {
    private val madurai = GeoLocation("Madurai", "Madurai", 9.92, 78.12, "Asia/Kolkata", "IN", "Tamil Nadu", "India")
    private val toronto = GeoLocation("Toronto", "Toronto", 43.70, -79.42, "America/Toronto", "CA", "Ontario", "Canada")
    private val chicago = GeoLocation("Chicago", "Chicago", 41.85, -87.65, "America/Chicago", "US", "Illinois", "United States")

    @Test fun naperville() {
        val n = GeoLocation.NAPERVILLE
        assertEquals(setOf(HolidayRegion.US_FEDERAL, HolidayRegion.ILLINOIS), LocationPolicy.localHolidayRegions(n))
        assertEquals(setOf(HolidayRegion.SCHOOL_D203, HolidayRegion.SCHOOL_D204), LocationPolicy.schoolDistricts(n))
        assertEquals(TimingConvention.SUNRISE_BASED, LocationPolicy.timingConvention(n))
        assertTrue(LocationPolicy.usesImperialUnits(n))
    }

    @Test fun chicagoGetsIllinoisButNotNapervilleSchools() {
        assertTrue(HolidayRegion.ILLINOIS in LocationPolicy.localHolidayRegions(chicago))
        assertTrue(LocationPolicy.schoolDistricts(chicago).isEmpty())
    }

    @Test fun tamilNaduUsesTraditionalTimingsAndLocalHolidays() {
        assertEquals(setOf(HolidayRegion.TAMIL_NADU), LocationPolicy.localHolidayRegions(madurai))
        assertEquals(TimingConvention.TRADITIONAL_6AM, LocationPolicy.timingConvention(madurai))
        assertFalse(LocationPolicy.usesImperialUnits(madurai))
    }

    @Test fun elsewhereHasNoLocalHolidaysAndMetricUnits() {
        assertTrue(LocationPolicy.localHolidayRegions(toronto).isEmpty())
        assertFalse(LocationPolicy.usesImperialUnits(toronto))
        assertEquals(TimingConvention.SUNRISE_BASED, LocationPolicy.timingConvention(toronto))
    }
}
