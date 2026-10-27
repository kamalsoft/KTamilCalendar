package com.kamalsoft.tamilcalendar.tv.ui.ambient

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import com.kamalsoft.tamilcalendar.tv.domain.model.WeatherState
import com.kamalsoft.tamilcalendar.tv.ui.components.Fmt
import com.kamalsoft.tamilcalendar.tv.ui.components.Text
import com.kamalsoft.tamilcalendar.tv.ui.theme.DisplayPrefs
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import com.kamalsoft.tamilcalendar.tv.ui.weather.WeatherScene
import com.kamalsoft.tamilcalendar.tv.ui.weather.iconRes
import com.kamalsoft.tamilcalendar.tv.ui.weather.seasonFor
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * "Panchangam at a glance": full-screen ambient clock shown when the TV is idle (and as the system
 * screensaver). Vector temple scene with a sky that follows the local time, flickering lamps, rising
 * embers and the live weather scene; the text block drifts slowly to protect OLED panels from burn-in.
 */
@Composable
fun AmbientScreen(day: TamilDayData?, weather: WeatherState, location: GeoLocation, modifier: Modifier = Modifier) {
    val zone = ZoneId.of(location.zoneId)
    val seconds by produceState(0f) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { value = (it - start) / 1e9f }
    }
    val now = LocalDateTime.now(zone)
    val hour = now.hour + now.minute / 60f
    val c = weather.report?.current

    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        Canvas(Modifier.fillMaxSize()) { templeScene(seconds, hour) }
        if (c != null) {
            WeatherScene(
                condition = c.condition, isDay = c.isDay, season = seasonFor(now.toLocalDate(), location), hour = now.hour,
                modifier = Modifier.fillMaxSize(), density = 0.7f, alpha = 0.8f,
            )
        }
        // Burn-in protection: a slow Lissajous drift of ±24 dp over ~4 minutes.
        val px = LocalDensity.current.density
        val drift = IntOffset(
            (sin(seconds * 2 * PI / 240) * 24 * px).roundToInt(),
            (sin(seconds * 2 * PI / 170) * 16 * px).roundToInt(),
        )
        val compact = DisplayPrefs.compact
        Column(
            Modifier.align(if (compact) Alignment.Center else Alignment.CenterStart).offset { drift }
                .padding(horizontal = if (compact) 24.dp else 64.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(Fmt.timeEn(now.toLocalTime()).let { if (DisplayPrefs.clock24h) it else it.substringBefore(' ') }, fontSize = if (compact) 72.sp else 96.sp, fontWeight = FontWeight.Light, color = Color.White)
            if (day != null) {
                Text(
                    "${day.weekdayTa} · ${day.tamilDate.displayTa} · ${Fmt.gregorianEn(day.date)}",
                    fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFE08A),
                )
                Glance("திதி · Tithi", "${day.panchangam.tithi.nameTa} — ${Fmt.until(day.panchangam.tithi.endsAt, day.date)}")
                val next = day.timings.nallaNeram.firstOrNull { it.end > now.toLocalTime() } ?: day.timings.nallaNeram.firstOrNull()
                next?.let { Glance("நல்ல நேரம் · Nalla Neram", Fmt.range(it)) }
                Glance("ராகு காலம் · Rahu Kalam", Fmt.range(day.timings.rahuKalam), alert = true)
            }
            if (c != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Image(painterResource(c.condition.iconRes(c.isDay)), contentDescription = null, modifier = Modifier.size(34.dp))
                    Text("  ${c.temperature.roundToInt()}° · ${c.condition.en} · ${location.nameEn}", fontSize = 18.sp, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
    }
}

@Composable
private fun Glance(label: String, value: String, alert: Boolean = false) {
    Row(
        Modifier.background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = if (alert) Color(0xFFFF8A80) else Color(0xFFFFD54F))
        Text("   $value", fontSize = 18.sp, color = Color.White)
    }
}

// ---------------------------------------------------------------------- vector temple scene

private fun skyColors(hour: Float): List<Color> = when {
    hour < 5f || hour >= 20f -> listOf(Color(0xFF070B1F), Color(0xFF141A3A), Color(0xFF241A35))
    hour < 7f -> listOf(Color(0xFF2B2350), Color(0xFFB8577A), Color(0xFFFFB46B))
    hour < 17f -> listOf(Color(0xFF2E6FB7), Color(0xFF6FA8DC), Color(0xFFF6D7A7))
    else -> listOf(Color(0xFF2A1E4E), Color(0xFFC0566A), Color(0xFFFF9E57))
}

