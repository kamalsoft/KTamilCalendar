package com.kamalsoft.tamilcalendar.tv.ui.components

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvDimens

enum class DpadDirection { UP, DOWN, LEFT, RIGHT }

/**
 * The single focus primitive for the whole app.
 *
 * Focused state follows tvOS: the card lifts (1.06x scale, 20dp shadow), brightens, and gets a soft
 * top sheen. Themes with [com.kamalsoft.tamilcalendar.tv.ui.theme.Palette.focusRing] (Temple Night,
 * High Contrast) add a 2dp accent ring and halo for stronger 10-foot visibility; the halo is drawn
 * manually because coloured shadows only render on API 28+.
 *
 * Key handling is explicit rather than relying on `clickable`:
 * - DPAD_CENTER / ENTER / NUMPAD_ENTER / BUTTON_A: click fires on key-up (like Leanback), long
 *   press fires once on the first auto-repeat.
 * - DPAD_UP/DOWN/LEFT/RIGHT: offered to [onDirectionalKey] first; if it returns false the event
 *   falls through to Compose's focus search, so normal spatial navigation keeps working.
 * Touch/mouse taps are kept as a fallback (emulator clicks, touch-enabled TVs).
 */
@Composable
fun TvFocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onDirectionalKey: ((DpadDirection) -> Boolean)? = null,
    onFocused: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    selected: Boolean = false,
    cornerRadius: Dp = TvDimens.CardRadius,
    containerColor: Color = TvColors.Surface,
    focusedContainerColor: Color = TvColors.SurfaceFocused,
    accent: Color = TvColors.Gold,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var longPressFired by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.97f
            focused -> 1.06f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 140),
        label = "focusScale",
    )
    val ring = TvColors.palette.focusRing
    val elevation by animateDpAsState(if (focused) (if (ring) 8.dp else 20.dp) else 0.dp, tween(160), label = "focusElevation")
    val background by animateColorAsState(
        when {
            focused -> focusedContainerColor
            selected -> TvColors.SurfaceSelected
            else -> containerColor
        },
        tween(140), label = "focusBackground",
    )
    val ringColor = when {
        focused && ring -> accent
        selected -> accent.copy(alpha = 0.45f)
        else -> Color.Transparent
    }
    val sheen = TvColors.Text.copy(alpha = if (TvColors.palette.isLight) 0.0f else 0.07f)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { state ->
                if (state.isFocused && !focused) onFocused?.invoke()
                focused = state.isFocused
                if (!state.isFocused) pressed = false
            }
            .onKeyEvent { event ->
                val native = event.nativeKeyEvent
                when (native.keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                    AndroidKeyEvent.KEYCODE_ENTER,
                    AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
                    AndroidKeyEvent.KEYCODE_BUTTON_A,
                    -> when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (native.repeatCount == 0) {
                                pressed = true
                                longPressFired = false
                            } else if (native.repeatCount == 1 && onLongClick != null) {
                                longPressFired = true
                                onLongClick()
                            }
                            true
                        }
                        KeyEventType.KeyUp -> {
                            if (pressed && !longPressFired) onClick()
                            pressed = false
                            true
                        }
                        else -> false
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_UP -> event.type == KeyEventType.KeyDown && onDirectionalKey?.invoke(DpadDirection.UP) == true
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> event.type == KeyEventType.KeyDown && onDirectionalKey?.invoke(DpadDirection.DOWN) == true
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT -> event.type == KeyEventType.KeyDown && onDirectionalKey?.invoke(DpadDirection.LEFT) == true
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> event.type == KeyEventType.KeyDown && onDirectionalKey?.invoke(DpadDirection.RIGHT) == true
                    else -> false
                }
            }
            .focusable()
            .semantics {
                role = Role.Button
                onClick { onClick(); true }
            }
            .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .goldHalo(visible = focused && ring, color = accent, cornerRadius = cornerRadius)
            .shadow(
                elevation, shape, clip = false,
                ambientColor = if (ring) accent else Color.Black, spotColor = if (ring) accent else Color.Black,
            )
            .background(background, shape)
            // tvOS-like specular sheen on the upper half of a focused card.
            .then(
                if (focused && !ring) {
                    Modifier.background(Brush.verticalGradient(0f to sheen, 0.55f to Color.Transparent), shape)
                } else {
                    Modifier
                },
            )
            .border(2.dp, ringColor, shape),
    ) {
        content(focused)
    }
}

/** Three translucent strokes outside the card bounds: a cheap glow that renders on every API level. */
private fun Modifier.goldHalo(visible: Boolean, color: Color, cornerRadius: Dp): Modifier =
    if (!visible) this else drawBehind {
        val radius = cornerRadius.toPx()
        listOf(3.dp to 0.30f, 6.dp to 0.14f, 10.dp to 0.06f).forEach { (spread, alpha) ->
            val s = spread.toPx()
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(-s, -s),
                size = Size(size.width + 2 * s, size.height + 2 * s),
                cornerRadius = CornerRadius(radius + s),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
