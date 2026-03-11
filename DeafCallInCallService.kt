package com.deafcall.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log
import androidx.core.app.NotificationCompat
import com.deafcall.DeafCallApp
import com.deafcall.R
import com.deafcall.model.CallDirection
import com.deafcall.model.CallInfo
import com.deafcall.model.CallState
import com.deafcall.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@AndroidEntryPoint
class DeafCallInCallService : InCallService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    @Inject
    lateinit var vibrationHelper: com.deafcall.utils.VibrationHelper

    companion object {
        private val _callState = MutableStateFlow<CallState>(CallState.Idle)
        val callState: StateFlow<CallState> = _callState

        private var currentCall: Call? = null
        private const val TAG = "DeafCallInCallService"
        private const val NOTIFICATION_ID = 1001

        fun acceptCall() {
            currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }

        fun rejectCall() {
            currentCall?.reject(false, null)
        }

        fun endCall() {
            currentCall?.disconnect()
        }
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.d(TAG, "Call state → $state")
            handleState(call, state)
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        call.registerCallback(callCallback)

        val state = call.details?.state ?: call.state
        Log.d(TAG, "onCallAdded state=$state")

        val callInfo = buildCallInfo(call)

        when (state) {
            Call.STATE_RINGING -> {
                _callState.value = CallState.Incoming(callInfo)
                vibrationHelper.vibrateIncomingCall()
                launchCallUi()
            }
            Call.STATE_ACTIVE -> {
                _callState.value = CallState.Active(callInfo)
                onCallBecameActive(callInfo)
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification(callInfo, state))
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)

        val prev = _callState.value
        val duration = when (prev) {
            is CallState.Active -> (System.currentTimeMillis() - prev.callInfo.startTime) / 1000
            else -> 0L
        }
        val info = when (prev) {
            is CallState.Incoming -> prev.callInfo
            is CallState.Active   -> prev.callInfo
            else -> buildCallInfo(call)
        }

        _callState.value = CallState.Ended(info, duration)
        vibrationHelper.stopVibration()
        currentCall = null

        // Выключаем громкую связь
        disableSpeakerphone()
        stopStt()
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "Call removed, duration=${duration}s")
    }

    private fun handleState(call: Call, state: Int) {
        val callInfo = buildCallInfo(call)
        when (state) {
            Call.STATE_RINGING -> {
                _callState.value = CallState.Incoming(callInfo)
                vibrationHelper.vibrateIncomingCall()
            }
            Call.STATE_ACTIVE -> {
                _callState.value = CallState.Active(callInfo)
                vibrationHelper.stopVibration()
                vibrationHelper.vibrateCallAccepted()
                onCallBecameActive(callInfo)
            }
            Call.STATE_DISCONNECTING,
            Call.STATE_DISCONNECTED -> { /* onCallRemoved обработает */ }
        }
    }

    /**
     * Вызывается когда звонок принят и стал активным.
     *
     * КЛЮЧЕВОЙ МОМЕНТ:
     * SpeechRecognizer слушает МИКРОФОН, а голос собеседника
     * идёт через телефонный канал (наушник). Чтобы STT слышал
     * собеседника — включаем ГРОМКУЮ СВЯЗЬ (speakerphone).
     * Тогда: голос собеседника → динамик → микрофон → STT → текст.
     */
    private fun onCallBecameActive(callInfo: CallInfo) {
        Log.d(TAG, "Call active — enabling speakerphone for STT")

        // Небольшая задержка чтобы аудио успело переключиться
        mainHandler.postDelayed({
            enableSpeakerphone()
            startStt()
        }, 800)
    }

    private fun enableSpeakerphone() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true
            Log.d(TAG, "Speakerphone ENABLED for STT")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable speakerphone: ${e.message}")
        }
    }

    private fun disableSpeakerphone() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable speakerphone: ${e.message}")
        }
    }

    private fun startStt() {
        Log.d(TAG, "Starting STT service")
        val intent = Intent(this, SpeechRecognitionService::class.java).apply {
            action = SpeechRecognitionService.ACTION_START_LISTENING
            putExtra(SpeechRecognitionService.EXTRA_LANGUAGE, "ru-RU")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopStt() {
        startService(Intent(this, SpeechRecognitionService::class.java).apply {
            action = SpeechRecognitionService.ACTION_STOP_LISTENING
        })
    }

    private fun buildCallInfo(call: Call): CallInfo {
        val details = call.details
        val phone = details?.handle?.schemeSpecificPart ?: "Неизвестный"
        val name  = details?.callerDisplayName?.takeIf { it.isNotBlank() }
        val state = details?.state ?: call.state
        return CallInfo(
            callId      = call.hashCode().toString(),
            phoneNumber = phone,
            contactName = name,
            direction   = if (state == Call.STATE_RINGING) CallDirection.INCOMING
                          else CallDirection.OUTGOING
        )
    }

    private fun launchCallUi() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            action = "com.deafcall.ACTION_INCOMING_CALL"
        }
        startActivity(intent)
    }

    private fun buildNotification(callInfo: CallInfo, state: Int): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val text = when (state) {
            Call.STATE_RINGING -> "Входящий звонок"
            Call.STATE_ACTIVE  -> "🎙️ Активный · STT включён · Громкая связь"
            else -> "Звонок"
        }
        return NotificationCompat.Builder(this, DeafCallApp.CHANNEL_ACTIVE_CALL)
            .setContentTitle(callInfo.contactName ?: callInfo.phoneNumber)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_hearing)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        disableSpeakerphone()
        stopStt()
    }
}
