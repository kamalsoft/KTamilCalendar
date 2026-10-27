package com.kamalsoft.tamilcalendar.tv.ui.app

import android.view.KeyEvent as AndroidKeyEvent
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import com.kamalsoft.tamilcalendar.tv.ui.theme.DisplayPrefs
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import com.kamalsoft.tamilcalendar.tv.ui.components.Text
import com.kamalsoft.tamilcalendar.tv.R
import com.kamalsoft.tamilcalendar.tv.ui.components.rememberBreath
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import androidx.compose.ui.draw.shadow

/** Short tab labels (the tab bar is one line, like tvOS). */
val Destination.tabTa: String
    get() = when (this) {
        Destination.DAILY -> "இன்று"
        Destination.MONTHLY -> "மாதம்"
        Destination.IMPORTANT_DATES -> "சிறப்பு"
        Destination.FESTIVALS -> "பண்டிகை"
        Destination.MUHURTHAM -> "முகூர்த்தம்"
        Destination.RASI_PALAN -> "ராசி"
        Destination.LOCATION -> "இடம்"
        Destination.THEME -> "நிறம்"
        Destination.SETTINGS -> "அமைப்பு"
        Destination.ABOUT -> "பற்றி"
    }

val Destination.tabEn: String
    get() = when (this) {
        Destination.DAILY -> "Today"
        Destination.MONTHLY -> "Month"
        Destination.IMPORTANT_DATES -> "Important"
        Destination.FESTIVALS -> "Festivals"
        Destination.MUHURTHAM -> "Muhurtham"
        Destination.RASI_PALAN -> "Rasi"
        Destination.LOCATION -> "Location"
        Destination.THEME -> "Theme"
        Destination.SETTINGS -> "Settings"
        Destination.ABOUT -> "About"
    }

/** Utility destinations shown as round icon buttons at the right end, like tvOS search/settings. */
@get:DrawableRes
private val Destination.utilityIcon: Int?
    get() = when (this) {
        Destination.LOCATION -> R.drawable.ic_nav_location
        Destination.THEME -> R.drawable.ic_nav_theme
        Destination.SETTINGS -> R.drawable.ic_settings
        Destination.ABOUT -> R.drawable.ic_info
        else -> null
    }

/**
 * Apple TV–style top tab bar. Moving focus across tabs switches the screen immediately (as tvOS
 * does); ▼ or OK enters the content. Coming back up always lands on the current tab.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TopTabBar(
    current: Destination,
    /** Tabs to show (sections hidden in Settings are left out). */
    visible: List<Destination>,
    tabRequesters: Map<Destination, FocusRequester>,
    onFocusChanged: (Boolean) -> Unit,
    onTabFocused: (Destination) -> Unit,
    onEnterContent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Phones: the bar scrolls sideways instead of squeezing the tabs.
    val compact = DisplayPrefs.compact
    val scroll = rememberScrollState()
    Row(
        modifier
            .fillMaxWidth()
            .then(if (compact) Modifier.horizontalScroll(scroll) else Modifier)
            .height(48.dp)
            .onFocusChanged { onFocusChanged(it.hasFocus) }
            .focusProperties { enter = { tabRequesters.getValue(current) } }
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.gopuram), contentDescription = "Tamil Calendar", modifier = Modifier.size(30.dp))
        Spacer(if (compact) Modifier.width(8.dp) else Modifier.weight(1f))
        // tvOS groups the tabs in a soft capsule.
        Row(
            Modifier
                .background(TvColors.Surface.copy(alpha = 0.55f), RoundedCornerShape(50))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            visible.filter { it.utilityIcon == null }.forEach { dest ->
                TabItem(
                    selected = dest == current,
                    requester = tabRequesters.getValue(dest),
                    onFocused = { onTabFocused(dest) },
                    onClick = onEnterContent,
                ) { color ->
                    Text(dest.tabTa, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1, softWrap = false)
                }
            }
        }
        Spacer(if (compact) Modifier.width(8.dp) else Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            visible.mapNotNull { d -> d.utilityIcon?.let { d to it } }.forEach { (dest, icon) ->
                TabItem(
                    selected = dest == current,
                    requester = tabRequesters.getValue(dest),
                    onFocused = { onTabFocused(dest) },
                    onClick = onEnterContent,
                    round = true,
                ) { color ->
                    Image(painterResource(icon), contentDescription = dest.tabEn, colorFilter = ColorFilter.tint(color), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** tvOS tab: the focused tab becomes a solid light capsule with dark text; the current tab stays highlighted. */
@Composable
private fun TabItem(
    selected: Boolean,
    requester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    round: Boolean = false,
    content: @Composable (contentColor: Color) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    // OK only counts when this tab saw the key going down, so a stray key-up (e.g. from a system dialog
    // that just closed) can't trigger it.
    var okDown by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(150), label = "tabScale")
    val bg by animateColorAsState(
        when {
            focused -> TvColors.Text
            selected -> TvColors.Text.copy(alpha = 0.16f)
            else -> Color.Transparent
        },
        tween(150), label = "tabBg",
    )
    // Focused tab glows softly, like the focused card (only composed while focused).
    val breath = if (focused) rememberBreath() else 0f
    val contentColor = when {
        focused -> TvColors.Midnight
        selected -> TvColors.Text
        else -> TvColors.TextMuted
    }
    Box(
        Modifier
            .focusRequester(requester)
            .onFocusChanged {
                if (it.isFocused && !focused) onFocused()
                focused = it.isFocused
                if (!it.isFocused) okDown = false
            }
            .onKeyEvent { e ->
                val code = e.nativeKeyEvent.keyCode
                val isOk = code == AndroidKeyEvent.KEYCODE_DPAD_CENTER || code == AndroidKeyEvent.KEYCODE_ENTER ||
                    code == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
                if (isOk && e.type == KeyEventType.KeyDown) okDown = true
                if (isOk && e.type == KeyEventType.KeyUp && okDown) {
                    okDown = false
                    onClick()
                }
                isOk
            }
            .focusable()
            .pointerInput(Unit) { detectTapGestures { onFocused(); onClick() } }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) (6 + 6 * breath).dp else 0.dp, if (round) CircleShape else RoundedCornerShape(50), clip = false)
            .background(bg, if (round) CircleShape else RoundedCornerShape(50))
            .then(if (round) Modifier.size(36.dp) else Modifier.height(36.dp).padding(horizontal = 12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        content(contentColor)
    }
}
