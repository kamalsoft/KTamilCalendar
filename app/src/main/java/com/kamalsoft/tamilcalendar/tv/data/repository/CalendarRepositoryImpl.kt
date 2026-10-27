package com.kamalsoft.tamilcalendar.tv.data.repository

import android.util.Log
import com.kamalsoft.tamilcalendar.tv.core.astro.TamilCalendarEngine
import com.kamalsoft.tamilcalendar.tv.core.holidays.UsHolidays
import com.kamalsoft.tamilcalendar.tv.core.holidays.WorldReligions
import com.kamalsoft.tamilcalendar.tv.data.local.CalendarDao
import com.kamalsoft.tamilcalendar.tv.data.local.CalendarMapper
import com.kamalsoft.tamilcalendar.tv.data.local.EventEntity
import com.kamalsoft.tamilcalendar.tv.data.local.RasiPalanEntity
import com.kamalsoft.tamilcalendar.tv.data.remote.TamilCalendarApi
import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.EventSource
import com.kamalsoft.tamilcalendar.tv.domain.model.HolidayRegion
import com.kamalsoft.tamilcalendar.tv.domain.model.RasiPalan
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import com.kamalsoft.tamilcalendar.tv.domain.repository.CalendarRepository
import com.kamalsoft.tamilcalendar.tv.domain.usecase.RasiPalanCalculator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class CalendarRepositoryImpl(
    private val dao: CalendarDao,
    private val engine: TamilCalendarEngine,
    /** Stored per cached day; rows with another version are recomputed. */
    private val dataVersion: Int,
    private val mapper: CalendarMapper,
    private val api: TamilCalendarApi?,
    private val zone: ZoneId,
    /** Regions whose rule-based holidays (US federal, Illinois) are generated alongside the panchangam. */
    private val civilHolidayRegions: Set<HolidayRegion>,
    /** Returns the bundled school calendar events (already filtered to the configured districts). */
    private val bundledEvents: () -> List<CalendarEvent>,
    /** Shared across location sessions: only one seed may write the day table at a time. */
    private val seedLock: Mutex,
    private val io: CoroutineDispatcher,
) : CalendarRepository {

    override suspend fun ensureRange(from: LocalDate, to: LocalDate) = withContext(io) {
        seedLock.withLock {
            val existing = dao.existingDays(from.toEpochDay(), to.toEpochDay(), dataVersion).toHashSet()
            // Recompute each contiguous run of missing days in one engine pass.
            var day = from
            while (!day.isAfter(to)) {
                if (day.toEpochDay() in existing) { day = day.plusDays(1); continue }
                var end = day
                while (end.plusDays(1).let { !it.isAfter(to) && it.toEpochDay() !in existing }) end = end.plusDays(1)
                val computed = engine.compute(day, end)
                val civil = UsHolidays.between(day, end, civilHolidayRegions) + WorldReligions.between(day, end)
                dao.replaceComputed(
                    from = day.toEpochDay(), to = end.toEpochDay(),
                    days = computed.map(mapper::toEntity),
                    timings = computed.flatMap(mapper::toTimingEntities),
                    events = (computed.flatMap { it.events } + civil).map(mapper::toEventEntity),
                )
                day = end.plusDays(1)
            }
        }
    }

    override suspend fun refreshBundledData() = withContext(io) {
        dao.replaceAllEvents(CalendarDao.SOURCE_BUNDLED, bundledEvents().map(mapper::toEventEntity))
    }

    override fun observeDay(date: LocalDate): Flow<TamilDayData?> =
        dao.observeDay(date.toEpochDay()).map { it?.let(mapper::toDomain) }.flowOn(io)

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<TamilDayData>> =
        dao.observeRange(from.toEpochDay(), to.toEpochDay()).map { list -> list.map(mapper::toDomain) }.flowOn(io)

    override fun observeEvents(from: LocalDate, to: LocalDate, kinds: Set<EventKind>): Flow<List<CalendarEvent>> =
        dao.observeEvents(from.toEpochDay(), to.toEpochDay(), kinds.map { it.name })
            .map { list -> list.map(mapper::toDomain) }
            .flowOn(io)

    override fun observeRasiPalan(date: LocalDate): Flow<List<RasiPalan>> {
        val moonRasi = dao.observeDay(date.toEpochDay()).filterNotNull().map { it.day.moonRasi }.distinctUntilChanged()
        return combine(moonRasi, dao.observeRasiPalan(date.toEpochDay())) { rasi, remote ->
            val overrides = remote.associateBy { it.rasi }
            RasiPalanCalculator.forMoonRasi(rasi).map { p ->
                overrides[p.rasi]?.let { p.copy(messageTa = it.messageTa, messageEn = it.messageEn, source = EventSource.REMOTE) } ?: p
            }
        }.flowOn(io)
    }

    override suspend fun sync(year: Int): Boolean = withContext(io) {
        val api = api ?: return@withContext false
        runCatching {
            val from = LocalDate.of(year, 1, 1)
            val to = LocalDate.of(year, 12, 31)
            val holidays = api.holidays(year).mapNotNull { dto ->
                val date = dto.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
                val kind = runCatching { EventKind.valueOf(dto.kind ?: "GOVT_HOLIDAY") }.getOrDefault(EventKind.GOVT_HOLIDAY)
                EventEntity(
                    epochDay = date.toEpochDay(), kind = kind.name,
                    titleTa = dto.titleTa ?: dto.titleEn ?: return@mapNotNull null, titleEn = dto.titleEn ?: dto.titleTa!!,
                    isHoliday = dto.isHoliday ?: true, source = CalendarDao.SOURCE_REMOTE, detail = null, region = null,
                    religion = null, deity = null,
                )
            }
            dao.replaceRemoteEvents(from.toEpochDay(), to.toEpochDay(), holidays)

            val today = LocalDate.now(zone)
            val palan = api.rasiPalan(today.toString(), today.plusDays(7).toString()).mapNotNull { dto ->
                val date = dto.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
                val rasi = dto.rasi?.takeIf { it in 0..11 } ?: return@mapNotNull null
                RasiPalanEntity(date.toEpochDay(), rasi, dto.messageTa ?: return@mapNotNull null, dto.messageEn.orEmpty())
            }
            dao.upsertRasiPalan(palan)
            true
        }.onFailure { Log.w("CalendarRepository", "Sync failed; staying on offline data", it) }.getOrDefault(false)
    }
}
