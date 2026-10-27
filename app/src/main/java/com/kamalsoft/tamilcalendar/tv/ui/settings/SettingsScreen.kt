package com.kamalsoft.tamilcalendar.tv.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.kamalsoft.tamilcalendar.tv.domain.model.AMBIENT_MINUTE_CHOICES
import com.kamalsoft.tamilcalendar.tv.domain.model.AppSettings
import com.kamalsoft.tamilcalendar.tv.domain.model.BellStyle
import com.kamalsoft.tamilcalendar.tv.domain.model.MusicMix
import com.kamalsoft.tamilcalendar.tv.domain.model.MusicPitch
import com.kamalsoft.tamilcalendar.tv.domain.model.MusicTempo
import com.kamalsoft.tamilcalendar.tv.domain.model.Raga
import com.kamalsoft.tamilcalendar.tv.domain.model.ConventionPref
import com.kamalsoft.tamilcalendar.tv.domain.model.Religion
import com.kamalsoft.tamilcalendar.tv.domain.model.UnitsPref
import com.kamalsoft.tamilcalendar.tv.ui.app.HIDEABLE_SECTIONS
import com.kamalsoft.tamilcalendar.tv.ui.app.tabEn
import com.kamalsoft.tamilcalendar.tv.ui.app.tabTa
import com.kamalsoft.tamilcalendar.tv.ui.components.Pill
import com.kamalsoft.tamilcalendar.tv.ui.components.Text
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors

private sealed interface Row {
    data class Header(val title: String) : Row
    data class Toggle(val title: String, val sub: String?, val on: Boolean, val flip: () -> Unit) : Row
    data class Choice(val title: String, val sub: String?, val value: String, val next: () -> Unit) : Row
    data class Action(val title: String, val sub: String?, val run: () -> Unit) : Row
}

private fun <T> List<T>.after(v: T): T = this[(indexOf(v) + 1) % size]

