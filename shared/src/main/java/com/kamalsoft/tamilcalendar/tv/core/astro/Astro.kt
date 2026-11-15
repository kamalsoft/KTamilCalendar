package com.kamalsoft.tamilcalendar.tv.core.astro

import java.time.Instant
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * Low-cost positional astronomy, accurate enough for panchangam work on TV hardware.
 *
 * - Sun: Meeus, Astronomical Algorithms ch. 25 (≈0.01°).
 * - Moon: Meeus ch. 47, the full 60-term longitude series (≈0.003°).
 * - Ayanamsa: Lahiri (Chitrapaksha), linear+quadratic precession from J2000.
 *
 * A 0.01° error in the Moon–Sun elongation shifts a tithi end by about 1 minute,
 * and a 0.01° solar error shifts a Tamil month start (sankranti) by about 15 minutes.
 */
object Astro {

    private const val RAD = PI / 180.0
    private const val J2000 = 2451545.0
    /** ΔT = TT − UT, ≈69 s for 2024–2028. */
    private const val DELTA_T_DAYS = 69.2 / 86400.0

    fun julianDay(instant: Instant): Double = instant.toEpochMilli() / 86_400_000.0 + 2440587.5

    fun instantOf(jd: Double): Instant = Instant.ofEpochMilli(((jd - 2440587.5) * 86_400_000.0).roundToLong())

    fun norm360(x: Double): Double {
        val r = x % 360.0
        return if (r < 0) r + 360.0 else r
    }

    private fun norm180(x: Double): Double {
        val r = norm360(x)
        return if (r > 180.0) r - 360.0 else r
    }

    private fun centuries(jdUt: Double) = (jdUt + DELTA_T_DAYS - J2000) / 36525.0

    private fun nutationLongitude(t: Double): Double {
        val omega = 125.04452 - 1934.136261 * t
        val lSun = 280.4665 + 36000.7698 * t
        val lMoon = 218.3165 + 481267.8813 * t
        return (-17.20 * sin(omega * RAD) - 1.32 * sin(2 * lSun * RAD) -
            0.23 * sin(2 * lMoon * RAD) + 0.21 * sin(2 * omega * RAD)) / 3600.0
    }

    /** Apparent tropical longitude of the Sun, degrees. */
    fun sunLongitude(jd: Double): Double {
        val t = centuries(jd)
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = (357.52911 + 35999.05029 * t - 0.0001537 * t * t) * RAD
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(m) +
            (0.019993 - 0.000101 * t) * sin(2 * m) + 0.000289 * sin(3 * m)
        // −20.4898" annual aberration.
        return norm360(l0 + c - 0.005691611 + nutationLongitude(t))
    }

    /** Apparent tropical longitude of the Moon, degrees. */
    fun moonLongitude(jd: Double): Double {
        val t = centuries(jd)
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t
        val lp = 218.3164477 + 481267.88123421 * t - 0.0015786 * t2 + t3 / 538841.0 - t4 / 65194000.0
        val d = 297.8501921 + 445267.1114034 * t - 0.0018819 * t2 + t3 / 545868.0 - t4 / 113065000.0
        val m = 357.5291092 + 35999.0502909 * t - 0.0001536 * t2 + t3 / 24490000.0
        val mp = 134.9633964 + 477198.8675055 * t + 0.0087414 * t2 + t3 / 69699.0 - t4 / 14712000.0
        val f = 93.2720950 + 483202.0175233 * t - 0.0036539 * t2 - t3 / 3526000.0 + t4 / 863310000.0
        val e = 1.0 - 0.002516 * t - 0.0000074 * t2

        var sum = 0.0
        var i = 0
        while (i < MOON_TERMS.size) {
            val cd = MOON_TERMS[i]
            val cm = MOON_TERMS[i + 1]
            val cmp = MOON_TERMS[i + 2]
            val cf = MOON_TERMS[i + 3]
            val coeff = MOON_TERMS[i + 4]
            val arg = (cd * d + cm * m + cmp * mp + cf * f) * RAD
            val eFactor = when (kotlin.math.abs(cm)) {
                1 -> e
                2 -> e * e
                else -> 1.0
            }
            sum += coeff * eFactor * sin(arg)
            i += 5
        }
        val a1 = (119.75 + 131.849 * t) * RAD
        val a2 = (53.09 + 479264.290 * t) * RAD
        sum += 3958.0 * sin(a1) + 1962.0 * sin((lp - f) * RAD) + 318.0 * sin(a2)

        return norm360(lp + sum / 1_000_000.0 + nutationLongitude(t))
    }

    /** Lahiri ayanamsa, degrees (≈24.2° in 2026). */
    fun ayanamsa(jd: Double): Double {
        val t = centuries(jd)
        return 23.857092 + 1.396971 * t + 0.000308 * t * t
    }

    fun sunSidereal(jd: Double): Double = norm360(sunLongitude(jd) - ayanamsa(jd))

    fun moonSidereal(jd: Double): Double = norm360(moonLongitude(jd) - ayanamsa(jd))

    /** Moon − Sun, 0..360. Tithi = floor(elongation / 12). */
    fun elongation(jd: Double): Double = norm360(moonLongitude(jd) - sunLongitude(jd))

