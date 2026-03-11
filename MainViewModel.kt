package com.deafcall.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deafcall.model.CallRecord
import com.deafcall.model.CallState
import com.deafcall.model.DEFAULT_QUICK_PHRASES
import com.deafcall.model.FontSize
import com.deafcall.model.QuickPhrase
import com.deafcall.model.SpeakerType
import com.deafcall.model.SttLanguage
import com.deafcall.model.TranscriptEntry
import com.deafcall.model.UserSettings
import com.deafcall.service.DeafCallInCallService
import com.deafcall.service.SpeechRecognitionService
import com.deafcall.utils.CallRepository
import com.deafcall.utils.SettingsRepository
import com.deafcall.utils.VibrationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale
import javax.inject.Inject

data class CallUiState(
    val callState: CallState = CallState.Idle,
    val transcriptEntries: List<TranscriptEntry> = emptyList(),
    val replyText: String = "",
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isRecording: Boolean = false,
    val callDurationSeconds: Long = 0L,
    val isTtsSpeaking: Boolean = false,
    val sttError: String? = null,
    val isDefaultDialer: Boolean = false,
    val isSttListening: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val callRepository: CallRepository,
    private val vibrationHelper: VibrationHelper
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val callHistory = callRepository.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickPhrases: List<QuickPhrase> = DEFAULT_QUICK_PHRASES

    // TTS — инициализируем прямо в ViewModel без Hilt (проще и надёжнее)
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var timerJob: Job? = null
    private var callStartTime = 0L

    // STT service binding
    private var sttService: SpeechRecognitionService? = null
    private var isSttBound = false

    private val sttConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as? SpeechRecognitionService.SttBinder ?: return
            sttService = b.getService()
            isSttBound = true
            Log.d(TAG, "STT service bound successfully")
            collectSttFlows()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            sttService = null
            isSttBound = false
            Log.d(TAG, "STT service disconnected")
        }
    }

    init {
        initTts()
        observeCallState()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.language = Locale("ru", "RU")
                Log.d(TAG, "TTS initialized OK")
            } else {
                Log.e(TAG, "TTS init failed: $status")
            }
        }
    }

    private fun observeCallState() {
        viewModelScope.launch {
            DeafCallInCallService.callState.collect { state ->
                _uiState.update { it.copy(callState = state) }
                when (state) {
                    is CallState.Active -> onCallActivated()
                    is CallState.Ended  -> onCallEnded(state)
                    else -> {}
                }
            }
        }
    }

    private fun onCallActivated() {
        callStartTime = System.currentTimeMillis()
        startCallTimer()
        // Привязываемся к STT сервису который InCallService уже запустил
        bindToSttService()
    }

    private fun onCallEnded(state: CallState.Ended) {
        timerJob?.cancel()
        saveTranscriptIfNeeded(state)
        unbindSttService()
        // Сбрасываем транскрипцию через 3 сек чтобы пользователь мог прочитать
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(transcriptEntries = emptyList()) }
        }
    }

    private fun startCallTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val elapsed = (System.currentTimeMillis() - callStartTime) / 1000
                _uiState.update { it.copy(callDurationSeconds = elapsed) }
            }
        }
    }

    private fun bindToSttService() {
        if (isSttBound) return
        try {
            val intent = Intent(context, SpeechRecognitionService::class.java)
            val bound = context.bindService(intent, sttConnection, Context.BIND_AUTO_CREATE)
            Log.d(TAG, "Binding to STT service: $bound")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind STT: ${e.message}")
        }
    }

    private fun unbindSttService() {
        if (isSttBound) {
            try {
                context.unbindService(sttConnection)
            } catch (e: Exception) { /* ignore */ }
            isSttBound = false
            sttService = null
        }
    }

    private fun collectSttFlows() {
        val service = sttService ?: return

        viewModelScope.launch {
            service.transcriptFlow.collect { entry ->
                Log.d(TAG, "Transcript received: ${entry.text} (partial=${entry.isPartial})")
                updateTranscript(entry)
                if (settings.value.isVibroOnNewTextEnabled && !entry.isPartial) {
                    vibrationHelper.vibrateOnNewWord()
                }
            }
        }

        viewModelScope.launch {
            service.isListening.collect { listening ->
                _uiState.update { it.copy(isSttListening = listening) }
            }
        }

        viewModelScope.launch {
            service.sttError.collect { error ->
                _uiState.update { it.copy(sttError = error) }
            }
        }
    }

    private fun updateTranscript(entry: TranscriptEntry) {
        _uiState.update { state ->
            val list = state.transcriptEntries.toMutableList()
            if (entry.isPartial) {
                val lastPartialIdx = list.indexOfLast {
                    it.isPartial && it.speakerType == SpeakerType.CALLER
                }
                if (lastPartialIdx >= 0) list[lastPartialIdx] = entry
                else list.add(entry)
            } else {
                val lastPartialIdx = list.indexOfLast { it.isPartial }
                if (lastPartialIdx >= 0) list[lastPartialIdx] = entry
                else list.add(entry)
            }
            state.copy(transcriptEntries = list)
        }
    }

    // ─── Управление звонком ───
    fun acceptCall() = DeafCallInCallService.acceptCall()
    fun rejectCall() = DeafCallInCallService.rejectCall()
    fun endCall()    = DeafCallInCallService.endCall()

    fun toggleMute() {
        val newMuted = !_uiState.value.isMuted
        audioManager.isMicrophoneMute = newMuted
        _uiState.update { it.copy(isMuted = newMuted) }
    }

    fun toggleSpeaker() {
        val newSpeaker = !_uiState.value.isSpeakerOn
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = newSpeaker
        _uiState.update { it.copy(isSpeakerOn = newSpeaker) }
    }

    // ─── TTS: озвучиваем ответ ───
    fun speakReply(text: String = _uiState.value.replyText) {
        if (text.isBlank()) return
        if (!isTtsReady) {
            Log.w(TAG, "TTS not ready yet")
            return
        }

        val s = settings.value
        tts?.setSpeechRate(s.ttsSpeed)
        tts?.setPitch(s.ttsPitch)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reply_${System.currentTimeMillis()}")

        vibrationHelper.vibrateSent()

        val userEntry = TranscriptEntry(
            text = text, speakerType = SpeakerType.USER, isPartial = false
        )
        _uiState.update { it.copy(
            transcriptEntries = it.transcriptEntries + userEntry,
            replyText = ""
        )}
    }

    fun updateReplyText(text: String) = _uiState.update { it.copy(replyText = text) }

    fun useQuickPhrase(phrase: QuickPhrase) = _uiState.update { it.copy(replyText = phrase.text) }

    // ─── Сохранение транскрипции ───
    private fun saveTranscriptIfNeeded(state: CallState.Ended) {
        if (!settings.value.isAutoSaveTranscriptEnabled) return
        val entries = _uiState.value.transcriptEntries
        if (entries.isEmpty()) return
        viewModelScope.launch {
            val json = JSONArray().apply { entries.forEach { put(it.text) } }.toString()
            callRepository.saveRecord(CallRecord(
                phoneNumber     = state.callInfo.phoneNumber,
                contactName     = state.callInfo.contactName,
                direction       = state.callInfo.direction,
                startTime       = state.callInfo.startTime,
                durationSeconds = state.durationSeconds,
                transcript      = json
            ))
        }
    }

    fun deleteRecord(record: CallRecord) =
        viewModelScope.launch { callRepository.deleteRecord(record) }

    // ─── Настройки ───
    fun setLargeFont(v: Boolean)      = viewModelScope.launch { settingsRepository.updateLargeFont(v) }
    fun setHighContrast(v: Boolean)   = viewModelScope.launch { settingsRepository.updateHighContrast(v) }
    fun setVibroOnText(v: Boolean)    = viewModelScope.launch { settingsRepository.updateVibroOnText(v) }
    fun setAutoSave(v: Boolean)       = viewModelScope.launch { settingsRepository.updateAutoSave(v) }
    fun setFlashOnCall(v: Boolean)    = viewModelScope.launch { settingsRepository.updateFlashOnCall(v) }
    fun setGestureControl(v: Boolean) = viewModelScope.launch { settingsRepository.updateGestureControl(v) }
    fun setTtsSpeed(v: Float)         = viewModelScope.launch { settingsRepository.updateTtsSpeed(v) }
    fun setTtsPitch(v: Float)         = viewModelScope.launch { settingsRepository.updateTtsPitch(v) }
    fun setFontSize(v: FontSize)      = viewModelScope.launch { settingsRepository.updateFontSize(v) }
    fun setLanguage(v: SttLanguage)   = viewModelScope.launch { settingsRepository.updateLanguage(v) }
    fun clearSttError()               = _uiState.update { it.copy(sttError = null) }

    override fun onCleared() {
        super.onCleared()
        unbindSttService()
        tts?.shutdown()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
