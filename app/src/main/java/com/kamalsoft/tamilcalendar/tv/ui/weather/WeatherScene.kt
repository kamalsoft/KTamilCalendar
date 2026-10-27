package com.kamalsoft.tamilcalendar.tv.ui.weather

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.ui.theme.DisplayPrefs
import com.kamalsoft.tamilcalendar.tv.domain.model.WeatherCondition
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class Season { WINTER, SPRING, SUMMER, MONSOON, AUTUMN }

/**
 * Season for a place and date. India uses its own seasons (summer Mar–May, monsoon Jun–Sep,
 * post-monsoon Oct–Nov, winter Dec–Feb); elsewhere meteorological seasons, flipped south of the equator.
 */
fun seasonFor(date: LocalDate, location: GeoLocation): Season {
    val m = date.monthValue
    if (location.countryCode == "IN") {
        return when (m) {
            in 3..5 -> Season.SUMMER
            in 6..9 -> Season.MONSOON
            10, 11 -> Season.AUTUMN
            else -> Season.WINTER
        }
    }
    val north = when (m) {
        12, 1, 2 -> Season.WINTER
        in 3..5 -> Season.SPRING
        in 6..8 -> Season.SUMMER
        else -> Season.AUTUMN
    }
    if (location.latitude >= 0) return north
    return when (north) {
        Season.WINTER -> Season.SUMMER
        Season.SUMMER -> Season.WINTER
        Season.SPRING -> Season.AUTUMN
        else -> Season.SPRING
    }
}

/** Deterministic pseudo-random in [0,1) for particle i, channel k: no allocation per frame. */
private fun rnd(i: Int, k: Int): Float {
    val x = sin((i * 127.1 + k * 311.7) * 0.0174533) * 43758.5453
    return (x - kotlin.math.floor(x)).toFloat()
}

/**
 * Animated weather backdrop. Draws only while [active] (e.g. the weather page is on screen), so a
 * hidden scene costs nothing. Particle counts scale with [density] (1 = full page, ~0.4 = small card).
 * Everything is procedural vector drawing: sharp at any resolution, no assets.
 */
@Composable
fun WeatherScene(
    condition: WeatherCondition,
    isDay: Boolean,
    season: Season,
    hour: Int,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    density: Float = 1f,
    alpha: Float = 1f,
) {
    if (!DisplayPrefs.weatherAnimations) return
    val time by produceState(0f, active) {
        if (!active) return@produceState
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { value = (it - start) / 1_000_000_000f }
        }
    }
    Canvas(
        modifier
            // Soft edges: the scene fades out towards every border instead of ending in a hard rectangle.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val edge = Brush.horizontalGradient(0f to Color.Transparent, 0.12f to Color.Black, 0.88f to Color.Black, 1f to Color.Transparent)
                val vEdge = Brush.verticalGradient(0f to Color.Transparent, 0.18f to Color.Black, 0.85f to Color.Black, 1f to Color.Transparent)
                drawRect(edge, blendMode = BlendMode.DstIn)
                drawRect(vEdge, blendMode = BlendMode.DstIn)
            },
    ) {
        val t = time
        skyTint(hour, alpha)
        when (condition) {
            WeatherCondition.CLEAR -> if (isDay) sun(t, alpha) else stars(t, density, alpha)
            WeatherCondition.PARTLY_CLOUDY -> {
                if (isDay) sun(t, alpha * 0.7f) else stars(t, density * 0.6f, alpha)
                clouds(t, 3, alpha * 0.8f)
            }
            WeatherCondition.CLOUDY -> clouds(t, 5, alpha)
            WeatherCondition.FOG -> fog(t, alpha)
            WeatherCondition.DRIZZLE -> { clouds(t, 3, alpha * 0.7f); rain(t, (40 * density).toInt(), 0.6f, alpha) }
            WeatherCondition.RAIN -> { clouds(t, 4, alpha * 0.8f); rain(t, (90 * density).toInt(), 1f, alpha) }
            WeatherCondition.THUNDERSTORM -> { clouds(t, 5, alpha); rain(t, (110 * density).toInt(), 1.2f, alpha); lightning(t, alpha) }
            WeatherCondition.SNOW -> { clouds(t, 3, alpha * 0.6f); snow(t, (70 * density).toInt(), alpha) }
        }
        // Seasonal accents only when the sky is calm (no rain/snow/fog to compete with).
        if (condition == WeatherCondition.CLEAR || condition == WeatherCondition.PARTLY_CLOUDY || condition == WeatherCondition.CLOUDY) {
            when (season) {
                Season.AUTUMN -> leaves(t, (14 * density).toInt(), alpha)
                Season.SPRING -> petals(t, (16 * density).toInt(), alpha)
                Season.WINTER -> sparkles(t, (24 * density).toInt(), alpha)
                else -> Unit
            }
        }
    }
}

