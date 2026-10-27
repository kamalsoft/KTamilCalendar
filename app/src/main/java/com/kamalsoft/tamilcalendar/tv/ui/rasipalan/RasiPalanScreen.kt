package com.kamalsoft.tamilcalendar.tv.ui.rasipalan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import com.kamalsoft.tamilcalendar.tv.data.audio.Speaker
import com.kamalsoft.tamilcalendar.tv.domain.model.RasiOutlook
import com.kamalsoft.tamilcalendar.tv.domain.model.RasiPalan
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilNames
import com.kamalsoft.tamilcalendar.tv.domain.usecase.RasiForecast
import com.kamalsoft.tamilcalendar.tv.domain.usecase.SkyPositions
import com.kamalsoft.tamilcalendar.tv.ui.components.DpadDirection
import com.kamalsoft.tamilcalendar.tv.ui.components.Fmt
import com.kamalsoft.tamilcalendar.tv.ui.components.Pill
import com.kamalsoft.tamilcalendar.tv.ui.components.Text
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.components.staggeredEnter
import com.kamalsoft.tamilcalendar.tv.ui.theme.DisplayPrefs
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import kotlinx.coroutines.delay
import java.time.LocalDate

/** South Indian chart: fixed (row, column) of each rasi on the 4×4 square; the centre 2×2 is the summary. */
private val CHART = mapOf(
    11 to (0 to 0), 0 to (0 to 1), 1 to (0 to 2), 2 to (0 to 3),
    10 to (1 to 0), 3 to (1 to 3),
    9 to (2 to 0), 4 to (2 to 3),
    8 to (3 to 0), 7 to (3 to 1), 6 to (3 to 2), 5 to (3 to 3),
)

private fun rasiAt(row: Int, col: Int) = CHART.entries.first { it.value == row to col }.key

private fun outlookColors(o: RasiOutlook) = when (o) {
    RasiOutlook.GOOD -> TvColors.Gold to TvColors.Gold.copy(alpha = 0.25f)
    RasiOutlook.NEUTRAL -> TvColors.TextMuted to TvColors.SurfaceSelected
    RasiOutlook.CAUTION -> TvColors.Amber to TvColors.Amber.copy(alpha = 0.25f)
    RasiOutlook.CHANDRASHTAMAM -> TvColors.Crimson to TvColors.Crimson.copy(alpha = 0.55f)
}

@Composable
fun RasiPalanScreen(
    date: LocalDate,
    palan: List<RasiPalan>,
    entryFocus: FocusRequester,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    sky: SkyPositions? = null,
    forecasts: List<RasiForecast> = emptyList(),
    speaker: Speaker.State = Speaker.State(),
    onToggleSpeech: (id: String, ta: String, en: String, repeat: Int) -> Unit = { _, _, _, _ -> },
    onStopSpeech: () -> Unit = {},
) {
    var open by remember { mutableStateOf<Int?>(null) }
    val tileFocus = remember { List(12) { FocusRequester() } }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallButton("◀", onPreviousDay)
                Column(Modifier.weight(1f)) {
                    Text(
                        if (DisplayPrefs.compact) Fmt.gregorianEn(date) else "ராசி பலன் · Rasi Palan · ${Fmt.gregorianEn(date)}",
                        style = MaterialTheme.typography.headlineSmall, color = TvColors.Gold, maxLines = 1,
                    )
                    Text("OK: விரிவான பலன் · Select a rasi for today, month and year", style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                SmallButton("▶", onNextDay)
            }
            if (palan.size != 12) return@Column
            val byRasi = palan.associateBy { it.rasi }
            @Composable
            fun RowScope.tile(row: Int, col: Int) {
                val r = rasiAt(row, col)
                RasiTile(
                    p = byRasi.getValue(r), planets = sky?.labelsIn(r).orEmpty(),
                    focusRequester = if (r == 0) entryFocus else tileFocus[r],
                    onClick = { open = r },
                    modifier = Modifier.weight(1f).fillMaxHeight().staggeredEnter(date, row + col),
                )
            }
            @Composable
            fun ColumnScope.tileIn(row: Int, col: Int) = Row(Modifier.weight(1f)) { tile(row, col) }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { for (c in 0..3) tile(0, c) }
                Row(Modifier.weight(2f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { tileIn(1, 0); tileIn(2, 0) }
                    Centre(palan, sky, Modifier.weight(2f).fillMaxHeight())
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { tileIn(1, 3); tileIn(2, 3) }
                }
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { for (c in 0..3) tile(3, c) }
            }
        }
        open?.let { r ->
            RasiDetail(
                p = palan.first { it.rasi == r }, forecast = forecasts.firstOrNull { it.rasi == r }, speaker = speaker,
                onToggleSpeech = onToggleSpeech,
                onClose = {
                    onStopSpeech()
                    open = null
                    runCatching { (if (r == 0) entryFocus else tileFocus[r]).requestFocus() }
                },
            )
        }
    }
}

