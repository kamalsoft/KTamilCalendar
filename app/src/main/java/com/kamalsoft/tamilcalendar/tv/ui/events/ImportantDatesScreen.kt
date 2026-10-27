package com.kamalsoft.tamilcalendar.tv.ui.events

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kamalsoft.tamilcalendar.tv.R
import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.DayTag
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.Religion
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilNames
import com.kamalsoft.tamilcalendar.tv.domain.usecase.forDisplay
import com.kamalsoft.tamilcalendar.tv.ui.components.Fmt
import com.kamalsoft.tamilcalendar.tv.ui.components.Pill
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.components.badgeRes
import com.kamalsoft.tamilcalendar.tv.ui.components.iconRes
import com.kamalsoft.tamilcalendar.tv.ui.daily.LoadingState
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** One row: a dated observance with an icon and a short pill (deity, or "Holiday"). */
private data class Item(
    val date: LocalDate,
    val titleTa: String,
    val titleEn: String,
    @DrawableRes val icon: Int?,
    val pill: String?,
    val isHoliday: Boolean,
)

/** A list entry: either a section heading or a row. */
private sealed interface Entry {
    data class Header(val text: String, val note: String? = null) : Entry
    data class Row(val item: Item) : Entry
}

/** Monthly observances worth listing (muhurtham and holiday markers are shown elsewhere). */
private val listedTags = DayTag.entries.filter { it.deity != null || it == DayTag.AMAVASAI || it == DayTag.POURNAMI }

private fun hinduItems(days: List<TamilDayData>): List<Item> = days.flatMap { d ->
    val festivals = d.events.filter { it.religion == Religion.HINDU }.map {
        Item(
            d.date, it.titleTa, it.titleEn, R.drawable.ic_nav_festival,
            it.deity?.let { deity -> "${deity.ta} · ${deity.en}" }, it.isLocalHoliday,
        )
    }
    val observances = d.tags.filter { it in listedTags }.sortedBy { it.ordinal }.map {
        Item(d.date, it.ta, it.en, it.badgeRes(), it.deity?.let { deity -> "${deity.ta} · ${deity.en}" }, false)
    }
    festivals + observances
}

private fun otherEntries(days: List<TamilDayData>): List<Entry> {
    val events = days.flatMap { it.events }.forDisplay()
    val out = mutableListOf<Entry>()
    Religion.entries.filter { it != Religion.HINDU }.forEach { religion ->
        val rows = events.filter { it.religion == religion }
        if (rows.isEmpty()) return@forEach
        // Islamic/Jewish rows carry a caveat (calculated date, evening start); show it once under the heading.
        out += Entry.Header("${religion.ta} · ${religion.en}", rows.firstNotNullOfOrNull { it.detail })
        rows.forEach { out += Entry.Row(it.toItem(religion.iconRes())) }
    }
    val holidays = events.filter { it.kind == EventKind.GOVT_HOLIDAY && it.religion == null }
    if (holidays.isNotEmpty()) {
        out += Entry.Header("அரசு விடுமுறைகள் · Holidays")
        holidays.forEach { out += Entry.Row(it.toItem(R.drawable.badge_holiday)) }
    }
    return out
}

private fun CalendarEvent.toItem(@DrawableRes icon: Int?) = Item(
    date = date, titleTa = titleTa, titleEn = titleEn, icon = icon,
    pill = when {
        isLocalHoliday -> "விடுமுறை · Holiday"
        region != null && isHoliday -> region.en
        else -> null
    },
    isHoliday = isLocalHoliday,
)

@Composable
fun ImportantDatesScreen(
    month: YearMonth,
    days: List<TamilDayData>,
    today: LocalDate,
    entryFocus: FocusRequester,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenDate: (LocalDate) -> Unit,
) {
    val inMonth = days.filter { YearMonth.from(it.date) == month }
    val ready = inMonth.size == month.lengthOfMonth()
    val hindu = remember(inMonth) { hinduItems(inMonth) }
    val others = remember(inMonth) { otherEntries(inMonth) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NavButton("◀", onPreviousMonth)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("முக்கிய நாட்கள் · Important Dates", style = MaterialTheme.typography.headlineSmall, color = TvColors.Gold)
                Text(
                    "${Fmt.gregorianMonthsTa[month.monthValue - 1]} ${month.year} · ${month.month.getDisplayName(TextStyle.FULL, Locale.US)}",
                    style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted,
                )
            }
            NavButton("▶", onNextMonth)
        }
        if (!ready) {
            LoadingState()
            return@Column
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                SectionTitle("இந்து · Hindu — பண்டிகைகள் & விரதங்கள் · Festivals & observances")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(10.dp)) {
                    items(hindu.size) { i ->
                        ItemRow(hindu[i], today, if (i == 0) entryFocus else null) { onOpenDate(hindu[i].date) }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                SectionTitle("பிற மதங்கள் & விடுமுறைகள் · Other faiths & holidays")
                if (others.isEmpty()) {
                    Text("இந்த மாதம் இல்லை · None this month", style = MaterialTheme.typography.bodyMedium, color = TvColors.TextDim, modifier = Modifier.padding(10.dp))
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(10.dp)) {
                    items(others.size) { i ->
                        when (val e = others[i]) {
                            is Entry.Header -> Column(Modifier.padding(top = if (i == 0) 0.dp else 8.dp)) {
                                Text(e.text, style = MaterialTheme.typography.labelMedium, color = TvColors.Gold)
                                e.note?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim) }
                            }
                            is Entry.Row -> ItemRow(e.item, today, null) { onOpenDate(e.item.date) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = TvColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp))
}

@Composable
private fun NavButton(label: String, onClick: () -> Unit) {
    TvFocusCard(onClick = onClick, cornerRadius = 12.dp, modifier = Modifier.width(64.dp).fillMaxHeight()) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = TvColors.Text, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ItemRow(item: Item, today: LocalDate, focusRequester: FocusRequester?, onClick: () -> Unit) {
    TvFocusCard(
        onClick = onClick,
        focusRequester = focusRequester,
        selected = item.date == today,
        accent = if (item.isHoliday) TvColors.Crimson else TvColors.Gold,
        cornerRadius = 12.dp,
        modifier = Modifier.fillMaxWidth().height(58.dp),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(62.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${item.date.dayOfMonth}", style = MaterialTheme.typography.titleMedium,
                    color = if (item.isHoliday) TvColors.CrimsonSoft else TvColors.Gold,
                )
                Text(
                    "${TamilNames.weekdays[item.date.dayOfWeek.value % 7].first.take(3)} ${Fmt.weekdayShortEn(item.date)}",
                    style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim, maxLines = 1,
                )
            }
            item.icon?.let { Image(painterResource(it), contentDescription = null, modifier = Modifier.size(22.dp)) }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(item.titleTa, style = MaterialTheme.typography.titleMedium, color = TvColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.titleEn, style = MaterialTheme.typography.bodySmall, color = TvColors.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            item.pill?.let { Pill(it, TvColors.SurfaceSelected) }
        }
    }
}
