package com.kamalsoft.tamilcalendar.tv.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.gson.annotations.SerializedName
import com.kamalsoft.tamilcalendar.tv.data.remote.NetworkModule
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

/**
 * Finds where the TV is, for "open with my location by default".
 *
 * 1. Device location (needs ACCESS_COARSE_LOCATION; many TVs have Wi-Fi/network location, few have GPS).
 * 2. Android's Geocoder names that point (city, state, country), if the TV provides one.
 * 3. No device fix or no permission → approximate location from the internet connection (ipapi.co),
 *    which is city-level. This is disclosed in the app's disclaimer.
 */
class LocationDetector(private val context: Context) {

    enum class Source { DEVICE, NETWORK_IP }

    data class Result(val location: GeoLocation, val source: Source)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun detect(): Result? {
        val fix = if (hasPermission()) runCatching { deviceFix() }.onFailure { Log.w(TAG, "Device location failed", it) }.getOrNull() else null
        if (fix != null) {
            val named = describe(fix.latitude, fix.longitude)
            if (named != null) return Result(named, Source.DEVICE)
        }
        return runCatching { ipLocation() }.onFailure { Log.w(TAG, "IP location failed", it) }.getOrNull()
            ?.let { Result(it, Source.NETWORK_IP) }
    }

    // ------------------------------------------------------------------ device

    @SuppressLint("MissingPermission") // checked by hasPermission() before every call
    private suspend fun deviceFix(): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = lm.getProviders(true)
        // A fresh fix first: the cached "last known" location can be from wherever the TV was before.
        // GPS first when the TV has it (most current); otherwise network location (Wi-Fi), which most TVs use.
        val active = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { it in providers }
        for (provider in active) {
            freshFix(lm, provider)?.let { return it }
        }
        // No fresh fix (e.g. GPS-less TV with network location off): fall back to a recent cached one.
        return providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.takeIf { System.currentTimeMillis() - it.time < 24 * 3600_000L }
    }

    @SuppressLint("MissingPermission")
    private suspend fun freshFix(lm: LocationManager, provider: String): Location? = withTimeoutOrNull(12_000) {
        suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    lm.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }
                @Deprecated("Required on API < 29") override fun onStatusChanged(p: String?, s: Int, e: Bundle?) = Unit
                override fun onProviderEnabled(p: String) = Unit
                override fun onProviderDisabled(p: String) = Unit
            }
            runCatching { lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper()) }
                .onFailure { if (cont.isActive) cont.resume(null) }
            cont.invokeOnCancellation { lm.removeUpdates(listener) }
        }
    }

    private data class ZoneResponse(val timezone: String?)

    private interface ZoneApi {
        @GET("v1/forecast?forecast_days=1&timezone=auto&current=temperature_2m")
        suspend fun zoneOf(@Query("latitude") lat: Double, @Query("longitude") lon: Double): ZoneResponse
    }

    private val zoneApi by lazy {
        Retrofit.Builder().baseUrl("https://api.open-meteo.com/").client(NetworkModule.okHttp())
            .addConverterFactory(GsonConverterFactory.create()).build().create(ZoneApi::class.java)
    }

    /**
     * The IANA time zone *of the coordinates* (Open-Meteo resolves it). The TV's own time-zone setting is
     * only a fallback: it can be wrong or unset, and a wrong zone would shift every time in the panchangam.
     */
    private suspend fun zoneFor(lat: Double, lon: Double): String? =
        runCatching { zoneApi.zoneOf(lat, lon).timezone }.getOrNull()?.takeIf { runCatching { ZoneId.of(it) }.isSuccess }
            ?: TimeZone.getDefault().id.takeIf { runCatching { ZoneId.of(it) }.isSuccess }

    /** Names a coordinate with the platform Geocoder; falls back to IP data for names only. */
    private suspend fun describe(lat: Double, lon: Double): GeoLocation? = withContext(Dispatchers.IO) {
        val zone = zoneFor(lat, lon) ?: return@withContext null
        val address = if (Geocoder.isPresent()) {
            @Suppress("DEPRECATION") // the async overload is API 33+
            runCatching { Geocoder(context, Locale.US).getFromLocation(lat, lon, 1)?.firstOrNull() }.getOrNull()
        } else {
            null
        }
        if (address != null) {
            val name = address.locality ?: address.subAdminArea ?: address.adminArea ?: "Current location"
            return@withContext GeoLocation(
                nameTa = knownTamilName(name, address.countryCode) ?: name, nameEn = name,
                latitude = lat, longitude = lon, zoneId = zone,
                countryCode = address.countryCode, admin1 = address.adminArea, countryName = address.countryName,
            )
        }
        val ip = runCatching { ipLocation() }.getOrNull()
        GeoLocation(
            nameTa = ip?.nameTa ?: "தற்போதைய இடம்", nameEn = ip?.nameEn ?: "Current location",
            latitude = lat, longitude = lon, zoneId = zone,
            countryCode = ip?.countryCode, admin1 = ip?.admin1, countryName = ip?.countryName,
        )
    }

    // ------------------------------------------------------------------ IP fallback

    private data class IpApiResponse(
        val city: String?,
        val region: String?,
        @SerializedName("country_code") val countryCode: String?,
        @SerializedName("country_name") val countryName: String?,
        val latitude: Double?,
        val longitude: Double?,
        val timezone: String?,
        val error: Boolean?,
    )

    private interface IpApi {
        @GET("json/")
        suspend fun lookup(): IpApiResponse
    }

    private val ipApi by lazy {
        Retrofit.Builder().baseUrl("https://ipapi.co/").client(NetworkModule.okHttp())
            .addConverterFactory(GsonConverterFactory.create()).build().create(IpApi::class.java)
    }

    private suspend fun ipLocation(): GeoLocation? = withContext(Dispatchers.IO) {
        val r = ipApi.lookup()
        if (r.error == true) return@withContext null
        val zone = r.timezone?.takeIf { runCatching { ZoneId.of(it) }.isSuccess } ?: return@withContext null
        val name = r.city ?: return@withContext null
        GeoLocation(
            nameTa = knownTamilName(name, r.countryCode) ?: name, nameEn = name,
            latitude = r.latitude ?: return@withContext null, longitude = r.longitude ?: return@withContext null,
            zoneId = zone, countryCode = r.countryCode, admin1 = r.region, countryName = r.countryName,
        )
    }

    private fun knownTamilName(name: String, countryCode: String?): String? =
        GeoLocation.QUICK
            .firstOrNull { it.nameEn.equals(name, ignoreCase = true) && it.countryCode == countryCode }?.nameTa

    private companion object {
        const val TAG = "LocationDetector"
    }
}
