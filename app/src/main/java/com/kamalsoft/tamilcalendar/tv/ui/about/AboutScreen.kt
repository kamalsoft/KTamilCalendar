package com.kamalsoft.tamilcalendar.tv.ui.about

import com.kamalsoft.tamilcalendar.tv.ui.theme.DisplayPrefs
import com.kamalsoft.tamilcalendar.tv.ui.components.AdaptivePanes
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import com.kamalsoft.tamilcalendar.tv.ui.components.Text
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
        "சூத்திரங்கள் · Formulas",
        listOf(
            "Tithi = floor(Moon − Sun elongation ÷ 12°) + 1: 1–15 Shukla, 16–30 Krishna.",
            "Nakshatram = floor(Moon's sidereal longitude ÷ 13°20′) + 1, of 27.",
            "Yogam = floor(((Sun + Moon) mod 360°) ÷ 13°20′) + 1, of 27.",
            "Karanam = floor(elongation ÷ 6°) + 1, of 60 half-tithis.",
            "Sidereal longitude = tropical longitude − Lahiri ayanamsa (about 24° in 2026).",
            "Sunrise and sunset: the Sun's upper edge meets the horizon, 0.833° below it (refraction 34′ + semi-diameter 16′).",
            "Ayanam: Uttarayanam when the sidereal Sun is from 270° to 90° (Makaram to Mithunam), else Dakshinayanam.",
            "Tamil month starts on the day the Sun enters a sidereal sign; after sunset, the next day.",
            "Tamil day = (civil date − month start) + 1.",
        ),
    ),
    Section(
        "எடுத்துக்காட்டுகள் · Worked examples",
        listOf(
            "Moon 100° ahead of Sun → 100 ÷ 12 = 8.33 → index 8 → the 9th Shukla tithi (Navami).",
            "Moon at 50° sidereal → 50 ÷ 13.33 = 3.75 → index 3 → Rohini, the 4th nakshatram.",
            "Elongation 90° → (1 − cos 90°) ÷ 2 = 0.5 → the Moon is 50% lit.",
            "Sun at 280° sidereal → inside 270° to 90° → Uttarayanam.",
            "Sunrise 06:00, sunset 18:00 → 12 h day → each of the 8 parts is 1 h 30 min.",
            "Sunday Rahu Kalam is part 8 → 16:30 to 18:00.",
            "Abhijit: noon 12:00 ± (12 h ÷ 30 = 24 min) → 11:36 to 12:24.",
            "Aparahna Kala (sraardham): 4th fifth of the day → 13:12 to 15:36.",
            "Hora: 12 h ÷ 12 = 60 min each. The first Sunday hora is the Sun, 06:00 to 07:00, then Venus.",
            "Thara: day star and birth star 10 apart → count 11 → 11 mod 9 = 2 → Sampath (a good Thara).",
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
            "Accuracy: astronomy is checked against the Swiss Ephemeris. Traditional rules (Muhurtham, Lagna, Thara, Chandrashtamam, Sraardham) are calculated and not verified by a religious authority. Before a marriage, ceremony or Sraardham, confirm the date and time with a qualified temple priest.",
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


@Composable
fun AboutScreen(location: GeoLocation, entryFocus: FocusRequester) {
    val items = remember(location) { sections(location) }

    AdaptivePanes(if (DisplayPrefs.compact) 8.dp else 28.dp) {
        // Left: app identity and version.
        Column(
            Modifier.pane(0.85f, compactWeight = 1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(painterResource(R.drawable.gopuram), contentDescription = null, modifier = Modifier.size(64.dp))
            Text("பஞ்சாங்கம்", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
            Text("Panchangam · Tamil panchangam for your place", style = MaterialTheme.typography.bodyMedium, color = TvColors.TextMuted)
            Text("Panchangam, Rasi Palan and Muhurtham", style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim)
            Text(
                "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Android ${Build.VERSION.RELEASE}",
                style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim,
            )
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
