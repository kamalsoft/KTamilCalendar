package com.kamalsoft.tamilcalendar.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.theme.AppTheme
import com.kamalsoft.tamilcalendar.tv.ui.theme.Palette
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors

/** Pick a colour theme; each card previews the theme in its own colours. OK applies it immediately. */
@Composable
fun ThemeScreen(current: AppTheme, entryFocus: FocusRequester, onChoose: (AppTheme) -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("நிறம் · Theme", style = MaterialTheme.typography.headlineSmall, color = TvColors.Gold)
        Text(
            "OK அழுத்தி தேர்வு செய்யவும் · Press OK to apply. The choice is remembered.",
            style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted,
        )
        // Ten themes: a scrolling 3-column grid; focus scrolls it (bring-into-view), padding leaves room for the lift.
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(12.dp),
        ) {
            items(AppTheme.entries.size) { i ->
                val theme = AppTheme.entries[i]
                ThemeCard(
                    theme = theme,
                    isCurrent = theme == current,
                    focusRequester = if (theme == current) entryFocus else null,
                    onClick = { onChoose(theme) },
                    modifier = Modifier.fillMaxWidth().height(170.dp),
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(theme: AppTheme, isCurrent: Boolean, focusRequester: FocusRequester?, onClick: () -> Unit, modifier: Modifier) {
    TvFocusCard(onClick = onClick, focusRequester = focusRequester, selected = isCurrent, modifier = modifier) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                Text(theme.ta, style = MaterialTheme.typography.titleMedium, color = TvColors.Text, maxLines = 1)
                Text(
                    if (isCurrent) "✓ ${theme.en}" else theme.en,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) TvColors.Gold else TvColors.TextDim, maxLines = 1,
                )
            }
            Preview(theme.palette, Modifier.fillMaxWidth().weight(1f))
        }
    }
}

/** A miniature of the Today screen drawn in [p]'s own colours (independent of the active theme). */
@Composable
private fun Preview(p: Palette, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .background(Brush.verticalGradient(listOf(p.background, p.backgroundMid, p.backgroundDeep)), shape)
            .border(1.dp, p.textDim.copy(alpha = 0.4f), shape)
            .padding(10.dp),
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                Modifier.weight(1.2f).fillMaxHeight().background(p.surface, RoundedCornerShape(8.dp))
                    .border(2.dp, p.accent, RoundedCornerShape(8.dp)).padding(8.dp),
            ) {
                Text("திங்கள்", style = MaterialTheme.typography.labelMedium, color = p.text, maxLines = 1)
                Text("ஐப்பசி 10", style = MaterialTheme.typography.titleMedium, color = p.accent, maxLines = 1)
                Text("Monday", style = MaterialTheme.typography.labelMedium, color = p.textDim, maxLines = 1)
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Swatch(p, p.accent, "நல்ல நேரம்", Modifier.weight(1f))
                Swatch(p, p.alert, "ராகு காலம்", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Swatch(p: Palette, bar: Color, label: String, modifier: Modifier) {
    Row(modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(6.dp)).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).background(bar, RoundedCornerShape(2.dp)))
        Text(label, style = MaterialTheme.typography.labelMedium, color = bar, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
    }
}
