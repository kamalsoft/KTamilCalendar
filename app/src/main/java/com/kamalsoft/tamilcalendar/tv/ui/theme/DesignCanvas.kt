package com.kamalsoft.tamilcalendar.tv.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.min

/** Every screen is designed for this canvas: 1080p at xhdpi, Android TV's reference size. */
const val DESIGN_WIDTH_DP = 960f
const val DESIGN_HEIGHT_DP = 540f

/**
 * Makes the UI resolution-independent, from 480p/720p boxes up to 4K and 8K panels.
 *
 * Android TV devices report very different densities for the same picture size (a 1080p box may
 * claim mdpi, hdpi or xhdpi), which would make a dp layout tiny or overflowing. Here the density is
 * re-derived so that the design canvas always fills the screen: 1 dp = screen width / 960 (or height
 * / 540, whichever is smaller, so nothing is cut off on non-16:9 screens). All art is vector, so it
 * stays sharp at 8K. The user's font-size setting is honoured within a range the layouts can absorb.
 * Portrait screens (phones, portrait tablets) switch to the compact layout ([DisplayPrefs.compact]).
 */
/** Phones (and any portrait screen) use a narrow design width; content scrolls vertically. */
const val COMPACT_DESIGN_WIDTH_DP = 412f

@Composable
fun DesignCanvas(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = constraints.maxWidth < constraints.maxHeight
        DisplayPrefs.compact = compact
        val scale = if (compact) {
            constraints.maxWidth / COMPACT_DESIGN_WIDTH_DP
        } else {
            min(constraints.maxWidth / DESIGN_WIDTH_DP, constraints.maxHeight / DESIGN_HEIGHT_DP)
        }
        val fontScale = LocalDensity.current.fontScale.coerceIn(0.85f, 1.15f)
        CompositionLocalProvider(LocalDensity provides Density(density = scale, fontScale = fontScale)) {
            content()
        }
    }
}
