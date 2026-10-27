package com.kamalsoft.tamilcalendar.tv.core.astro

import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.Chandrashtamam
import com.kamalsoft.tamilcalendar.tv.domain.model.DayTag
import com.kamalsoft.tamilcalendar.tv.domain.model.Deity
import com.kamalsoft.tamilcalendar.tv.domain.model.DayTimings
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.EventSource
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.domain.model.HolidayRegion
import com.kamalsoft.tamilcalendar.tv.domain.model.Gowri
import com.kamalsoft.tamilcalendar.tv.domain.model.Paksha
import com.kamalsoft.tamilcalendar.tv.domain.model.PanchangaElement
import com.kamalsoft.tamilcalendar.tv.domain.model.Panchangam
import com.kamalsoft.tamilcalendar.tv.domain.model.Religion
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDate
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilNames
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilTime
import com.kamalsoft.tamilcalendar.tv.domain.model.TimingConvention
import com.kamalsoft.tamilcalendar.tv.domain.model.TimingType
import com.kamalsoft.tamilcalendar.tv.domain.model.TimingWindow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId

/**
 * Computes Tamil calendar + panchangam data from first principles (no bundled tables), so the
 * offline database can be generated on-device for any year.
 *
 * Conventions (documented in README):
 * - Tamil solar month starts on the civil day the Sun's sidereal sankranti falls, if it happens
 *   before that day's sunset; otherwise the next day.
 * - Tithi / nakshatram / yogam are "as at sunrise". Pradosham uses tithi at sunset.
 * - Lunar months are amanta (new moon to new moon), used only for lunar festival rules.
 */
