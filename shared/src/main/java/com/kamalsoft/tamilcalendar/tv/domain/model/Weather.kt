package com.kamalsoft.tamilcalendar.tv.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/** WMO weather interpretation codes grouped into what a TV tile can show. */
enum class WeatherCondition(val ta: String, val en: String) {
    CLEAR("தெளிவான வானம்", "Clear"),
    PARTLY_CLOUDY("ஓரளவு மேகமூட்டம்", "Partly cloudy"),
    CLOUDY("மேகமூட்டம்", "Cloudy"),
    FOG("மூடுபனி", "Fog"),
    DRIZZLE("தூறல்", "Drizzle"),
    RAIN("மழை", "Rain"),
    SNOW("பனிப்பொழிவு", "Snow"),
    THUNDERSTORM("இடியுடன் மழை", "Thunderstorm"),
    ;

    companion object {
        fun fromWmo(code: Int): WeatherCondition = when (code) {
            0 -> CLEAR
            1, 2 -> PARTLY_CLOUDY
            3 -> CLOUDY
            45, 48 -> FOG
            51, 53, 55, 56, 57 -> DRIZZLE
            61, 63, 65, 66, 67, 80, 81, 82 -> RAIN
            71, 73, 75, 77, 85, 86 -> SNOW
            95, 96, 99 -> THUNDERSTORM
            else -> CLOUDY
        }
    }
}

/** Temperatures and wind are in the units the forecast was requested in (see WeatherRepository.imperial). */
data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val humidityPct: Int,
    val windSpeed: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
)

data class HourForecast(val time: LocalDateTime, val temperature: Double, val condition: WeatherCondition, val precipitationPct: Int, val isDay: Boolean)

data class DayForecast(val date: LocalDate, val high: Double, val low: Double, val condition: WeatherCondition, val precipitationPct: Int)

data class WeatherReport(
    val current: CurrentWeather,
    val hourly: List<HourForecast>,
    val daily: List<DayForecast>,
    val fetchedAt: Instant,
)

/** What the UI shows: the last good report (possibly cached), plus whether the latest refresh failed. */
data class WeatherState(val report: WeatherReport?, val refreshFailed: Boolean, val isLoading: Boolean)
