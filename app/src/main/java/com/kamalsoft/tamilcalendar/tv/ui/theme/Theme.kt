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

/** Temple palette, tuned for high contrast on budget LED panels (no mid-grey text on dark). */
object TvColors {
    val Midnight = Color(0xFF0F0E17)
    val MidnightDeep = Color(0xFF08070D)
    val Surface = Color(0xFF1B1A2A)
    val SurfaceFocused = Color(0xFF2B2842)
    val SurfaceSelected = Color(0xFF231F38)
    val Gold = Color(0xFFFFD700)
    val GoldSoft = Color(0xFFE8C44A)
    val Crimson = Color(0xFFDC143C)
    val CrimsonSoft = Color(0xFFFF4D6D)
    val Amber = Color(0xFFFFB020)
    val Text = Color(0xFFFFFFFE)
    val TextMuted = Color(0xFFC3C4D6)
    val TextDim = Color(0xFF8C8DA3)

    val Background = Brush.verticalGradient(listOf(Midnight, Color(0xFF17132B), MidnightDeep))
}

/** TV-safe overscan: 5% of a 960x540dp canvas (1080p @ xhdpi). */
object TvDimens {
    val SafeHorizontal = 48.dp
    val SafeVertical = 27.dp
    val CardRadius = 16.dp
    val Gap = 14.dp
}

private val typography = Typography(
    displayMedium = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 52.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
    labelMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp, letterSpacing = 0.2.sp),
)

@Composable
fun TamilCalendarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = TvColors.Gold,
            onPrimary = TvColors.Midnight,
            secondary = TvColors.Crimson,
            background = TvColors.Midnight,
            surface = TvColors.Surface,
            onSurface = TvColors.Text,
            onBackground = TvColors.Text,
            border = TvColors.Gold,
        ),
        typography = typography,
        content = content,
    )
}
