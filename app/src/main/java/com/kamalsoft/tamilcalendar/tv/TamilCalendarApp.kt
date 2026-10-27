package com.kamalsoft.tamilcalendar.tv

import android.app.Application
import com.kamalsoft.tamilcalendar.tv.data.remote.NetworkModule
import com.kamalsoft.tamilcalendar.tv.di.AppContainer

class TamilCalendarApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        NetworkModule.installModernTls()
        container = AppContainer(this)
    }
}