// ---------------------------------------------------------------------- layers

private fun DrawScope.skyTint(hour: Int, a: Float) {
    val c = when (hour) {
        in 5..7 -> Color(0xFFFF9E6B)  // dawn
        in 8..16 -> Color(0xFF6BB8FF) // day
        in 17..19 -> Color(0xFFFF7A59) // dusk
        else -> Color(0xFF3B4C9B)     // night
    }
    drawRect(Brush.verticalGradient(listOf(c.copy(alpha = 0.18f * a), Color.Transparent)))
}

private fun DrawScope.sun(t: Float, a: Float) {
    val c = Offset(size.width * 0.86f, size.height * 0.14f)
    val r = min(size.width, size.height) * 0.09f
    val pulse = 1f + 0.06f * sin(t * 1.4f)
    drawCircle(Brush.radialGradient(listOf(Color(0x66FFD54F), Color.Transparent), c, r * 4f * pulse), r * 4f * pulse, c, alpha = a)
    rotate(t * 8f, c) {
        for (i in 0 until 12) {
            val ang = i * (2 * PI / 12).toFloat()
            val from = Offset(c.x + cos(ang) * r * 1.4f, c.y + sin(ang) * r * 1.4f)
            val to = Offset(c.x + cos(ang) * r * 2.2f, c.y + sin(ang) * r * 2.2f)
            drawLine(Color(0xFFFFD54F).copy(alpha = 0.55f * a), from, to, strokeWidth = r * 0.16f, cap = StrokeCap.Round)
        }
    }
    drawCircle(Color(0xFFFFE082).copy(alpha = 0.85f * a), r, c)
}

private fun DrawScope.stars(t: Float, density: Float, a: Float) {
    val n = (60 * density).toInt()
    for (i in 0 until n) {
        val tw = 0.35f + 0.65f * abs(sin(t * (0.6f + rnd(i, 3)) + rnd(i, 4) * 6f))
        drawCircle(
            Color.White.copy(alpha = 0.7f * tw * a), size.minDimension * (0.0015f + 0.0025f * rnd(i, 2)),
            Offset(rnd(i, 0) * size.width, rnd(i, 1) * size.height * 0.7f),
        )
    }
    val moon = Offset(size.width * 0.86f, size.height * 0.15f)
    val r = size.minDimension * 0.06f
    drawCircle(Brush.radialGradient(listOf(Color(0x40E8E6F5), Color.Transparent), moon, r * 3.5f), r * 3.5f, moon, alpha = a)
}

private fun DrawScope.clouds(t: Float, n: Int, a: Float) {
    for (i in 0 until n) {
        val speed = 0.012f + 0.02f * rnd(i, 1)
        val w = size.width * (0.25f + 0.2f * rnd(i, 2))
        val x = ((rnd(i, 0) + t * speed) % 1.3f - 0.15f) * size.width
        val y = size.height * (0.05f + 0.35f * rnd(i, 3))
        val col = Color(0xFFDDE3EE).copy(alpha = (0.07f + 0.06f * rnd(i, 4)) * a)
        cloudShape(Offset(x, y), w, col)
    }
}

private fun DrawScope.cloudShape(c: Offset, w: Float, col: Color) {
    val h = w * 0.32f
    drawOval(col, Offset(c.x - w / 2, c.y - h / 2), Size(w, h))
    drawCircle(col, h * 0.75f, Offset(c.x - w * 0.15f, c.y - h * 0.35f))
    drawCircle(col, h * 0.6f, Offset(c.x + w * 0.15f, c.y - h * 0.3f))
}

