package com.kamalsoft.tamilcalendar.tv.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {

    @Transaction
    @Query("SELECT * FROM calendar_day WHERE epochDay = :epochDay")
    fun observeDay(epochDay: Long): Flow<DayWithDetails?>

    @Transaction
    @Query("SELECT * FROM calendar_day WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    fun observeRange(from: Long, to: Long): Flow<List<DayWithDetails>>

    @Query("SELECT epochDay FROM calendar_day WHERE engineVersion = :version AND epochDay BETWEEN :from AND :to")
    suspend fun existingDays(from: Long, to: Long, version: Int): List<Long>

    @Query("SELECT * FROM calendar_event WHERE epochDay BETWEEN :from AND :to AND kind IN (:kinds) ORDER BY epochDay, isHoliday DESC")
    fun observeEvents(from: Long, to: Long, kinds: List<String>): Flow<List<EventEntity>>

    @Query("SELECT * FROM rasi_palan WHERE epochDay = :epochDay")
    fun observeRasiPalan(epochDay: Long): Flow<List<RasiPalanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDays(days: List<DayEntity>)

    @Insert
    suspend fun insertTimings(timings: List<TimingEntity>)

    @Insert
    suspend fun insertEvents(events: List<EventEntity>)

    @Query("DELETE FROM timing_window WHERE epochDay BETWEEN :from AND :to")
    suspend fun deleteTimings(from: Long, to: Long)

    @Query("DELETE FROM calendar_event WHERE source = :source AND epochDay BETWEEN :from AND :to")
    suspend fun deleteEvents(from: Long, to: Long, source: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRasiPalan(items: List<RasiPalanEntity>)

    /** Replaces computed data for a contiguous day range atomically. */
    @Transaction
    suspend fun replaceComputed(from: Long, to: Long, days: List<DayEntity>, timings: List<TimingEntity>, events: List<EventEntity>) {
        deleteTimings(from, to)
        deleteEvents(from, to, SOURCE_COMPUTED)
        upsertDays(days)
        insertTimings(timings)
        insertEvents(events)
    }

    @Query("DELETE FROM calendar_event WHERE source = :source")
    suspend fun deleteAllEvents(source: String)

    @Transaction
    suspend fun replaceAllEvents(source: String, events: List<EventEntity>) {
        deleteAllEvents(source)
        insertEvents(events)
    }

    @Transaction
    suspend fun replaceRemoteEvents(from: Long, to: Long, events: List<EventEntity>) {
        deleteEvents(from, to, SOURCE_REMOTE)
        insertEvents(events)
    }

    companion object {
        const val SOURCE_COMPUTED = "COMPUTED"
        const val SOURCE_REMOTE = "REMOTE"
        const val SOURCE_BUNDLED = "BUNDLED"
    }
}
