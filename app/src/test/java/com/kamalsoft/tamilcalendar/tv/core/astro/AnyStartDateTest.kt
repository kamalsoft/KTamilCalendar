package com.kamalsoft.tamilcalendar.tv.core.astro

import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.domain.model.TimingConvention
import org.junit.Test
import java.time.LocalDate

/** Regression: computing must work whatever day the app is first launched on. */
class AnyStartDateTest {
    @Test fun everyStartDayOfAYear() {
        val engine = TamilCalendarEngine(GeoLocation.NAPERVILLE, TimingConvention.SUNRISE_BASED)
        var d = LocalDate.of(2026, 1, 1)
        while (d.year == 2026) {
            engine.compute(d, d)
            d = d.plusDays(1)
        }
    }
}
