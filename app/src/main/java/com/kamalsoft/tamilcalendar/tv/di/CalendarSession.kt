package com.kamalsoft.tamilcalendar.tv.di

import com.kamalsoft.tamilcalendar.tv.core.astro.TamilCalendarEngine
import com.kamalsoft.tamilcalendar.tv.data.local.CalendarDao
import com.kamalsoft.tamilcalendar.tv.data.local.CalendarMapper
import com.kamalsoft.tamilcalendar.tv.data.local.SchoolCalendars
import com.kamalsoft.tamilcalendar.tv.data.remote.TamilCalendarApi
import com.kamalsoft.tamilcalendar.tv.data.repository.CalendarRepositoryImpl
import com.kamalsoft.tamilcalendar.tv.data.repository.WeatherRepository
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.domain.model.LocationPolicy
import com.kamalsoft.tamilcalendar.tv.domain.repository.CalendarRepository
import com.kamalsoft.tamilcalendar.tv.domain.usecase.GetDayUseCase
import com.kamalsoft.tamilcalendar.tv.domain.usecase.GetMonthGridUseCase
import com.kamalsoft.tamilcalendar.tv.domain.usecase.GetRasiPalanUseCase
import com.kamalsoft.tamilcalendar.tv.domain.usecase.GetUpcomingEventsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import java.time.ZoneId

/**
 * Everything that depends on the chosen location. A new session is created when the location
 * changes; the database is shared, and cached days are recomputed because [dataVersion] changes.
 */
class CalendarSession(
    val location: GeoLocation,
    dao: CalendarDao,
    api: TamilCalendarApi?,
    seedLock: Mutex,
    schoolJson: () -> String,
    weatherFactory: (GeoLocation, imperial: Boolean) -> WeatherRepository,
    io: CoroutineDispatcher,
) {
    val zone: ZoneId = ZoneId.of(location.zoneId)
    val imperialUnits: Boolean = LocationPolicy.usesImperialUnits(location)

    private val localHolidayRegions = LocationPolicy.localHolidayRegions(location)
    private val schoolDistricts = LocationPolicy.schoolDistricts(location)
    private val engine = TamilCalendarEngine(location, LocationPolicy.timingConvention(location))

    /** Changing the engine config or the holiday regions regenerates the cached days. */
    private val dataVersion = listOf(engine.dataVersion, localHolidayRegions).hashCode()

    val repository: CalendarRepository = CalendarRepositoryImpl(
        dao = dao,
        engine = engine,
        dataVersion = dataVersion,
        mapper = CalendarMapper(zone, dataVersion, localHolidayRegions),
        api = api,
        zone = zone,
        civilHolidayRegions = localHolidayRegions,
        bundledEvents = { SchoolCalendars.parse(schoolJson(), schoolDistricts) },
        seedLock = seedLock,
        io = io,
    )

    val weather: WeatherRepository = weatherFactory(location, imperialUnits)

    val getDay = GetDayUseCase(repository)
    val getMonthGrid = GetMonthGridUseCase(repository)
    val getUpcomingEvents = GetUpcomingEventsUseCase(repository)
    val getRasiPalan = GetRasiPalanUseCase(repository)
}
