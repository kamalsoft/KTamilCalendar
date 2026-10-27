package com.kamalsoft.tamilcalendar.tv.data.remote

import com.kamalsoft.tamilcalendar.tv.data.remote.dto.OpenMeteoResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo forecast API (https://open-meteo.com): no API key, HTTPS, uses NOAA/NWS models in the US.
 * Free for non-commercial use; commercial distribution needs an Open-Meteo API plan.
 */
interface WeatherApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("timezone") timezone: String,
        @Query("current") current: String = "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day",
        @Query("hourly") hourly: String = "temperature_2m,weather_code,precipitation_probability,is_day",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
        @Query("temperature_unit") temperatureUnit: String,
        @Query("wind_speed_unit") windSpeedUnit: String,
        @Query("forecast_days") days: Int = 7,
    ): OpenMeteoResponse

    companion object {
        fun create(): WeatherApi = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(NetworkModule.okHttp())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
}
