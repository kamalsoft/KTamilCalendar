package com.kamalsoft.tamilcalendar.tv.domain.usecase

import com.kamalsoft.tamilcalendar.tv.core.astro.Astro
import com.kamalsoft.tamilcalendar.tv.core.astro.Planets
import com.kamalsoft.tamilcalendar.tv.domain.content.Gochara
import com.kamalsoft.tamilcalendar.tv.domain.model.RasiOutlook
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilNames
import java.time.LocalDate
import java.time.ZoneId

/** Rasi (0 = Mesham) each graha occupies on a date (local noon). */
data class SkyPositions(val sun: Int, val moon: Int, val jupiter: Int, val saturn: Int, val rahu: Int, val ketu: Int) {
    /** Short Tamil graha labels for planets in [rasi], for the chart. */
    fun labelsIn(rasi: Int): List<String> = buildList {
        if (sun == rasi) add("சூ")
        if (moon == rasi) add("சந்")
        if (jupiter == rasi) add("கு")
        if (saturn == rasi) add("ச")
        if (rahu == rasi) add("ரா")
        if (ketu == rasi) add("கே")
    }
}

data class RasiForecast(
    val rasi: Int,
    val monthTa: String, val monthEn: String, val monthOutlook: RasiOutlook,
    val yearTa: String, val yearEn: String, val yearOutlook: RasiOutlook,
)

/** Rule-based monthly (Sun + Jupiter) and yearly (Jupiter, Saturn, Rahu/Ketu) gochara forecasts. */
object TransitForecast {
    private fun rasiOf(longitude: Double) = (longitude / 30).toInt() % 12

    fun positions(date: LocalDate, zone: ZoneId): SkyPositions {
        val jd = Astro.julianDay(date.atTime(12, 0).atZone(zone).toInstant())
        return SkyPositions(
            sun = rasiOf(Astro.sunSidereal(jd)), moon = rasiOf(Astro.moonSidereal(jd)),
            jupiter = rasiOf(Planets.jupiterSidereal(jd)), saturn = rasiOf(Planets.saturnSidereal(jd)),
            rahu = rasiOf(Planets.rahuSidereal(jd)), ketu = rasiOf(Planets.ketuSidereal(jd)),
        )
    }

    private fun house(planet: Int, rasi: Int) = Math.floorMod(planet - rasi, 12) + 1

    fun forecast(rasi: Int, sky: SkyPositions): RasiForecast {
        val sunH = house(sky.sun, rasi)
        val jupH = house(sky.jupiter, rasi)
        val satH = house(sky.saturn, rasi)
        val rahH = house(sky.rahu, rasi)
        val sun = Gochara.SUN[sunH - 1]
        val jup = Gochara.JUPITER[jupH - 1]
        val sat = Gochara.SATURN[satH - 1]
        val rahu = if (Gochara.rahuGood(rahH)) Gochara.RAHU_GOOD else Gochara.RAHU_BAD
        val sunIn = TamilNames.rasis[sky.sun]
        return RasiForecast(
            rasi = rasi,
            monthTa = "சூரியன் ${sunIn.first} ராசியில் (${sunH}-ஆம் இடம்): ${sun.ta}\nகுரு ${jupH}-ஆம் இடம்: ${jup.ta}",
            monthEn = "Sun in ${sunIn.second} (house $sunH): ${sun.en}\nJupiter in house $jupH: ${jup.en}",
            monthOutlook = Gochara.outlook(listOf(Gochara.sunGood(sunH), Gochara.jupiterGood(jupH)).count { it }, 2),
            yearTa = "குரு ${jupH}-ஆம் இடம்: ${jup.ta}\nசனி ${satH}-ஆம் இடம்: ${sat.ta}\n${rahu.ta}",
            yearEn = "Jupiter in house $jupH: ${jup.en}\nSaturn in house $satH: ${sat.en}\n${rahu.en}",
            yearOutlook = Gochara.outlook(listOf(Gochara.jupiterGood(jupH), Gochara.saturnGood(satH), Gochara.rahuGood(rahH)).count { it }, 3),
        )
    }
}
