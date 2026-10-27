package com.kamalsoft.tamilcalendar.tv.di

import android.content.Context
import com.kamalsoft.tamilcalendar.tv.data.audio.Speaker
import com.kamalsoft.tamilcalendar.tv.data.local.CalendarDatabase
import com.kamalsoft.tamilcalendar.tv.data.notify.ReminderScheduler
import com.kamalsoft.tamilcalendar.tv.data.location.LocationDetector
import com.kamalsoft.tamilcalendar.tv.data.remote.GeocodingApi
import com.kamalsoft.tamilcalendar.tv.data.remote.NetworkModule
import com.kamalsoft.tamilcalendar.tv.data.remote.WeatherApi
import com.kamalsoft.tamilcalendar.tv.data.repository.LocationRepository
import com.kamalsoft.tamilcalendar.tv.data.repository.SettingsRepository
import com.kamalsoft.tamilcalendar.tv.data.repository.WeatherRepository
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

/** Manual DI: a TV app with one activity doesn't need a DI framework's startup cost. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = CalendarDatabase.create(appContext)
    private val calendarApi = NetworkModule.api()
    private val weatherApi by lazy { WeatherApi.create() }
    private val weatherPrefs = appContext.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
    private val schoolJson by lazy { appContext.assets.open("school_calendars.json").bufferedReader().use { it.readText() } }

    /** One lock for all sessions, so a location switch can't interleave with a seed still running. */
    private val seedLock = Mutex()

    val settings = appContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    val appSettings = SettingsRepository(settings)

    val locationDetector = LocationDetector(appContext)

    val speaker = Speaker(appContext)

    val reminders by lazy { ReminderScheduler(appContext, this) }

    val locations = LocationRepository(GeocodingApi.create(), settings, default = GeoLocation.NAPERVILLE, io = Dispatchers.IO)

    private val _session = MutableStateFlow(createSession(locations.current()))
    val session: StateFlow<CalendarSession> = _session.asStateFlow()

    fun changeLocation(location: GeoLocation) {
        locations.save(location)
        _session.value = createSession(location)
    }

    /** Rebuilds the session when a calculation setting (units, Rahu Kalam convention) changes. */
    fun refreshSession() {
        _session.value = createSession(_session.value.location)
    }

    private fun createSession(location: GeoLocation) = CalendarSession(
        location = location,
        dao = database.calendarDao(),
        api = calendarApi,
        seedLock = seedLock,
        schoolJson = { schoolJson },
        weatherFactory = { loc, imperial -> WeatherRepository(weatherApi, loc, imperial, weatherPrefs, Dispatchers.IO) },
        io = Dispatchers.IO,
        units = appSettings.settings.value.units,
        convention = appSettings.settings.value.convention,
    )
}
