package com.vocabulary.app.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class TtsStatus { IDLE, INITIALIZING, READY, UNAVAILABLE }

/**
 * Thin wrapper around Android's TextToSpeech.
 * The engine is created lazily on the first [speak] call and reused afterwards.
 */
@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private var engine: TextToSpeech? = null
    private var pendingText: String? = null

    private val _status = MutableStateFlow(TtsStatus.IDLE)
    val status: StateFlow<TtsStatus> = _status.asStateFlow()

    fun speak(text: String) {
        if (text.isBlank()) return
        when (_status.value) {
            TtsStatus.READY -> speakNow(text)
            TtsStatus.INITIALIZING -> pendingText = text
            TtsStatus.IDLE, TtsStatus.UNAVAILABLE -> {
                pendingText = text
                initEngine()
            }
        }
    }

    fun stop() {
        engine?.stop()
    }

    private fun initEngine() {
        engine?.shutdown()
        engine = null
        _status.value = TtsStatus.INITIALIZING
        engine = TextToSpeech(context, this)
    }

    override fun onInit(initStatus: Int) {
        val tts = engine
        if (initStatus != TextToSpeech.SUCCESS || tts == null) {
            markUnavailable()
            return
        }
        val result = tts.setLanguage(Locale.US)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            markUnavailable()
            return
        }
        tts.setSpeechRate(0.9f)
        _status.value = TtsStatus.READY
        pendingText?.let { speakNow(it) }
        pendingText = null
    }

    private fun speakNow(text: String) {
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private fun markUnavailable() {
        engine?.shutdown()
        engine = null
        pendingText = null
        _status.value = TtsStatus.UNAVAILABLE
    }

    private companion object {
        const val UTTERANCE_ID = "vocabulary_utterance"
    }
}