@Composable
private fun Centre(palan: List<RasiPalan>, sky: SkyPositions?, modifier: Modifier) {
    val moon = palan.firstOrNull { it.moonHouse == 1 }
    val ashtama = palan.firstOrNull { it.outlook == RasiOutlook.CHANDRASHTAMAM }
    Column(
        modifier.border(1.dp, TvColors.TextDim.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text("ராசி சக்கரம்", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TvColors.Gold)
        Text("Rasi chart · today's transits", style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim)
        moon?.let { Text("சந்திரன் · Moon: ${it.rasiTa}", style = MaterialTheme.typography.bodyMedium, color = TvColors.Text, modifier = Modifier.padding(top = 6.dp)) }
        if (sky != null && !DisplayPrefs.compact) {
            Text("குரு ${TamilNames.rasis[sky.jupiter].first} · சனி ${TamilNames.rasis[sky.saturn].first}", style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted)
            Text("ராகு ${TamilNames.rasis[sky.rahu].first} · கேது ${TamilNames.rasis[sky.ketu].first}", style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted)
        }
        ashtama?.let { Text("சந்திராஷ்டமம்: ${it.rasiTa}", style = MaterialTheme.typography.bodySmall, color = TvColors.CrimsonSoft, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@Composable
private fun SmallButton(label: String, onClick: () -> Unit) {
    TvFocusCard(onClick = onClick, cornerRadius = 12.dp, modifier = Modifier.width(56.dp).fillMaxHeight()) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = TvColors.Text, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun RasiTile(p: RasiPalan, planets: List<String>, focusRequester: FocusRequester, onClick: () -> Unit, modifier: Modifier) {
    val (accent, pill) = outlookColors(p.outlook)
    TvFocusCard(onClick = onClick, focusRequester = focusRequester, accent = accent, cornerRadius = 12.dp, modifier = modifier) {
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(p.rasiTa, style = MaterialTheme.typography.titleMedium, color = accent, maxLines = 1, softWrap = false)
            if (!DisplayPrefs.compact) Text(p.rasiEn, style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim, maxLines = 1)
            Text(p.outlook.ta, style = MaterialTheme.typography.labelMedium, color = TvColors.Text, maxLines = 1,
                modifier = Modifier.background(pill, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 1.dp))
            Text(planets.joinToString(" "), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TvColors.GoldSoft, maxLines = 1)
        }
    }
}

/** Expanded rasi: grows out of the chart; Today / Month / Year tabs and read-aloud. */
@Composable
private fun RasiDetail(
    p: RasiPalan,
    forecast: RasiForecast?,
    speaker: Speaker.State,
    onToggleSpeech: (String, String, String, Int) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var tab by remember { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    val grow = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) {
        grow.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) { repeat(5) { if (runCatching { first.requestFocus() }.isSuccess) return@LaunchedEffect; delay(16) } }
    val (ta, en, outlook) = when (tab) {
        1 -> Triple(forecast?.monthTa.orEmpty(), forecast?.monthEn.orEmpty(), forecast?.monthOutlook ?: p.outlook)
        2 -> Triple(forecast?.yearTa.orEmpty(), forecast?.yearEn.orEmpty(), forecast?.yearOutlook ?: p.outlook)
        else -> Triple(p.messageTa, p.messageEn, p.outlook)
    }
    val speechId = "rasi-${p.rasi}-$tab"
    val speaking = speaker.speakingId == speechId
    val tabs = listOf("இன்று · Today", "மாதம் · Month", "ஆண்டு · Year")
    Box(Modifier.fillMaxSize().background(TvColors.MidnightDeep.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.graphicsLayer { scaleX = grow.value; scaleY = grow.value; alpha = (grow.value - 0.85f) / 0.15f }
                .fillMaxWidth(0.94f).widthIn(max = 640.dp)
                .background(TvColors.Surface, RoundedCornerShape(22.dp)).padding(22.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val (accent, pill) = outlookColors(outlook)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${p.rasiTa} · ${p.rasiEn}", style = MaterialTheme.typography.headlineSmall, color = accent, modifier = Modifier.weight(1f))
                Pill("${outlook.ta} · ${outlook.en}", pill)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { i, label ->
                    TvFocusCard(
                        onClick = { tab = i }, onFocused = { tab = i }, selected = tab == i, cornerRadius = 20.dp,
                        focusRequester = if (i == 0) first else null,
                        onDirectionalKey = { d -> d == DpadDirection.UP || (d == DpadDirection.LEFT && i == 0) },
                        modifier = Modifier.height(42.dp).weight(1f),
                    ) { Text(label, style = MaterialTheme.typography.bodyMedium, color = TvColors.Text, maxLines = 1, modifier = Modifier.align(Alignment.Center)) }
                }
            }
            Text(ta, style = MaterialTheme.typography.titleMedium, color = TvColors.Text)
            Text(en, style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted)
            if (tab > 0) Text("சந்திர ராசியிலிருந்து கோசாரம் · Rule-based gochara from current planetary transits", style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim)
            TvFocusCard(
                onClick = { onToggleSpeech(speechId, ta, en, 1) }, cornerRadius = 22.dp,
                onDirectionalKey = { d -> d == DpadDirection.DOWN || d == DpadDirection.LEFT || d == DpadDirection.RIGHT },
                modifier = Modifier.width(240.dp).height(46.dp),
            ) {
                Text(if (speaking) "■  நிறுத்து · Stop" else "🔊  வாசி · Read aloud", style = MaterialTheme.typography.titleMedium, color = TvColors.Text, modifier = Modifier.align(Alignment.Center))
            }
            Text("BACK: திரும்ப · close", style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim)
        }
    }
}
