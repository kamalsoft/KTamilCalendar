package com.kamalsoft.tamilcalendar.tv.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Open-Meteo wire format. Times are local ISO strings in the requested time zone. */
data class OpenMeteoResponse(
    val current: Current?,
    val hourly: Hourly?,
    val daily: Daily?,
) {
    data class Current(
        @SerializedName("temperature_2m") val temperature: Double?,
        @SerializedName("apparent_temperature") val apparentTemperature: Double?,
        @SerializedName("relative_humidity_2m") val humidity: Int?,
        @SerializedName("weather_code") val weatherCode: Int?,
        @SerializedName("wind_speed_10m") val windSpeed: Double?,
        @SerializedName("is_day") val isDay: Int?,
    )

    data class Hourly(
        val time: List<String>?,
        @SerializedName("temperature_2m") val temperature: List<Double?>?,
        @SerializedName("weather_code") val weatherCode: List<Int?>?,
        @SerializedName("precipitation_probability") val precipitationProbability: List<Int?>?,
        @SerializedName("is_day") val isDay: List<Int?>?,
    )

    data class Daily(
        val time: List<String>?,
        @SerializedName("weather_code") val weatherCode: List<Int?>?,
        @SerializedName("temperature_2m_max") val max: List<Double?>?,
        @SerializedName("temperature_2m_min") val min: List<Double?>?,
        @SerializedName("precipitation_probability_max") val precipitationProbability: List<Int?>?,
    )
}
