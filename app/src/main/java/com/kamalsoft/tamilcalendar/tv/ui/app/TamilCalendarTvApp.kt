package com.kamalsoft.tamilcalendar.tv.ui.app

import android.app.Activity
import android.os.SystemClock
import android.view.KeyEvent as AndroidKeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.kamalsoft.tamilcalendar.tv.ui.components.Text
import androidx.tv.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import com.kamalsoft.tamilcalendar.tv.ui.ambient.NoWeather
import com.kamalsoft.tamilcalendar.tv.ui.ambient.AmbientScreen
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.kamalsoft.tamilcalendar.tv.ui.theme.ambientBackground
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kamalsoft.tamilcalendar.tv.ui.components.DetailContent
import com.kamalsoft.tamilcalendar.tv.ui.components.DetailHost
import com.kamalsoft.tamilcalendar.tv.ui.components.rememberDetailHostState
import com.kamalsoft.tamilcalendar.tv.ui.about.AboutScreen
import com.kamalsoft.tamilcalendar.tv.ui.daily.DailyScreen
import com.kamalsoft.tamilcalendar.tv.ui.daily.ErrorState
import com.kamalsoft.tamilcalendar.tv.ui.events.EventListScreen
import com.kamalsoft.tamilcalendar.tv.ui.events.ImportantDatesScreen
import com.kamalsoft.tamilcalendar.tv.ui.location.LocationScreen
import com.kamalsoft.tamilcalendar.tv.ui.monthly.MonthlyScreen
import com.kamalsoft.tamilcalendar.tv.ui.rasipalan.RasiPalanScreen
import com.kamalsoft.tamilcalendar.tv.ui.settings.SettingsScreen
import com.kamalsoft.tamilcalendar.tv.ui.settings.ThemeScreen
import com.kamalsoft.tamilcalendar.tv.ui.theme.DisplayPrefs
import com.kamalsoft.tamilcalendar.tv.ui.theme.AppTheme
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvDimens
import kotlinx.coroutines.delay

