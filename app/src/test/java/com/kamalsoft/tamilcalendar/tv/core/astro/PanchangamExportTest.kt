package com.kamalsoft.tamilcalendar.tv.core.astro

import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.domain.model.TimingConvention
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Writes a year of output for tools/verify_panchangam.py. Skipped unless -DexportPath=… is given. */
class PanchangamExportTest {
    @Test fun export() {
        val path = System.getProperty("exportPath")
        assumeTrue("set -DexportPath to run", !path.isNullOrBlank())
        val from = LocalDate.now()
        val days = TamilCalendarEngine(GeoLocation.NAPERVILLE, TimingConvention.SUNRISE_BASED).compute(from, from.plusDays(364))
        File(path!!).printWriter().use { w ->
            w.println("date,sunrise,sunset,tithi,tithiEnd,nak,nakEnd,yoga,yogaEnd,tmonth,tday")
            days.forEach { d ->
                val p = d.panchangam
                w.println(
                    listOf(
                        d.date, d.sunrise, d.sunset, p.tithi.index, p.tithi.endsAt, p.nakshatram.index, p.nakshatram.endsAt,
                        p.yogam.index, p.yogam.endsAt, d.tamilDate.monthIndex, d.tamilDate.day,
                    ).joinToString(","),
                )
            }
        }
    }
}
