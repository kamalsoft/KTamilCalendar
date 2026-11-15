package com.kamalsoft.tamilcalendar.tv.core.astro

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Sidereal (Lahiri) longitudes of the slow planets used for monthly/yearly Rasi Palan (gochara).
 * Jupiter and Saturn from JPL "Keplerian Elements for Approximate Positions of the Major Planets"
 * (valid 1800–2050, error well under a degree — ample for which rasi a planet occupies).
 * Rahu = mean lunar node; Ketu = opposite.
 */
object Planets {
    private const val RAD = PI / 180

    private class Elements(val a: Double, val aR: Double, val e: Double, val eR: Double, val i: Double, val iR: Double,
                           val l: Double, val lR: Double, val w: Double, val wR: Double, val o: Double, val oR: Double)

    private val EARTH = Elements(1.00000261, 0.00000562, 0.01671123, -0.00004392, -0.00001531, -0.01294668,
        100.46457166, 35999.37244981, 102.93768193, 0.32327364, 0.0, 0.0)
    private val JUPITER = Elements(5.20288700, -0.00011607, 0.04838624, -0.00013253, 1.30439695, -0.00183714,
        34.39644051, 3034.74612775, 14.72847983, 0.21252668, 100.47390909, 0.20469106)
    private val SATURN = Elements(9.53667594, -0.00125060, 0.05386179, -0.00050991, 2.48599187, 0.00193609,
        49.95424423, 1222.49362201, 92.59887831, -0.41897216, 113.66242448, -0.28867794)

    /** Heliocentric ecliptic x, y, z (AU) at T Julian centuries from J2000. */
    private fun helio(el: Elements, t: Double): DoubleArray {
        val a = el.a + el.aR * t
        val e = el.e + el.eR * t
        val i = (el.i + el.iR * t) * RAD
        val l = el.l + el.lR * t
        val w = el.w + el.wR * t
        val o = (el.o + el.oR * t) * RAD
        val argP = (w * RAD) - o
        var m = ((l - w) % 360 + 360) % 360 * RAD
        if (m > PI) m -= 2 * PI
        var ea = m + e * sin(m)
        repeat(8) { ea -= (ea - e * sin(ea) - m) / (1 - e * cos(ea)) }
        val xp = a * (cos(ea) - e)
        val yp = a * sqrt(1 - e * e) * sin(ea)
        val cw = cos(argP); val sw = sin(argP); val co = cos(o); val so = sin(o); val ci = cos(i); val si = sin(i)
        return doubleArrayOf(
            (cw * co - sw * so * ci) * xp + (-sw * co - cw * so * ci) * yp,
            (cw * so + sw * co * ci) * xp + (-sw * so + cw * co * ci) * yp,
            (sw * si) * xp + (cw * si) * yp,
        )
    }

    private fun geocentricTropical(el: Elements, jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val p = helio(el, t)
        val e = helio(EARTH, t)
        return Astro.norm360(atan2(p[1] - e[1], p[0] - e[0]) / RAD)
    }

    fun jupiterSidereal(jd: Double) = Astro.norm360(geocentricTropical(JUPITER, jd) - Astro.ayanamsa(jd))
    fun saturnSidereal(jd: Double) = Astro.norm360(geocentricTropical(SATURN, jd) - Astro.ayanamsa(jd))

    /** Mean ascending node of the Moon. */
    fun rahuSidereal(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        return Astro.norm360(125.0445479 - 1934.1362891 * t + 0.0020754 * t * t - Astro.ayanamsa(jd))
    }

    fun ketuSidereal(jd: Double) = Astro.norm360(rahuSidereal(jd) + 180)
}
