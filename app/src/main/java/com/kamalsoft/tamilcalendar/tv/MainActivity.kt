package com.kamalsoft.tamilcalendar.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.kamalsoft.tamilcalendar.tv.ui.app.CalendarViewModel
import com.kamalsoft.tamilcalendar.tv.ui.app.TamilCalendarTvApp
import com.kamalsoft.tamilcalendar.tv.ui.theme.TamilCalendarTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CalendarViewModel by viewModels { CalendarViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
