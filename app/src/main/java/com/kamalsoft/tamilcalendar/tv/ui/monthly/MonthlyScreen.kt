package com.kamalsoft.tamilcalendar.tv.ui.monthly

import androidx.compose.ui.unit.TextUnit
import com.kamalsoft.tamilcalendar.tv.ui.theme.DisplayPrefs
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
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import com.kamalsoft.tamilcalendar.tv.ui.components.Text
import com.kamalsoft.tamilcalendar.tv.domain.model.Religion
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import com.kamalsoft.tamilcalendar.tv.ui.components.DpadDirection
import com.kamalsoft.tamilcalendar.tv.ui.components.Fmt
import com.kamalsoft.tamilcalendar.tv.ui.components.TagIcons
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.components.staggeredEnter
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

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Title left, controls right (tvOS-style), in one compact row so the grid gets the height.
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${Fmt.gregorianMonthsTa[month.monthValue - 1]} ${month.year}" +
                        if (DisplayPrefs.compact) "" else " · ${month.month.getDisplayName(TextStyle.FULL, Locale.US)}",
                    style = MaterialTheme.typography.headlineSmall, color = TvColors.Text, maxLines = 1,
                    fontSize = if (DisplayPrefs.compact) 19.sp else TextUnit.Unspecified,
                )
                if (gridReady) {
                    val tamilMonths = days.filter { YearMonth.from(it.date) == month }.map { "${it.tamilDate.monthTa} (${it.tamilDate.monthEn})" }.distinct()
                    Text(tamilMonths.joinToString(" – "), style = MaterialTheme.typography.labelMedium, color = TvColors.TextMuted, maxLines = 1)
                }
            }
            val c = DisplayPrefs.compact
            HeaderButton("◀", onPreviousMonth, width = if (c) 44 else 64)
            HeaderButton(if (c) "இன்று" else "இன்று · Today", onToday, width = if (c) 76 else 150)
            HeaderButton("▶", onNextMonth, width = if (c) 44 else 64)
        }
        Row(Modifier.fillMaxWidth()) {
            Fmt.weekdayShortTa.forEachIndexed { i, label ->
                Text(
                    if (DisplayPrefs.compact) label else "$label · ${java.time.DayOfWeek.of(if (i == 0) 7 else i).getDisplayName(TextStyle.SHORT, Locale.US)}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
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
        // Only as many weeks as the month needs (usually 5): taller cards leave room for the day's important dates.
        val weeks = (days.indexOfFirst { it.date == month.atEndOfMonth() } / 7) + 1
        Column(Modifier.fillMaxSize().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (row in 0 until weeks) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (col in 0 until 7) {
                        val index = row * 7 + col
                        val day = days[index]
                        DayCell(
                            day = day,
                            inMonth = YearMonth.from(day.date) == month,
                            isToday = day.date == today,
                            isSelected = day.date == selected,
                            maxDates = if (weeks <= 5) 2 else 1,
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
                            modifier = Modifier.weight(1f).fillMaxHeight().staggeredEnter(month, row + col),
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
    maxDates: Int,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    onDirectionalKey: (DpadDirection) -> Boolean,
    modifier: Modifier,
) {
    val red = day.weekdayIndex == 0 || day.isGovernmentHoliday
    val dates = day.importantDates
    TvFocusCard(
        onClick = onClick,
        onDirectionalKey = onDirectionalKey,
        focusRequester = focusRequester,
        selected = isSelected,
        cornerRadius = 10.dp,
        modifier = modifier.alpha(if (inMonth) 1f else 0.4f),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 4.dp)) {
            // Date, observance icons, Tamil date.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                // Phone cells are ~50 dp wide: no room for icons; the names below carry the information.
                if (!DisplayPrefs.compact) Box(Modifier.padding(start = 4.dp)) { TagIcons(day.tags, day.otherReligions, size = 12.dp, max = 3) }
                Text(
                    if (day.tamilDate.day == 1) day.tamilDate.monthTa else "${day.tamilDate.day}",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = if (DisplayPrefs.compact) 10.sp else TextUnit.Unspecified,
                    color = if (day.tamilDate.day == 1) TvColors.Gold else TvColors.TextDim,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
            }
            // The day's important dates (same as the Important Dates screen), most important first.
            dates.take(maxDates).forEachIndexed { i, d ->
                val more = dates.size - maxDates
                Text(
                    if (i == maxDates - 1 && more > 0) "${d.titleTa} +$more" else d.titleTa,
                    fontSize = if (DisplayPrefs.compact) 11.sp else 12.sp, lineHeight = if (DisplayPrefs.compact) 13.sp else 15.sp,
                    maxLines = if (DisplayPrefs.compact) 3 else 1, overflow = TextOverflow.Ellipsis,
                    color = when {
                        d.isHoliday -> TvColors.CrimsonSoft
                        d.religion != null && d.religion != Religion.HINDU -> TvColors.TextMuted
                        else -> TvColors.GoldSoft
                    },
                )
            }
        }
    }
}
