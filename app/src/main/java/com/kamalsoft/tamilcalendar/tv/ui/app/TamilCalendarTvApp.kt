package com.kamalsoft.tamilcalendar.tv.ui.app

import android.app.Activity
import android.os.SystemClock
import android.view.KeyEvent as AndroidKeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kamalsoft.tamilcalendar.tv.ui.components.DetailContent
import com.kamalsoft.tamilcalendar.tv.ui.components.DetailHost
import com.kamalsoft.tamilcalendar.tv.ui.components.rememberDetailHostState
import com.kamalsoft.tamilcalendar.tv.ui.daily.DailyScreen
import com.kamalsoft.tamilcalendar.tv.ui.daily.ErrorState
import com.kamalsoft.tamilcalendar.tv.ui.events.EventListScreen
import com.kamalsoft.tamilcalendar.tv.ui.events.ImportantDatesScreen
import com.kamalsoft.tamilcalendar.tv.ui.location.LocationScreen
import com.kamalsoft.tamilcalendar.tv.ui.monthly.MonthlyScreen
import com.kamalsoft.tamilcalendar.tv.ui.rasipalan.RasiPalanScreen
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvDimens
import kotlinx.coroutines.delay

private val HELP = DetailContent(
    title = "ரிமோட் பயன்பாடு · Remote help",
    subtitle = "D-pad, OK, BACK மட்டும் போதும்",
    lines = listOf(
        "▲ ▼ ◀ ▶" to "நகர்த்த · Move",
        "OK" to "தேர்வு / விவரங்கள் · Select / details",
        "◀ (இடது ஓரம்)" to "பக்க மெனு · Open the menu",
        "▼ (இன்றைய திரை)" to "கீழே வானிலை · Weather below the calendar",
        "BACK" to "மெனு → முகப்பு → இரண்டு முறை அழுத்தி வெளியேறு · Menu → Home → press twice to exit",
        "CH ▲▼ / 1–7" to "ரிமோட்டில் இருந்தால்: நாள் மாற்ற / பிரிவுகள் · If your remote has them",
    ),
)

/**
 * Root scaffold. Everything is reachable with D-pad + OK + BACK, because the standard Google TV
 * remote has no number, channel or menu keys; those keys are supported as shortcuts only.
 */
