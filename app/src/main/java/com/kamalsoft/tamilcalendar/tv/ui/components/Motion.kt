package com.kamalsoft.tamilcalendar.tv.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kamalsoft.tamilcalendar.tv.ui.theme.DisplayPrefs
import kotlinx.coroutines.delay

/**
 * Fade + rise + slight grow when [key] changes (page opened, month flipped), delayed by [order]
 * so a grid "ripples" in. The node is never recreated, so focus is never lost during the animation.
 * Only layer properties animate: cheap even on low-end TV GPUs.
 */
fun Modifier.staggeredEnter(key: Any?, order: Int, stepMs: Long = 28L): Modifier = composed {
    if (!DisplayPrefs.animations) return@composed Modifier
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        delay((order.coerceIn(0, 14)) * stepMs)
        progress.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 12.dp.toPx()
        val s = 0.96f + 0.04f * p
        scaleX = s
        scaleY = s
    }
}

/** 0 → 1 → 0 over [periodMs], forever. Used for the focus "breathing" glow and floating icons. */
@Composable
fun rememberBreath(periodMs: Int = 1800): Float {
    if (!DisplayPrefs.animations) return 0.5f
    val t = rememberInfiniteTransition(label = "breath")
    val v = t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathValue",
    )
    return v.value
}

/** Gentle vertical float (±3dp) for decorative icons. */
fun Modifier.floating(): Modifier = composed {
    if (!DisplayPrefs.animations) return@composed Modifier
    val b = rememberBreath(2600)
    graphicsLayer { translationY = (b - 0.5f) * 6.dp.toPx() }
}
