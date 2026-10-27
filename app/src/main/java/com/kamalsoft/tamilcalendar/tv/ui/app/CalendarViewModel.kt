package com.kamalsoft.tamilcalendar.tv.ui.app

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kamalsoft.tamilcalendar.tv.TamilCalendarApp
import com.kamalsoft.tamilcalendar.tv.di.AppContainer
import com.kamalsoft.tamilcalendar.tv.di.CalendarSession
import com.kamalsoft.tamilcalendar.tv.domain.model.AppSettings
import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.applying
import com.kamalsoft.tamilcalendar.tv.domain.model.isVisible
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.domain.model.RasiPalan
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import com.kamalsoft.tamilcalendar.tv.domain.model.WeatherState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class Destination { DAILY, MONTHLY, IMPORTANT_DATES, FESTIVALS, MUHURTHAM, RASI_PALAN, LOCATION, THEME, SETTINGS, ABOUT }

/** Sections that can be hidden from the tab bar in Settings. */
val HIDEABLE_SECTIONS = listOf(Destination.MONTHLY, Destination.IMPORTANT_DATES, Destination.FESTIVALS, Destination.MUHURTHAM, Destination.RASI_PALAN)

enum class LoadStatus { LOADING, READY, ERROR }

sealed interface LocationSearch {
    data object Idle : LocationSearch
    data object Searching : LocationSearch
    data class Results(val places: List<GeoLocation>) : LocationSearch
    data object Failed : LocationSearch
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CalendarViewModel(private val container: AppContainer) : ViewModel() {

    private val session: StateFlow<CalendarSession> = container.session
    private val sharing = SharingStarted.WhileSubscribed(5_000)

    val location: StateFlow<GeoLocation> = session.map { it.location }.stateIn(viewModelScope, SharingStarted.Eagerly, session.value.location)

    /** Rolls over at local midnight (TVs stay on for days, so this can't be fixed at start-up). */
    private val _today = MutableStateFlow(LocalDate.now(session.value.zone))
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private val _destination = MutableStateFlow(Destination.DAILY)
    val destination: StateFlow<Destination> = _destination.asStateFlow()

    private val _selectedDate = MutableStateFlow(_today.value)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _visibleMonth = MutableStateFlow(YearMonth.from(_today.value))
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    private val _status = MutableStateFlow(LoadStatus.LOADING)
    val status: StateFlow<LoadStatus> = _status.asStateFlow()

    /** Bumped by [retry] so streams that ended in an error are subscribed again. */
    private val reload = MutableStateFlow(0)

    /** A failing database read must not crash a TV app; surface it as an error state instead. */
    private fun <T> Flow<T>.guard(fallback: T): Flow<T> = catch {
        Log.e("CalendarViewModel", "Data stream failed", it)
        _status.value = LoadStatus.ERROR
        emit(fallback)
    }

    /** Re-runs [block] whenever the key, the location session or the reload counter changes. */
    private fun <K, T> StateFlow<K>.perSession(fallback: T, block: (CalendarSession, K) -> Flow<T>): StateFlow<T> =
        combine(this, session, reload) { key, s, _ -> s to key }
            .flatMapLatest { (s, key) -> block(s, key).guard(fallback) }
            .stateIn(viewModelScope, sharing, fallback)

    /** User settings (feature toggles). Calendar data below is filtered through them. */
    val settings: StateFlow<AppSettings> = container.appSettings.settings

    private fun <T> Flow<T>.filtered(f: (T, AppSettings) -> T): Flow<T> = combine(this, settings) { v, st -> f(v, st) }

    /** Today (not the browsed date): for ambient mode and reminders. */
    val todayData: StateFlow<TamilDayData?> = _today.perSession(null) { s, d -> s.getDay(d).filtered { v, st -> v?.applying(st) } }

    val day: StateFlow<TamilDayData?> = _selectedDate.perSession(null) { s, d -> s.getDay(d).filtered { v, st -> v?.applying(st) } }

    val month: StateFlow<List<TamilDayData>> = _visibleMonth.perSession(emptyList()) { s, m -> s.getMonthGrid(m).filtered { v, st -> v.map { it.applying(st) } } }

    val festivals: StateFlow<List<CalendarEvent>> = _today.perSession(emptyList()) { s, t ->
        s.getUpcomingEvents(t, 365, setOf(EventKind.FESTIVAL, EventKind.VIRATHAM, EventKind.GOVT_HOLIDAY, EventKind.SCHOOL))
            .filtered { v, st -> v.filter { it.isVisible(st) } }
    }

    val muhurthams: StateFlow<List<CalendarEvent>> = _today.perSession(emptyList()) { s, t ->
        s.getUpcomingEvents(t, 365, setOf(EventKind.MUHURTHAM))
    }

    /** Graha positions and gochara forecasts for the selected date (Rasi chart). */
    val rasiSky: StateFlow<com.kamalsoft.tamilcalendar.tv.domain.usecase.SkyPositions?> =
        combine(_selectedDate, session) { d, s -> d to s.zone }
            .map { (d, z) -> com.kamalsoft.tamilcalendar.tv.domain.usecase.TransitForecast.positions(d, z) }
            .flowOn(kotlinx.coroutines.Dispatchers.Default)
            .stateIn(viewModelScope, sharing, null)

    val rasiForecasts: StateFlow<List<com.kamalsoft.tamilcalendar.tv.domain.usecase.RasiForecast>> = rasiSky
        .map { sky -> sky?.let { s -> (0 until 12).map { com.kamalsoft.tamilcalendar.tv.domain.usecase.TransitForecast.forecast(it, s) } }.orEmpty() }
        .stateIn(viewModelScope, sharing, emptyList())

    val rasiPalan: StateFlow<List<RasiPalan>> = _selectedDate.perSession(emptyList()) { s, d -> s.getRasiPalan(d) }

    val weather: StateFlow<WeatherState> = session
        .flatMapLatest { it.weather.observe() }
        .stateIn(viewModelScope, sharing, WeatherState(null, refreshFailed = false, isLoading = true))

    val imperialUnits: StateFlow<Boolean> = session.map { it.imperialUnits }.stateIn(viewModelScope, SharingStarted.Eagerly, session.value.imperialUnits)

    // ------------------------------------------------------------------ location search

    private val _locationQuery = MutableStateFlow("")
    val locationQuery: StateFlow<String> = _locationQuery.asStateFlow()

    val locationSearch: StateFlow<LocationSearch> = _locationQuery
        .debounce(350)
        .flatMapLatest { q ->
            flow {
                if (q.trim().length < 2) {
                    emit(LocationSearch.Idle)
                    return@flow
                }
                emit(LocationSearch.Searching)
                emit(
                    runCatching { container.locations.search(q) }
                        .onFailure { Log.w("CalendarViewModel", "Location search failed", it) }
                        .fold({ LocationSearch.Results(it) }, { LocationSearch.Failed }),
                )
            }
        }
        .stateIn(viewModelScope, sharing, LocationSearch.Idle)

    fun onLocationQueryChange(text: String) {
        _locationQuery.value = text
    }

    /** Switches every calculation (panchangam, holidays, weather) to [place] and returns to Today. */
    fun chooseLocation(place: GeoLocation) {
        container.locations.autoDetect = false // a hand-picked place wins until "Use my location"
        container.changeLocation(place)
        _locationQuery.value = ""
        _destination.value = Destination.DAILY
    }

    // ------------------------------------------------------------------ speech (slokas, read-aloud)

    val speaker: StateFlow<com.kamalsoft.tamilcalendar.tv.data.audio.Speaker.State> = container.speaker.state

    fun toggleSpeech(id: String, ta: String, en: String, repeat: Int = 1) = container.speaker.toggle(id, ta, en, repeat)

    fun stopSpeech() = container.speaker.stop()

    override fun onCleared() {
        container.speaker.stop()
    }

    // ------------------------------------------------------------------ auto location

    val autoLocation: Boolean get() = container.locations.autoDetect

    /** Recently used places (for the Location screen's quick row). */
    fun recentLocations(): List<GeoLocation> = container.locations.recent()

    private val _locationStatus = MutableStateFlow<String?>(null)
    /** Last detection result, shown on the Location screen. */
    val locationStatus: StateFlow<String?> = _locationStatus.asStateFlow()

    /** Emitted when the user asks for their location; the Activity handles the permission prompt. */
    private val _locationRequests = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val locationRequests: kotlinx.coroutines.flow.SharedFlow<Unit> = _locationRequests

    fun useMyLocation() {
        container.locations.autoDetect = true
        _locationStatus.value = "இடம் கண்டறியப்படுகிறது… · Detecting your location…"
        _locationRequests.tryEmit(Unit)
    }

    /**
     * Detects the TV's location and switches to it. On launch ([userInitiated] = false) it only switches
     * when the place really changed (> 25 km or another time zone), so an unchanged TV doesn't recompute.
     */
    fun detectLocation(userInitiated: Boolean) {
        if (!container.locations.autoDetect) return
        viewModelScope.launch {
            val result = container.locationDetector.detect()
            if (result == null) {
                _locationStatus.value = "இடத்தைக் கண்டறிய முடியவில்லை · Couldn't detect the location — search for your city below"
                return@launch
            }
            val current = session.value.location
            val moved = distanceKm(current, result.location) > 25 || current.zoneId != result.location.zoneId
            Log.i("CalendarViewModel", "Detected ${result.location.displayName} (${result.source}), moved=$moved, user=$userInitiated")
            if (userInitiated || moved) container.changeLocation(result.location)
            val how = if (result.source == com.kamalsoft.tamilcalendar.tv.data.location.LocationDetector.Source.DEVICE) {
                "சாதன இருப்பிடம் · device location"
            } else {
                "இணைய இணைப்பு மூலம் தோராயமாக · approximate, from the internet connection"
            }
            _locationStatus.value = "✓ ${result.location.displayName} ($how)"
        }
    }

    private fun distanceKm(a: GeoLocation, b: GeoLocation): Double {
        val r = 6371.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(a.latitude)) * Math.cos(Math.toRadians(b.latitude)) * Math.sin(dLon / 2).let { it * it }
        return 2 * r * Math.asin(Math.sqrt(h))
    }

