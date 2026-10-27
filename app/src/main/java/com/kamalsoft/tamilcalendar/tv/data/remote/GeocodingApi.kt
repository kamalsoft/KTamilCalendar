package com.kamalsoft.tamilcalendar.tv.data.remote

import com.kamalsoft.tamilcalendar.tv.data.remote.dto.GeocodingResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/** Open-Meteo place search (https://open-meteo.com/en/docs/geocoding-api): free, no key, returns the IANA time zone. */
interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json",
    ): GeocodingResponse

    companion object {
        fun create(): GeocodingApi = Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(NetworkModule.okHttp())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingApi::class.java)
    }
}
