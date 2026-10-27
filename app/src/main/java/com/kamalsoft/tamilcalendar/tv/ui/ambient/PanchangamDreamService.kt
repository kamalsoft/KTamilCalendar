package com.kamalsoft.tamilcalendar.tv.ui.ambient

import android.service.dreams.DreamService
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kamalsoft.tamilcalendar.tv.TamilCalendarApp
import com.kamalsoft.tamilcalendar.tv.domain.model.WeatherState
import com.kamalsoft.tamilcalendar.tv.domain.model.applying
import com.kamalsoft.tamilcalendar.tv.ui.app.CalendarViewModel
import com.kamalsoft.tamilcalendar.tv.ui.theme.AppTheme
import com.kamalsoft.tamilcalendar.tv.ui.theme.DesignCanvas
import com.kamalsoft.tamilcalendar.tv.ui.theme.TamilCalendarTheme
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import java.time.LocalDate

/**
 * System screensaver ("Daydream"): the same ambient screen, shown by Android when the TV idles even
 * if the app isn't open. Pick it in Settings → System → Ambient mode / Screen saver (where the TV
 * allows third-party screensavers).
 */
class PanchangamDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        val container = (application as TamilCalendarApp).container
        TvColors.palette = AppTheme.fromName(container.settings.getString(CalendarViewModel.KEY_THEME, null)).palette
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PanchangamDreamService)
            setViewTreeSavedStateRegistryOwner(this@PanchangamDreamService)
            setContent {
                val session by container.session.collectAsState()
                val settings by container.appSettings.settings.collectAsState()
                val today = LocalDate.now(session.zone)
                LaunchedEffect(session) { runCatching { session.repository.ensureRange(today, today) } }
                val day by produceState<com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData?>(null, session, settings) {
                    session.getDay(today).collect { value = it?.applying(settings) }
                }
                val weather by session.weather.observe().collectAsState(WeatherState(null, refreshFailed = false, isLoading = true))
                TamilCalendarTheme {
                    DesignCanvas { AmbientScreen(day, if (settings.weather) weather else NoWeather, session.location) }
                }
            }
        }
        setContentView(view)
        registry.currentState = Lifecycle.State.STARTED
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        registry.currentState = Lifecycle.State.RESUMED
    }

    override fun onDreamingStopped() {
        registry.currentState = Lifecycle.State.STARTED
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        registry.currentState = Lifecycle.State.DESTROYED
        super.onDetachedFromWindow()
    }
}
