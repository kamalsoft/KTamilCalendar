package com.kamalsoft.tamilcalendar.tv.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Theme-tinted backdrop: the base gradient, two soft light pools (accent top-left, alert bottom-right)
 * and a faint kolam dot lattice. Everything is built once per size/theme in drawWithCache, so it costs
 * nothing per frame even on low-end boxes, and is resolution-independent (dp-based, vector).
 */
fun Modifier.ambientBackground(): Modifier = drawWithCache {
    val p = TvColors.palette
    val base = Brush.verticalGradient(listOf(p.background, p.backgroundMid, p.backgroundDeep))
    val glowA = Brush.radialGradient(
        listOf(p.accent.copy(alpha = if (p.isLight) 0.10f else 0.12f), p.accent.copy(alpha = 0f)),
        center = Offset(size.width * 0.12f, size.height * 0.05f), radius = max(size.width, size.height) * 0.55f,
    )
    val glowB = Brush.radialGradient(
        listOf(p.alert.copy(alpha = if (p.isLight) 0.06f else 0.08f), p.alert.copy(alpha = 0f)),
        center = Offset(size.width * 0.95f, size.height * 1.0f), radius = max(size.width, size.height) * 0.5f,
    )
    val step = 28.dp.toPx()
    val dot = 1.1.dp.toPx()
    val dotColor = p.text.copy(alpha = if (p.isLight) 0.05f else 0.035f)
    val w = size.width
    val h = size.height
    // Kolam-style lattice: dots on a diamond grid (every other row offset by half a step).
    val dots = buildList {
        var row = 0
        var y = step / 2
        while (y < h) {
            var x = if (row % 2 == 0) step / 2 else step
            while (x < w) { add(Offset(x, y)); x += step }
            y += step * 0.866f
            row++
        }
    }
    onDrawBehind {
        drawRect(base)
        drawRect(glowA)
        drawRect(glowB)
        dots.forEach { drawCircle(dotColor, dot, it) }
    }
}