class TamilCalendarEngine(
    private val location: GeoLocation = GeoLocation.CHENNAI,
    private val convention: TimingConvention = TimingConvention.TRADITIONAL_6AM,
) {
    private val zone: ZoneId = ZoneId.of(location.zoneId)

    /**
     * Identifies the data this engine produces. Stored on every cached day so that changing the
     * location, time zone or timing convention (or the engine itself) regenerates the offline DB.
     */
    val dataVersion: Int = listOf(VERSION, location.latitude, location.longitude, location.zoneId, convention).hashCode()

    fun compute(from: LocalDate, to: LocalDate): List<TamilDayData> {
        require(!to.isBefore(from)) { "to < from" }
        val sky = SkyContext(from.minusDays(45), to.plusDays(45))
        val cores = HashMap<LocalDate, DayCore>()
        val core: (LocalDate) -> DayCore = { d -> cores.getOrPut(d) { DayCore(d, sky) } }

        val dates = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
        val events = computeFestivals(dates, core)
        return dates.map { buildDay(core(it), core(it.plusDays(1)), core(it.minusDays(1)), events[it].orEmpty()) }
    }

    // ---------------------------------------------------------------- per-day astronomy

    /** Instants a rule can be evaluated at. APARAHNA ≈ middle of the 4th fifth of daytime. */
    private enum class At { SUNRISE, SUNSET, NOON, APARAHNA, MOONRISE_K4, MIDNIGHT }

    private inner class DayCore(val date: LocalDate, val sky: SkyContext) {
        val sunriseJd: Double
        val sunsetJd: Double
        private val midnightJd: Double = Astro.julianDay(date.plusDays(1).atStartOfDay(zone).toInstant())
        private val tithiCache = IntArray(At.entries.size) { -1 }
        private val starCache = IntArray(At.entries.size) { -1 }

        init {
            val (rise, set) = Astro.sunriseSunset(date, location.latitude, location.longitude)
            sunriseJd = rise
            sunsetJd = set
        }

        fun jd(at: At): Double = when (at) {
            At.SUNRISE -> sunriseJd
            At.SUNSET -> sunsetJd
            At.NOON -> (sunriseJd + sunsetJd) / 2
            At.APARAHNA -> sunriseJd + 0.7 * (sunsetJd - sunriseJd)
            // On Krishna Chathurthi the Moon rises ~3¼ h after sunset; Sankatahara is kept by the tithi then.
            At.MOONRISE_K4 -> sunsetJd + 3.25 / 24.0
            At.MIDNIGHT -> midnightJd
        }

        fun tithi(at: At): Int {
            if (tithiCache[at.ordinal] < 0) tithiCache[at.ordinal] = Astro.floorDiv(Astro.elongation(jd(at)), 12.0) % 30
            return tithiCache[at.ordinal]
        }

        fun star(at: At): Int {
            if (starCache[at.ordinal] < 0) starCache[at.ordinal] = Astro.floorDiv(Astro.moonSidereal(jd(at)), NAK_SPAN) % 27
            return starCache[at.ordinal]
        }

        val tamilMonthStart: Pair<LocalDate, Int> by lazy { sky.monthStartFor(date) }
        val tamilMonth: Int get() = tamilMonthStart.second
        val tamilDay: Int get() = (date.toEpochDay() - tamilMonthStart.first.toEpochDay()).toInt() + 1

        fun lunarMonth(at: At): LunarMonth = sky.lunarMonthAt(jd(at))
    }

    private data class LunarMonth(val index: Int, val adhika: Boolean)

    private inner class SkyContext(from: LocalDate, to: LocalDate) {
        /** (civil start date, rasi index == Tamil month index). */
        private val monthStarts = ArrayList<Pair<LocalDate, Int>>()
        private val newMoons = ArrayList<Double>()
        private val newMoonSunRasi = ArrayList<Int>()

        init {
            val startJd = Astro.julianDay(from.atStartOfDay(zone).toInstant())
            val endJd = Astro.julianDay(to.atStartOfDay(zone).toInstant())

            // Start a full solar month early so a sankranti on or before `from` is always found.
            var jd = startJd - 35.0
            var rasi = Astro.floorDiv(Astro.sunSidereal(jd), 30.0)
            while (jd <= endJd) {
                val next = (rasi + 1) % 12
                val sankranti = Astro.findCrossing(jd, 40.0, 1.0, next * 30.0, Astro::sunSidereal) ?: break
                val civil = Astro.instantOf(sankranti).atZone(zone).toLocalDate()
                val sunset = Astro.sunriseSunset(civil, location.latitude, location.longitude).second
                monthStarts += (if (sankranti < sunset) civil else civil.plusDays(1)) to next
                rasi = next
                jd = sankranti + 1.0
            }

            jd = startJd - 30.0
            while (jd <= endJd + 30.0) {
                val nm = Astro.findCrossing(jd, 35.0, 1.0, 0.0, Astro::elongation) ?: break
                newMoons += nm
                newMoonSunRasi += Astro.floorDiv(Astro.sunSidereal(nm), 30.0)
                jd = nm + 1.0
            }
        }

        fun monthStartFor(date: LocalDate): Pair<LocalDate, Int> =
            monthStarts.lastOrNull { !it.first.isAfter(date) } ?: error("No sankranti before $date")

        fun lunarMonthAt(jd: Double): LunarMonth {
            var i = newMoons.indexOfLast { it <= jd }
            if (i < 0) i = 0
            val rasi = newMoonSunRasi[i]
            val adhika = i + 1 < newMoonSunRasi.size && newMoonSunRasi[i + 1] == rasi
            return LunarMonth((rasi + 1) % 12, adhika)
        }
    }

    // ---------------------------------------------------------------- day assembly

    private fun buildDay(c: DayCore, next: DayCore, prev: DayCore, events: List<CalendarEvent>): TamilDayData {
        val sunrise = localTime(c.sunriseJd)
        val sunset = localTime(c.sunsetJd)

        val tithiIdx = c.tithi(At.SUNRISE)
        val starIdx = c.star(At.SUNRISE)
        val yogaIdx = Astro.floorDiv(Astro.yogaSum(c.sunriseJd), NAK_SPAN) % 27
        val moonRasi = Astro.floorDiv(Astro.moonSidereal(c.sunriseJd), 30.0)

        val tithiNames = Panchangam.tithiNames(tithiIdx)
        val panchangam = Panchangam(
            tithi = PanchangaElement(
                tithiIdx, tithiNames.first, tithiNames.second,
                endOf(c.sunriseJd, ((tithiIdx + 1) % 30) * 12.0, Astro::elongation),
            ),
            nakshatram = PanchangaElement(
                starIdx, TamilNames.nakshatras[starIdx].first, TamilNames.nakshatras[starIdx].second,
                endOf(c.sunriseJd, ((starIdx + 1) % 27) * NAK_SPAN, Astro::moonSidereal),
            ),
            yogam = PanchangaElement(
                yogaIdx, TamilNames.yogas[yogaIdx].first, TamilNames.yogas[yogaIdx].second,
                endOf(c.sunriseJd, ((yogaIdx + 1) % 27) * NAK_SPAN, Astro::yogaSum),
            ),
            paksha = if (tithiIdx < 15) Paksha.SHUKLA else Paksha.KRISHNA,
            moonRasi = moonRasi,
            moonRasiEndsAt = endOf(c.sunriseJd, ((moonRasi + 1) % 12) * 30.0, Astro::moonSidereal, maxDays = 3.0),
            sunRasi = Astro.floorDiv(Astro.sunSidereal(c.sunriseJd), 30.0),
        )

        val timings = timingsFor(c.date, sunrise, sunset)

        val tags = mutableSetOf<DayTag>()
        if (observed(c, next, prev, 14, At.SUNRISE)) tags += DayTag.POURNAMI
        if (observed(c, next, prev, 29, At.SUNRISE)) tags += DayTag.AMAVASAI
        if (observed(c, next, prev, 10, At.SUNRISE) || observed(c, next, prev, 25, At.SUNRISE)) tags += DayTag.EKADASI
        if (observed(c, next, prev, 12, At.SUNSET) || observed(c, next, prev, 27, At.SUNSET)) tags += DayTag.PRADOSHAM
        if (starObserved(c, next, prev, 2, At.SUNRISE)) tags += DayTag.KRITHIGAI
        // Valarpirai Sashti viratham is kept on the day Sashti prevails at sunset (as for Soorasamharam).
        if (observed(c, next, prev, 5, At.SUNSET)) tags += DayTag.SASHTI
        if (observed(c, next, prev, 28, At.MIDNIGHT)) tags += DayTag.MASA_SIVARATRI
        if (observed(c, next, prev, 18, At.MOONRISE_K4)) tags += DayTag.SANKATAHARA
        if (starObserved(c, next, prev, 21, At.SUNRISE)) tags += DayTag.THIRUVONAM
        val weekday = c.date.dayOfWeek.value % 7
        if (c.tamilMonth == 5 && weekday == 6) tags += DayTag.PURATTASI_SANI
        if (c.tamilMonth == 3 && weekday == 5) tags += DayTag.AADI_VELLI

        val allEvents = events.toMutableList()
        if (isMuhurthamCandidate(c, tithiIdx, starIdx)) {
            tags += DayTag.SUBHA_MUHURTHAM
            val window = timings.nallaNeram.firstOrNull { it.start.hour < 12 } ?: timings.nallaNeram.firstOrNull()
            allEvents += CalendarEvent(
                date = c.date, kind = EventKind.MUHURTHAM,
                titleTa = "சுப முகூர்த்தம் (${panchangam.paksha.ta})", titleEn = "Subha Muhurtham",
                isHoliday = false, source = EventSource.COMPUTED,
                detail = window?.let { TamilTime.range(it.start, it.end) },
            )
        }

        val chandrashtamaRasi = (moonRasi + 5) % 12
        return TamilDayData(
            date = c.date,
            tamilDate = tamilDate(c),
            sunrise = sunrise,
            sunset = sunset,
            panchangam = panchangam,
            timings = timings,
            chandrashtamam = Chandrashtamam(
                rasi = chandrashtamaRasi,
                nakshatras = nakshatrasIn(chandrashtamaRasi),
                until = panchangam.moonRasiEndsAt,
            ),
            tags = tags,
            events = allEvents,
        )
    }

    private fun tamilDate(c: DayCore): TamilDate {
        val m = c.tamilMonth
        // Gregorian year in which this Tamil year's Chithirai 1 fell.
        val startYear = if (m >= 9 || (m == 8 && c.date.monthValue <= 2)) c.date.year - 1 else c.date.year
        return TamilDate(
            monthIndex = m,
            day = c.tamilDay,
            yearCycleIndex = Math.floorMod(startYear - 1987, 60),
            // Thiruvalluvar Aandu increments on Thai 1.
            thiruvalluvarAandu = startYear + 31 + if (m >= 9) 1 else 0,
        )
    }

    /** True when tithi [x] is the one "observed" on [c] at [at], handling kshaya (skipped) and vriddhi (repeated) tithis. */
    private fun observed(c: DayCore, next: DayCore, prev: DayCore, x: Int, at: At): Boolean {
        fun matches(a: DayCore, b: DayCore): Boolean {
            val t0 = a.tithi(at)
            return t0 == x || (t0 == (x + 29) % 30 && b.tithi(at) == (x + 1) % 30)
        }
        return matches(c, next) && prev.tithi(at) != x
    }

    private fun starObserved(c: DayCore, next: DayCore, prev: DayCore, n: Int, at: At): Boolean {
        val s0 = c.star(at)
        val hit = s0 == n || (s0 == (n + 26) % 27 && next.star(at) == (n + 1) % 27)
        return hit && prev.star(at) != n
    }

    private fun endOf(fromJd: Double, target: Double, angle: (Double) -> Double, maxDays: Double = 2.0): LocalDateTime? =
        Astro.findCrossing(fromJd, maxDays, 0.25, target, angle)?.let {
            LocalDateTime.ofInstant(Astro.instantOf(it), zone)
        }

    private fun localTime(jd: Double): LocalTime =
        Astro.instantOf(jd).atZone(zone).toLocalTime().withNano(0)

    // ---------------------------------------------------------------- timing windows

    private fun timingsFor(date: LocalDate, sunrise: LocalTime, sunset: LocalTime): DayTimings {
        val (dayStart, part) = when (convention) {
            TimingConvention.TRADITIONAL_6AM -> 6 * 3600 to 90 * 60
            TimingConvention.SUNRISE_BASED -> sunrise.toSecondOfDay() to (sunset.toSecondOfDay() - sunrise.toSecondOfDay()) / 8
        }
        val w = date.dayOfWeek.value % 7 // Sunday = 0
        fun segment(type: TimingType, n: Int, gowri: Gowri? = null): TimingWindow {
            val s = dayStart + (n - 1) * part
            return TimingWindow(type, LocalTime.ofSecondOfDay(s.toLong()), LocalTime.ofSecondOfDay((s + part).toLong()), gowri)
        }

        val rahu = segment(TimingType.RAHU_KALAM, RAHU_SEGMENT[w])
        val yama = segment(TimingType.YAMAGANDAM, YAMA_SEGMENT[w])
        val kuligai = segment(TimingType.KULIGAI, KULIGAI_SEGMENT[w])

        val gowriAll = (1..8).map { n ->
            segment(TimingType.GOWRI_NALLA_NERAM, n, GOWRI_CYCLE[(w + n - 1) % 8])
        }
        val gowriGood = gowriAll.filter { it.gowri!!.isGood }

        // Nalla Neram = good Gowri periods, with Rahu Kalam and Yamagandam cut out, merged.
        val blocked = listOf(rahu, yama).map { it.start.toSecondOfDay() to it.end.toSecondOfDay() }
        val free = mutableListOf<Pair<Int, Int>>()
        for (g in gowriGood) {
            var pieces = listOf(g.start.toSecondOfDay() to g.end.toSecondOfDay())
            for ((bs, be) in blocked) {
                pieces = pieces.flatMap { (s, e) ->
                    if (be <= s || bs >= e) listOf(s to e)
                    else listOfNotNull((s to bs).takeIf { bs > s }, (be to e).takeIf { be < e })
                }
            }
            for (p in pieces) {
                val last = free.lastOrNull()
                if (last != null && last.second == p.first) free[free.lastIndex] = last.first to p.second else free += p
            }
        }
        val nalla = free.filter { it.second - it.first >= 30 * 60 }.map { (s, e) ->
            TimingWindow(TimingType.NALLA_NERAM, LocalTime.ofSecondOfDay(s.toLong()), LocalTime.ofSecondOfDay(e.toLong()))
        }
        return DayTimings(nalla, gowriGood, rahu, yama, kuligai)
    }

    private fun nakshatrasIn(rasi: Int): List<Int> {
        val lo = rasi * 30.0
        val hi = lo + 30.0
        return (0 until 27).filter { k -> k * NAK_SPAN < hi - 1e-6 && (k + 1) * NAK_SPAN > lo + 1e-6 }
    }

    // ---------------------------------------------------------------- muhurtham

    private fun isMuhurthamCandidate(c: DayCore, tithi: Int, star: Int): Boolean {
        if (c.tamilMonth in AVOIDED_MUHURTHAM_MONTHS) return false
        if (c.lunarMonth(At.SUNRISE).adhika) return false
        val w = c.date.dayOfWeek.value % 7
        if (w == 2 || w == 6) return false // Tuesday, Saturday
        val tithiOk = if (tithi < 15) tithi in SHUKLA_MUHURTHAM_TITHIS else (tithi - 15) in KRISHNA_MUHURTHAM_TITHIS
        return tithiOk && star in MUHURTHAM_STARS
    }

    // ---------------------------------------------------------------- festivals & holidays

    private class FestivalRule(
        val titleTa: String,
        val titleEn: String,
        val kind: EventKind,
        /** Public holiday in Tamil Nadu (shown as such; whether it reddens the day is decided by region config). */
        val isHoliday: Boolean,
        val deity: Deity? = null,
        val religion: Religion? = if (kind == EventKind.FESTIVAL || kind == EventKind.VIRATHAM) Religion.HINDU else null,
        val matches: (date: LocalDate) -> Boolean,
    )

    private fun computeFestivals(dates: List<LocalDate>, core: (LocalDate) -> DayCore): Map<LocalDate, List<CalendarEvent>> {
        fun tithiObs(d: LocalDate, x: Int, at: At) = observed(core(d), core(d.plusDays(1)), core(d.minusDays(1)), x, at)
        fun starObs(d: LocalDate, n: Int, at: At) = starObserved(core(d), core(d.plusDays(1)), core(d.minusDays(1)), n, at)
        fun solar(d: LocalDate, month: Int, day: Int) = core(d).let { it.tamilMonth == month && it.tamilDay == day }
        fun inSolar(d: LocalDate, month: Int) = core(d).tamilMonth == month
        fun lunar(d: LocalDate, month: Int, at: At) = core(d).lunarMonth(at).let { it.index == month && !it.adhika }
        fun nearFullMoon(d: LocalDate, at: At) = core(d).tithi(at) in 11..16
        fun fixed(d: LocalDate, m: Month, day: Int) = d.month == m && d.dayOfMonth == day

        val rules = listOf(
            FestivalRule("தமிழ்ப் புத்தாண்டு", "Tamil New Year", EventKind.FESTIVAL, true) { solar(it, 0, 1) },
            FestivalRule("போகி", "Bhogi", EventKind.FESTIVAL, false) { solar(it.plusDays(1), 9, 1) },
            FestivalRule("தைப் பொங்கல்", "Thai Pongal", EventKind.FESTIVAL, true) { solar(it, 9, 1) },
            FestivalRule("திருவள்ளுவர் தினம்", "Thiruvalluvar Day", EventKind.GOVT_HOLIDAY, true) { solar(it, 9, 2) },
            FestivalRule("உழவர் திருநாள்", "Uzhavar Thirunal", EventKind.GOVT_HOLIDAY, true) { solar(it, 9, 3) },
            FestivalRule("ஆடிப் பெருக்கு", "Aadi Perukku", EventKind.FESTIVAL, false, Deity.AMMAN) { solar(it, 3, 18) },
            FestivalRule("தைப்பூசம்", "Thaipusam", EventKind.FESTIVAL, false, Deity.MURUGAN) {
                inSolar(it, 9) && starObs(it, 7, At.SUNRISE) && nearFullMoon(it, At.SUNRISE)
            },
            FestivalRule("பங்குனி உத்திரம்", "Panguni Uthiram", EventKind.FESTIVAL, false, Deity.MURUGAN) {
                inSolar(it, 11) && starObs(it, 11, At.SUNRISE) && nearFullMoon(it, At.SUNRISE)
            },
            FestivalRule("வைகாசி விசாகம்", "Vaikasi Visakam", EventKind.FESTIVAL, false, Deity.MURUGAN) {
                inSolar(it, 1) && starObs(it, 15, At.SUNRISE) && nearFullMoon(it, At.SUNRISE)
            },
            FestivalRule("கார்த்திகை தீபம்", "Karthigai Deepam", EventKind.FESTIVAL, false, Deity.SHIVA) {
                // Krittika on or before Pournami (Shukla paksha), never the waning day after it.
                inSolar(it, 7) && starObs(it, 2, At.SUNSET) && core(it).tithi(At.SUNSET) in 9..14
            },
            FestivalRule("வைகுண்ட ஏகாதசி", "Vaikunta Ekadasi", EventKind.VIRATHAM, false, Deity.PERUMAL) {
                inSolar(it, 8) && tithiObs(it, 10, At.SUNRISE)
            },
            FestivalRule("ஆடி அமாவாசை", "Aadi Amavasai", EventKind.VIRATHAM, false) { inSolar(it, 3) && tithiObs(it, 29, At.SUNRISE) },
            FestivalRule("தை அமாவாசை", "Thai Amavasai", EventKind.VIRATHAM, false) { inSolar(it, 9) && tithiObs(it, 29, At.SUNRISE) },
            FestivalRule("மஹாளய அமாவாசை", "Mahalaya Amavasai", EventKind.VIRATHAM, false) {
                lunar(it, 5, At.SUNRISE) && tithiObs(it, 29, At.SUNRISE)
            },
            FestivalRule("யுகாதி", "Ugadi", EventKind.FESTIVAL, true) { lunar(it, 0, At.SUNRISE) && tithiObs(it, 0, At.SUNRISE) },
            FestivalRule("ஸ்ரீ ராம நவமி", "Sri Rama Navami", EventKind.FESTIVAL, false, Deity.PERUMAL) { lunar(it, 0, At.NOON) && tithiObs(it, 8, At.NOON) },
            FestivalRule("கிருஷ்ண ஜெயந்தி", "Krishna Jayanthi", EventKind.FESTIVAL, true, Deity.PERUMAL) {
                lunar(it, 4, At.MIDNIGHT) && tithiObs(it, 22, At.MIDNIGHT)
            },
            FestivalRule("விநாயகர் சதுர்த்தி", "Vinayagar Chathurthi", EventKind.FESTIVAL, true, Deity.VINAYAGAR) {
                lunar(it, 5, At.NOON) && tithiObs(it, 3, At.NOON)
            },
            FestivalRule("ஆயுத பூஜை", "Ayutha Pooja", EventKind.FESTIVAL, true, Deity.AMMAN) { lunar(it, 6, At.APARAHNA) && tithiObs(it, 8, At.APARAHNA) },
            FestivalRule("விஜயதசமி", "Vijayadasami", EventKind.FESTIVAL, true, Deity.AMMAN) { lunar(it, 6, At.APARAHNA) && tithiObs(it, 9, At.APARAHNA) },
            FestivalRule("தீபாவளி", "Deepavali", EventKind.FESTIVAL, true) { lunar(it, 6, At.SUNRISE) && tithiObs(it, 28, At.SUNRISE) },
            FestivalRule("மகா சிவராத்திரி", "Maha Shivaratri", EventKind.VIRATHAM, false, Deity.SHIVA) {
                lunar(it, 10, At.MIDNIGHT) && tithiObs(it, 28, At.MIDNIGHT)
            },
            // ---- more Tamil temple festivals
            FestivalRule("கந்த சஷ்டி · சூரசம்ஹாரம்", "Skanda Sashti (Soorasamharam)", EventKind.FESTIVAL, false, Deity.MURUGAN) {
                // Soorasamharam is held in the evening: Sashti must prevail at sunset.
                inSolar(it, 6) && tithiObs(it, 5, At.SUNSET)
            },
            FestivalRule("ஆருத்ரா தரிசனம்", "Arudra Darshanam", EventKind.FESTIVAL, false, Deity.SHIVA) {
                inSolar(it, 8) && starObs(it, 5, At.SUNRISE) && nearFullMoon(it, At.SUNRISE)
            },
            FestivalRule("மாசி மகம்", "Masi Magam", EventKind.FESTIVAL, false, Deity.SHIVA) {
                inSolar(it, 10) && starObs(it, 9, At.SUNRISE) && nearFullMoon(it, At.SUNRISE)
            },
            FestivalRule("ஆடிப் பூரம்", "Aadi Pooram", EventKind.FESTIVAL, false, Deity.AMMAN) {
                inSolar(it, 3) && starObs(it, 10, At.SUNRISE)
            },
            FestivalRule("சித்ரா பௌர்ணமி", "Chitra Pournami", EventKind.FESTIVAL, false, Deity.AMMAN) {
                inSolar(it, 0) && tithiObs(it, 14, At.SUNRISE)
            },
            FestivalRule("அனுமன் ஜெயந்தி", "Hanuman Jayanthi", EventKind.FESTIVAL, false, Deity.PERUMAL) {
                inSolar(it, 8) && tithiObs(it, 29, At.SUNRISE)
            },
            FestivalRule("நவராத்திரி ஆரம்பம்", "Navaratri begins", EventKind.FESTIVAL, false, Deity.AMMAN) {
                lunar(it, 6, At.SUNRISE) && tithiObs(it, 0, At.SUNRISE)
            },
            FestivalRule("ஆவணி அவிட்டம்", "Avani Avittam", EventKind.FESTIVAL, false) {
                inSolar(it, 4) && starObs(it, 22, At.SUNRISE) && nearFullMoon(it, At.SUNRISE)
            },
            FestivalRule("ஓணம்", "Onam", EventKind.FESTIVAL, false, Deity.PERUMAL) {
                inSolar(it, 4) && starObs(it, 21, At.SUNRISE)
            },
            FestivalRule("அட்சய திருதியை", "Akshaya Tritiya", EventKind.FESTIVAL, false) {
                lunar(it, 1, At.SUNRISE) && tithiObs(it, 2, At.SUNRISE)
            },
            FestivalRule("ஹோலி", "Holi", EventKind.FESTIVAL, false) {
                lunar(it, 11, At.SUNRISE) && tithiObs(it, 15, At.SUNRISE)
            },
            FestivalRule("ரக்ஷா பந்தன்", "Raksha Bandhan", EventKind.FESTIVAL, false) {
                lunar(it, 4, At.APARAHNA) && tithiObs(it, 14, At.APARAHNA)
            },

            // ---- Jain (lunar dates, amanta months; Udaya tithi)
            FestivalRule("மகாவீர் ஜெயந்தி", "Mahavir Jayanti", EventKind.RELIGIOUS, false, religion = Religion.JAIN) {
                lunar(it, 0, At.SUNRISE) && tithiObs(it, 12, At.SUNRISE)
            },
            FestivalRule("பர்யுஷன் ஆரம்பம் (ஸ்வேதாம்பரர்)", "Paryushan begins (Shvetambara)", EventKind.RELIGIOUS, false, religion = Religion.JAIN) {
                lunar(it.plusDays(7), 5, At.SUNRISE) && tithiObs(it.plusDays(7), 3, At.SUNRISE)
            },
            FestivalRule("சம்வத்சரி", "Samvatsari", EventKind.RELIGIOUS, false, religion = Religion.JAIN) {
                lunar(it, 5, At.SUNRISE) && tithiObs(it, 3, At.SUNRISE)
            },
            FestivalRule("தச லட்சண பர்வம் ஆரம்பம் (திகம்பரர்)", "Das Lakshana Parva begins (Digambara)", EventKind.RELIGIOUS, false, religion = Religion.JAIN) {
                lunar(it, 5, At.SUNRISE) && tithiObs(it, 4, At.SUNRISE)
            },
            FestivalRule("மகாவீர் நிர்வாணம் (தீபாவளி)", "Mahavir Nirvana (Diwali)", EventKind.RELIGIOUS, false, religion = Religion.JAIN) {
                lunar(it, 6, At.SUNSET) && tithiObs(it, 29, At.SUNSET)
            },

            // ---- Sikh / Buddhist
            FestivalRule("வைசாகி", "Vaisakhi", EventKind.RELIGIOUS, false, religion = Religion.SIKH) { solar(it, 0, 1) },
            FestivalRule("குரு நானக் ஜெயந்தி", "Guru Nanak Gurpurab", EventKind.RELIGIOUS, false, religion = Religion.SIKH) {
                lunar(it, 7, At.SUNRISE) && tithiObs(it, 14, At.SUNRISE)
            },
            FestivalRule("புத்த பூர்ணிமா · வேசாக்", "Buddha Purnima (Vesak)", EventKind.RELIGIOUS, false, religion = Religion.BUDDHIST) {
                lunar(it, 1, At.SUNRISE) && tithiObs(it, 14, At.SUNRISE)
            },

            FestivalRule("ஆங்கிலப் புத்தாண்டு", "New Year's Day", EventKind.GOVT_HOLIDAY, true) { fixed(it, Month.JANUARY, 1) },
            FestivalRule("குடியரசு தினம்", "Republic Day", EventKind.GOVT_HOLIDAY, true) { fixed(it, Month.JANUARY, 26) },
            FestivalRule("அம்பேத்கர் ஜெயந்தி", "Ambedkar Jayanthi", EventKind.GOVT_HOLIDAY, true) { fixed(it, Month.APRIL, 14) },
            FestivalRule("மே தினம்", "May Day", EventKind.GOVT_HOLIDAY, true) { fixed(it, Month.MAY, 1) },
            FestivalRule("இந்திய சுதந்திர தினம்", "Independence Day (India)", EventKind.GOVT_HOLIDAY, true) { fixed(it, Month.AUGUST, 15) },
            FestivalRule("காந்தி ஜெயந்தி", "Gandhi Jayanthi", EventKind.GOVT_HOLIDAY, true) { fixed(it, Month.OCTOBER, 2) },
            FestivalRule("கிறிஸ்துமஸ்", "Christmas", EventKind.GOVT_HOLIDAY, true, religion = Religion.CHRISTIAN) { fixed(it, Month.DECEMBER, 25) },
            FestivalRule("புனித வெள்ளி", "Good Friday", EventKind.GOVT_HOLIDAY, true, religion = Religion.CHRISTIAN) { it == easter(it.year).minusDays(2) },
        )

        val out = HashMap<LocalDate, MutableList<CalendarEvent>>()
        val lastHit = HashMap<FestivalRule, LocalDate>()
        // Scan a little before the range so a rule matched just before `from` still suppresses duplicates.
        val scan = generateSequence(dates.first().minusDays(25)) { it.plusDays(1) }.takeWhile { !it.isAfter(dates.last()) }
        for (d in scan) for (rule in rules) {
            if (!rule.matches(d)) continue
            val prev = lastHit[rule]
            lastHit[rule] = d
            if (prev != null && d.toEpochDay() - prev.toEpochDay() < 25) continue
            if (d.isBefore(dates.first())) continue
            out.getOrPut(d) { mutableListOf() } += CalendarEvent(
                date = d, kind = rule.kind, titleTa = rule.titleTa, titleEn = rule.titleEn,
                isHoliday = rule.isHoliday, source = EventSource.COMPUTED,
                region = if (rule.isHoliday) HolidayRegion.TAMIL_NADU else null,
                religion = rule.religion, deity = rule.deity,
            )
        }
        return out
    }

    /** Anonymous Gregorian computus. */
    private fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    companion object {
        /** Engine output version; bump to force the offline DB to regenerate. */
        const val VERSION = 3

        private const val NAK_SPAN = 360.0 / 27.0

        // 1-based 1/8th-of-day segments, indexed Sunday..Saturday.
        private val RAHU_SEGMENT = intArrayOf(8, 2, 7, 5, 6, 4, 3)
        private val YAMA_SEGMENT = intArrayOf(5, 4, 3, 2, 1, 7, 6)
        private val KULIGAI_SEGMENT = intArrayOf(7, 6, 5, 4, 3, 2, 1)

        /** Daytime Gowri sequence; Sunday starts at index 0, each later weekday shifts by one. */
        private val GOWRI_CYCLE = listOf(
            Gowri.UTHI, Gowri.AMIRDHAM, Gowri.ROGAM, Gowri.LAABAM,
            Gowri.DHANAM, Gowri.SUGAM, Gowri.SORAM, Gowri.VISHAM,
        )

        /** Aadi, Purattasi, Margazhi — traditionally avoided for weddings in Tamil Nadu. */
        private val AVOIDED_MUHURTHAM_MONTHS = setOf(3, 5, 8)
        // 0-based within paksha: Dwitiyai, Tritiyai, Panchami, Sapthami, Dasami, Ekadasi, Dwadasi, Thrayodasi.
        private val SHUKLA_MUHURTHAM_TITHIS = setOf(1, 2, 4, 6, 9, 10, 11, 12)
        private val KRISHNA_MUHURTHAM_TITHIS = setOf(1, 2, 4)
        // Rohini, Mirugasirisham, Magam, Uthiram, Hastham, Swathi, Anusham, Moolam, Uthiradam, Uthirattathi, Revathi.
        private val MUHURTHAM_STARS = setOf(3, 4, 9, 11, 12, 14, 16, 18, 20, 25, 26)
    }
}
