package com.deafcall.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManager @Inject constructor(
    private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Idle)
    val ttsState: StateFlow<TtsState> = _ttsState

    sealed class TtsState {
        object Idle : TtsState()
        data class Speaking(val utteranceId: String) : TtsState()
        object Error : TtsState()
    }

    init {
        initialize()
    }

    private fun initialize() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                tts?.language = Locale("ru", "RU")
                setupProgressListener()
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
                _ttsState.value = TtsState.Error
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                _ttsState.value = TtsState.Speaking(utteranceId ?: "")
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                _ttsState.value = TtsState.Idle
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _ttsState.value = TtsState.Error
            }
        })
    }

    /**
     * Произнести текст вслух (для собеседника через динамик)
     */
    fun speak(
        text: String,
        speed: Float = 1.0f,
        pitch: Float = 1.0f
    ): String {
        if (!isReady || tts == null) {
            Log.w(TAG, "TTS not ready")
            return ""
        }

        val utteranceId = UUID.randomUUID().toString()
        tts?.setSpeechRate(speed.coerceIn(0.1f, 3.0f))
        tts?.setPitch(pitch.coerceIn(0.1f, 2.0f))

        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        )

        return utteranceId
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _ttsState.value = TtsState.Idle
    }

    fun setLanguage(locale: Locale) {
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language not supported: ${locale.language}")
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady = false
    }

    companion object {
        private const val TAG = "TtsManager"
    }
}
