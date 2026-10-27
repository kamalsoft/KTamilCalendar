package com.kamalsoft.tamilcalendar.tv.data.remote.dto

/** Wire format of the (self-hosted) calendar API. All fields nullable: Gson ignores Kotlin defaults. */
data class HolidayDto(
    /** ISO-8601 date, e.g. "2026-03-21". */
    val date: String?,
    val titleTa: String?,
    val titleEn: String?,
    /** FESTIVAL | VIRATHAM | GOVT_HOLIDAY */
    val kind: String?,
    val isHoliday: Boolean?,
)

data class RasiPalanDto(
    val date: String?,
    /** 0 = Mesham … 11 = Meenam */
    val rasi: Int?,
    val messageTa: String?,
    val messageEn: String?,
)
