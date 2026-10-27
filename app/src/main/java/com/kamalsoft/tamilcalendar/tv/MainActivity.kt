package com.kamalsoft.tamilcalendar.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.kamalsoft.tamilcalendar.tv.ui.app.CalendarViewModel
import com.kamalsoft.tamilcalendar.tv.ui.app.TamilCalendarTvApp
import com.kamalsoft.tamilcalendar.tv.ui.theme.AppTheme
import com.kamalsoft.tamilcalendar.tv.ui.theme.TamilCalendarTheme
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors

class MainActivity : ComponentActivity() {
    private val viewModel: CalendarViewModel by viewModels { CalendarViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply the saved colour theme before the first frame, so there's no flash of the default theme.
        val settings = (application as TamilCalendarApp).container.settings
        TvColors.palette = AppTheme.fromName(settings.getString(CalendarViewModel.KEY_THEME, null)).palette
        setContent {
            TamilCalendarTheme {
                TamilCalendarTvApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // TV woken from standby (possibly the next day): refresh "today".
        viewModel.checkDateRollover()
    }
}
