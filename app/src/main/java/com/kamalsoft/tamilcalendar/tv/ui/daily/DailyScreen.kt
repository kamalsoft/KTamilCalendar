package com.kamalsoft.tamilcalendar.tv.ui.daily

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kamalsoft.tamilcalendar.tv.R
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilNames
import com.kamalsoft.tamilcalendar.tv.domain.model.TimingWindow
import com.kamalsoft.tamilcalendar.tv.domain.model.WeatherState
import com.kamalsoft.tamilcalendar.tv.ui.weather.WeatherSection
import com.kamalsoft.tamilcalendar.tv.ui.weather.iconRes
import com.kamalsoft.tamilcalendar.tv.ui.components.DetailContent
import com.kamalsoft.tamilcalendar.tv.ui.components.DetailHost
import com.kamalsoft.tamilcalendar.tv.ui.components.Fmt
import com.kamalsoft.tamilcalendar.tv.ui.components.Pill
import com.kamalsoft.tamilcalendar.tv.ui.components.TagChip
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.components.rememberDetailHostState
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvDimens
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Focus map (D-pad):
 *
 *   [ Hero card        ] [ Tithi        ] [ Nalla Neram  ]
 *   [                  ] [ Natchathiram ] [ Gowri        ]
 *   [                  ] [ Yogam        ] [ Rahu Kalam   ]
 *   [                  ] [ Sun          ] [ Yamagandam   ]
 *   [ ◀ ][ இன்று ][ ▶ ] [ Chandrashtamam] [ Kuligai      ]
 *
 *   ──────────── ▼ ────────────
 *   [ Weather now ][ Next hours ]
 *   [ 7-day forecast tiles     ]
 *
 * Only D-pad + OK + BACK are needed (most Google TV remotes have no number/channel keys).
 * LEFT from the first column opens the drawer; ▼ from the bottom row pages down to the weather,
 * ▲ from the weather pages back. Each page snaps fully into view.
 */
