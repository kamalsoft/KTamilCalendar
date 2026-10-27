package com.kamalsoft.tamilcalendar.tv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import kotlinx.coroutines.delay

data class DetailContent(val title: String, val subtitle: String? = null, val lines: List<Pair<String, String>>)

/** Holds the open panel and the card to refocus once the panel has actually left composition. */
class DetailHostState {
    var content by mutableStateOf<DetailContent?>(null)
        private set
    private var origin: FocusRequester? = null
    internal var pendingReturn by mutableStateOf<FocusRequester?>(null)

    fun show(content: DetailContent, origin: FocusRequester) {
        this.origin = origin
        this.content = content
    }

    internal fun dismiss() {
        pendingReturn = origin
        content = null
    }
}

@Composable
fun rememberDetailHostState() = remember { DetailHostState() }

/** Place last inside the screen's root Box so the panel draws on top. */
@Composable
fun DetailHost(state: DetailHostState) {
    state.content?.let { DetailOverlay(it, onDismiss = state::dismiss) }
    LaunchedEffect(state.pendingReturn) {
        val target = state.pendingReturn ?: return@LaunchedEffect
        // Requesting while the focused overlay is still attached loses focus when it detaches.
        delay(16)
        runCatching { target.requestFocus() }
        state.pendingReturn = null
    }
}

/**
 * Modal panel. Focus is trapped on the single "close" card, so D-pad can't wander behind it;
 * BACK or CENTER dismisses; [DetailHost] then returns focus to the card that opened it.
 */
@Composable
private fun DetailOverlay(content: DetailContent, onDismiss: () -> Unit) {
    val closeFocus = remember { FocusRequester() }
    BackHandler(onBack = onDismiss)
    LaunchedEffect(Unit) {
        repeat(5) {
            if (runCatching { closeFocus.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(16)
        }
    }
    Box(Modifier.fillMaxSize().background(TvColors.MidnightDeep.copy(alpha = 0.82f)), contentAlignment = Alignment.Center) {
        TvFocusCard(
            onClick = onDismiss,
            focusRequester = closeFocus,
            onDirectionalKey = { true }, // trap focus inside the modal
            modifier = Modifier.width(520.dp),
        ) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(content.title, style = MaterialTheme.typography.headlineSmall, color = TvColors.Gold)
                content.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted) }
                content.lines.forEach { (label, value) ->
                    Column {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim)
                        Text(value, style = MaterialTheme.typography.bodyMedium, color = TvColors.Text)
                    }
                }
                Text("மூட OK / BACK அழுத்தவும் · Press OK or BACK to close", style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim)
            }
        }
    }
}
