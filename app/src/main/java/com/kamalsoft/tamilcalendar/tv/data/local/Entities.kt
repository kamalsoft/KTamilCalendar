package com.kamalsoft.tamilcalendar.tv.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "calendar_day")
data class DayEntity(
    @PrimaryKey val epochDay: Long,
    val engineVersion: Int,
    val tamilMonth: Int,
    val tamilDay: Int,
    val yearCycleIndex: Int,
    val thiruvalluvarAandu: Int,
    val sunriseSec: Int,
    val sunsetSec: Int,
    val tithi: Int,
    /** Epoch seconds, null if not found in search window. */
    val tithiEnd: Long?,
    val nakshatram: Int,
    val nakshatramEnd: Long?,
    val yogam: Int,
    val yogamEnd: Long?,
    val moonRasi: Int,
    val moonRasiEnd: Long?,
    val sunRasi: Int,
    val chandrashtamaRasi: Int,
    /** Comma-separated nakshatra indices. */
    val chandrashtamaStars: String,
    /** Comma-separated [com.kamalsoft.tamilcalendar.tv.domain.model.DayTag] names (computed tags only). */
    val tags: String,
)

@Entity(tableName = "timing_window", indices = [Index("epochDay")])
data class TimingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val type: String,
    val startSec: Int,
    val endSec: Int,
    val gowri: String?,
)

@Entity(tableName = "calendar_event", indices = [Index("epochDay"), Index("source")])
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val kind: String,
    val titleTa: String,
    val titleEn: String,
    val isHoliday: Boolean,
    val source: String,
    val detail: String?,
    /** [com.kamalsoft.tamilcalendar.tv.domain.model.HolidayRegion] name, null for non-holiday festivals. */
    val region: String?,
    /** [com.kamalsoft.tamilcalendar.tv.domain.model.Religion] / [com.kamalsoft.tamilcalendar.tv.domain.model.Deity] names. */
    val religion: String?,
    val deity: String?,
)

@Entity(tableName = "rasi_palan", primaryKeys = ["epochDay", "rasi"])
data class RasiPalanEntity(
    val epochDay: Long,
    val rasi: Int,
    val messageTa: String,
    val messageEn: String,
)

data class DayWithDetails(
    @Embedded val day: DayEntity,
    @Relation(parentColumn = "epochDay", entityColumn = "epochDay") val timings: List<TimingEntity>,
    @Relation(parentColumn = "epochDay", entityColumn = "epochDay") val events: List<EventEntity>,
)
