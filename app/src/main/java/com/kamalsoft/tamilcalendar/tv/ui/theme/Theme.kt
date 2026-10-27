package com.kamalsoft.tamilcalendar.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** One colour scheme. Names describe roles, so every theme maps the same semantics. */
data class Palette(
    /** Screen background base (the "midnight" of the default theme). */
    val background: Color,
    /** Deepest background tone: gradient end, scrims, drawer. */
    val backgroundDeep: Color,
    val backgroundMid: Color,
    val surface: Color,
    val surfaceFocused: Color,
    val surfaceSelected: Color,
    /** Primary accent: focus ring, headings, good times. */
    val accent: Color,
    val accentSoft: Color,
    /** Alerts: holidays, Rahu Kalam, Sundays. */
    val alert: Color,
    val alertSoft: Color,
    val amber: Color,
    val text: Color,
    val textMuted: Color,
    val textDim: Color,
    val isLight: Boolean = false,
    /** Draw an accent ring around focused cards (temple / high-contrast looks). tvOS-style themes use lift + shadow only. */
    val focusRing: Boolean = false,
)

/** User-selectable colour themes (persisted by name). */
enum class AppTheme(val ta: String, val en: String, val palette: Palette) {
    /** Apple TV look: system greys, system yellow / red. */
    GRAPHITE(
        "கிராஃபைட்", "Graphite (Apple TV)",
        Palette(
            background = Color(0xFF1C1C1E), backgroundDeep = Color(0xFF000000), backgroundMid = Color(0xFF0E0E10),
            surface = Color(0xFF2A2A2D), surfaceFocused = Color(0xFF48484A), surfaceSelected = Color(0xFF333336),
            accent = Color(0xFFFFD60A), accentSoft = Color(0xFFFFE066),
            alert = Color(0xFFFF453A), alertSoft = Color(0xFFFF6961), amber = Color(0xFFFF9F0A),
            text = Color(0xFFFFFFFF), textMuted = Color(0xFFD1D1D6), textDim = Color(0xFF98989F),
        ),
    ),
    TEMPLE_NIGHT(
        "கோயில் இரவு", "Temple Night",
        Palette(
            background = Color(0xFF0F0E17), backgroundDeep = Color(0xFF08070D), backgroundMid = Color(0xFF17132B),
            surface = Color(0xFF1B1A2A), surfaceFocused = Color(0xFF2B2842), surfaceSelected = Color(0xFF231F38),
            accent = Color(0xFFFFD700), accentSoft = Color(0xFFE8C44A),
            alert = Color(0xFFDC143C), alertSoft = Color(0xFFFF4D6D), amber = Color(0xFFFFB020),
            text = Color(0xFFFFFFFE), textMuted = Color(0xFFC3C4D6), textDim = Color(0xFF8C8DA3),
            focusRing = true,
        ),
    ),
    PEACOCK(
        "மயில்", "Peacock",
        Palette(
            background = Color(0xFF061A1E), backgroundDeep = Color(0xFF030E11), backgroundMid = Color(0xFF0A2A30),
            surface = Color(0xFF0F2C33), surfaceFocused = Color(0xFF184651), surfaceSelected = Color(0xFF133840),
            accent = Color(0xFF3DD9C1), accentSoft = Color(0xFFE9C46A),
            alert = Color(0xFFFF5A5F), alertSoft = Color(0xFFFF8A8E), amber = Color(0xFFF4A261),
            text = Color(0xFFF5FFFD), textMuted = Color(0xFFB9D6D2), textDim = Color(0xFF7FA29D),
        ),
    ),
    OCEAN(
        "கடல்", "Ocean",
        Palette(
            background = Color(0xFF0B1426), backgroundDeep = Color(0xFF060B16), backgroundMid = Color(0xFF12203D),
            surface = Color(0xFF15233F), surfaceFocused = Color(0xFF223763), surfaceSelected = Color(0xFF1B2D50),
            accent = Color(0xFFFFB627), accentSoft = Color(0xFFFFD27F),
            alert = Color(0xFFFF4D6D), alertSoft = Color(0xFFFF8099), amber = Color(0xFFFF9F43),
            text = Color(0xFFF7FAFF), textMuted = Color(0xFFBFCBE6), textDim = Color(0xFF8394B8),
        ),
    ),
    JASMINE(
        "மல்லிகை", "Jasmine (light)",
        Palette(
            background = Color(0xFFFFF8E7), backgroundDeep = Color(0xFFF3E6C4), backgroundMid = Color(0xFFFBF0D5),
            surface = Color(0xFFFFFFFF), surfaceFocused = Color(0xFFFFF1C9), surfaceSelected = Color(0xFFFDF0D2),
            accent = Color(0xFF9A6A00), accentSoft = Color(0xFFB8860B),
            alert = Color(0xFFB3122E), alertSoft = Color(0xFFC62842), amber = Color(0xFFB45309),
            text = Color(0xFF2B2118), textMuted = Color(0xFF5C4D3C), textDim = Color(0xFF7D6B57),
            isLight = true,
        ),
    ),
    HIGH_CONTRAST(
        "உயர் மாறுபாடு", "High Contrast",
        Palette(
            background = Color(0xFF000000), backgroundDeep = Color(0xFF000000), backgroundMid = Color(0xFF000000),
            surface = Color(0xFF141414), surfaceFocused = Color(0xFF2A2A2A), surfaceSelected = Color(0xFF1F1F1F),
            accent = Color(0xFFFFFF00), accentSoft = Color(0xFFFFEA00),
            alert = Color(0xFFFF3B3B), alertSoft = Color(0xFFFF6B6B), amber = Color(0xFFFFA000),
            text = Color(0xFFFFFFFF), textMuted = Color(0xFFEDEDED), textDim = Color(0xFFCFCFCF),
            focusRing = true,
        ),
    ),
    ;