    /** Sidereal Sun + Moon, 0..360. Yogam = floor(sum / (360/27)). */
    fun yogaSum(jd: Double): Double = norm360(sunSidereal(jd) + moonSidereal(jd))

    /**
     * First instant in [fromJd, fromJd + maxDays] at which the monotonically increasing angle
     * [angle] reaches [target] (mod 360). Returns the JD or null if not reached.
     */
    fun findCrossing(fromJd: Double, maxDays: Double, stepDays: Double, target: Double, angle: (Double) -> Double): Double? {
        var t0 = fromJd
        var g0 = norm180(angle(t0) - target)
        if (g0 == 0.0) return t0
        val end = fromJd + maxDays
        while (t0 < end) {
            val t1 = minOf(t0 + stepDays, end)
            val g1 = norm180(angle(t1) - target)
            if (g0 < 0 && g1 >= 0 && g1 - g0 < 180.0) {
                var lo = t0
                var hi = t1
                repeat(40) {
                    val mid = (lo + hi) / 2
                    if (norm180(angle(mid) - target) < 0) lo = mid else hi = mid
                }
                return (lo + hi) / 2
            }
            t0 = t1
            g0 = g1
        }
        return null
    }

    /**
     * Sunrise and sunset (upper limb, standard refraction) as JD (UT) for the local civil [date].
     * "Sunrise equation" formulation; ≈1 minute accuracy at Indian latitudes.
     */
    fun sunriseSunset(date: LocalDate, latitude: Double, longitude: Double): Pair<Double, Double> {
        val n = (date.toEpochDay() + 2440588.0 - J2000)
        val jStar = n - longitude / 360.0
        val m = norm360(357.5291 + 0.98560028 * jStar)
        val mr = m * RAD
        val c = 1.9148 * sin(mr) + 0.0200 * sin(2 * mr) + 0.0003 * sin(3 * mr)
        val lambda = norm360(m + c + 180.0 + 102.9372) * RAD
        val transit = J2000 + jStar + 0.0053 * sin(mr) - 0.0069 * sin(2 * lambda)
        val decl = asin(sin(lambda) * sin(23.4397 * RAD))
        val phi = latitude * RAD
        val cosW = (sin(-0.833 * RAD) - sin(phi) * sin(decl)) / (cos(phi) * cos(decl))
        val w = acos(cosW.coerceIn(-1.0, 1.0)) / RAD
        return (transit - w / 360.0) to (transit + w / 360.0)
    }

    fun floorDiv(angle: Double, span: Double): Int = floor(angle / span).toInt()

    /** Meeus Table 47.A (longitude): D, M, M', F, Σl coefficient (1e-6 degrees). */
    private val MOON_TERMS = intArrayOf(
        0, 0, 1, 0, 6288774, 2, 0, -1, 0, 1274027, 2, 0, 0, 0, 658314, 0, 0, 2, 0, 213618,
        0, 1, 0, 0, -185116, 0, 0, 0, 2, -114332, 2, 0, -2, 0, 58793, 2, -1, -1, 0, 57066,
        2, 0, 1, 0, 53322, 2, -1, 0, 0, 45758, 0, 1, -1, 0, -40923, 1, 0, 0, 0, -34720,
        0, 1, 1, 0, -30383, 2, 0, 0, -2, 15327, 0, 0, 1, 2, -12528, 0, 0, 1, -2, 10980,
        4, 0, -1, 0, 10675, 0, 0, 3, 0, 10034, 4, 0, -2, 0, 8548, 2, 1, -1, 0, -7888,
        2, 1, 0, 0, -6766, 1, 0, -1, 0, -5163, 1, 1, 0, 0, 4987, 2, -1, 1, 0, 4036,
        2, 0, 2, 0, 3994, 4, 0, 0, 0, 3861, 2, 0, -3, 0, 3665, 0, 1, -2, 0, -2689,
        2, 0, -1, 2, -2602, 2, -1, -2, 0, 2390, 1, 0, 1, 0, -2348, 2, -2, 0, 0, 2236,
        0, 1, 2, 0, -2120, 0, 2, 0, 0, -2069, 2, -2, -1, 0, 2048, 2, 0, 1, -2, -1773,
        2, 0, 0, 2, -1595, 4, -1, -1, 0, 1215, 0, 0, 2, 2, -1110, 3, 0, -1, 0, -892,
        2, 1, 1, 0, -810, 4, -1, -2, 0, 759, 0, 2, -1, 0, -713, 2, 2, -1, 0, -700,
        2, 1, -2, 0, 691, 2, -1, 0, -2, 596, 4, 0, 1, 0, 549, 0, 0, 4, 0, 537,
        4, -1, 0, 0, 520, 1, 0, -2, 0, -487, 2, 1, 0, -2, -399, 0, 0, 2, -2, -381,
        1, 1, 1, 0, 351, 3, 0, -2, 0, -340, 4, 0, -3, 0, 330, 2, -1, 2, 0, 327,
        0, 2, 1, 0, -323, 1, 1, -1, 0, 299, 2, 0, 3, 0, 294,
    )
}
