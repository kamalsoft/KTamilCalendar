package com.kamalsoft.tamilcalendar.tv.ui.about

import com.kamalsoft.tamilcalendar.tv.ui.theme.DisplayPrefs
import com.kamalsoft.tamilcalendar.tv.ui.components.AdaptivePanes
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import com.kamalsoft.tamilcalendar.tv.ui.components.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.kamalsoft.tamilcalendar.tv.BuildConfig
import com.kamalsoft.tamilcalendar.tv.R
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors

private data class Section(val title: String, val lines: List<String>)

private fun sections(location: GeoLocation) = listOf(
    Section(
        "பஞ்சாங்கம் · Panchangam",
        listOf(
            "சாதனத்திலேயே கணிக்கப்படுகிறது · Computed on the TV from Sun/Moon positions (Meeus algorithms, Lahiri ayanamsa).",
            "Swiss Ephemeris-உடன் சரிபார்க்கப்பட்டது · Verified against the Swiss Ephemeris for 365 days: tithi, nakshatram, yogam and Tamil date 0 mismatches; end times within 1–2 minutes.",
            "இடம் · Location: ${location.displayName} (${location.coordinates}, ${location.zoneId}).",
        ),
    ),
    Section(
        "சிறப்பு நாட்கள் · Religious dates",
        listOf(
            "Hindu, Jain, Sikh, Buddhist: panchangam rules. Christian: Easter computus. Jewish: Hebrew calendar arithmetic.",
            "Islamic: astronomical (Umm al-Qura / FCNA style). Local moon sighting may differ by a day.",
            "Muhurtham days are rule-based; confirm with your priest.",
        ),
    ),
    Section(
        "தரவு மூலங்கள் · Data sources",
        listOf(
            "Weather & place search: Open-Meteo.com (NOAA models in the US). Approximate location: ipapi.co.",
            "US federal / Illinois holidays: computed from the legal rules.",
            "School calendars: Naperville 203 and Indian Prairie 204 official 2026–27 calendars.",
        ),
    ),
    Section(
        "அறிவிப்பு · Disclaimer",
        listOf(
            "For information only. The panchangam is calculated, not issued by a religious authority: confirm muhurtham and temple festival days with your priest or temple.",
            "Islamic dates are calculated and may differ by a day from local moon sighting. Weather is provided as-is, without warranty.",
            "Not affiliated with Apple, Google, the school districts, or any temple or government body.",
        ),
    ),
    Section(
        "தனியுரிமை · Privacy",
        listOf(
            "Location: with permission, the TV's city-level location; otherwise an approximate city from the internet connection (ipapi.co). Stored only on this TV.",
            "No account, no ads, no analytics. Network calls: Open-Meteo (weather, place search) and ipapi.co (approximate location).",
        ),
    ),
    Section(
        "ரிமோட் · Remote",
        listOf(
            "◀ ▶ on the top bar: switch sections · ▼ / OK: enter · ▲ from the top row: back to the tabs.",
            "BACK: content → tabs → Today → press twice to exit.  CH ▲▼: next / previous day or month.",
        ),
    ),
)

/** QR code bitmap of [text]; generated once, black on white for reliable scanning in any theme. */
private fun qrBitmap(text: String, size: Int = 360): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
    val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

@Composable
fun AboutScreen(location: GeoLocation, entryFocus: FocusRequester) {
    val qr = remember { runCatching { qrBitmap(BuildConfig.REPO_URL).asImageBitmap() }.getOrNull() }
    val items = remember(location) { sections(location) }

    AdaptivePanes(if (DisplayPrefs.compact) 8.dp else 28.dp) {
        // Left: identity + GitHub QR (TVs can't open links, so the phone does).
        Column(
            Modifier.pane(0.85f, compactWeight = 1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(painterResource(R.drawable.gopuram), contentDescription = null, modifier = Modifier.size(64.dp))
            Text("தமிழ் நாள்காட்டி", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
            Text("Tamil Calendar for Google TV", style = MaterialTheme.typography.bodyMedium, color = TvColors.TextMuted)
            Text(
                "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Android ${Build.VERSION.RELEASE}",
                style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim,
            )
            qr?.let {
                Image(
                    it, contentDescription = "QR code: ${BuildConfig.REPO_URL}", filterQuality = FilterQuality.None,
                    modifier = Modifier.padding(top = 14.dp).size(150.dp)
                        .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(12.dp)).padding(6.dp),
                )
            }
            Text("GitHub", style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim, modifier = Modifier.padding(top = 8.dp))
            Text(BuildConfig.REPO_URL.removePrefix("https://"), style = MaterialTheme.typography.bodyMedium, color = TvColors.Gold, fontWeight = FontWeight.SemiBold)
            Text("கைபேசியில் ஸ்கேன் செய்யவும் · Scan with your phone", style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim)
        }
        // Right: focusable info cards (D-pad scrolls through them).
        LazyColumn(
            Modifier.pane(1.4f, compactWeight = 1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
        ) {
            itemsIndexed(items) { i, section ->
                TvFocusCard(onClick = {}, focusRequester = if (i == 0) entryFocus else null, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleMedium, color = TvColors.Gold)
                        section.lines.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted) }
                    }
                }
            }
        }
    }
}