/** Shown once on first launch (and kept in About). The navigation hint matches the device. */
private val DISCLAIMER get() = DetailContent(
    title = "அறிவிப்பு · Disclaimer",
    subtitle = "தகவலுக்காக மட்டும் · For information only",
    lines = listOf(
        "பஞ்சாங்கம் · Panchangam" to "Calculated on this TV from astronomy; not a religious authority. Confirm muhurtham and temple festival days with your priest or temple.",
        "பிற மதங்கள் · Other faiths" to "Islamic dates are calculated and may differ by a day from local moon sighting.",
        "இடம் · Location" to "With your permission the TV's location is used; otherwise an approximate city from your internet connection (ipapi.co). It is stored only on this TV.",
        "வானிலை · Weather" to "From Open-Meteo, provided as-is without warranty.",
        if (DisplayPrefs.compact) {
            "வழிசெலுத்தல் · Navigation" to "Bottom bar for sections; ☰ More for Location, Theme, Settings and About."
        } else {
            "ரிமோட் · Remote" to "▲ for the top tabs · OK to select · BACK to go back. More under About (ⓘ)."
        },
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
    val locationStatus by vm.locationStatus.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val speaker by vm.speaker.collectAsStateWithLifecycle()
    val todayData by vm.todayData.collectAsStateWithLifecycle()
    val banner by vm.banner.collectAsStateWithLifecycle()
    val rasiSky by vm.rasiSky.collectAsStateWithLifecycle()
    val rasiForecasts by vm.rasiForecasts.collectAsStateWithLifecycle()
    val visibleTabs = Destination.entries.filter { it.name !in settings.hiddenSections }
    // A section hidden in Settings while open: fall back to Today.
    LaunchedEffect(destination, visibleTabs) { if (destination !in visibleTabs) vm.navigate(Destination.DAILY) }

    val context = LocalContext.current
    val contentFocus = remember { FocusRequester() }
    val tabs = remember { Destination.entries.associateWith { FocusRequester() } }
    val overlay = rememberDetailHostState()
    var tabBarHasFocus by remember { mutableStateOf(false) }
    // Counts user key presses (KeyDown), so a pending focus request yields to real navigation.
    var userKeyPresses by remember { mutableIntStateOf(0) }
    var lastBackAt by remember { mutableLongStateOf(0L) }
    var moreOpen by remember { mutableStateOf(false) }

    // Ambient mode: shown after N idle minutes; any key or touch wakes the app (that input is swallowed).
    var lastInput by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var ambient by remember { mutableStateOf(false) }
    LaunchedEffect(lastInput, settings.ambient, settings.ambientMinutes, status) {
        ambient = false
        if (!settings.ambient || status != LoadStatus.READY) return@LaunchedEffect
        delay(settings.ambientMinutes * 60_000L)
        if (!speaker.speakingId.isNullOrEmpty()) return@LaunchedEffect // don't interrupt a sloka
        ambient = true
    }
    // TV overscan margins don't apply to phones.
    val sidePad = if (DisplayPrefs.compact) 12.dp else TvDimens.SafeHorizontal

    // One-shot "move focus into the page" request. Made on launch, OK on a tab, opening a date, or a
    // location change, but NEVER by merely focusing a tab (tvOS tabs switch on focus, and focus must stay
    // on the tab bar so the user can keep moving along it).
    var focusRequestId by remember { mutableIntStateOf(0) }
    var pendingFocusAtPresses by remember { mutableStateOf<Int?>(0) } // launch: pending from the start
    fun focusContent() {
        pendingFocusAtPresses = userKeyPresses
        focusRequestId++
    }

    // Touch devices (phones/tablets) don't show a focus cursor, so programmatic focus is skipped there.
    val inputMode = LocalInputModeManager.current
    val touch = inputMode.inputMode == InputMode.Touch

    LaunchedEffect(focusRequestId, destination, status) {
        if (touch) return@LaunchedEffect
        val requestedAt = pendingFocusAtPresses ?: return@LaunchedEffect
        // The target may not exist until Room emits (first launch seeds a whole year), so retry briefly.
        repeat(100) {
            // Any key pressed after the request means the user is navigating: drop the request.
            if (userKeyPresses != requestedAt || overlay.content != null) {
                pendingFocusAtPresses = null
                return@LaunchedEffect
            }
            if (runCatching { contentFocus.requestFocus() }.isSuccess) {
                pendingFocusAtPresses = null
                return@LaunchedEffect
            }
            delay(50)
        }
        pendingFocusAtPresses = null
    }

    LaunchedEffect(status) {
        if (status == LoadStatus.READY && vm.consumeFirstLaunch()) overlay.show(DISCLAIMER, contentFocus)
    }

    // Confirm a location change and put focus back on Today, which is being recalculated for the new place.
    var lastLocation by remember { mutableStateOf(location) }
    LaunchedEffect(location) {
        if (location != lastLocation) {
            lastLocation = location
            Toast.makeText(context, "இடம் மாற்றப்பட்டது · Location set: ${location.displayName}", Toast.LENGTH_LONG).show()
            focusContent()
        }
    }

    fun go(to: Destination) {
        vm.navigate(to)
        focusContent()
    }

    // BACK, like tvOS: content → current tab → Today tab → (twice) exit.
    // Touch: BACK goes to Today, then (twice) exits; there is no tab-bar focus to move to.
    BackHandler(enabled = touch && destination != Destination.DAILY) { vm.navigate(Destination.DAILY) }
    BackHandler(enabled = !touch && !tabBarHasFocus) {
        runCatching { tabs.getValue(destination).requestFocus() }
    }
    BackHandler(enabled = tabBarHasFocus && destination != Destination.DAILY) {
        vm.navigate(Destination.DAILY)
        runCatching { tabs.getValue(Destination.DAILY).requestFocus() }
    }
    // A single stray BACK shouldn't close a wall-calendar app: ask for a second press.
    BackHandler(enabled = (tabBarHasFocus || touch) && destination == Destination.DAILY) {
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
            .ambientBackground()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastInput = SystemClock.elapsedRealtime()
                    }
                }
            }
            .onPreviewKeyEvent { event ->
                lastInput = SystemClock.elapsedRealtime()
                if (ambient) return@onPreviewKeyEvent true // wake only
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                userKeyPresses++
                // Digits, page and channel keys are typed/used in the search box there; no shortcuts.
                if (destination == Destination.LOCATION) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_MENU, AndroidKeyEvent.KEYCODE_INFO -> {
                        go(Destination.ABOUT)
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
                if (digit in visibleTabs.indices) {
                    go(visibleTabs[digit])
                    return@onPreviewKeyEvent true
                }
                false
            },
    ) {
        if (DisplayPrefs.compact) CompactTopBar(destination, Modifier.padding(top = 4.dp)) else TopTabBar(
            current = destination,
            visible = visibleTabs,
            tabRequesters = tabs,
            onFocusChanged = { tabBarHasFocus = it },
            onTabFocused = { vm.navigate(it) },
            onEnterContent = { focusContent() },
            modifier = Modifier.padding(start = sidePad, end = sidePad, top = 16.dp),
        )
        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    start = sidePad,
                    end = sidePad,
                    top = if (DisplayPrefs.compact) 4.dp + COMPACT_TOP_BAR_HEIGHT else 16.dp + 48.dp + 10.dp,
                    bottom = if (DisplayPrefs.compact) COMPACT_BOTTOM_BAR_HEIGHT + 4.dp else TvDimens.SafeVertical,
                ),
        ) {
            // Fade-and-rise on page change. Only the new page is composed (no cross-fade), so focus
            // targets are never duplicated during the animation.
            val enter = remember(destination) { Animatable(0f) }
            LaunchedEffect(destination) { if (DisplayPrefs.animations) enter.animateTo(1f, tween(260)) else enter.snapTo(1f) }
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = enter.value; translationY = (1f - enter.value) * 14.dp.toPx() }) {
                if (status == LoadStatus.ERROR) {
                    ErrorState(focusRequester = contentFocus, onRetry = { vm.retry(); focusContent() })
                } else when (destination) {
                    Destination.DAILY -> DailyScreen(
                        day = day,
                        today = today,
                        location = location,
                        weather = weather,
                        weatherEnabled = settings.weather,
                        imperialUnits = imperial,
                        entryFocus = contentFocus,
                        onPreviousDay = { vm.shiftDay(-1) },
                        onNextDay = { vm.shiftDay(1) },
                        onToday = vm::goToday,
                        onOpenMonth = { go(Destination.MONTHLY) },
                        speaker = speaker,
                        slokaVoice = settings.slokaVoice,
                        onToggleSpeech = vm::toggleSpeech,
                        onStopSpeech = vm::stopSpeech,
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
                        onOpenDate = { vm.openDate(it); focusContent() },
                    )
                    Destination.IMPORTANT_DATES -> ImportantDatesScreen(
                        month = visibleMonth,
                        days = month,
                        today = today,
                        entryFocus = contentFocus,
                        onPreviousMonth = { vm.shiftMonth(-1) },
                        onNextMonth = { vm.shiftMonth(1) },
                        onOpenDate = { vm.openDate(it); focusContent() },
                    )
                    Destination.FESTIVALS -> EventListScreen(
                        title = "பண்டிகைகள் & விடுமுறைகள் · Festivals & Holidays",
                        subtitle = "அடுத்த 12 மாதங்கள் · Next 12 months · சிவப்பு / Red = local holiday · மஞ்சள் / Amber = school",
                        events = festivals,
                        today = today,
                        entryFocus = contentFocus,
                        onOpenDate = { vm.openDate(it); focusContent() },
                    )
                    Destination.MUHURTHAM -> EventListScreen(
                        title = "சுப முகூர்த்த நாட்கள் · Subha Muhurtham",
                        subtitle = "பஞ்சாங்க விதிப்படி கணிக்கப்பட்டவை · Computed by panchangam rules — confirm with your priest",
                        events = muhurthams,
                        today = today,
                        entryFocus = contentFocus,
                        onOpenDate = { vm.openDate(it); focusContent() },
                    )
                    Destination.RASI_PALAN -> RasiPalanScreen(
                        date = selectedDate,
                        palan = rasiPalan,
                        entryFocus = contentFocus,
                        onPreviousDay = { vm.shiftDay(-1) },
                        onNextDay = { vm.shiftDay(1) },
                        sky = rasiSky,
                        forecasts = rasiForecasts,
                        speaker = speaker,
                        onToggleSpeech = vm::toggleSpeech,
                        onStopSpeech = vm::stopSpeech,
                    )
                    Destination.THEME -> ThemeScreen(
                        current = AppTheme.entries.firstOrNull { it.palette == TvColors.palette } ?: AppTheme.GRAPHITE,
                        entryFocus = contentFocus,
                        onChoose = { theme ->
                            TvColors.palette = theme.palette
                            vm.saveTheme(theme.name)
                        },
                    )
                    Destination.SETTINGS -> SettingsScreen(
                        settings = settings,
                        autoLocation = vm.autoLocation,
                        entryFocus = contentFocus,
                        onUpdate = vm::updateSettings,
                        onAutoLocation = vm::setAutoLocation,
                        onShowDisclaimer = { vm.resetFirstLaunch(); overlay.show(DISCLAIMER, contentFocus) },
                    )
                    Destination.ABOUT -> AboutScreen(location = location, entryFocus = contentFocus)
                    Destination.LOCATION -> LocationScreen(
                        current = location,
                        query = locationQuery,
                        search = locationSearch,
                        entryFocus = contentFocus,
                        autoDetect = vm.autoLocation,
                        detectStatus = locationStatus,
                        onUseMyLocation = vm::useMyLocation,
                        onQueryChange = vm::onLocationQueryChange,
                        onChoose = vm::chooseLocation,
                        recent = vm.recentLocations(),
                    )
                }
            }
        }
        if (DisplayPrefs.compact) {
            if (moreOpen) {
                CompactMoreSheet(destination, visibleTabs, onSelect = { moreOpen = false; vm.navigate(it) }, onDismiss = { moreOpen = false })
            }
            CompactBottomBar(
                current = destination, visible = visibleTabs, moreOpen = moreOpen,
                onSelect = { moreOpen = false; vm.navigate(it) }, onMore = { moreOpen = !moreOpen },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        DetailHost(overlay)
        // Reminder banner (Rahu Kalam soon / Nalla Neram begun) while the app is open.
        AnimatedVisibility(
            visible = banner != null && !ambient,
            enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
        ) {
            Text(
                "🔔 " + (banner ?: ""), style = MaterialTheme.typography.titleMedium, color = TvColors.Midnight,
                modifier = Modifier.background(TvColors.Gold, RoundedCornerShape(50)).padding(horizontal = 22.dp, vertical = 10.dp),
            )
        }
        if (ambient) {
            AmbientScreen(
                day = todayData, weather = if (settings.weather) weather else NoWeather, location = location,
                modifier = Modifier.pointerInput(Unit) { detectTapGestures { lastInput = SystemClock.elapsedRealtime() } },
            )
        }
    }
}
