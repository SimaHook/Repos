package com.deafcall.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

// ─────────────────────────────────────────────
//  CallState — состояние текущего звонка
// ─────────────────────────────────────────────
sealed class CallState {
    object Idle : CallState()
    data class Incoming(val callInfo: CallInfo) : CallState()
    data class Active(val callInfo: CallInfo) : CallState()
    data class Ended(val callInfo: CallInfo, val durationSeconds: Long) : CallState()
}

// ─────────────────────────────────────────────
//  CallInfo — информация о звонке
// ─────────────────────────────────────────────
data class CallInfo(
    val callId: String,
    val phoneNumber: String,
    val contactName: String?,
    val contactPhotoUri: String? = null,
    val startTime: Long = System.currentTimeMillis(),
    val direction: CallDirection = CallDirection.INCOMING
)

enum class CallDirection { INCOMING, OUTGOING }

// ─────────────────────────────────────────────
//  TranscriptEntry — одна строка транскрипции
// ─────────────────────────────────────────────
data class TranscriptEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPartial: Boolean = false,   // true = промежуточный результат STT
    val speakerType: SpeakerType = SpeakerType.CALLER
)

enum class SpeakerType { CALLER, USER }

// ─────────────────────────────────────────────
//  CallRecord — сохранённая запись разговора (Room)
// ─────────────────────────────────────────────
@Entity(tableName = "call_records")
@TypeConverters(CallRecordConverters::class)
data class CallRecord(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val phoneNumber: String,
    val contactName: String?,
    val direction: CallDirection,
    val startTime: Long,
    val durationSeconds: Long,
    val transcript: String,      // JSON serialized list of entries
    val savedAt: Long = System.currentTimeMillis()
)

class CallRecordConverters {
    @TypeConverter
    fun fromDirection(direction: CallDirection): String = direction.name
    @TypeConverter
    fun toDirection(value: String): CallDirection = CallDirection.valueOf(value)
}

// ─────────────────────────────────────────────
//  QuickPhrase — быстрая фраза для ответа
// ─────────────────────────────────────────────
data class QuickPhrase(
    val id: Int,
    val text: String,
    val emoji: String = "💬"
)

val DEFAULT_QUICK_PHRASES = listOf(
    QuickPhrase(1, "Подожди минуту", "⏳"),
    QuickPhrase(2, "Да, я понял", "✅"),
    QuickPhrase(3, "Нет, не могу", "❌"),
    QuickPhrase(4, "Перезвони позже", "📞"),
    QuickPhrase(5, "Хорошо, договорились", "🤝"),
    QuickPhrase(6, "Я сейчас занят", "🚫"),
    QuickPhrase(7, "Повтори пожалуйста", "🔄"),
    QuickPhrase(8, "Всё хорошо", "👍"),
)

// ─────────────────────────────────────────────
//  UserSettings — настройки приложения
// ─────────────────────────────────────────────
data class UserSettings(
    val isLargeFontEnabled: Boolean = true,
    val isHighContrastEnabled: Boolean = false,
    val isVibroOnNewTextEnabled: Boolean = true,
    val isAutoSaveTranscriptEnabled: Boolean = true,
    val isFlashOnCallEnabled: Boolean = true,
    val isGestureControlEnabled: Boolean = false,
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val fontSize: FontSize = FontSize.LARGE,
    val language: SttLanguage = SttLanguage.RUSSIAN
)

enum class FontSize(val sp: Int) {
    NORMAL(16), LARGE(20), EXTRA_LARGE(26)
}

enum class SttLanguage(val locale: String, val displayName: String) {
    RUSSIAN("ru-RU", "Русский"),
    ENGLISH("en-US", "English"),
    AUTO("", "Авто")
}