    // ------------------------------------------------------------------ loading / dates

    private var seedJob: Job? = null

    /** In-app reminder banner (Rahu Kalam soon / Nalla Neram begun), shown for two minutes. */
    private val _banner = MutableStateFlow<String?>(null)
    val banner: StateFlow<String?> = _banner.asStateFlow()

    init {
        load()
        container.reminders.reschedule()
        viewModelScope.launch {
            while (true) {
                _banner.value = com.kamalsoft.tamilcalendar.tv.data.notify.bannerFor(
                    todayData.value, settings.value, java.time.LocalDateTime.now(session.value.zone),
                )
                delay(20_000)
            }
        }
        viewModelScope.launch {
            session.drop(1).collect { s ->
                // New place → new time zone: "today" may differ, and the offline year must be rebuilt.
                _today.value = LocalDate.now(s.zone)
                selectDate(_today.value)
                load(restart = true)
                container.reminders.reschedule()
            }
        }
        viewModelScope.launch {
            // Checking once a minute (rather than one long delay to midnight) also survives TV standby.
            while (true) {
                delay(60_000)
                checkDateRollover()
            }
        }
    }

    /** Builds/extends the offline year. Called on start, from the error screen, after midnight and on location change. */
    fun load(restart: Boolean = false) {
        if (restart) seedJob?.cancel() else if (seedJob?.isActive == true) return
        val s = session.value
        seedJob = viewModelScope.launch {
            _status.value = LoadStatus.LOADING
            runCatching {
                s.repository.refreshBundledData()
                s.repository.ensureRange(_today.value.minusDays(31), _today.value.plusDays(365))
            }.onSuccess {
                _status.value = LoadStatus.READY
                s.repository.sync(_today.value.year)
            }.onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                Log.e("CalendarViewModel", "Offline data build failed", it)
                _status.value = LoadStatus.ERROR
            }
        }
    }

    fun retry() {
        load(restart = true)
        reload.update { it + 1 }
    }

    /** Also called from Activity.onResume, so a TV woken from standby the next morning shows the right day. */
    fun checkDateRollover() {
        val now = LocalDate.now(session.value.zone)
        val old = _today.value
        if (now == old) return
        _today.value = now
        // Follow "today" only if the user was looking at today; don't yank them off a browsed date.
        if (_selectedDate.value == old) selectDate(now)
        load()
    }

    /**
     * Applies a settings change. Units / Rahu Kalam convention change the calculation, so the session is
     * rebuilt (the offline year is regenerated); auto-location re-detects when switched on.
     */
    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val before = settings.value
        container.appSettings.update(transform)
        val after = settings.value
        if (before.units != after.units || before.convention != after.convention) container.refreshSession()
        if (before.rahuReminder != after.rahuReminder || before.nallaReminder != after.nallaReminder) container.reminders.reschedule()
    }

    fun setAutoLocation(on: Boolean) {
        if (on) useMyLocation() else container.locations.autoDetect = false
    }

    /** Clears the "seen" flag so the first-launch disclaimer shows again. */
    fun resetFirstLaunch() {
        container.settings.edit().putBoolean(KEY_HELP_SEEN, false).apply()
    }

    /** True exactly once per install: show the remote-control help on first launch. */
    fun consumeFirstLaunch(): Boolean {
        val first = !container.settings.getBoolean(KEY_HELP_SEEN, false)
        if (first) container.settings.edit().putBoolean(KEY_HELP_SEEN, true).apply()
        return first
    }

    /** Saved colour theme name (see AppTheme); applied by MainActivity before the first frame. */
    fun saveTheme(name: String) {
        container.settings.edit().putString(KEY_THEME, name).apply()
    }

    fun navigate(to: Destination) {
        _destination.value = to
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _visibleMonth.value = YearMonth.from(date)
    }

    fun shiftDay(days: Long) = selectDate(_selectedDate.value.plusDays(days))

    fun shiftMonth(months: Long) = _visibleMonth.update { it.plusMonths(months) }

    fun goToday() = selectDate(_today.value)

    fun openDate(date: LocalDate) {
        selectDate(date)
        _destination.value = Destination.DAILY
    }

    companion object {
        private const val KEY_HELP_SEEN = "help_seen"
        const val KEY_THEME = "theme"

        val Factory = viewModelFactory {
            initializer { CalendarViewModel((this[APPLICATION_KEY] as TamilCalendarApp).container) }
        }
    }
}
