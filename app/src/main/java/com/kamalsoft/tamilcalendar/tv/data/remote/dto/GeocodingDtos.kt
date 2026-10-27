package com.kamalsoft.tamilcalendar.tv.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GeocodingResponse(val results: List<GeocodingResult>?)

data class GeocodingResult(
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val timezone: String?,
    val country: String?,
    @SerializedName("country_code") val countryCode: String?,
    val admin1: String?,
    val population: Long?,
)
