package com.kamalsoft.tamilcalendar.tv.ui.weather

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kamalsoft.tamilcalendar.tv.R
import com.kamalsoft.tamilcalendar.tv.domain.model.DayForecast
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.domain.model.WeatherCondition
import com.kamalsoft.tamilcalendar.tv.domain.model.WeatherState
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.components.floating
import com.kamalsoft.tamilcalendar.tv.ui.components.staggeredEnter
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvDimens
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@DrawableRes
fun WeatherCondition.iconRes(isDay: Boolean = true): Int = when (this) {
    WeatherCondition.CLEAR -> if (isDay) R.drawable.wx_clear_day else R.drawable.wx_clear_night
    WeatherCondition.PARTLY_CLOUDY -> if (isDay) R.drawable.wx_partly_cloudy else R.drawable.wx_clear_night
    WeatherCondition.CLOUDY -> R.drawable.wx_cloudy
    WeatherCondition.FOG -> R.drawable.wx_fog
    WeatherCondition.DRIZZLE, WeatherCondition.RAIN -> R.drawable.wx_rain
    WeatherCondition.SNOW -> R.drawable.wx_snow
    WeatherCondition.THUNDERSTORM -> R.drawable.wx_storm
}

private fun Double.deg() = "${roundToInt()}°"

private val clockEn = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val hourEn = DateTimeFormatter.ofPattern("h a", Locale.US)

/**
 * Second "page" of the Daily screen, reached by pressing ▼ from the bottom row. All weather text is
 * English. Every tile is focusable so D-pad focus drives the page slide (no touch scrolling on TV).
 */
@Composable
fun WeatherSection(
    state: WeatherState,
    location: GeoLocation,
    imperial: Boolean,
    today: LocalDate,
    zone: ZoneId,
    entryFocus: FocusRequester,
    modifier: Modifier = Modifier,
    /** False while the page is scrolled out of view: the animated scene stops drawing. */
    active: Boolean = true,
) {
    val tempUnit = if (imperial) "°F" else "°C"
    val windUnit = if (imperial) "mph" else "km/h"
    // Focusing a forecast day previews that day's weather in the animated scene.
    var preview by remember { mutableStateOf<DayForecast?>(null) }
    val season = seasonFor(preview?.date ?: today, location)
    val current = state.report?.current
    Box(modifier) {
    WeatherScene(
        condition = preview?.condition ?: current?.condition ?: WeatherCondition.CLEAR,
        isDay = if (preview != null) true else current?.isDay ?: true,
        season = season,
        hour = if (preview != null) 12 else java.time.LocalTime.now(zone).hour,
        modifier = Modifier.matchParentSize(),
        active = active && state.report != null,
    )
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(TvDimens.Gap)) {
        val report = state.report
        Column {
            Text(
                "Weather · ${location.nameEn}" + (preview?.let { " · ${it.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)} forecast" } ?: ""),
                style = MaterialTheme.typography.headlineSmall, color = TvColors.Gold,
            )
            val status = when {
                report == null && state.isLoading -> "Loading…"
                report == null -> "No internet connection — will retry automatically"
                else -> "Updated ${report.fetchedAt.atZone(zone).toLocalTime().format(clockEn)}" +
                    if (state.refreshFailed) " · Offline, showing saved forecast" else ""
            }
            Text(status, style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted)
        }

        if (report == null) {
            TvFocusCard(onClick = {}, focusRequester = entryFocus, modifier = Modifier.fillMaxWidth().height(120.dp)) {
                Text(
                    "Weather not available yet",
                    style = MaterialTheme.typography.titleMedium, color = TvColors.TextMuted,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            return@Column
        }

        Row(Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.spacedBy(TvDimens.Gap)) {
            val c = report.current
            TvFocusCard(onClick = {}, onFocused = { preview = null }, focusRequester = entryFocus, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxSize().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(c.condition.iconRes(c.isDay)), contentDescription = c.condition.en, modifier = Modifier.size(96.dp).floating())
                    Column(Modifier.padding(start = 20.dp)) {
                        Text(c.temperature.deg() + tempUnit.removePrefix("°"), fontSize = 52.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
                        Text(c.condition.en, style = MaterialTheme.typography.titleMedium, color = TvColors.Gold)
                        Text(
                            "Feels like ${c.feelsLike.deg()} · Humidity ${c.humidityPct}% · Wind ${c.windSpeed.roundToInt()} $windUnit",
                            style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted,
                        )
                    }
                }
            }
            TvFocusCard(onClick = {}, onFocused = { preview = null }, modifier = Modifier.weight(1.3f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Next 12 hours", style = MaterialTheme.typography.labelMedium, color = TvColors.Gold)
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        report.hourly.filterIndexed { i, _ -> i % 2 == 0 }.take(6).forEach { h ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(h.time.toLocalTime().format(hourEn), style = MaterialTheme.typography.labelMedium, color = TvColors.TextMuted, maxLines = 1)
                                Image(painterResource(h.condition.iconRes(h.isDay)), contentDescription = null, modifier = Modifier.size(36.dp).padding(vertical = 2.dp))
                                Text(h.temperature.deg(), style = MaterialTheme.typography.titleMedium, color = TvColors.Text)
                                Text("${h.precipitationPct}%", style = MaterialTheme.typography.labelMedium, color = if (h.precipitationPct >= 50) TvColors.Amber else TvColors.TextDim)
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            report.daily.take(7).forEachIndexed { i, d ->
                DayTile(d, isToday = d.date == today, onFocused = { preview = d }, modifier = Modifier.weight(1f).fillMaxHeight().staggeredEnter(report.fetchedAt, i))
            }
        }
        Text(
            "Source: Open-Meteo.com (NOAA models) · Press ▲ to return to the calendar",
            style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim,
        )
    }
    }
}

@Composable
private fun DayTile(d: DayForecast, isToday: Boolean, onFocused: () -> Unit, modifier: Modifier) {
    TvFocusCard(onClick = {}, onFocused = onFocused, selected = isToday, cornerRadius = 12.dp, modifier = modifier) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (isToday) "Today" else d.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
                style = MaterialTheme.typography.labelMedium, color = if (isToday) TvColors.Gold else TvColors.TextMuted,
                textAlign = TextAlign.Center, maxLines = 1,
            )
            Image(painterResource(d.condition.iconRes()), contentDescription = d.condition.en, modifier = Modifier.size(40.dp))
            Text("${d.high.deg()} / ${d.low.deg()}", style = MaterialTheme.typography.titleMedium, color = TvColors.Text, maxLines = 1)
            Text("Rain ${d.precipitationPct}%", style = MaterialTheme.typography.labelMedium, color = if (d.precipitationPct >= 50) TvColors.Amber else TvColors.TextDim, maxLines = 1)
        }
    }
}
