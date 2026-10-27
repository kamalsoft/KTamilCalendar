package com.kamalsoft.tamilcalendar.tv.data.remote

import com.kamalsoft.tamilcalendar.tv.data.remote.dto.HolidayDto
import com.kamalsoft.tamilcalendar.tv.data.remote.dto.RasiPalanDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Contract for the optional sync backend. Computed data covers everything astronomical;
 * the server only supplies what cannot be computed: the official Tamil Nadu G.O. holiday list
 * (incl. moon-sighting based Islamic holidays) and editorial Rasi Palan.
 */
interface TamilCalendarApi {
    @GET("v1/holidays")
    suspend fun holidays(@Query("year") year: Int, @Query("state") state: String = "TN"): List<HolidayDto>

    @GET("v1/rasi-palan")
    suspend fun rasiPalan(@Query("from") fromIso: String, @Query("to") toIso: String): List<RasiPalanDto>
}
