package com.deafcall.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VibrationHelper @Inject constructor(
    private val context: Context
) {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /** Короткий сигнал — новое слово в транскрипции */
    fun vibrateOnNewWord() {
        vibrate(VibrationEffect.createOneShot(30, 60))
    }

    /** Входящий звонок — ритмичный паттерн */
    fun vibrateIncomingCall() {
        val pattern = longArrayOf(0, 600, 300, 600, 300, 600)
        val amplitudes = intArrayOf(0, 200, 0, 200, 0, 200)
        vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
    }

    /** Звонок принят */
    fun vibrateCallAccepted() {
        val pattern = longArrayOf(0, 100, 50, 100)
        val amplitudes = intArrayOf(0, 150, 0, 255)
        vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
    }

    /** Звонок завершён */
    fun vibrateCallEnded() {
        val pattern = longArrayOf(0, 300, 100, 150)
        val amplitudes = intArrayOf(0, 200, 0, 80)
        vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
    }

    /** Сообщение отправлено (TTS) */
    fun vibrateSent() {
        vibrate(VibrationEffect.createOneShot(80, 120))
    }

    fun stopVibration() {
        vibrator.cancel()
    }

    private fun vibrate(effect: VibrationEffect) {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(effect)
        }
    }
}
