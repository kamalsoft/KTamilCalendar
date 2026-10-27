package com.kamalsoft.tamilcalendar.tv.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import com.kamalsoft.tamilcalendar.tv.R
import com.kamalsoft.tamilcalendar.tv.ui.components.Text
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors

val COMPACT_TOP_BAR_HEIGHT = 48.dp
val COMPACT_BOTTOM_BAR_HEIGHT = 64.dp

/** Primary phone destinations in the bottom bar; everything else is under "More". */
private val PRIMARY = listOf(Destination.DAILY, Destination.MONTHLY, Destination.IMPORTANT_DATES, Destination.RASI_PALAN)

private val Destination.icon: Int
    get() = when (this) {
        Destination.DAILY -> R.drawable.ic_nav_daily
        Destination.MONTHLY -> R.drawable.ic_nav_month
        Destination.IMPORTANT_DATES -> R.drawable.ic_nav_important
        Destination.FESTIVALS -> R.drawable.ic_nav_festival
        Destination.MUHURTHAM -> R.drawable.ic_nav_muhurtham
        Destination.RASI_PALAN -> R.drawable.ic_nav_rasi
        Destination.LOCATION -> R.drawable.ic_nav_location
        Destination.THEME -> R.drawable.ic_nav_theme
        Destination.SETTINGS -> R.drawable.ic_settings
        Destination.ABOUT -> R.drawable.ic_info
    }

/** Phone header: logo + the current section's name. */
@Composable
fun CompactTopBar(current: Destination, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(COMPACT_TOP_BAR_HEIGHT).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.gopuram), contentDescription = null, modifier = Modifier.size(26.dp))
        Text(
            "${current.tabTa} · ${current.tabEn}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TvColors.Text,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp),
        )
    }
}

/** Phone bottom navigation: four main sections + More. Always visible, so every section is one or two taps away. */
@Composable
fun CompactBottomBar(
    current: Destination,
    visible: List<Destination>,
    moreOpen: Boolean,
    onSelect: (Destination) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = PRIMARY.filter { it in visible }
    Row(
        modifier.fillMaxWidth().height(COMPACT_BOTTOM_BAR_HEIGHT)
            .background(TvColors.Surface.copy(alpha = 0.96f))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        primary.forEach { d -> NavItem(d.icon, d.tabTa, selected = d == current && !moreOpen, modifier = Modifier.weight(1f)) { onSelect(d) } }
        NavItem(R.drawable.ic_more, "மேலும்", selected = moreOpen || (current !in primary), modifier = Modifier.weight(1f), onClick = onMore)
    }
}

@Composable
private fun NavItem(icon: Int, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    TvFocusCard(
        onClick = onClick, cornerRadius = 14.dp, selected = false,
        containerColor = if (selected) TvColors.Text.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent,
        modifier = modifier.fillMaxHeight().padding(horizontal = 3.dp),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(
                painterResource(icon), contentDescription = label,
                colorFilter = ColorFilter.tint(if (selected) TvColors.Gold else TvColors.TextMuted), modifier = Modifier.size(22.dp),
            )
            Text(label, fontSize = 11.sp, color = if (selected) TvColors.Gold else TvColors.TextMuted, maxLines = 1)
        }
    }
}

/** "More" panel above the bottom bar: the remaining sections as large touch rows. */
@Composable
fun CompactMoreSheet(current: Destination, visible: List<Destination>, onSelect: (Destination) -> Unit, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val rest = visible.filter { it !in PRIMARY }
    Box(
        Modifier.fillMaxSize().background(TvColors.MidnightDeep.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = COMPACT_BOTTOM_BAR_HEIGHT + 8.dp)
                .background(TvColors.Surface, RoundedCornerShape(20.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("மேலும் · More", style = MaterialTheme.typography.titleMedium, color = TvColors.Gold, modifier = Modifier.padding(6.dp))
            rest.forEach { d ->
                TvFocusCard(onClick = { onSelect(d) }, selected = d == current, cornerRadius = 12.dp, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(d.icon), contentDescription = null, colorFilter = ColorFilter.tint(TvColors.Gold), modifier = Modifier.size(22.dp))
                        Text("${d.tabTa} · ${d.tabEn}", style = MaterialTheme.typography.titleMedium, color = TvColors.Text, modifier = Modifier.padding(start = 14.dp))
                    }
                }
            }
        }
    }
}
