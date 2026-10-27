package com.kamalsoft.tamilcalendar.tv.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DayEntity::class, TimingEntity::class, EventEntity::class, RasiPalanEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class CalendarDatabase : RoomDatabase() {
    abstract fun calendarDao(): CalendarDao

    companion object {
        fun create(context: Context): CalendarDatabase =
            Room.databaseBuilder(context, CalendarDatabase::class.java, "tamil_calendar.db")
                // Every row is either derived by the engine or re-syncable, so a wipe is safe.
                .fallbackToDestructiveMigration()
                .build()
    }
}
