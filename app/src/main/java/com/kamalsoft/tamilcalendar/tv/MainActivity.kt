package com.kamalsoft.tamilcalendar.tv

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.kamalsoft.tamilcalendar.tv.ui.app.CalendarViewModel
import com.kamalsoft.tamilcalendar.tv.ui.app.TamilCalendarTvApp
import com.kamalsoft.tamilcalendar.tv.ui.theme.AppTheme
import com.kamalsoft.tamilcalendar.tv.ui.theme.DesignCanvas
import com.kamalsoft.tamilcalendar.tv.ui.theme.DisplayPrefs
import com.kamalsoft.tamilcalendar.tv.ui.theme.TamilCalendarTheme
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: CalendarViewModel by viewModels { CalendarViewModel.Factory }

    private val container get() = (application as TamilCalendarApp).container

    /** Whether the pending permission prompt came from "Use my location" (always switch) or launch. */
    private var userInitiated = false

    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.settings.edit().putBoolean(KEY_PERMISSION_ASKED, true).apply()
        // Granted → device location; denied → the detector falls back to approximate (IP) location.
        viewModel.detectLocation(userInitiated)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply the saved colour theme before the first frame, so there's no flash of the default theme.
        TvColors.palette = AppTheme.fromName(container.settings.getString(CalendarViewModel.KEY_THEME, null)).palette
        setContent {
            TamilCalendarTheme {
                DesignCanvas {
                    TamilCalendarTvApp(viewModel)
                }
            }
        }
        lifecycleScope.launch { viewModel.locationRequests.collect { startLocationDetection(userInitiated = true) } }
        // Push display settings to the UI helpers (motion, time format, Tamil-only labels).
        lifecycleScope.launch {
            viewModel.settings.collect {
                DisplayPrefs.animations = it.animations
                DisplayPrefs.weatherAnimations = it.weatherAnimations
                DisplayPrefs.clock24h = it.clock24h
                DisplayPrefs.tamilOnly = it.tamilOnly
            }
        }
        // Open with the TV's own location by default (unless the user picked a place by hand).
        if (savedInstanceState == null && viewModel.autoLocation) startLocationDetection(userInitiated = false)
    }

    private fun startLocationDetection(userInitiated: Boolean) {
        this.userInitiated = userInitiated
        val asked = container.settings.getBoolean(KEY_PERMISSION_ASKED, false)
        when {
            container.locationDetector.hasPermission() -> viewModel.detectLocation(userInitiated)
            // Ask once on first launch; afterwards only when the user explicitly asks for their location.
            !asked || userInitiated -> locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            else -> viewModel.detectLocation(userInitiated)
        }
    }

    override fun onResume() {
        super.onResume()
        // TV woken from standby (possibly the next day): refresh "today".
        viewModel.checkDateRollover()
    }

    private companion object {
        const val KEY_PERMISSION_ASKED = "location_permission_asked"
    }
}
