package com.kamalsoft.tamilcalendar.tv.di

import com.kamalsoft.tamilcalendar.tv.core.astro.TamilCalendarEngine
import com.kamalsoft.tamilcalendar.tv.data.local.CalendarDao
import com.kamalsoft.tamilcalendar.tv.data.local.CalendarMapper
import com.kamalsoft.tamilcalendar.tv.data.local.SchoolCalendars
import com.kamalsoft.tamilcalendar.tv.data.remote.TamilCalendarApi
import com.kamalsoft.tamilcalendar.tv.data.repository.CalendarRepositoryImpl
import com.kamalsoft.tamilcalendar.tv.data.repository.WeatherRepository
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.domain.model.ConventionPref
import com.kamalsoft.tamilcalendar.tv.domain.model.LocationPolicy
import com.kamalsoft.tamilcalendar.tv.domain.model.TimingConvention
import com.kamalsoft.tamilcalendar.tv.domain.model.UnitsPref
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
    units: UnitsPref = UnitsPref.AUTO,
    convention: ConventionPref = ConventionPref.AUTO,
) {
    val zone: ZoneId = ZoneId.of(location.zoneId)
    val imperialUnits: Boolean = when (units) {
        UnitsPref.AUTO -> LocationPolicy.usesImperialUnits(location)
        UnitsPref.IMPERIAL -> true
        UnitsPref.METRIC -> false
    }

    private val localHolidayRegions = LocationPolicy.localHolidayRegions(location)
    private val schoolDistricts = LocationPolicy.schoolDistricts(location)
    private val engine = TamilCalendarEngine(
        location,
        when (convention) {
            ConventionPref.AUTO -> LocationPolicy.timingConvention(location)
            ConventionPref.TRADITIONAL_6AM -> TimingConvention.TRADITIONAL_6AM
            ConventionPref.SUNRISE_BASED -> TimingConvention.SUNRISE_BASED
        },
    )

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
