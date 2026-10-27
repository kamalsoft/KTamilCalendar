package com.kamalsoft.tamilcalendar.tv.ui.app

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kamalsoft.tamilcalendar.tv.R
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors

val Destination.titleTa: String
    get() = when (this) {
        Destination.DAILY -> "இன்றைய நாள்காட்டி"
        Destination.MONTHLY -> "மாதாந்திர காட்டி"
        Destination.IMPORTANT_DATES -> "முக்கிய நாட்கள்"
        Destination.FESTIVALS -> "பண்டிகைகள்"
        Destination.MUHURTHAM -> "சுப முகூர்த்தம்"
        Destination.RASI_PALAN -> "ராசி பலன்"
        Destination.LOCATION -> "இடம்"
    }

val Destination.titleEn: String
    get() = when (this) {
        Destination.DAILY -> "Daily View"
        Destination.MONTHLY -> "Monthly Grid"
        Destination.IMPORTANT_DATES -> "Important Dates"
        Destination.FESTIVALS -> "Festivals"
        Destination.MUHURTHAM -> "Subha Muhurtham"
        Destination.RASI_PALAN -> "Rasi Palan"
        Destination.LOCATION -> "Location"
    }

@get:DrawableRes
private val Destination.icon: Int
    get() = when (this) {
        Destination.DAILY -> R.drawable.ic_nav_daily
        Destination.MONTHLY -> R.drawable.ic_nav_month
        Destination.IMPORTANT_DATES -> R.drawable.ic_nav_important
        Destination.FESTIVALS -> R.drawable.ic_nav_festival
        Destination.MUHURTHAM -> R.drawable.ic_nav_muhurtham
        Destination.RASI_PALAN -> R.drawable.ic_nav_rasi
        Destination.LOCATION -> R.drawable.ic_nav_location
    }

val DrawerCollapsedWidth = 88.dp
private val DrawerExpandedWidth = 290.dp

/**
 * Collapsed rail (icons) that expands over the content while it holds focus. Expanding overlays
 * rather than resizes content, so the content never re-measures during the animation (matters
 * on 1–1.5 GB RAM boxes). Entering from the content always lands on the selected item.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NavDrawer(
    current: Destination,
    expanded: Boolean,
    itemRequesters: Map<Destination, FocusRequester>,
    helpRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onSelect: (Destination) -> Unit,
    onHelp: () -> Unit,
) {
    val width by animateDpAsState(if (expanded) DrawerExpandedWidth else DrawerCollapsedWidth, tween(180), label = "drawerWidth")
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    if (expanded) listOf(TvColors.MidnightDeep, TvColors.Midnight, TvColors.Midnight.copy(alpha = 0.92f))
                    else listOf(TvColors.MidnightDeep.copy(alpha = 0.6f), Color.Transparent),
                ),
            )
            .onFocusChanged { onFocusChanged(it.hasFocus) }
            .focusProperties { enter = { itemRequesters.getValue(current) } }
            .focusGroup()
            .padding(horizontal = 14.dp, vertical = 27.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.height(56.dp).padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.gopuram), contentDescription = null, modifier = Modifier.size(40.dp))
            if (expanded) {
                Column(Modifier.padding(start = 12.dp)) {
                    Text("தமிழ் நாள்காட்டி", style = MaterialTheme.typography.titleMedium, color = TvColors.Gold, maxLines = 1)
                    Text("Tamil Calendar", style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Destination.entries.forEach { dest ->
            val selected = dest == current
            TvFocusCard(
                onClick = { onSelect(dest) },
                focusRequester = itemRequesters.getValue(dest),
                selected = selected,
                cornerRadius = 14.dp,
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { focused ->
                Row(Modifier.padding(horizontal = 16.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painterResource(dest.icon), contentDescription = dest.titleTa,
                        colorFilter = ColorFilter.tint(if (focused || selected) TvColors.Gold else TvColors.TextMuted),
                        modifier = Modifier.size(26.dp),
                    )
                    if (expanded) {
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(dest.titleTa, style = MaterialTheme.typography.titleMedium, color = if (focused) TvColors.Gold else TvColors.Text, maxLines = 1)
                            Text(dest.titleEn, style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim, maxLines = 1)
                        }
                    }
                }
            }
        }
        Box(Modifier.weight(1f))
        TvFocusCard(
            onClick = onHelp,
            focusRequester = helpRequester,
            cornerRadius = 14.dp,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { focused ->
            Row(Modifier.padding(horizontal = 16.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                Text("?", style = MaterialTheme.typography.headlineSmall, color = if (focused) TvColors.Gold else TvColors.TextMuted, modifier = Modifier.width(26.dp))
                if (expanded) {
                    Text("உதவி · Help", style = MaterialTheme.typography.titleMedium, color = if (focused) TvColors.Gold else TvColors.Text, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}
