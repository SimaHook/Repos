package com.deafcall.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.deafcall.model.FontSize
import com.deafcall.model.SttLanguage
import com.deafcall.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val LARGE_FONT       = booleanPreferencesKey("large_font")
        val HIGH_CONTRAST    = booleanPreferencesKey("high_contrast")
        val VIBRO_ON_TEXT    = booleanPreferencesKey("vibro_on_text")
        val AUTO_SAVE        = booleanPreferencesKey("auto_save")
        val FLASH_ON_CALL    = booleanPreferencesKey("flash_on_call")
        val GESTURE_CONTROL  = booleanPreferencesKey("gesture_control")
        val TTS_SPEED        = floatPreferencesKey("tts_speed")
        val TTS_PITCH        = floatPreferencesKey("tts_pitch")
        val FONT_SIZE        = stringPreferencesKey("font_size")
        val STT_LANGUAGE     = stringPreferencesKey("stt_language")
    }

    val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            isLargeFontEnabled        = prefs[Keys.LARGE_FONT] ?: true,
            isHighContrastEnabled     = prefs[Keys.HIGH_CONTRAST] ?: false,
            isVibroOnNewTextEnabled   = prefs[Keys.VIBRO_ON_TEXT] ?: true,
            isAutoSaveTranscriptEnabled = prefs[Keys.AUTO_SAVE] ?: true,
            isFlashOnCallEnabled      = prefs[Keys.FLASH_ON_CALL] ?: true,
            isGestureControlEnabled   = prefs[Keys.GESTURE_CONTROL] ?: false,
            ttsSpeed                  = prefs[Keys.TTS_SPEED] ?: 1.0f,
            ttsPitch                  = prefs[Keys.TTS_PITCH] ?: 1.0f,
            fontSize   = FontSize.valueOf(prefs[Keys.FONT_SIZE] ?: FontSize.LARGE.name),
            language   = SttLanguage.valueOf(prefs[Keys.STT_LANGUAGE] ?: SttLanguage.RUSSIAN.name)
        )
    }

    suspend fun updateLargeFont(enabled: Boolean) =
        dataStore.edit { it[Keys.LARGE_FONT] = enabled }

    suspend fun updateHighContrast(enabled: Boolean) =
        dataStore.edit { it[Keys.HIGH_CONTRAST] = enabled }

    suspend fun updateVibroOnText(enabled: Boolean) =
        dataStore.edit { it[Keys.VIBRO_ON_TEXT] = enabled }

    suspend fun updateAutoSave(enabled: Boolean) =
        dataStore.edit { it[Keys.AUTO_SAVE] = enabled }

    suspend fun updateFlashOnCall(enabled: Boolean) =
        dataStore.edit { it[Keys.FLASH_ON_CALL] = enabled }

    suspend fun updateGestureControl(enabled: Boolean) =
        dataStore.edit { it[Keys.GESTURE_CONTROL] = enabled }

    suspend fun updateTtsSpeed(speed: Float) =
        dataStore.edit { it[Keys.TTS_SPEED] = speed }

    suspend fun updateTtsPitch(pitch: Float) =
        dataStore.edit { it[Keys.TTS_PITCH] = pitch }

    suspend fun updateFontSize(size: FontSize) =
        dataStore.edit { it[Keys.FONT_SIZE] = size.name }

    suspend fun updateLanguage(language: SttLanguage) =
        dataStore.edit { it[Keys.STT_LANGUAGE] = language.name }
}
