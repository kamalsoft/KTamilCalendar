package com.kamalsoft.tamilcalendar.tv.data.repository

import android.content.SharedPreferences
import com.google.gson.Gson
import com.kamalsoft.tamilcalendar.tv.data.remote.GeocodingApi
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.ZoneId

/** Remembers the chosen location and searches for new ones by name. */
class LocationRepository(
    private val api: GeocodingApi,
    private val prefs: SharedPreferences,
    private val default: GeoLocation,
    private val io: CoroutineDispatcher,
) {
    private val gson = Gson()

    fun current(): GeoLocation = runCatching {
        prefs.getString(KEY, null)?.let { gson.fromJson(it, GeoLocation::class.java) }
            ?.takeIf { runCatching { ZoneId.of(it.zoneId) }.isSuccess }
    }.getOrNull() ?: default

    fun save(location: GeoLocation) {
        prefs.edit().putString(KEY, gson.toJson(location)).apply()
    }

    /** Auto: follow where the TV is (default). Turned off when the user picks a place by hand. */
    var autoDetect: Boolean
        get() = prefs.getBoolean(KEY_AUTO, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO, value).apply()

    suspend fun search(query: String): List<GeoLocation> = withContext(io) {
        api.search(query.trim()).results.orEmpty().mapNotNull { r ->
            val zone = r.timezone?.takeIf { runCatching { ZoneId.of(it) }.isSuccess } ?: return@mapNotNull null
            val name = r.name ?: return@mapNotNull null
            // Keep the Tamil names of the built-in places when the user picks them again.
            val known = listOf(GeoLocation.NAPERVILLE, GeoLocation.CHENNAI).firstOrNull {
                it.nameEn.equals(name, ignoreCase = true) && it.countryCode == r.countryCode
            }
            GeoLocation(
                nameTa = known?.nameTa ?: name,
                nameEn = name,
                latitude = r.latitude ?: return@mapNotNull null,
                longitude = r.longitude ?: return@mapNotNull null,
                zoneId = zone,
                countryCode = r.countryCode,
                admin1 = r.admin1,
                countryName = r.country,
            )
        }
    }

    private companion object {
        const val KEY = "location_json"
        const val KEY_AUTO = "location_auto"
    }
}
