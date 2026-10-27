package com.kamalsoft.tamilcalendar.tv.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Display settings read directly by low-level UI helpers (motion, time formatting, Text). Compose
 * state, so changing a value recomposes every reader. Set from the AppSettings flow in MainActivity.
 */
object DisplayPrefs {
    var animations by mutableStateOf(true)
    var weatherAnimations by mutableStateOf(true)
    var clock24h by mutableStateOf(false)
    var tamilOnly by mutableStateOf(false)

    /** Layout class chosen by DesignCanvas: false = TV / landscape tablet, true = phone (portrait). */
    var compact by mutableStateOf(false)
}
