package com.kamalsoft.tamilcalendar.tv.ui.daily

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import com.kamalsoft.tamilcalendar.tv.ui.components.DpadDirection
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import com.kamalsoft.tamilcalendar.tv.data.audio.Speaker
import com.kamalsoft.tamilcalendar.tv.domain.content.Sloka
import com.kamalsoft.tamilcalendar.tv.ui.components.Text
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.components.rememberBreath
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import kotlinx.coroutines.delay

/** Repetitions per play (a sloka is traditionally chanted thrice; ≈ 1 minute). */
const val SLOKA_REPEAT = 3

/** "Now playing" panel for the day's sloka. Opens playing; OK toggles; BACK stops and closes. */
@Composable
fun SlokaPanel(sloka: Sloka, speaker: Speaker.State, onToggle: () -> Unit, onClose: () -> Unit) {
    val playing = speaker.speakingId == sloka.id
    val playFocus = remember { FocusRequester() }
    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) {
        if (!playing) onToggle()
        repeat(5) { if (runCatching { playFocus.requestFocus() }.isSuccess) return@LaunchedEffect; delay(16) }
    }
    Box(
        Modifier.fillMaxSize().background(TvColors.MidnightDeep.copy(alpha = 0.85f))
            // Tap/click outside the panel closes it; Esc on a keyboard acts like BACK.
            .pointerInput(Unit) { detectTapGestures { onClose() } }
            .onPreviewKeyEvent { e ->
                if (e.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_ESCAPE && e.type == KeyEventType.KeyUp) { onClose(); true } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth(0.92f).widthIn(max = 620.dp)
                .pointerInput(Unit) { detectTapGestures { } } // taps on the panel itself don't close it
                .background(TvColors.Surface, androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                .padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("இன்றைய ஸ்லோகம் · Today's sloka", style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim)
            Text("${sloka.titleTa} · ${sloka.titleEn}", style = MaterialTheme.typography.headlineSmall, color = TvColors.Gold)
            Text(sloka.textTa, fontSize = 22.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, color = TvColors.Text)
            Text(sloka.transliteration, style = MaterialTheme.typography.bodyMedium, color = TvColors.TextMuted)
            Text(sloka.meaningEn, style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted)
            Text(sloka.source, style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TvFocusCard(
                    onClick = onToggle, focusRequester = playFocus, cornerRadius = 26.dp,
                    // Only ▶ leaves this button (to Close); focus stays inside the panel.
                    onDirectionalKey = { d -> d != DpadDirection.RIGHT },
                    modifier = Modifier.width(200.dp).height(52.dp),
                ) {
                    Text(
                        if (playing) "■  நிறுத்து · Stop" else "▶  இயக்கு · Play",
                        style = MaterialTheme.typography.titleMedium, color = TvColors.Text, modifier = Modifier.align(Alignment.Center),
                    )
                }
                TvFocusCard(
                    onClick = onClose, cornerRadius = 26.dp,
                    onDirectionalKey = { d -> d != DpadDirection.LEFT },
                    modifier = Modifier.width(190.dp).height(52.dp),
                ) {
                    Text(
                        "✕  நிறைவு · Close", style = MaterialTheme.typography.titleMedium, color = TvColors.Text,
                        maxLines = 1, softWrap = false, modifier = Modifier.align(Alignment.Center),
                    )
                }
                if (playing) {
                    // Simple level meter so it's visible that audio is playing.
                    val b = rememberBreath(600)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                        listOf(0.5f, 1f, 0.7f, 0.9f).forEachIndexed { i, base ->
                            val h = (8 + 22 * ((base + b * (if (i % 2 == 0) 1 else -1)).coerceIn(0.2f, 1f))).dp
                            Box(Modifier.width(5.dp).height(h).background(TvColors.Gold, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)))
                        }
                    }
                }
            }
            Text(
                when {
                    speaker.unavailable -> "இந்தச் சாதனத்தில் குரல் இல்லை · Text-to-speech isn't available on this device."
                    speaker.tamilVoice == false -> "தமிழ்க் குரல் இல்லை; ஒலிபெயர்ப்பு வாசிக்கப்படுகிறது · No Tamil voice installed — reading the transliteration."
                    else -> "சாதனக் குரல் · Device text-to-speech · read $SLOKA_REPEAT times · check the TV volume"
                },
                style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim,
            )
        }
    }
}
