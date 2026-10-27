package com.kamalsoft.tamilcalendar.tv.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Text-to-speech for slokas and Rasi Palan read-aloud. Tamil voice when the device has one,
 * otherwise English (callers pass both texts). Created lazily: TV boxes pay nothing until first use.
 */
class Speaker(context: Context) {
    data class State(
        /** Id of what is being spoken, null when idle. */
        val speakingId: String? = null,
        /** Null until the engine has initialised. */
        val tamilVoice: Boolean? = null,
        val unavailable: Boolean = false,
    )

    private val app = context.applicationContext
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: (() -> Unit)? = null
    private var lastUtterance: String? = null

    private val tamil = Locale("ta", "IN")
    private val ambience = DevotionalAmbience()

    private fun withEngine(action: () -> Unit) {
        if (ready) return action()
        pending = action
        if (tts != null) return
        tts = TextToSpeech(app) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.w("Speaker", "TTS init failed: $status")
                _state.value = State(unavailable = true)
                return@TextToSpeech
            }
            val engine = tts ?: return@TextToSpeech
            val hasTamil = engine.isLanguageAvailable(tamil) >= TextToSpeech.LANG_AVAILABLE
            engine.setSpeechRate(0.85f)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) {
                    if (id == lastUtterance) {
                        _state.value = _state.value.copy(speakingId = null)
                        ambience.stop()
                    }
                }
                @Deprecated("Required override") override fun onError(id: String?) {
                    _state.value = _state.value.copy(speakingId = null)
                    ambience.stop()
                }
            })
            ready = true
            _state.value = State(tamilVoice = hasTamil)
            pending?.invoke()
            pending = null
        }
    }

    /**
     * Speaks [ta] (or [en] without a Tamil voice) [repeat] times, optionally over the synthesised
     * devotional background ([withMusic]). A second call with the same id stops.
     */
    fun toggle(
        id: String, ta: String, en: String, repeat: Int = 1, withMusic: Boolean = false,
        style: com.kamalsoft.tamilcalendar.tv.domain.model.MusicStyle = com.kamalsoft.tamilcalendar.tv.domain.model.MusicStyle(),
    ) {
        if (_state.value.speakingId == id) return stop()
        withEngine {
            if (withMusic) { ambience.loud = false; ambience.style = style; ambience.start() }
            val engine = tts ?: return@withEngine
            val useTamil = _state.value.tamilVoice == true
            engine.language = if (useTamil) tamil else Locale.US
            val text = if (useTamil) ta else en
            repeat(repeat) { i ->
                val uid = "$id#$i"
                lastUtterance = uid
                engine.speak(text, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, uid)
                engine.playSilentUtterance(900, TextToSpeech.QUEUE_ADD, "$id#gap$i")
            }
            _state.value = _state.value.copy(speakingId = id)
        }
    }

    /** Music only (no voice): plays the devotional ambience until [stop]. A second call with the same id stops. */
    fun toggleMusic(id: String, style: com.kamalsoft.tamilcalendar.tv.domain.model.MusicStyle = com.kamalsoft.tamilcalendar.tv.domain.model.MusicStyle()) {
        if (_state.value.speakingId == id) return stop()
        tts?.stop()
        ambience.loud = true
        ambience.style = style
        ambience.start()
        _state.value = _state.value.copy(speakingId = id)
    }

    fun stop() {
        tts?.stop()
        ambience.stop()
        _state.value = _state.value.copy(speakingId = null)
    }
}
