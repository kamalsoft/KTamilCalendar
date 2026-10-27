package com.kamalsoft.tamilcalendar.tv.data.repository

import android.content.SharedPreferences
import com.kamalsoft.tamilcalendar.tv.domain.model.AppSettings
import com.kamalsoft.tamilcalendar.tv.domain.model.ConventionPref
import com.kamalsoft.tamilcalendar.tv.domain.model.MusicStyle
import com.kamalsoft.tamilcalendar.tv.domain.model.Religion
import com.kamalsoft.tamilcalendar.tv.domain.model.UnitsPref
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Settings in SharedPreferences, one key per field (robust to adding fields later), exposed as a StateFlow. */
class SettingsRepository(private val prefs: SharedPreferences) {
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        save(next)
        _settings.value = next
    }

    private fun load(): AppSettings {
        val d = AppSettings()
        fun b(k: String, def: Boolean) = prefs.getBoolean("s_$k", def)
        return AppSettings(
            hiddenSections = prefs.getStringSet("s_hiddenSections", emptySet()).orEmpty().toSet(),
            weather = b("weather", d.weather),
            hiddenFaiths = prefs.getStringSet("s_hiddenFaiths", emptySet()).orEmpty()
                .mapNotNull { n -> Religion.entries.firstOrNull { it.name == n } }.toSet(),
            usHolidays = b("usHolidays", d.usHolidays),
            schoolCalendars = b("schoolCalendars", d.schoolCalendars),
            tamilNaduHolidays = b("tamilNaduHolidays", d.tamilNaduHolidays),
            animations = b("animations", d.animations),
            weatherAnimations = b("weatherAnimations", d.weatherAnimations),
            clock24h = b("clock24h", d.clock24h),
            tamilOnly = b("tamilOnly", d.tamilOnly),
            units = UnitsPref.entries.firstOrNull { it.name == prefs.getString("s_units", null) } ?: d.units,
            convention = ConventionPref.entries.firstOrNull { it.name == prefs.getString("s_convention", null) } ?: d.convention,
            ambient = b("ambient", d.ambient),
            ambientMinutes = prefs.getInt("s_ambientMinutes", d.ambientMinutes),
            rahuReminder = b("rahuReminder", d.rahuReminder),
            nallaReminder = b("nallaReminder", d.nallaReminder),
            slokaMusic = b("slokaMusic", d.slokaMusic),
            slokaVoice = b("slokaVoice", d.slokaVoice),
            music = MusicStyle(
                raga = enumOr("s_musicRaga", d.music.raga),
                tempo = enumOr("s_musicTempo", d.music.tempo),
                bell = enumOr("s_musicBell", d.music.bell),
                pitch = enumOr("s_musicPitch", d.music.pitch),
                mix = enumOr("s_musicMix", d.music.mix),
            ),
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(key: String, default: E): E =
        enumValues<E>().firstOrNull { it.name == prefs.getString(key, null) } ?: default

    private fun save(s: AppSettings) {
        prefs.edit()
            .putStringSet("s_hiddenSections", s.hiddenSections)
            .putBoolean("s_weather", s.weather)
            .putStringSet("s_hiddenFaiths", s.hiddenFaiths.map { it.name }.toSet())
            .putBoolean("s_usHolidays", s.usHolidays)
            .putBoolean("s_schoolCalendars", s.schoolCalendars)
            .putBoolean("s_tamilNaduHolidays", s.tamilNaduHolidays)
            .putBoolean("s_animations", s.animations)
            .putBoolean("s_weatherAnimations", s.weatherAnimations)
            .putBoolean("s_clock24h", s.clock24h)
            .putBoolean("s_tamilOnly", s.tamilOnly)
            .putString("s_units", s.units.name)
            .putString("s_convention", s.convention.name)
            .putBoolean("s_ambient", s.ambient)
            .putInt("s_ambientMinutes", s.ambientMinutes)
            .putBoolean("s_rahuReminder", s.rahuReminder)
            .putBoolean("s_nallaReminder", s.nallaReminder)
            .putBoolean("s_slokaMusic", s.slokaMusic)
            .putBoolean("s_slokaVoice", s.slokaVoice)
            .putString("s_musicRaga", s.music.raga.name)
            .putString("s_musicTempo", s.music.tempo.name)
            .putString("s_musicBell", s.music.bell.name)
            .putString("s_musicPitch", s.music.pitch.name)
            .putString("s_musicMix", s.music.mix.name)
            .apply()
    }
}
