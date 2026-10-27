package com.kamalsoft.tamilcalendar.tv.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.EventSource
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilNames
import com.kamalsoft.tamilcalendar.tv.ui.components.Fmt
import com.kamalsoft.tamilcalendar.tv.ui.components.Pill
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.components.staggeredEnter
import androidx.compose.foundation.layout.Box
import com.kamalsoft.tamilcalendar.tv.ui.daily.LoadingState
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import java.time.LocalDate

/** Shared by Festivals and Subha Muhurtham. Focused rows auto-scroll into view (bringIntoView). */
@Composable
fun EventListScreen(
    title: String,
    subtitle: String,
    events: List<CalendarEvent>,
    today: LocalDate,
    entryFocus: FocusRequester,
    onOpenDate: (LocalDate) -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = TvColors.Gold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted)
        if (events.isEmpty()) {
            LoadingState()
            return@Column
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // Room for the 1.05x focus scale + halo so the first/last rows are never clipped.
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
        ) {
            itemsIndexed(events, key = { _, e -> "${e.date}-${e.titleEn}-${e.source}-${e.region}" }) { index, e ->
                Box(Modifier.staggeredEnter(Unit, index)) {
                    EventRow(e, isToday = today in e.date..(e.endDate ?: e.date), focusRequester = if (index == 0) entryFocus else null) { onOpenDate(e.date) }
                }
            }
        }
    }
}

@Composable
private fun EventRow(e: CalendarEvent, isToday: Boolean, focusRequester: FocusRequester?, onClick: () -> Unit) {
    TvFocusCard(
        onClick = onClick,
        focusRequester = focusRequester,
        accent = when {
            e.isLocalHoliday -> TvColors.Crimson
            e.kind == EventKind.SCHOOL -> TvColors.Amber
            else -> TvColors.Gold
        },
        modifier = Modifier.fillMaxWidth().height(68.dp),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(150.dp)) {
                Text(
                    "${e.date.dayOfMonth} ${Fmt.gregorianMonthsTa[e.date.monthValue - 1]}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (e.isLocalHoliday) TvColors.CrimsonSoft else TvColors.Gold,
                )
                Text(
                    e.endDate?.let { "– ${it.dayOfMonth} ${Fmt.gregorianMonthsTa[it.monthValue - 1]}" }
                        ?: "${TamilNames.weekdays[e.date.dayOfWeek.value % 7].first} · ${Fmt.weekdayShortEn(e.date)}",
                    style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(e.titleTa, style = MaterialTheme.typography.titleMedium, color = TvColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(e.titleEn, e.detail).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = TvColors.TextDim, maxLines = 1,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isToday) Pill("இன்று · Today", TvColors.Crimson)
                if (e.isLocalHoliday) Pill("விடுமுறை · Holiday", TvColors.Crimson.copy(alpha = 0.5f))
                if (e.kind == EventKind.SCHOOL && e.isHoliday) Pill("பள்ளி இல்லை · No school", TvColors.Amber.copy(alpha = 0.3f))
                e.region?.let { Pill(it.en, TvColors.SurfaceSelected) }
                if (e.kind == EventKind.VIRATHAM) Pill("விரதம் · Fast", TvColors.SurfaceSelected)
                if (e.source == EventSource.REMOTE) Pill("அதிகாரப்பூர்வம் · Official", TvColors.SurfaceSelected)
            }
        }
    }
}
