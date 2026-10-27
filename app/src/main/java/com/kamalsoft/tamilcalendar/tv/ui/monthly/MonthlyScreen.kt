package com.kamalsoft.tamilcalendar.tv.ui.monthly

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import com.kamalsoft.tamilcalendar.tv.ui.components.DpadDirection
import com.kamalsoft.tamilcalendar.tv.ui.components.Fmt
import com.kamalsoft.tamilcalendar.tv.ui.components.TagIcons
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.daily.LoadingState
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * 6x7 Sunday-first grid. Plain Rows (not a lazy grid): 42 fixed cells give deterministic,
 * jank-free D-pad traversal on low-end TV SoCs. RIGHT on the Saturday column flips to the next
 * month; LEFT on Sunday falls through to the drawer.
 */
@Composable
fun MonthlyScreen(
    month: YearMonth,
    days: List<TamilDayData>,
    today: LocalDate,
    selected: LocalDate,
    entryFocus: FocusRequester,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onOpenDate: (LocalDate) -> Unit,
) {
    var refocusAfterFlip by remember { mutableStateOf(false) }
    val gridReady = days.size == 42 && YearMonth.from(days[15].date) == month

    LaunchedEffect(month, gridReady) {
        if (refocusAfterFlip && gridReady) {
            runCatching { entryFocus.requestFocus() }
            refocusAfterFlip = false
        }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeaderButton("◀", onPreviousMonth)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${Fmt.gregorianMonthsTa[month.monthValue - 1]} ${month.year} · ${month.month.getDisplayName(TextStyle.FULL, Locale.US)}",
                    style = MaterialTheme.typography.headlineSmall, color = TvColors.Gold,
                )
                if (gridReady) {
                    val tamilMonths = days.filter { YearMonth.from(it.date) == month }.map { "${it.tamilDate.monthTa} (${it.tamilDate.monthEn})" }.distinct()
                    Text(tamilMonths.joinToString(" – "), style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted)
                }
            }
            HeaderButton("▶", onNextMonth)
            HeaderButton("இன்று · Today", onToday, width = 170)
        }
        Row(Modifier.fillMaxWidth()) {
            Fmt.weekdayShortTa.forEachIndexed { i, label ->
                Text(
                    "$label · ${java.time.DayOfWeek.of(if (i == 0) 7 else i).getDisplayName(TextStyle.SHORT, Locale.US)}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium, color = if (i == 0) TvColors.CrimsonSoft else TvColors.TextMuted,
                )
            }
        }
        if (!gridReady) {
            LoadingState()
            return@Column
        }
        val focusTarget = days.indexOfFirst { it.date == selected }.takeIf { it >= 0 && YearMonth.from(selected) == month }
            ?: days.indexOfFirst { it.date == month.atDay(1) }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0 until 6) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0 until 7) {
                        val index = row * 7 + col
                        val day = days[index]
                        DayCell(
                            day = day,
                            inMonth = YearMonth.from(day.date) == month,
                            isToday = day.date == today,
                            isSelected = day.date == selected,
                            focusRequester = if (index == focusTarget) entryFocus else null,
                            onClick = { onOpenDate(day.date) },
                            onDirectionalKey = { dir ->
                                if (dir == DpadDirection.RIGHT && col == 6) {
                                    refocusAfterFlip = true
                                    onNextMonth()
                                    true
                                } else {
                                    false
                                }
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderButton(label: String, onClick: () -> Unit, width: Int = 64) {
    TvFocusCard(onClick = onClick, cornerRadius = 12.dp, modifier = Modifier.width(width.dp).fillMaxHeight()) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = TvColors.Text, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun DayCell(
    day: TamilDayData,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    onDirectionalKey: (DpadDirection) -> Boolean,
    modifier: Modifier,
) {
    val red = day.weekdayIndex == 0 || day.isGovernmentHoliday
    TvFocusCard(
        onClick = onClick,
        onDirectionalKey = onDirectionalKey,
        focusRequester = focusRequester,
        selected = isSelected,
        cornerRadius = 10.dp,
        modifier = modifier.alpha(if (inMonth) 1f else 0.4f),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 5.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    "${day.date.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        isToday -> TvColors.Midnight
                        red -> TvColors.CrimsonSoft
                        else -> TvColors.Text
                    },
                    modifier = if (isToday) Modifier.background(TvColors.Gold, RoundedCornerShape(6.dp)).padding(horizontal = 5.dp) else Modifier,
                )
                Text(
                    if (day.tamilDate.day == 1) day.tamilDate.monthTa else "${day.tamilDate.day}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (day.tamilDate.day == 1) TvColors.Gold else TvColors.GoldSoft,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                )
            }
            Box(Modifier.weight(1f))
            day.headline?.let { (ta, _) ->
                Text(
                    ta, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (day.isGovernmentHoliday) TvColors.CrimsonSoft else TvColors.GoldSoft,
                )
            }
            TagIcons(day.tags, day.otherReligions, size = 13.dp)
        }
    }
}