private fun DrawScope.rain(t: Float, n: Int, strength: Float, a: Float) {
    val len = size.height * 0.05f * strength
    for (i in 0 until n) {
        val speed = 0.55f + 0.5f * rnd(i, 1)
        val y = ((rnd(i, 0) + t * speed) % 1f) * (size.height + len) - len
        val x = rnd(i, 2) * size.width + (y / size.height) * size.width * 0.05f
        drawLine(
            Color(0xFF9FD3FF).copy(alpha = (0.25f + 0.25f * rnd(i, 3)) * a),
            Offset(x, y), Offset(x - len * 0.18f, y + len), strokeWidth = size.minDimension * 0.0022f, cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.lightning(t: Float, a: Float) {
    // Brief double flash roughly every 6 s.
    val phase = t % 6f
    val flash = when {
        phase < 0.08f -> 1f
        phase in 0.18f..0.26f -> 0.6f
        else -> 0f
    }
    if (flash > 0f) drawRect(Color.White.copy(alpha = 0.18f * flash * a))
}

private fun DrawScope.snow(t: Float, n: Int, a: Float) {
    for (i in 0 until n) {
        val speed = 0.05f + 0.07f * rnd(i, 1)
        val y = ((rnd(i, 0) + t * speed) % 1f) * size.height
        val sway = sin(t * (0.8f + rnd(i, 3)) + i) * size.width * 0.015f
        drawCircle(Color.White.copy(alpha = (0.45f + 0.4f * rnd(i, 4)) * a), size.minDimension * (0.003f + 0.004f * rnd(i, 2)), Offset(rnd(i, 2) * size.width + sway, y))
    }
}

private fun DrawScope.fog(t: Float, a: Float) {
    for (i in 0 until 4) {
        val y = size.height * (0.25f + i * 0.18f)
        val x = (sin(t * 0.15f + i) * 0.15f) * size.width
        drawRect(
            Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFCFD8DC).copy(alpha = 0.12f * a), Color.Transparent), x, x + size.width),
            Offset(0f, y), Size(size.width, size.height * 0.1f),
        )
    }
}

private fun DrawScope.leaves(t: Float, n: Int, a: Float) {
    val colors = listOf(Color(0xFFE07A2E), Color(0xFFC94F25), Color(0xFFE8B04A), Color(0xFFA8432B))
    for (i in 0 until n) {
        val speed = 0.035f + 0.04f * rnd(i, 1)
        val y = ((rnd(i, 0) + t * speed) % 1.1f - 0.05f) * size.height
        val x = rnd(i, 2) * size.width + sin(t * 0.9f + i) * size.width * 0.03f
        val s = size.minDimension * (0.012f + 0.01f * rnd(i, 3))
        rotate(t * 60f * (if (i % 2 == 0) 1 else -1) + i * 40, Offset(x, y)) {
            drawOval(colors[i % colors.size].copy(alpha = 0.55f * a), Offset(x - s, y - s / 2), Size(s * 2, s))
        }
    }
}

private fun DrawScope.petals(t: Float, n: Int, a: Float) {
    for (i in 0 until n) {
        val speed = 0.03f + 0.03f * rnd(i, 1)
        val y = ((rnd(i, 0) + t * speed) % 1.1f - 0.05f) * size.height
        val x = rnd(i, 2) * size.width + sin(t * 1.1f + i) * size.width * 0.04f
        val s = size.minDimension * (0.007f + 0.006f * rnd(i, 3))
        rotate(t * 45f + i * 30, Offset(x, y)) {
            drawOval(Color(0xFFFFB7D5).copy(alpha = 0.55f * a), Offset(x - s, y - s * 0.6f), Size(s * 2, s * 1.2f))
        }
    }
}

private fun DrawScope.sparkles(t: Float, n: Int, a: Float) {
    for (i in 0 until n) {
        val tw = max(0f, sin(t * (1f + rnd(i, 3)) + rnd(i, 4) * 6f))
        val c = Offset(rnd(i, 0) * size.width, rnd(i, 1) * size.height)
        val s = size.minDimension * 0.006f * tw
        val col = Color.White.copy(alpha = 0.5f * tw * a)
        drawLine(col, Offset(c.x - s, c.y), Offset(c.x + s, c.y), strokeWidth = s * 0.25f)
        drawLine(col, Offset(c.x, c.y - s), Offset(c.x, c.y + s), strokeWidth = s * 0.25f)
    }
}
