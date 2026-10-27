package com.kamalsoft.tamilcalendar.tv.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.kamalsoft.tamilcalendar.tv.data.remote.WeatherApi
import com.kamalsoft.tamilcalendar.tv.data.remote.dto.OpenMeteoResponse
import com.kamalsoft.tamilcalendar.tv.domain.model.CurrentWeather
import com.kamalsoft.tamilcalendar.tv.domain.model.DayForecast
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.domain.model.HourForecast
import com.kamalsoft.tamilcalendar.tv.domain.model.WeatherCondition
import com.kamalsoft.tamilcalendar.tv.domain.model.WeatherReport
import com.kamalsoft.tamilcalendar.tv.domain.model.WeatherState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes

/**
 * Weather is a nice-to-have on a calendar: it must never block or break the screen.
 * The last good response is cached, shown immediately on start, and kept when refreshes fail.
 */
class WeatherRepository(
    private val api: WeatherApi,
    private val location: GeoLocation,
    /** °F + mph when true, °C + km/h otherwise. */
    val imperial: Boolean,
    private val prefs: SharedPreferences,
    private val io: CoroutineDispatcher,
) {
    private val gson = Gson()
    private val zone = ZoneId.of(location.zoneId)

    /** Emits cached data first, then refreshes every 30 minutes while collected. */
    fun observe(): Flow<WeatherState> = flow {
        var report = loadCache()
        emit(WeatherState(report, refreshFailed = false, isLoading = report == null))
        while (true) {
            val fresh = runCatching { fetch() }
                .onFailure { Log.w("WeatherRepository", "Weather refresh failed", it) }
                .getOrNull()
            if (fresh != null) report = fresh
            emit(WeatherState(report, refreshFailed = fresh == null, isLoading = false))
            delay(if (fresh != null) 30.minutes else 5.minutes)
        }
    }.flowOn(io)

    private suspend fun fetch(): WeatherReport {
        val raw = api.forecast(
            location.latitude, location.longitude, location.zoneId,
            temperatureUnit = if (imperial) "fahrenheit" else "celsius",
            windSpeedUnit = if (imperial) "mph" else "kmh",
        )
        val report = map(raw, Instant.now()) ?: error("Incomplete weather response")
        prefs.edit()
            .putString(KEY_JSON, gson.toJson(raw))
            .putLong(KEY_AT, report.fetchedAt.toEpochMilli())
            .putString(KEY_FOR, cacheKey)
            .apply()
        return report
    }

    /** A cached forecast is only valid for the same place and units. */
    private val cacheKey = "${location.latitude},${location.longitude},$imperial"

    private fun loadCache(): WeatherReport? = runCatching {
        if (prefs.getString(KEY_FOR, null) != cacheKey) return null
        val json = prefs.getString(KEY_JSON, null) ?: return null
        map(gson.fromJson(json, OpenMeteoResponse::class.java), Instant.ofEpochMilli(prefs.getLong(KEY_AT, 0)))
    }.getOrNull()

    private fun map(r: OpenMeteoResponse, fetchedAt: Instant): WeatherReport? {
        val c = r.current ?: return null
        val now = LocalDateTime.now(zone).withMinute(0).withSecond(0).withNano(0)
        val hourly = r.hourly?.let { h ->
            h.time.orEmpty().mapIndexedNotNull { i, t ->
                val time = LocalDateTime.parse(t)
                if (time.isBefore(now)) return@mapIndexedNotNull null
                HourForecast(
                    time = time,
                    temperature = h.temperature?.getOrNull(i) ?: return@mapIndexedNotNull null,
                    condition = WeatherCondition.fromWmo(h.weatherCode?.getOrNull(i) ?: 3),
                    precipitationPct = h.precipitationProbability?.getOrNull(i) ?: 0,
                    isDay = h.isDay?.getOrNull(i) != 0,
                )
            }
        }.orEmpty()
        val daily = r.daily?.let { d ->
            d.time.orEmpty().mapIndexedNotNull { i, t ->
                DayForecast(
                    date = LocalDate.parse(t),
                    high = d.max?.getOrNull(i) ?: return@mapIndexedNotNull null,
                    low = d.min?.getOrNull(i) ?: return@mapIndexedNotNull null,
                    condition = WeatherCondition.fromWmo(d.weatherCode?.getOrNull(i) ?: 3),
                    precipitationPct = d.precipitationProbability?.getOrNull(i) ?: 0,
                )
            }
        }.orEmpty()
        return WeatherReport(
            current = CurrentWeather(
                temperature = c.temperature ?: return null,
                feelsLike = c.apparentTemperature ?: c.temperature,
                humidityPct = c.humidity ?: 0,
                windSpeed = c.windSpeed ?: 0.0,
                condition = WeatherCondition.fromWmo(c.weatherCode ?: 3),
                isDay = c.isDay != 0,
            ),
            hourly = hourly,
            daily = daily,
            fetchedAt = fetchedAt,
        )
    }

    private companion object {
        const val KEY_JSON = "weather_json"
        const val KEY_AT = "weather_fetched_at"
        const val KEY_FOR = "weather_cache_key"
    }
}