/**
 * Feature toggles. OK (or a tap) flips a switch / cycles a choice; every change applies immediately
 * and is saved. Calculation changes (units, Rahu Kalam convention) regenerate the offline year.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    autoLocation: Boolean,
    entryFocus: FocusRequester,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onAutoLocation: (Boolean) -> Unit,
    onShowDisclaimer: () -> Unit,
) {
    val faiths = listOf(Religion.CHRISTIAN, Religion.ISLAM, Religion.JAIN, Religion.SIKH, Religion.BUDDHIST, Religion.JEWISH)
    val rows = buildList {
        add(Row.Header("பிரிவுகள் · Sections"))
        HIDEABLE_SECTIONS.forEach { d ->
            add(Row.Toggle("${d.tabTa} · ${d.tabEn}", "Show this tab", d.name !in settings.hiddenSections) {
                onUpdate { s -> s.copy(hiddenSections = if (d.name in s.hiddenSections) s.hiddenSections - d.name else s.hiddenSections + d.name) }
            })
        }
        add(Row.Toggle("வானிலை · Weather", "Weather page below Today and the temperature on the date card", settings.weather) {
            onUpdate { it.copy(weather = !it.weather) }
        })

        add(Row.Header("மதங்கள் & விடுமுறைகள் · Faiths & holidays"))
        faiths.forEach { r ->
            add(Row.Toggle("${r.ta} · ${r.en}", "Observances on the month grid and Important Dates", r !in settings.hiddenFaiths) {
                onUpdate { s -> s.copy(hiddenFaiths = if (r in s.hiddenFaiths) s.hiddenFaiths - r else s.hiddenFaiths + r) }
            })
        }
        add(Row.Toggle("அமெரிக்க / இல்லினாய் விடுமுறைகள் · US & Illinois holidays", "Shown when the location is in the US", settings.usHolidays) {
            onUpdate { it.copy(usHolidays = !it.usHolidays) }
        })
        add(Row.Toggle("பள்ளி நாட்காட்டி · School calendars", "Naperville 203 and Indian Prairie 204", settings.schoolCalendars) {
            onUpdate { it.copy(schoolCalendars = !it.schoolCalendars) }
        })
        add(Row.Toggle("தமிழ்நாடு அரசு விடுமுறைகள் · Tamil Nadu govt. holidays", "Festivals are always shown", settings.tamilNaduHolidays) {
            onUpdate { it.copy(tamilNaduHolidays = !it.tamilNaduHolidays) }
        })

        add(Row.Header("காட்சி & இயக்கம் · Display & motion"))
        add(Row.Toggle("அசைவூட்டங்கள் · Animations", "Turn off on slow TVs to save power", settings.animations) {
            onUpdate { it.copy(animations = !it.animations) }
        })
        add(Row.Toggle("வானிலை அசைவூட்டம் · Weather animations", "Rain, snow, stars, seasonal leaves", settings.weatherAnimations) {
            onUpdate { it.copy(weatherAnimations = !it.weatherAnimations) }
        })
        add(Row.Toggle("24 மணி நேரம் · 24-hour clock", null, settings.clock24h) { onUpdate { it.copy(clock24h = !it.clock24h) } })
        add(Row.Toggle("தமிழ் மட்டும் · Tamil only", "Hide the English half of labels (weather stays English)", settings.tamilOnly) {
            onUpdate { it.copy(tamilOnly = !it.tamilOnly) }
        })
        add(Row.Choice("அலகுகள் · Units", "Weather temperature and wind", settings.units.en) {
            onUpdate { it.copy(units = UnitsPref.entries.after(it.units)) }
        })
        add(Row.Toggle("ஸ்லோக இசை · Sloka music", "Tanpura, flute and temple bell", settings.slokaMusic) {
            onUpdate { it.copy(slokaMusic = !it.slokaMusic) }
        })
        val m = settings.music
        add(Row.Choice("ராகம் · Raga", "Flute melody", "${m.raga.ta} · ${m.raga.en}") {
            onUpdate { it.copy(music = it.music.copy(raga = Raga.entries.after(it.music.raga))) }
        })
        add(Row.Choice("தாளம் · Tempo", null, "${m.tempo.ta} · ${m.tempo.en}") {
            onUpdate { it.copy(music = it.music.copy(tempo = MusicTempo.entries.after(it.music.tempo))) }
        })
        add(Row.Choice("மணி · Bell", "Temple bell spacing and loudness", m.bell.en) {
            onUpdate { it.copy(music = it.music.copy(bell = BellStyle.entries.after(it.music.bell))) }
        })
        add(Row.Choice("ஸ்ருதி · Pitch (Sa)", "Higher = lighter, less bass", m.pitch.label) {
            onUpdate { it.copy(music = it.music.copy(pitch = MusicPitch.entries.after(it.music.pitch))) }
        })
        add(Row.Choice("கலவை · Mix", "Tanpura vs flute", "${m.mix.ta} · ${m.mix.en}") {
            onUpdate { it.copy(music = it.music.copy(mix = MusicMix.entries.after(it.music.mix))) }
        })
        add(Row.Toggle("ஸ்லோக குரல் · Sloka voice", "Also read the verse aloud (text-to-speech)", settings.slokaVoice) {
            onUpdate { it.copy(slokaVoice = !it.slokaVoice) }
        })
        add(Row.Toggle("சூழல் திரை · Ambient mode", "Full-screen clock with Tithi, Nalla Neram and weather when idle", settings.ambient) {
            onUpdate { it.copy(ambient = !it.ambient) }
        })
        add(Row.Choice("சூழல் திரை நேரம் · Ambient after", "Minutes without using the remote", "${settings.ambientMinutes} min") {
            onUpdate { it.copy(ambientMinutes = AMBIENT_MINUTE_CHOICES.after(it.ambientMinutes)) }
        })

        add(Row.Header("நினைவூட்டல்கள் · Reminders"))
        add(Row.Toggle("ராகு காலம் · Rahu Kalam", "10 minutes before it starts", settings.rahuReminder) {
            onUpdate { it.copy(rahuReminder = !it.rahuReminder) }
        })
        add(Row.Toggle("நல்ல நேரம் · Nalla Neram", "When each good-time window begins", settings.nallaReminder) {
            onUpdate { it.copy(nallaReminder = !it.nallaReminder) }
        })

        add(Row.Header("இடம் & கணிப்பு · Location & calculation"))
        add(Row.Toggle("தானியங்கி இடம் · Auto location", "Follow where the TV is (permission or approximate from the internet)", autoLocation) {
            onAutoLocation(!autoLocation)
        })
        add(Row.Choice("ராகு காலம் முறை · Rahu Kalam convention", settings.convention.en, settings.convention.ta) {
            onUpdate { it.copy(convention = ConventionPref.entries.after(it.convention)) }
        })
        add(Row.Action("அறிவிப்பை மீண்டும் காட்டு · Show disclaimer again", "Also shown once more on next launch", onShowDisclaimer))
    }
    val firstControl = rows.indexOfFirst { it !is Row.Header }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("அமைப்புகள் · Settings", style = MaterialTheme.typography.headlineSmall, color = TvColors.Gold)
        Text("OK: மாற்று · Press OK to change. Changes apply immediately and are saved.", style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp, horizontal = 14.dp)) {
            items(rows.size) { i ->
                val req = if (i == firstControl) entryFocus else null
                when (val r = rows[i]) {
                    is Row.Header -> Text(r.title, style = MaterialTheme.typography.titleMedium, color = TvColors.Gold, modifier = Modifier.padding(top = 10.dp))
                    is Row.Toggle -> SettingCard(r.title, r.sub, req, r.flip) { Switch(r.on) }
                    is Row.Choice -> SettingCard(r.title, r.sub, req, r.next) { Pill("${r.value}  ⟳", TvColors.SurfaceSelected) }
                    is Row.Action -> SettingCard(r.title, r.sub, req, r.run) { Pill("OK", TvColors.SurfaceSelected) }
                }
            }
        }
    }
}

@Composable
private fun SettingCard(title: String, sub: String?, focusRequester: FocusRequester?, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    TvFocusCard(onClick = onClick, focusRequester = focusRequester, cornerRadius = 12.dp, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TvColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                sub?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = TvColors.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            trailing()
        }
    }
}

/** On/off switch drawn in theme colours (accent track when on). */
@Composable
private fun Switch(on: Boolean) {
    val track by animateColorAsState(if (on) TvColors.Gold else TvColors.TextDim.copy(alpha = 0.4f), label = "track")
    val x by animateDpAsState(if (on) 22.dp else 2.dp, label = "knob")
    Box(Modifier.size(width = 46.dp, height = 26.dp).background(track, RoundedCornerShape(50))) {
        Box(Modifier.offset(x = x, y = 3.dp).size(20.dp).background(TvColors.Midnight, CircleShape))
    }
}
