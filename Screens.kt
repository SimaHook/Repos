package com.deafcall.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deafcall.model.CallDirection
import com.deafcall.model.CallState
import com.deafcall.model.FontSize
import com.deafcall.model.SttLanguage
import com.deafcall.ui.components.SettingsToggleRow
import com.deafcall.ui.theme.DeafCallColors
import com.deafcall.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.os.Build
import androidx.compose.foundation.border
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

// ═══════════════════════════════════════════════════════
//  HOME SCREEN
// ═══════════════════════════════════════════════════════
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToIncoming: () -> Unit,
    onNavigateToActive: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Автоматически переходим при изменении состояния звонка
    when (uiState.callState) {
        is CallState.Incoming -> onNavigateToIncoming()
        is CallState.Active   -> onNavigateToActive()
        else -> {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DeafCall",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = DeafCallColors.CyanAccent
                )
                Text(
                    text = "Звонилка для глухонемых",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            Row {
                IconButton(onClick = onNavigateToHistory) {
                    Icon(Icons.Default.History, "История", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = onNavigateToDiagnostics) {
                    Icon(Icons.Default.BugReport, "Диагностика", tint = DeafCallColors.YellowWarn)
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, "Настройки", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Status card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(DeafCallColors.GreenSuccess)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("STT АКТИВЕН", style = MaterialTheme.typography.labelSmall,
                        color = DeafCallColors.GreenSuccess)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Готов к звонку", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Входящие звонки будут автоматически\nпреобразованы в текст",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Feature cards
        val features = listOf(
            Triple("🎙️→📝", "Голос в текст", "Real-time STT во время звонка"),
            Triple("📝→🔊", "Текст в голос", "TTS озвучивает ваши ответы"),
            Triple("📳", "Умная вибрация", "Паттерны для разных событий"),
            Triple("⚡", "Быстрые фразы", "Ответы одним нажатием")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            features.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (emoji, title, desc) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(emoji, fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(title, fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(desc, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 11.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
//  SETTINGS SCREEN
// ═══════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("♿  Доступность")

            SettingsToggleRow(
                title       = "Крупный шрифт",
                description = "Увеличить текст транскрипции",
                isChecked   = settings.isLargeFontEnabled,
                onCheckedChange = viewModel::setLargeFont
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsToggleRow(
                title       = "Высококонтрастный режим",
                description = "Максимальный контраст текста",
                isChecked   = settings.isHighContrastEnabled,
                onCheckedChange = viewModel::setHighContrast
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsToggleRow(
                title       = "Вибрация при новом тексте",
                description = "Короткий сигнал при каждом слове",
                isChecked   = settings.isVibroOnNewTextEnabled,
                onCheckedChange = viewModel::setVibroOnText
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsToggleRow(
                title       = "Мигание экрана",
                description = "Вспышка дисплея при входящем вызове",
                isChecked   = settings.isFlashOnCallEnabled,
                onCheckedChange = viewModel::setFlashOnCall
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsToggleRow(
                title       = "Автосохранение транскрипций",
                description = "Сохранять текст всех разговоров",
                isChecked   = settings.isAutoSaveTranscriptEnabled,
                onCheckedChange = viewModel::setAutoSave
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("🔤  Размер шрифта")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FontSize.entries.forEach { size ->
                    val isSelected = settings.fontSize == size
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) DeafCallColors.PurpleAccent
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { viewModel.setFontSize(size) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = when (size) {
                                FontSize.NORMAL -> "Нормальный"
                                FontSize.LARGE -> "Крупный"
                                FontSize.EXTRA_LARGE -> "Огромный"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("🌐  Язык распознавания")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SttLanguage.entries.forEach { lang ->
                    val isSelected = settings.language == lang
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) DeafCallColors.CyanAccent.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { viewModel.setLanguage(lang) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = lang.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) DeafCallColors.CyanAccent
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("🔊  Параметры голоса (TTS)")

            Text("Скорость речи: ${"%.1f".format(settings.ttsSpeed)}x",
                style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.ttsSpeed,
                onValueChange = viewModel::setTtsSpeed,
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(thumbColor = DeafCallColors.PurpleAccent,
                    activeTrackColor = DeafCallColors.PurpleAccent)
            )

            Text("Тон голоса: ${"%.1f".format(settings.ttsPitch)}x",
                style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.ttsPitch,
                onValueChange = viewModel::setTtsPitch,
                valueRange = 0.5f..1.5f,
                colors = SliderDefaults.colors(thumbColor = DeafCallColors.CyanAccent,
                    activeTrackColor = DeafCallColors.CyanAccent)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════
//  HISTORY SCREEN
// ═══════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val history by viewModel.callHistory.collectAsState()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История звонков") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("История пуста", style = MaterialTheme.typography.titleLarge)
                    Text("Транскрипции разговоров появятся здесь",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.id }) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (record.direction == CallDirection.INCOMING) "📲" else "📤",
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = record.contactName ?: record.phoneNumber,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = dateFormat.format(Date(record.startTime)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "⏱ ${formatDuration(record.durationSeconds)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeafCallColors.GreenSuccess
                                )
                            }
                            IconButton(onClick = { viewModel.deleteRecord(record) }) {
                                Icon(Icons.Default.Delete, "Удалить",
                                    tint = DeafCallColors.RedDanger)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
//  PERMISSIONS SCREEN — с авто-запросом через Accompanist
// ═══════════════════════════════════════════════════════
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(
    onRequestDefaultDialer: () -> Unit,
    onPermissionsGranted: () -> Unit
) {
    val permissionsToRequest = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CALL_PHONE,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val multiplePermissionsState = rememberMultiplePermissionsState(permissionsToRequest)

    val allGranted = multiplePermissionsState.allPermissionsGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🦻", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "DeafCall",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = DeafCallColors.CyanAccent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Звонилка для людей с нарушением слуха",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Список разрешений с визуальным статусом
        val permissionItems = listOf(
            Triple("📞", "Телефон и звонки", Manifest.permission.READ_PHONE_STATE),
            Triple("🎙️", "Микрофон — для STT", Manifest.permission.RECORD_AUDIO),
            Triple("👤", "Контакты — имена звонящих", Manifest.permission.READ_CONTACTS),
        )

        permissionItems.forEach { (emoji, label, perm) ->
            val isGranted = multiplePermissionsState.permissions
                .find { it.permission == perm }
                ?.status?.isGranted ?: false

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isGranted) DeafCallColors.GreenSuccess.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        1.dp,
                        if (isGranted) DeafCallColors.GreenSuccess.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = if (isGranted) "✅" else "❌",
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка запроса разрешений
        if (!allGranted) {
            Button(
                onClick = { multiplePermissionsState.launchMultiplePermissionRequest() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeafCallColors.PurpleAccent
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "ВЫДАТЬ РАЗРЕШЕНИЯ",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Кнопка установки как звонилки
        Button(
            onClick = onRequestDefaultDialer,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeafCallColors.CyanAccent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "УСТАНОВИТЬ КАК ЗВОНИЛКУ",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onPermissionsGranted,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (allGranted) "✅  ПРОДОЛЖИТЬ" else "ПРОДОЛЖИТЬ (пропустить)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


// ─── Helpers ───
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = DeafCallColors.CyanAccent,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

internal fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
