package com.kamalsoft.tamilcalendar.tv.domain.usecase

import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.HolidayRegion
import com.kamalsoft.tamilcalendar.tv.domain.model.RasiPalan
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import com.kamalsoft.tamilcalendar.tv.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

class GetDayUseCase(private val repo: CalendarRepository) {
    operator fun invoke(date: LocalDate): Flow<TamilDayData?> =
        repo.observeDay(date).onStart { repo.ensureRange(date, date) }
}

/** A Sunday-first 6x7 grid covering [month]. */
class GetMonthGridUseCase(private val repo: CalendarRepository) {
    fun gridRange(month: YearMonth): ClosedRange<LocalDate> {
        val start = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return start..start.plusDays(41)
    }

    operator fun invoke(month: YearMonth): Flow<List<TamilDayData>> {
        val range = gridRange(month)
        return repo.observeRange(range.start, range.endInclusive)
            .onStart { repo.ensureRange(range.start, range.endInclusive) }
    }
}

class GetUpcomingEventsUseCase(private val repo: CalendarRepository) {
    operator fun invoke(from: LocalDate, days: Long, kinds: Set<EventKind>): Flow<List<CalendarEvent>> =
        repo.observeEvents(from, from.plusDays(days), kinds).map { it.forDisplay() }
}

/**
 * List shaping:
 * - The same holiday from two regions on one day (e.g. Christmas: US + Tamil Nadu) is shown once,
 *   preferring the entry that is a day off.
 * - Consecutive days of one multi-day entry (e.g. Winter Break) collapse into a single row with [CalendarEvent.endDate].
 */
fun List<CalendarEvent>.forDisplay(): List<CalendarEvent> {
    // School entries stay per district; other same-day, same-title entries merge across regions.
    val deduped = groupBy { Triple(it.date, it.titleEn, if (it.kind == EventKind.SCHOOL) it.region else null) }
        .values.map { same -> same.maxBy { (if (it.isHoliday) 2 else 0) + (if (it.region != HolidayRegion.TAMIL_NADU) 1 else 0) } }
        .sortedBy { it.date }
    val out = mutableListOf<CalendarEvent>()
    val open = HashMap<String, Int>() // run key -> index in out
    for (e in deduped) {
        val key = "${e.kind}|${e.region}|${e.titleEn}"
        val idx = open[key]
        val prev = idx?.let { out[it] }
        if (prev != null && (prev.endDate ?: prev.date).plusDays(1) == e.date) {
            out[idx] = prev.copy(endDate = e.date)
        } else {
            open[key] = out.size
            out += e
        }
    }
    return out.sortedBy { it.date }
}

class GetRasiPalanUseCase(private val repo: CalendarRepository) {
    operator fun invoke(date: LocalDate): Flow<List<RasiPalan>> =
        repo.observeRasiPalan(date).onStart { repo.ensureRange(date, date) }
}
