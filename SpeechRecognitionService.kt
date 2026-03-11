package com.deafcall.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.deafcall.DeafCallApp
import com.deafcall.R
import com.deafcall.model.SpeakerType
import com.deafcall.model.TranscriptEntry
import com.deafcall.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@AndroidEntryPoint
class SpeechRecognitionService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Публичные потоки — ViewModel подписывается на них
    private val _transcriptFlow = MutableSharedFlow<TranscriptEntry>(extraBufferCapacity = 64)
    val transcriptFlow: SharedFlow<TranscriptEntry> = _transcriptFlow

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _sttError = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val sttError: SharedFlow<String> = _sttError

    private var currentLanguage = "ru-RU"
    private var isActiveCall = false
    private var restartCount = 0
    private val MAX_RESTARTS = 100 // непрерывно на протяжении всего звонка

    inner class SttBinder : Binder() {
        fun getService(): SpeechRecognitionService = this@SpeechRecognitionService
    }

    private val binder = SttBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> {
                currentLanguage = intent.getStringExtra(EXTRA_LANGUAGE) ?: "ru-RU"
                isActiveCall = true
                restartCount = 0
                // SpeechRecognizer ДОЛЖЕН создаваться в Main thread
                mainHandler.post { initAndStart() }
            }
            ACTION_STOP_LISTENING -> {
                isActiveCall = false
                mainHandler.post { stopRecognizer() }
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun initAndStart() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "SpeechRecognizer NOT available!")
            return
        }
        stopRecognizer() // сначала останавливаем старый если был
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(createListener())
        startListeningInternal()
        Log.d(TAG, "STT initialized and started")
    }

    private fun startListeningInternal() {
        if (!isActiveCall) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Минимальная пауза чтобы не прерывать речь
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
            Log.d(TAG, "STT startListening (restart #$restartCount)")
        } catch (e: Exception) {
            Log.e(TAG, "startListening failed: ${e.message}")
            scheduleRestart(1000)
        }
    }

    private fun createListener() = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            Log.d(TAG, "onReadyForSpeech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech detected!")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // громкость звука — можно использовать для визуализации
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
        }

        /**
         * PARTIAL RESULTS — текст появляется МГНОВЕННО пока человек говорит
         * Это ключевой метод для real-time транскрипции!
         */
        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                ?: return

            if (text.isEmpty()) return
            Log.d(TAG, "PARTIAL: $text")

            _transcriptFlow.tryEmit(
                TranscriptEntry(text = text, isPartial = true, speakerType = SpeakerType.CALLER)
            )
        }

        /**
         * FINAL RESULT — финальный точный текст
         */
        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                ?: return

            if (text.isEmpty()) return
            Log.d(TAG, "FINAL: $text")

            _transcriptFlow.tryEmit(
                TranscriptEntry(text = text, isPartial = false, speakerType = SpeakerType.CALLER)
            )

            // Сразу перезапускаем для непрерывного прослушивания
            if (isActiveCall && restartCount < MAX_RESTARTS) {
                restartCount++
                mainHandler.postDelayed({ startListeningInternal() }, 100)
            }
        }

        override fun onError(error: Int) {
            val msg = errorMessage(error)
            Log.w(TAG, "STT Error: $msg (code=$error)")
            _isListening.value = false

            if (!isActiveCall) return

            // Для большинства ошибок — перезапускаем с задержкой
            val delay = when (error) {
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1500L
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    _sttError.tryEmit("Нет разрешения на микрофон!")
                    return // не перезапускаем
                }
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 300L
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    _sttError.tryEmit("Нет интернета для STT. Проверьте подключение.")
                    1000L
                }
                else -> 500L
            }

            scheduleRestart(delay)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!isActiveCall) return
        mainHandler.postDelayed({
            if (isActiveCall) {
                // Пересоздаём recognizer при ошибке
                stopRecognizer()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                speechRecognizer?.setRecognitionListener(createListener())
                startListeningInternal()
            }
        }, delayMs)
    }

    private fun stopRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognizer: ${e.message}")
        }
        speechRecognizer = null
        _isListening.value = false
    }

    fun startListening() {
        isActiveCall = true
        mainHandler.post { startListeningInternal() }
    }

    fun stopListening() {
        isActiveCall = false
        mainHandler.post { stopRecognizer() }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, DeafCallApp.CHANNEL_STT)
            .setContentTitle("DeafCall — STT активен")
            .setContentText("🎙️ Распознавание речи работает")
            .setSmallIcon(R.drawable.ic_hearing)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    private fun errorMessage(code: Int) = when (code) {
        SpeechRecognizer.ERROR_AUDIO              -> "Ошибка аудио"
        SpeechRecognizer.ERROR_CLIENT             -> "Ошибка клиента"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет разрешения"
        SpeechRecognizer.ERROR_NETWORK            -> "Нет интернета"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT    -> "Таймаут сети"
        SpeechRecognizer.ERROR_NO_MATCH           -> "Речь не распознана"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY    -> "Recognizer занят"
        SpeechRecognizer.ERROR_SERVER             -> "Ошибка сервера"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT     -> "Нет речи"
        else -> "Неизвестная ошибка ($code)"
    }

    override fun onDestroy() {
        super.onDestroy()
        isActiveCall = false
        mainHandler.removeCallbacksAndMessages(null)
        stopRecognizer()
    }

    companion object {
        private const val TAG = "SpeechRecognitionService"
        const val NOTIFICATION_ID       = 1002
        const val ACTION_START_LISTENING = "com.deafcall.START_LISTENING"
        const val ACTION_STOP_LISTENING  = "com.deafcall.STOP_LISTENING"
        const val EXTRA_LANGUAGE         = "extra_language"
    }
}
