package com.kamalsoft.tamilcalendar.tv.domain.repository

import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.RasiPalan
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface CalendarRepository {
    /** Makes sure [from]..[to] exists in the offline store, computing missing days. */
    suspend fun ensureRange(from: LocalDate, to: LocalDate)

    /** Reloads data shipped in the APK (school calendars). Cheap; safe to call on every start. */
    suspend fun refreshBundledData()

    fun observeDay(date: LocalDate): Flow<TamilDayData?>

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<TamilDayData>>

    fun observeEvents(from: LocalDate, to: LocalDate, kinds: Set<EventKind>): Flow<List<CalendarEvent>>

    fun observeRasiPalan(date: LocalDate): Flow<List<RasiPalan>>

    /** Pulls holiday / rasi palan overrides. Returns false when sync is disabled or fails. */
    suspend fun sync(year: Int): Boolean
}