@Composable
fun TamilCalendarTvApp(vm: CalendarViewModel) {
    val destination by vm.destination.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val selectedDate by vm.selectedDate.collectAsStateWithLifecycle()
    val visibleMonth by vm.visibleMonth.collectAsStateWithLifecycle()
    val day by vm.day.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    val festivals by vm.festivals.collectAsStateWithLifecycle()
    val muhurthams by vm.muhurthams.collectAsStateWithLifecycle()
    val rasiPalan by vm.rasiPalan.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val location by vm.location.collectAsStateWithLifecycle()
    val imperial by vm.imperialUnits.collectAsStateWithLifecycle()
    val locationQuery by vm.locationQuery.collectAsStateWithLifecycle()
    val locationSearch by vm.locationSearch.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val contentFocus = remember { FocusRequester() }
    val drawerItems = remember { Destination.entries.associateWith { FocusRequester() } }
    val helpItem = remember { FocusRequester() }
    val overlay = rememberDetailHostState()
    var drawerHasFocus by remember { mutableStateOf(false) }
    // Incremented whenever focus should move into the content area once it is composed.
    var focusContentRequest by remember { mutableIntStateOf(1) }
    // Counts user key presses, so a pending focus request yields to real navigation but not to the
    // system's initial focus (which lands on the drawer before the content has loaded).
    var userKeyPresses by remember { mutableIntStateOf(0) }
    var lastBackAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(focusContentRequest, destination, status) {
        if (focusContentRequest == 0) return@LaunchedEffect
        // The target may not exist until Room emits (first launch seeds a whole year), so retry briefly.
        val pressesAtRequest = userKeyPresses
        repeat(100) {
            if (userKeyPresses != pressesAtRequest || overlay.content != null) return@LaunchedEffect
            if (runCatching { contentFocus.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(50)
        }
    }

    LaunchedEffect(status) {
        if (status == LoadStatus.READY && vm.consumeFirstLaunch()) overlay.show(HELP, contentFocus)
    }

    // Confirm a location change and put focus back on Today, which is being recalculated for the new place.
    var lastLocation by remember { mutableStateOf(location) }
    LaunchedEffect(location) {
        if (location != lastLocation) {
            lastLocation = location
            Toast.makeText(context, "இடம் மாற்றப்பட்டது · Location set: ${location.displayName}", Toast.LENGTH_LONG).show()
            focusContentRequest++
        }
    }

    fun go(to: Destination) {
        vm.navigate(to)
        focusContentRequest++
    }

    BackHandler(enabled = !drawerHasFocus) {
        runCatching { drawerItems.getValue(destination).requestFocus() }
    }
    BackHandler(enabled = drawerHasFocus && destination != Destination.DAILY) {
        go(Destination.DAILY)
    }
    // A single stray BACK shouldn't close a wall-calendar app: ask for a second press.
    BackHandler(enabled = drawerHasFocus && destination == Destination.DAILY) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBackAt < 2500) {
            (context as? Activity)?.finish()
        } else {
            lastBackAt = now
            Toast.makeText(context, "வெளியேற மீண்டும் BACK அழுத்தவும் · Press BACK again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                userKeyPresses++
                // Digits, page and channel keys are typed/used in the search box there; no shortcuts.
                if (destination == Destination.LOCATION) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_MENU, AndroidKeyEvent.KEYCODE_INFO -> {
                        overlay.show(HELP, contentFocus)
                        return@onPreviewKeyEvent true
                    }
                }
                val step = when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_CHANNEL_UP, AndroidKeyEvent.KEYCODE_PAGE_UP, AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> 1L
                    AndroidKeyEvent.KEYCODE_CHANNEL_DOWN, AndroidKeyEvent.KEYCODE_PAGE_DOWN, AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> -1L
                    else -> 0L
                }
                if (step != 0L) {
                    if (destination == Destination.MONTHLY || destination == Destination.IMPORTANT_DATES) vm.shiftMonth(step) else vm.shiftDay(step)
                    return@onPreviewKeyEvent true
                }
                val digit = event.nativeKeyEvent.keyCode - AndroidKeyEvent.KEYCODE_1
                if (digit in Destination.entries.indices) {
                    go(Destination.entries[digit])
                    return@onPreviewKeyEvent true
                }
                false
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    start = DrawerCollapsedWidth + TvDimens.Gap,
                    end = TvDimens.SafeHorizontal,
                    top = TvDimens.SafeVertical,
                    bottom = TvDimens.SafeVertical,
                ),
        ) {
            if (status == LoadStatus.ERROR) {
                ErrorState(focusRequester = contentFocus, onRetry = { vm.retry(); focusContentRequest++ })
            } else when (destination) {
                Destination.DAILY -> DailyScreen(
                    day = day,
                    today = today,
                    location = location,
                    weather = weather,
                    imperialUnits = imperial,
                    entryFocus = contentFocus,
                    onPreviousDay = { vm.shiftDay(-1) },
                    onNextDay = { vm.shiftDay(1) },
                    onToday = vm::goToday,
                    onOpenMonth = { go(Destination.MONTHLY) },
                )
                Destination.MONTHLY -> MonthlyScreen(
                    month = visibleMonth,
                    days = month,
                    today = today,
                    selected = selectedDate,
                    entryFocus = contentFocus,
                    onPreviousMonth = { vm.shiftMonth(-1) },
                    onNextMonth = { vm.shiftMonth(1) },
                    onToday = vm::goToday,
                    onOpenDate = { vm.openDate(it); focusContentRequest++ },
                )
                Destination.IMPORTANT_DATES -> ImportantDatesScreen(
                    month = visibleMonth,
                    days = month,
                    today = today,
                    entryFocus = contentFocus,
                    onPreviousMonth = { vm.shiftMonth(-1) },
                    onNextMonth = { vm.shiftMonth(1) },
                    onOpenDate = { vm.openDate(it); focusContentRequest++ },
                )
                Destination.FESTIVALS -> EventListScreen(
                    title = "பண்டிகைகள் & விடுமுறைகள் · Festivals & Holidays",
                    subtitle = "அடுத்த 12 மாதங்கள் · Next 12 months · சிவப்பு / Red = local holiday · மஞ்சள் / Amber = school",
                    events = festivals,
                    today = today,
                    entryFocus = contentFocus,
                    onOpenDate = { vm.openDate(it); focusContentRequest++ },
                )
                Destination.MUHURTHAM -> EventListScreen(
                    title = "சுப முகூர்த்த நாட்கள் · Subha Muhurtham",
                    subtitle = "பஞ்சாங்க விதிப்படி கணிக்கப்பட்டவை · Computed by panchangam rules — confirm with your priest",
                    events = muhurthams,
                    today = today,
                    entryFocus = contentFocus,
                    onOpenDate = { vm.openDate(it); focusContentRequest++ },
                )
                Destination.RASI_PALAN -> RasiPalanScreen(
                    date = selectedDate,
                    palan = rasiPalan,
                    entryFocus = contentFocus,
                    onPreviousDay = { vm.shiftDay(-1) },
                    onNextDay = { vm.shiftDay(1) },
                )
                Destination.LOCATION -> LocationScreen(
                    current = location,
                    query = locationQuery,
                    search = locationSearch,
                    entryFocus = contentFocus,
                    onQueryChange = vm::onLocationQueryChange,
                    onChoose = vm::chooseLocation,
                )
            }
        }
        if (drawerHasFocus) {
            Box(Modifier.fillMaxSize().background(TvColors.MidnightDeep.copy(alpha = 0.55f)))
        }
        NavDrawer(
            current = destination,
            expanded = drawerHasFocus,
            itemRequesters = drawerItems,
            helpRequester = helpItem,
            onFocusChanged = { drawerHasFocus = it },
            onSelect = ::go,
            onHelp = { overlay.show(HELP, helpItem) },
        )
        DetailHost(overlay)
    }
}
