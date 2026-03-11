package com.deafcall.ui.screens

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deafcall.ui.theme.DeafCallColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var diagResults by remember { mutableStateOf<List<DiagItem>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔧 Диагностика STT/TTS") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Проверяет все компоненты необходимые для работы STT и TTS",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    isRunning = true
                    diagResults = runDiagnostics(context)
                    isRunning = false
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeafCallColors.CyanAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (isRunning) "⏳ Проверяем..." else "▶  ЗАПУСТИТЬ ДИАГНОСТИКУ",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            diagResults.forEach { item ->
                DiagRow(item)
            }

            if (diagResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val allOk = diagResults.all { it.status == DiagStatus.OK }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (allOk) DeafCallColors.GreenSuccess.copy(alpha = 0.12f)
                            else DeafCallColors.RedDanger.copy(alpha = 0.12f)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (allOk)
                            "✅ Всё в порядке — STT и TTS должны работать"
                        else
                            "❌ Есть проблемы — исправь пункты со статусом ОШИБКА",
                        fontWeight = FontWeight.Medium,
                        color = if (allOk) DeafCallColors.GreenSuccess else DeafCallColors.RedDanger
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagRow(item: DiagItem) {
    val (bg, border, icon) = when (item.status) {
        DiagStatus.OK      -> Triple(
            DeafCallColors.GreenSuccess.copy(alpha = 0.08f),
            DeafCallColors.GreenSuccess.copy(alpha = 0.3f), "✅")
        DiagStatus.ERROR   -> Triple(
            DeafCallColors.RedDanger.copy(alpha = 0.08f),
            DeafCallColors.RedDanger.copy(alpha = 0.3f), "❌")
        DiagStatus.WARNING -> Triple(
            DeafCallColors.YellowWarn.copy(alpha = 0.08f),
            DeafCallColors.YellowWarn.copy(alpha = 0.3f), "⚠️")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = item.title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(icon, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        if (item.fix != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "→ ${item.fix}",
                style = MaterialTheme.typography.bodyMedium,
                color = DeafCallColors.CyanAccent,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

enum class DiagStatus { OK, ERROR, WARNING }
data class DiagItem(
    val title: String,
    val detail: String,
    val status: DiagStatus,
    val fix: String? = null
)

fun runDiagnostics(context: Context): List<DiagItem> {
    val results = mutableListOf<DiagItem>()

    // 1. SpeechRecognizer доступен?
    val sttAvailable = SpeechRecognizer.isRecognitionAvailable(context)
    results.add(DiagItem(
        title = "SpeechRecognizer (STT)",
        detail = if (sttAvailable) "Доступен на этом устройстве" else "НЕДОСТУПЕН на этом устройстве",
        status = if (sttAvailable) DiagStatus.OK else DiagStatus.ERROR,
        fix = if (!sttAvailable) "Установи приложение Google или обнови Google Services" else null
    ))

    // 2. Интернет?
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork
    val caps = cm.getNetworkCapabilities(network)
    val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val hasValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    results.add(DiagItem(
        title = "Интернет (нужен для Google STT)",
        detail = when {
            hasValidated -> "Подключён и работает"
            hasInternet  -> "Подключён, но не проверен"
            else -> "НЕТ ИНТЕРНЕТА — STT не будет работать!"
        },
        status = when {
            hasValidated -> DiagStatus.OK
            hasInternet  -> DiagStatus.WARNING
            else -> DiagStatus.ERROR
        },
        fix = if (!hasInternet) "Включи Wi-Fi или мобильный интернет" else null
    ))

    // 3. TTS доступен?
    var ttsStatus = DiagStatus.WARNING
    var ttsDetail = "Проверяется..."
    try {
        val tts = TextToSpeech(context) { status ->
            ttsStatus = if (status == TextToSpeech.SUCCESS) DiagStatus.OK else DiagStatus.ERROR
            ttsDetail = if (status == TextToSpeech.SUCCESS) "TextToSpeech инициализирован" else "Ошибка инициализации TTS (код $status)"
        }
        Thread.sleep(500)
        val ruLocale = Locale("ru", "RU")
        val langResult = tts.isLanguageAvailable(ruLocale)
        ttsDetail = when (langResult) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> "Русский язык доступен ✓"
            TextToSpeech.LANG_MISSING_DATA -> "Нет данных для русского языка"
            TextToSpeech.LANG_NOT_SUPPORTED -> "Русский язык не поддерживается"
            else -> "Статус языка: $langResult"
        }
        ttsStatus = if (langResult >= TextToSpeech.LANG_AVAILABLE) DiagStatus.OK else DiagStatus.ERROR
        tts.shutdown()
    } catch (e: Exception) {
        ttsStatus = DiagStatus.ERROR
        ttsDetail = "Исключение: ${e.message}"
    }
    results.add(DiagItem(
        title = "TextToSpeech (TTS)",
        detail = ttsDetail,
        status = ttsStatus,
        fix = if (ttsStatus == DiagStatus.ERROR)
            "Скачай голосовые данные: Настройки → Доступность → Синтез речи → Google TTS" else null
    ))

    // 4. Микрофон/AudioManager
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val hasMic = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) != null
    @Suppress("DEPRECATION")
    val speakerOn = audioManager.isSpeakerphoneOn
    results.add(DiagItem(
        title = "AudioManager",
        detail = "Громкая связь: ${if (speakerOn) "ВКЛ ✓" else "ВЫКЛ"} | Режим: ${audioModeStr(audioManager.mode)}",
        status = DiagStatus.OK,
        fix = null
    ))

    // 5. Разрешение RECORD_AUDIO
    val micPerm = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
    val micGranted = micPerm == android.content.pm.PackageManager.PERMISSION_GRANTED
    results.add(DiagItem(
        title = "Разрешение: Микрофон",
        detail = if (micGranted) "Выдано" else "НЕ ВЫДАНО — STT не запустится",
        status = if (micGranted) DiagStatus.OK else DiagStatus.ERROR,
        fix = if (!micGranted) "Настройки → Приложения → DeafCall → Разрешения → Микрофон" else null
    ))

    return results
}

private fun audioModeStr(mode: Int) = when (mode) {
    AudioManager.MODE_NORMAL           -> "NORMAL"
    AudioManager.MODE_RINGTONE         -> "RINGTONE"
    AudioManager.MODE_IN_CALL          -> "IN_CALL"
    AudioManager.MODE_IN_COMMUNICATION -> "IN_COMMUNICATION"
    else -> "UNKNOWN($mode)"
}
