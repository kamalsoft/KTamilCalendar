package com.kamalsoft.tamilcalendar.tv.ui.rasipalan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kamalsoft.tamilcalendar.tv.domain.model.EventSource
import com.kamalsoft.tamilcalendar.tv.domain.model.RasiOutlook
import com.kamalsoft.tamilcalendar.tv.domain.model.RasiPalan
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilNames
import com.kamalsoft.tamilcalendar.tv.ui.components.DetailContent
import com.kamalsoft.tamilcalendar.tv.ui.components.DetailHost
import com.kamalsoft.tamilcalendar.tv.ui.components.Fmt
import com.kamalsoft.tamilcalendar.tv.ui.components.Pill
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.components.rememberDetailHostState
import com.kamalsoft.tamilcalendar.tv.ui.daily.LoadingState
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import java.time.LocalDate

@Composable
fun RasiPalanScreen(
    date: LocalDate,
    palan: List<RasiPalan>,
    entryFocus: FocusRequester,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
) {
    val details = rememberDetailHostState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallButton("◀", onPreviousDay)
                Column(Modifier.weight(1f)) {
                    Text("ராசி பலன் · Rasi Palan · ${Fmt.gregorianEn(date)}", style = MaterialTheme.typography.headlineSmall, color = TvColors.Gold)
                    val moon = palan.firstOrNull { it.moonHouse == 1 }
                    Text(
                        (moon?.let { "சந்திரன் ${it.rasiTa} ராசியில் · Moon in ${it.rasiEn} · " } ?: "") + "சந்திர கோசாரம் · Based on the Moon's transit",
                        style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted,
                    )
                }
                SmallButton("▶", onNextDay)
            }
            if (palan.size != 12) {
                LoadingState()
                return@Column
            }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (row in 0 until 3) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (col in 0 until 4) {
                            val p = palan[row * 4 + col]
                            val self = remember(p.rasi) { FocusRequester() }
                            RasiCard(
                                p = p,
                                focusRequester = if (p.rasi == 0) entryFocus else self,
                                onClick = {
                                    details.show(
                                        DetailContent(
                                        "${p.rasiTa} · ${p.rasiEn} — ${p.outlook.ta} · ${p.outlook.en}",
                                        "சந்திரன் ${p.moonHouse}-ஆம் இடத்தில் · Moon in house ${p.moonHouse}",
                                        listOf(
                                            "பலன் · Outlook" to p.messageTa,
                                            "Palan" to p.messageEn,
                                            "நட்சத்திரங்கள் · Stars" to starsOf(p.rasi),
                                        ),
                                        ),
                                        if (p.rasi == 0) entryFocus else self,
                                    )
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
        DetailHost(details)
    }
}

private fun starsOf(rasi: Int): String {
    val span = 360.0 / 27.0
    return (0 until 27).filter { k -> k * span < (rasi + 1) * 30.0 - 1e-6 && (k + 1) * span > rasi * 30.0 + 1e-6 }
        .joinToString(", ") { TamilNames.nakshatras[it].first }
}

@Composable
private fun SmallButton(label: String, onClick: () -> Unit) {
    TvFocusCard(onClick = onClick, cornerRadius = 12.dp, modifier = Modifier.width(64.dp).fillMaxHeight()) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = TvColors.Text, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun RasiCard(p: RasiPalan, focusRequester: FocusRequester, onClick: () -> Unit, modifier: Modifier) {
    val (accent, pill) = when (p.outlook) {
        RasiOutlook.GOOD -> TvColors.Gold to TvColors.Gold.copy(alpha = 0.25f)
        RasiOutlook.NEUTRAL -> TvColors.TextMuted to TvColors.SurfaceSelected
        RasiOutlook.CAUTION -> TvColors.Amber to TvColors.Amber.copy(alpha = 0.25f)
        RasiOutlook.CHANDRASHTAMAM -> TvColors.Crimson to TvColors.Crimson.copy(alpha = 0.55f)
    }
    TvFocusCard(onClick = onClick, focusRequester = focusRequester, accent = accent, modifier = modifier) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.rasiTa, style = MaterialTheme.typography.titleMedium, color = accent, maxLines = 1, softWrap = false)
                Text("  ${p.rasiEn}", style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Pill("${p.outlook.ta} · ${p.outlook.en}", pill)
            Text(p.messageTa, style = MaterialTheme.typography.bodySmall, color = TvColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(p.messageEn, style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (p.source == EventSource.REMOTE) Text("அதிகாரப்பூர்வ பலன் · Official", style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim)
        }
    }
}
