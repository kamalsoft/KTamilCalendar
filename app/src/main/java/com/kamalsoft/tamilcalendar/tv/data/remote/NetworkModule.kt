package com.kamalsoft.tamilcalendar.tv.data.remote

import android.os.Build
import android.util.Log
import com.kamalsoft.tamilcalendar.tv.BuildConfig
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import org.conscrypt.Conscrypt
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.Security
import java.util.concurrent.TimeUnit

object NetworkModule {

    /**
     * Android enables TLS 1.3 natively only from API 29. Installing Conscrypt as the top
     * security provider gives the same TLS 1.3 stack on API 21–28 TV boxes.
     * Must run before the first OkHttpClient is built.
     */
    fun installModernTls() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            runCatching { Security.insertProviderAt(Conscrypt.newProvider(), 1) }
                .onFailure { Log.w("NetworkModule", "Conscrypt unavailable; falling back to platform TLS", it) }
        }
    }

    private val tlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
        .build()

    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectionSpecs(listOf(tlsSpec)) // no CLEARTEXT
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .apply {
            if (BuildConfig.DEBUG) addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        }
        .build()

    /** Null when no backend is configured; the app is then fully offline. */
    fun api(baseUrl: String = BuildConfig.API_BASE_URL): TamilCalendarApi? {
        if (baseUrl.isBlank()) return null
        require(baseUrl.startsWith("https://")) { "API_BASE_URL must be https" }
        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(okHttp())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TamilCalendarApi::class.java)
    }
}