private fun DrawScope.templeScene(t: Float, hour: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(skyColors(hour)))
    // Slow parallax pan (≈ 2 minute period).
    val pan = sin(t * 2 * PI.toFloat() / 120f) * w * 0.015f
    val night = hour < 5f || hour >= 19.5f
    val silhouette = if (night) Color(0xFF05060F) else Color(0xFF2A1810)
    val far = silhouette.copy(alpha = 0.55f)

    // Distant hills.
    val hills = Path().apply {
        moveTo(0f, h * 0.72f)
        for (i in 0..8) {
            val x = w * i / 8f
            quadraticTo(x - w / 16f + pan * 0.5f, h * (0.62f + 0.05f * sin(i * 1.7f)), x, h * 0.7f)
        }
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(hills, far)
    // Three gopurams: the main tower right of centre, two smaller ones.
    gopuram(Offset(w * 0.70f + pan, h * 0.88f), w * 0.18f, h * 0.62f, silhouette)
    gopuram(Offset(w * 0.47f + pan * 1.2f, h * 0.88f), w * 0.11f, h * 0.38f, silhouette.copy(alpha = 0.85f))
    gopuram(Offset(w * 0.90f + pan * 1.2f, h * 0.88f), w * 0.10f, h * 0.33f, silhouette.copy(alpha = 0.85f))
    // Mandapam wall and ground.
    drawRect(silhouette, Offset(0f, h * 0.86f), Size(w, h * 0.14f))
    for (i in 0 until 14) {
        val x = w * (0.05f + i * 0.07f) + pan * 1.5f
        drawRect(silhouette, Offset(x, h * 0.80f), Size(w * 0.012f, h * 0.06f))
    }
    drawRect(silhouette, Offset(0f, h * 0.79f), Size(w, h * 0.015f))
    // Oil lamps along the wall, flickering.
    for (i in 0 until 12) {
        val x = w * (0.06f + i * 0.08f) + pan * 1.5f
        val y = h * 0.775f
        val flick = 0.75f + 0.25f * sin(t * (7f + i) + i * 1.3f)
        drawCircle(Brush.radialGradient(listOf(Color(0x99FFB347), Color.Transparent), Offset(x, y), h * 0.04f * flick), h * 0.04f * flick, Offset(x, y))
        drawCircle(Color(0xFFFFE08A).copy(alpha = flick), h * 0.005f, Offset(x, y))
    }
    // Embers rising from the lamps.
    for (i in 0 until 26) {
        val speed = 0.03f + 0.02f * ((i * 37) % 10) / 10f
        val p = (t * speed + i * 0.137f) % 1f
        val x = w * (0.06f + (i % 12) * 0.08f) + sin(t * 0.8f + i) * w * 0.01f + pan * 1.5f
        val y = h * 0.77f - p * h * 0.45f
        drawCircle(Color(0xFFFFC46B).copy(alpha = (1f - p) * 0.6f), h * 0.0035f, Offset(x, y))
    }
}

/** Stepped gopuram silhouette: base, five shrinking tiers, barrel-vault crown and kalasams. */
private fun DrawScope.gopuram(base: Offset, width: Float, height: Float, color: Color) {
    val tiers = 6
    val tierH = height * 0.8f / tiers
    var y = base.y
    var wTier = width
    for (i in 0 until tiers) {
        val top = wTier * 0.86f
        val path = Path().apply {
            moveTo(base.x - wTier / 2, y)
            lineTo(base.x + wTier / 2, y)
            lineTo(base.x + top / 2, y - tierH)
            lineTo(base.x - top / 2, y - tierH)
            close()
        }
        drawPath(path, color)
        // Niche windows give the tiers texture.
        if (i > 0) {
            for (k in -2..2) {
                drawRect(Color.Black.copy(alpha = 0.18f), Offset(base.x + k * wTier * 0.16f - wTier * 0.02f, y - tierH * 0.75f), Size(wTier * 0.04f, tierH * 0.45f))
            }
        }
        y -= tierH
        wTier = top
    }
    val crownW = wTier * 0.9f
    val crownH = height * 0.12f
    drawRoundRect(color, Offset(base.x - crownW / 2, y - crownH), Size(crownW, crownH), androidx.compose.ui.geometry.CornerRadius(crownH / 2, crownH / 2))
    for (k in -1..1) {
        val kx = base.x + k * crownW * 0.3f
        drawRect(color, Offset(kx - width * 0.006f, y - crownH - height * 0.05f), Size(width * 0.012f, height * 0.05f))
        drawCircle(color, width * 0.012f, Offset(kx, y - crownH - height * 0.05f))
    }
}

/** "No weather" state for callers with weather turned off. */
val NoWeather = WeatherState(null, refreshFailed = false, isLoading = false)