@Composable
fun DailyScreen(
    day: TamilDayData?,
    today: LocalDate,
    location: GeoLocation,
    weather: WeatherState,
    imperialUnits: Boolean,
    entryFocus: FocusRequester,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onOpenMonth: () -> Unit,
) {
    if (day == null) {
        LoadingState()
        return
    }
    val details = rememberDetailHostState()
    val show: (DetailContent, FocusRequester) -> Unit = details::show
    val weatherFocus = remember { FocusRequester() }
    // ▶ from the tall date card should land on the first panchangam card, not the geometrically nearest (middle) one.
    val firstPanchangamCard = remember { FocusRequester() }
    var weatherPage by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        // Two fixed pages that slide as a whole, driven by which page holds focus. (A scroll container
        // fights Compose's own bring-into-view and stops half-way; a fixed offset always lands exactly.)
        val edge = 12.dp
        val pageHeight = maxHeight - edge * 2
        val pageStride = pageHeight + edge * 2
        val offset by animateDpAsState(if (weatherPage) pageStride else 0.dp, tween(280), label = "page")
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .offset { IntOffset(0, -offset.roundToPx()) }
                .padding(vertical = edge, horizontal = edge),
            verticalArrangement = Arrangement.spacedBy(edge * 2),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(pageHeight)
                    .onFocusChanged { if (it.hasFocus) weatherPage = false },
                horizontalArrangement = Arrangement.spacedBy(TvDimens.Gap),
            ) {
                Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(TvDimens.Gap)) {
                    HeroCard(
                        day = day,
                        isToday = day.date == today,
                        weather = weather,
                        focusRequester = entryFocus,
                        onClick = onOpenMonth,
                        modifier = Modifier.weight(1f).fillMaxWidth().focusProperties { right = firstPanchangamCard },
                    )
                    Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.spacedBy(TvDimens.Gap)) {
                        DayButton("◀", onPreviousDay, Modifier.width(56.dp))
                        DayButton("இன்று · Today", onToday, Modifier.weight(1f), highlighted = day.date != today)
                        DayButton("▶", onNextDay, Modifier.width(56.dp))
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(TvDimens.Gap)) {
                    PanchangamColumn(day, location.label, firstPanchangamCard, show)
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(TvDimens.Gap)) {
                    TimingColumn(day, show)
                }
            }
            WeatherSection(
                state = weather,
                location = location,
                imperial = imperialUnits,
                today = today,
                zone = ZoneId.of(location.zoneId),
                entryFocus = weatherFocus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pageHeight)
                    .onFocusChanged { if (it.hasFocus) weatherPage = true },
            )
        }
        DetailHost(details)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroCard(
    day: TamilDayData,
    isToday: Boolean,
    weather: WeatherState,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    TvFocusCard(
        onClick = onClick,
        focusRequester = focusRequester,
        modifier = modifier,
    ) { focused ->
        Image(
            painter = painterResource(R.drawable.gopuram),
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(150.dp).alpha(if (focused) 0.14f else 0.08f),
        )
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    day.weekdayTa,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (day.weekdayIndex == 0 || day.isGovernmentHoliday) TvColors.CrimsonSoft else TvColors.Text,
                )
                if (isToday) Pill("இன்று · Today", TvColors.Crimson)
            }
            Text("${Fmt.gregorian(day.date)} · ${Fmt.gregorianEn(day.date)}", style = MaterialTheme.typography.bodyMedium, color = TvColors.TextMuted)
            Spacer(Modifier.height(10.dp))
            Text(day.tamilDate.displayTa, style = MaterialTheme.typography.displayMedium, color = TvColors.Gold)
            Text(
                "${day.tamilDate.yearDisplayTa} · திருவள்ளுவர் ஆண்டு ${day.tamilDate.thiruvalluvarAandu}",
                style = MaterialTheme.typography.bodyMedium, color = TvColors.Text,
            )
            Text(
                "${day.tamilDate.monthEn} ${day.tamilDate.day} · ${day.tamilDate.yearEn} · ${day.weekdayEn}",
                style = MaterialTheme.typography.bodySmall, color = TvColors.TextDim,
            )
            Spacer(Modifier.height(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                day.tags.forEach { TagChip(it) }
            }
            Spacer(Modifier.weight(1f))
            day.events.take(3).forEach { e ->
                val prefix = if (e.kind == EventKind.SCHOOL) "${e.region?.ta}: " else ""
                Text(
                    "• $prefix${e.titleTa}" + (e.detail?.let { "  ($it)" } ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (e.isLocalHoliday) TvColors.CrimsonSoft else TvColors.GoldSoft,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Glanceable weather; the full forecast is one ▼ away.
                weather.report?.current?.let { c ->
                    Image(painterResource(c.condition.iconRes(c.isDay)), contentDescription = c.condition.en, modifier = Modifier.size(28.dp))
                    Text(" ${c.temperature.roundToInt()}°  ", style = MaterialTheme.typography.titleMedium, color = TvColors.Text, maxLines = 1)
                }
                Text(
                    if (focused) "▼ Weather · OK: மாதம்" else "▼ Weather",
                    style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim, maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DayButton(label: String, onClick: () -> Unit, modifier: Modifier, highlighted: Boolean = false) {
    TvFocusCard(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        cornerRadius = 12.dp,
        containerColor = if (highlighted) TvColors.Crimson.copy(alpha = 0.35f) else TvColors.Surface,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = TvColors.Text, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ColumnScope.PanchangamColumn(
    day: TamilDayData,
    locationLabel: String,
    firstCard: FocusRequester,
    show: (DetailContent, FocusRequester) -> Unit,
) {
    val p = day.panchangam
    InfoCard(
        label = "திதி · Tithi · ${p.paksha.ta}", value = p.tithi.nameTa, sub = "${p.tithi.nameEn} · ${Fmt.untilEn(p.tithi.endsAt, day.date)}",
        accent = TvColors.Gold, show = show, focusRequester = firstCard,
        detail = DetailContent(
            "திதி · Tithi — ${p.tithi.nameTa}", "${p.tithi.nameEn} · ${p.paksha.ta} · ${p.paksha.en}",
            listOf("முடிவு · Ends" to "${Fmt.until(p.tithi.endsAt, day.date)} · ${Fmt.untilEn(p.tithi.endsAt, day.date)}"),
        ),
    )
    InfoCard(
        label = "நட்சத்திரம் · Nakshatram", value = p.nakshatram.nameTa, sub = "${p.nakshatram.nameEn} · ${Fmt.untilEn(p.nakshatram.endsAt, day.date)}",
        accent = TvColors.Gold, show = show,
        detail = DetailContent(
            "நட்சத்திரம் · Nakshatram — ${p.nakshatram.nameTa}", p.nakshatram.nameEn,
            listOf(
                "முடிவு · Ends" to "${Fmt.until(p.nakshatram.endsAt, day.date)} · ${Fmt.untilEn(p.nakshatram.endsAt, day.date)}",
                "சந்திர ராசி · Moon sign" to "${p.moonRasiTa} · ${p.moonRasiEn} (${Fmt.untilEn(p.moonRasiEndsAt, day.date)})",
            ),
        ),
    )
    InfoCard(
        label = "யோகம் · Yogam", value = p.yogam.nameTa, sub = "${p.yogam.nameEn} · ${Fmt.untilEn(p.yogam.endsAt, day.date)}",
        accent = TvColors.Gold, show = show,
        detail = DetailContent(
            "யோகம் · Yogam — ${p.yogam.nameTa}", p.yogam.nameEn,
            listOf("முடிவு · Ends" to "${Fmt.until(p.yogam.endsAt, day.date)} · ${Fmt.untilEn(p.yogam.endsAt, day.date)}"),
        ),
    )
    InfoCard(
        label = "சூரியன் · Sun", value = "உதயம் ${Fmt.time(day.sunrise)}", sub = "Rise ${Fmt.timeEn(day.sunrise)} · Set ${Fmt.timeEn(day.sunset)}",
        accent = TvColors.Amber, show = show,
        detail = DetailContent(
            "சூரியன் · Sun", locationLabel,
            listOf(
                "உதயம் · Sunrise" to "${Fmt.time(day.sunrise)} · ${Fmt.timeEn(day.sunrise)}",
                "அஸ்தமனம் · Sunset" to "${Fmt.time(day.sunset)} · ${Fmt.timeEn(day.sunset)}",
            ),
        ),
    )
    InfoCard(
        label = "சந்திராஷ்டமம்", value = day.chandrashtamam.rasiTa,
        sub = "Chandrashtamam · ${TamilNames.rasis[day.chandrashtamam.rasi].second}",
        accent = TvColors.Crimson, show = show,
        detail = DetailContent(
            "சந்திராஷ்டமம் · Chandrashtamam — ${day.chandrashtamam.rasiTa}",
            "சந்திரன் ${p.moonRasiTa} ராசியில் · Moon in ${p.moonRasiEn}: 8th house from ${TamilNames.rasis[day.chandrashtamam.rasi].second}",
            listOf(
                "நட்சத்திரங்கள் · Stars" to day.chandrashtamam.nakshatras.joinToString(", ") { "${TamilNames.nakshatras[it].first} (${TamilNames.nakshatras[it].second})" },
                "காலம் · Until" to "${Fmt.until(day.chandrashtamam.until, day.date)} · ${Fmt.untilEn(day.chandrashtamam.until, day.date)}",
            ),
        ),
    )
}

@Composable
private fun ColumnScope.TimingColumn(day: TamilDayData, show: (DetailContent, FocusRequester) -> Unit) {
    val t = day.timings
    fun windows(list: List<TimingWindow>) = if (list.isEmpty()) "—" else list.joinToString("\n") { Fmt.range(it) }
    InfoCard(
        label = "நல்ல நேரம் · Nalla Neram", value = windows(t.nallaNeram.take(2)), accent = TvColors.Gold, show = show, valueLines = 2,
        detail = DetailContent(
            "நல்ல நேரம் · Nalla Neram",
            "நல்ல கௌரி நேரங்களில் ராகு காலம் / எமகண்டம் நீக்கப்பட்டது · Good Gowri periods minus Rahu Kalam & Yamagandam",
            t.nallaNeram.map { "" to "${Fmt.range(it)} · ${Fmt.timeEn(it.start)} – ${Fmt.timeEn(it.end)}" },
        ),
    )
    InfoCard(
        label = "கௌரி நல்ல நேரம் · Gowri", value = windows(t.gowriNallaNeram.take(2)), accent = TvColors.Gold, show = show, valueLines = 2,
        detail = DetailContent(
            "கௌரி நல்ல நேரம் · Gowri Nalla Neram", "பகல் கௌரி பஞ்சாங்கம் · Daytime Gowri Panchangam",
            t.gowriNallaNeram.map { "${it.gowri!!.ta} · ${it.gowri.en}" to "${Fmt.range(it)} · ${Fmt.timeEn(it.start)} – ${Fmt.timeEn(it.end)}" },
        ),
    )
    listOf(t.rahuKalam to TvColors.Crimson, t.yamagandam to TvColors.Crimson, t.kuligai to TvColors.Amber).forEach { (w, color) ->
        InfoCard(
            label = "${w.type.ta} · ${w.type.en}", value = Fmt.range(w), sub = "${Fmt.timeEn(w.start)} – ${Fmt.timeEn(w.end)}",
            accent = color, show = show,
            detail = DetailContent("${w.type.ta} · ${w.type.en}", null, listOf("நேரம் · Time" to "${Fmt.range(w)} · ${Fmt.timeEn(w.start)} – ${Fmt.timeEn(w.end)}")),
        )
    }
}

@Composable
private fun ColumnScope.InfoCard(
    label: String,
    value: String,
    accent: Color,
    detail: DetailContent,
    show: (DetailContent, FocusRequester) -> Unit,
    sub: String? = null,
    valueLines: Int = 1,
    focusRequester: FocusRequester? = null,
) {
    val self = focusRequester ?: remember { FocusRequester() }
    TvFocusCard(
        onClick = { show(detail, self) },
        focusRequester = self,
        accent = accent,
        modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier.padding(start = 10.dp, top = 12.dp, bottom = 12.dp).width(4.dp).fillMaxHeight()
                    .background(accent, RoundedCornerShape(2.dp)),
            )
            Column(
                Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = accent, maxLines = 1)
                Text(
                    value, style = MaterialTheme.typography.titleMedium, color = TvColors.Text,
                    maxLines = valueLines, overflow = TextOverflow.Ellipsis,
                )
                sub?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.gopuram), contentDescription = null, modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(12.dp))
            Text("பஞ்சாங்கம் கணிக்கப்படுகிறது… · Calculating panchangam…", style = MaterialTheme.typography.titleMedium, color = TvColors.Gold)
        }
    }
}

/** Shown instead of a screen when the offline data can't be built/read; never leave the user on a spinner. */
@Composable
fun ErrorState(focusRequester: FocusRequester, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.gopuram), contentDescription = null, modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(12.dp))
            Text("பஞ்சாங்கத் தரவை ஏற்ற முடியவில்லை", style = MaterialTheme.typography.headlineSmall, color = TvColors.Gold)
            Text("Couldn't load calendar data", style = MaterialTheme.typography.bodyMedium, color = TvColors.TextMuted)
            Spacer(Modifier.height(20.dp))
            TvFocusCard(onClick = onRetry, focusRequester = focusRequester, cornerRadius = 12.dp, modifier = Modifier.width(260.dp).height(56.dp)) {
                Text("மீண்டும் முயற்சி · Retry", style = MaterialTheme.typography.titleMedium, color = TvColors.Text, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