    companion object {
        fun fromName(name: String?): AppTheme = entries.firstOrNull { it.name == name } ?: GRAPHITE
    }
}

/**
 * The app's colours. Backed by Compose state, so assigning [palette] recolours every screen at once.
 * Property names are the original temple-theme roles (Gold = accent, Crimson = alert, Midnight = background).
 */
object TvColors {
    var palette: Palette by mutableStateOf(AppTheme.GRAPHITE.palette)

    val Midnight get() = palette.background
    val MidnightDeep get() = palette.backgroundDeep
    val Surface get() = palette.surface
    val SurfaceFocused get() = palette.surfaceFocused
    val SurfaceSelected get() = palette.surfaceSelected
    val Gold get() = palette.accent
    val GoldSoft get() = palette.accentSoft
    val Crimson get() = palette.alert
    val CrimsonSoft get() = palette.alertSoft
    val Amber get() = palette.amber
    val Text get() = palette.text
    val TextMuted get() = palette.textMuted
    val TextDim get() = palette.textDim

    val Background get() = Brush.verticalGradient(listOf(palette.background, palette.backgroundMid, palette.backgroundDeep))
}

/** TV-safe overscan: 5% of a 960x540dp canvas (1080p @ xhdpi). */
object TvDimens {
    val SafeHorizontal = 48.dp
    val SafeVertical = 27.dp
    val CardRadius = 16.dp
    val Gap = 14.dp
}

private val typography = Typography(
    displayMedium = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.Bold, lineHeight = 54.sp),
    headlineSmall = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
    labelMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp, letterSpacing = 0.2.sp),
)

@Composable
fun TamilCalendarTheme(content: @Composable () -> Unit) {
    val p = TvColors.palette
    val scheme = if (p.isLight) {
        lightColorScheme(
            primary = p.accent, onPrimary = p.background, secondary = p.alert,
            background = p.background, onBackground = p.text, surface = p.surface, onSurface = p.text, border = p.accent,
        )
    } else {
        darkColorScheme(
            primary = p.accent, onPrimary = p.background, secondary = p.alert,
            background = p.background, onBackground = p.text, surface = p.surface, onSurface = p.text, border = p.accent,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
