package com.deafcall.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

// ─────────────────────────────────────────────
//  DeafCallScreeningService
//  Обрабатывает входящие звонки ДО их приёма
// ─────────────────────────────────────────────
class DeafCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        Log.d(TAG, "Screening call from: ${callDetails.handle?.schemeSpecificPart}")

        // Пропускаем все звонки без изменений
        // В будущем здесь можно добавить блокировку спама
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)
    }

    companion object {
        private const val TAG = "DeafCallScreeningService"
    }
}

// ─────────────────────────────────────────────
//  BootReceiver — восстанавливаем настройки после перезагрузки
// ─────────────────────────────────────────────
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d(TAG, "Device booted — DeafCall services ready")
            // Сервисы запустятся по требованию через TelecomManager
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

// ─────────────────────────────────────────────
//  DeafCallNotificationService
// ─────────────────────────────────────────────
class DeafCallNotificationService : android.app.Service() {

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
}

// ─────────────────────────────────────────────
//  OutgoingCallReceiver
//  Нужен для регистрации как полноценная звонилка на старых API
// ─────────────────────────────────────────────
class OutgoingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Просто пропускаем исходящие звонки без изменений
        // Наличие этого receiver'а в манифесте регистрирует нас как звонилку
    }
}
