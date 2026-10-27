package com.kamalsoft.tamilcalendar.tv.ui.location

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kamalsoft.tamilcalendar.tv.R
import com.kamalsoft.tamilcalendar.tv.domain.model.GeoLocation
import com.kamalsoft.tamilcalendar.tv.ui.app.LocationSearch
import com.kamalsoft.tamilcalendar.tv.ui.components.Pill
import com.kamalsoft.tamilcalendar.tv.ui.components.TvFocusCard
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors

/**
 * Type a city name, pick a result; every calculation then switches to that place.
 *
 * The text box behaves like a button on TV: D-pad focus lands on the box without opening the
 * keyboard; OK starts typing; "Search" on the keyboard (or BACK) closes it and ▼ reaches the results.
 */
@Composable
fun LocationScreen(
    current: GeoLocation,
    query: String,
    search: LocationSearch,
    entryFocus: FocusRequester,
    onQueryChange: (String) -> Unit,
    onChoose: (GeoLocation) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    val firstResult = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(editing) {
        if (editing) {
            runCatching { field.requestFocus() }
            keyboard?.show()
        }
    }

    fun finishTyping() {
        keyboard?.hide()
        editing = false
        runCatching { entryFocus.requestFocus() }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("இடம் · Location", style = MaterialTheme.typography.headlineSmall, color = TvColors.Gold)
        Text(
            "தற்போது · Current: ${current.displayName} · ${current.coordinates} · ${current.zoneId}",
            style = MaterialTheme.typography.bodySmall, color = TvColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )

        TvFocusCard(
            onClick = { editing = true },
            focusRequester = entryFocus,
            selected = editing,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.ic_search), contentDescription = null,
                    colorFilter = ColorFilter.tint(TvColors.Gold), modifier = Modifier.size(26.dp),
                )
                Box(Modifier.weight(1f).padding(start = 16.dp)) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(color = TvColors.Text),
                        cursorBrush = SolidColor(TvColors.Gold),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { finishTyping() }, onDone = { finishTyping() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(field)
                            // Only focusable while typing, so passing over the box never pops the keyboard.
                            .focusProperties { canFocus = editing }
                            .onFocusChanged { if (!it.isFocused && editing) editing = false }
                            // Single-line fields swallow ▲/▼; hand them back to focus navigation.
                            .onPreviewKeyEvent { e ->
                                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (e.nativeKeyEvent.keyCode) {
                                    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                        keyboard?.hide()
                                        editing = false
                                        if (search is LocationSearch.Results && search.places.isNotEmpty()) runCatching { firstResult.requestFocus() }
                                        else focusManager.moveFocus(FocusDirection.Down)
                                        true
                                    }
                                    AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                        finishTyping()
                                        true
                                    }
                                    else -> false
                                }
                            },
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    if (editing) "நகரத்தின் பெயர் · Type a city name…" else "OK அழுத்தி தேடவும் · Press OK to search for a city",
                                    style = MaterialTheme.typography.titleMedium, color = TvColors.TextDim,
                                )
                            }
                            inner()
                        },
                    )
                }
                if (editing) Pill("Search ⏎", TvColors.SurfaceSelected)
            }
        }

        val (message, places) = when (search) {
            LocationSearch.Idle -> "பரிந்துரைகள் · Suggestions" to listOf(GeoLocation.NAPERVILLE, GeoLocation.CHENNAI).filter { it != current }
            LocationSearch.Searching -> "தேடுகிறது… · Searching…" to emptyList()
            LocationSearch.Failed -> "தேட முடியவில்லை — இணைய இணைப்பைச் சரிபார்க்கவும் · Search failed — check the internet connection" to emptyList()
            is LocationSearch.Results ->
                (if (search.places.isEmpty()) "எதுவும் கிடைக்கவில்லை · No places found" else "முடிவுகள் · Results (${search.places.size})") to search.places
        }
        Text(message, style = MaterialTheme.typography.labelMedium, color = TvColors.TextMuted)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
        ) {
            itemsIndexed(places, key = { _, p -> "${p.latitude},${p.longitude},${p.nameEn}" }) { index, place ->
                TvFocusCard(
                    onClick = { onChoose(place) },
                    focusRequester = if (index == 0) firstResult else null,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painterResource(R.drawable.ic_nav_location), contentDescription = null,
                            colorFilter = ColorFilter.tint(TvColors.Gold), modifier = Modifier.size(24.dp),
                        )
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(
                                if (place.nameTa != place.nameEn) "${place.nameTa} · ${place.displayName}" else place.displayName,
                                style = MaterialTheme.typography.titleMedium, color = TvColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text("${place.coordinates} · ${place.zoneId}", style = MaterialTheme.typography.bodySmall, color = TvColors.TextDim, maxLines = 1)
                        }
                        Pill("OK: தேர்வு · Select", TvColors.SurfaceSelected)
                    }
                }
            }
        }
    }
}
