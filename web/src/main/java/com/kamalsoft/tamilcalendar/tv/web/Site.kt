package com.kamalsoft.tamilcalendar.tv.web

import com.kamalsoft.tamilcalendar.tv.core.astro.TamilCalendarEngine
import com.kamalsoft.tamilcalendar.tv.core.holidays.UsHolidays
import com.kamalsoft.tamilcalendar.tv.core.holidays.WorldReligions
import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.domain.model.LocationPolicy
import com.kamalsoft.tamilcalendar.tv.domain.model.PanchangaElement
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilNames
import com.kamalsoft.tamilcalendar.tv.domain.model.TimingWindow
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/**
 * Reference output for the browser engine: `data/<place>/<yyyy-MM>.json` per place and month, computed by the
 * Kotlin engine. The page computes days itself; web/tools/check_engine.mjs compares the two field by field.
 * Run with `./gradlew :web:referenceData`. Usage: `Site <outputDir>`.
 */
fun main(args: Array<String>) {
    val out = File(args.single())
    val months = generateSequence(YearMonth.of(FIRST_YEAR, 1)) { it.plusMonths(1) }.takeWhile { it.year <= LAST_YEAR }
    for (place in GeoLocation.QUICK) {
        val dir = File(out, "data/${placeKey(place)}").apply { mkdirs() }
        for (month in months) {
            val first = month.atDay(1)
            val last = month.atEndOfMonth()
            val from = first.minusDays((first.dayOfWeek.value % 7).toLong())
            val to = last.plusDays((6 - last.dayOfWeek.value % 7).toLong())
            File(dir, "$month.json").writeText(toJson(monthJson(place, from, to)))
        }
        println("${placeKey(place)}: ${months.count()} months")
    }
}

private const val FIRST_YEAR = 2026
private const val LAST_YEAR = 2028

/** Lower-case place name used in the URL, e.g. "chennai". */
private fun placeKey(place: GeoLocation) = place.nameEn.lowercase()

private fun monthJson(place: GeoLocation, from: LocalDate, to: LocalDate) = mapOf(
    "weekdays" to TamilNames.weekdays.map(::label),
    "days" to computeDays(place, from, to).map(::dayJson),
)

/** Engine output plus the rule-based holidays, the same composition the Android repository stores. */
private fun computeDays(place: GeoLocation, from: LocalDate, to: LocalDate): List<TamilDayData> {
    val engine = TamilCalendarEngine(place, LocationPolicy.timingConvention(place))
    val localRegions = LocationPolicy.localHolidayRegions(place)
    val civil: Map<LocalDate, List<CalendarEvent>> =
        (UsHolidays.between(from, to, localRegions) + WorldReligions.between(from, to)).groupBy { it.date }
    return engine.compute(from, to).map { day ->
        val events = (day.events + civil[day.date].orEmpty()).map {
            it.copy(isLocalHoliday = it.isHoliday && it.kind != EventKind.SCHOOL && it.region in localRegions)
        }
        day.copy(events = events)
    }
}

private fun dayJson(d: TamilDayData): Map<String, Any?> = mapOf(
    "date" to d.date,
    "weekday" to label(TamilNames.weekdays[d.weekdayIndex]),
    "holiday" to d.events.any { it.isLocalHoliday },
    "tamilDate" to mapOf(
        "month" to label(d.tamilDate.monthTa, d.tamilDate.monthEn),
        "day" to d.tamilDate.day,
        "year" to label(d.tamilDate.yearTa, d.tamilDate.yearEn),
    ),
    "sunrise" to d.sunrise,
    "sunset" to d.sunset,
    "tithi" to element(d.panchangam.tithi),
    "nakshatram" to element(d.panchangam.nakshatram),
    "yogam" to element(d.panchangam.yogam),
    "paksha" to label(d.panchangam.paksha.ta, d.panchangam.paksha.en),
    "moonRasi" to label(d.panchangam.moonRasiTa, d.panchangam.moonRasiEn),
    "lagnam" to d.panchangam.lagnam.map { mapOf("rasi" to it.rasi, "start" to it.start.toString()) },
    "karanam" to d.panchangam.karanam,
    "sraardhaTithi" to (d.panchangam.sraardhaTithi ?: -1),
    "timings" to d.timings.all().map(::timingJson),
    "chandrashtamam" to mapOf(
        "rasi" to label(TamilNames.rasis[d.chandrashtamam.rasi]),
        "nakshatras" to d.chandrashtamam.nakshatrasTa,
        "until" to d.chandrashtamam.until,
    ),
    "importantDates" to d.importantDates.map {
        mapOf("title" to label(it.titleTa, it.titleEn), "holiday" to it.isHoliday)
    },
)

private fun element(e: PanchangaElement) =
    mapOf("name" to label(e.nameTa, e.nameEn), "endsAt" to e.endsAt)

private fun timingJson(w: TimingWindow) = mapOf(
    "type" to label(w.type.ta, w.type.en),
    "quality" to w.quality.name,
    "start" to w.start,
    "end" to w.end,
    "gowri" to w.gowri?.let { mapOf("name" to label(it.ta, it.en), "good" to it.isGood) },
)

private fun label(ta: String, en: String) = mapOf("ta" to ta, "en" to en)
private fun label(pair: Pair<String, String>) = label(pair.first, pair.second)

/**
 * Minimal JSON writer for maps, lists, enums and scalars. Java time values fall through to
 * `toString()`, which is ISO-8601 and what the page expects.
 */
internal fun toJson(value: Any?): String = StringBuilder().also { it.writeJson(value) }.toString()

private fun StringBuilder.writeJson(value: Any?) {
    when (value) {
        null -> append("null")
        is String -> append('"').append(escape(value)).append('"')
        is Number, is Boolean -> append(value)
        is Enum<*> -> writeJson(value.name)
        is Map<*, *> -> {
            append('{')
            value.entries.forEachIndexed { i, (k, v) ->
                if (i > 0) append(',')
                writeJson(k.toString())
                append(':')
                writeJson(v)
            }
            append('}')
        }
        is Iterable<*> -> {
            append('[')
            value.forEachIndexed { i, v ->
                if (i > 0) append(',')
                writeJson(v)
            }
            append(']')
        }
        else -> writeJson(value.toString())
    }
}

private fun escape(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
