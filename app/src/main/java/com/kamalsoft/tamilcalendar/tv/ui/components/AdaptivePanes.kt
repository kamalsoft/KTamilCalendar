package com.kamalsoft.tamilcalendar.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.kamalsoft.tamilcalendar.tv.ui.theme.DisplayPrefs

/** Gives panes their share of the space in whichever direction [AdaptivePanes] is laying out. */
class PaneScope internal constructor(private val row: RowScope?, private val column: ColumnScope?) {
    fun Modifier.pane(weight: Float, compactWeight: Float = weight): Modifier =
        if (row != null) with(row) { this@pane.weight(weight).fillMaxHeight() }
        else with(column!!) { this@pane.weight(compactWeight).fillMaxWidth() }
}

/** Side-by-side panes on TV / landscape; stacked top-to-bottom on phones. */
@Composable
fun AdaptivePanes(gap: Dp, modifier: Modifier = Modifier.fillMaxSize(), content: @Composable PaneScope.() -> Unit) {
    if (DisplayPrefs.compact) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) { PaneScope(null, this).content() }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) { PaneScope(this, null).content() }
    }
}
