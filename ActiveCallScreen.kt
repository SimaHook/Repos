package com.deafcall.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deafcall.model.CallState
import com.deafcall.model.FontSize
import com.deafcall.model.SpeakerType
import com.deafcall.ui.components.CallerAvatar
import com.deafcall.ui.components.ControlButton
import com.deafcall.ui.components.QuickPhraseChip
import com.deafcall.ui.components.SoundWaveIndicator
import com.deafcall.ui.components.TranscriptBubble
import com.deafcall.ui.theme.DeafCallColors
import com.deafcall.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveCallScreen(
    viewModel: MainViewModel,
    onCallEnded: () -> Unit
) {
    val uiState  by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val callInfo = when (val s = uiState.callState) {
        is CallState.Active -> s.callInfo
        is CallState.Ended  -> { LaunchedEffect(Unit) { onCallEnded() }; return }
        else -> null
    }

    val lazyState = rememberLazyListState()
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    val fontSize = when (settings.fontSize) {
        FontSize.NORMAL      -> 16.sp
        FontSize.LARGE       -> 20.sp
        FontSize.EXTRA_LARGE -> 26.sp
    }

    // Автоскролл к последнему сообщению
    LaunchedEffect(uiState.transcriptEntries.size) {
        if (uiState.transcriptEntries.isNotEmpty()) {
            lazyState.animateScrollToItem(uiState.transcriptEntries.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {

        // ── Хедер звонка ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallerAvatar(emoji = "👤", size = 48.dp, isPulsing = false)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = callInfo?.contactName ?: callInfo?.phoneNumber ?: "Неизвестный",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDuration(uiState.callDurationSeconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeafCallColors.GreenSuccess
                )
            }

            // LIVE badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DeafCallColors.RedDanger)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("LIVE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // ── Заголовок транскрипции ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = "🎙️  ГОЛОС → ТЕКСТ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            SoundWaveIndicator(isActive = true, modifier = Modifier.height(16.dp))
        }

        // ── Список транскрипции ──
        LazyColumn(
            state = lazyState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (uiState.transcriptEntries.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SoundWaveIndicator(isActive = false, modifier = Modifier.height(20.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ожидание речи собеседника...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            items(
                items = uiState.transcriptEntries,
                key = { it.id }
            ) { entry ->
                TranscriptBubble(
                    text       = entry.text,
                    timestamp  = timeFormat.format(Date(entry.timestamp)),
                    isPartial  = entry.isPartial,
                    isUserMessage = entry.speakerType == SpeakerType.USER,
                    fontSize   = fontSize
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Быстрые фразы ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(viewModel.quickPhrases) { phrase ->
                QuickPhraseChip(
                    emoji = phrase.emoji,
                    text  = phrase.text,
                    onClick = { viewModel.useQuickPhrase(phrase) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Поле ввода ответа ──
        Text(
            text = "⌨️  ВАШ ОТВЕТ → ГОЛОС",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = uiState.replyText,
                onValueChange = viewModel::updateReplyText,
                placeholder = {
                    Text(
                        "Напишите ответ — будет озвучен...",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = DeafCallColors.PurpleAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.speakReply() },
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DeafCallColors.PurpleAccent)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить",
                    tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Кнопки управления ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlButton(
                icon     = Icons.Default.MicOff,
                label    = "МУТ",
                isActive = uiState.isMuted,
                onClick  = viewModel::toggleMute,
                modifier = Modifier.weight(1f)
            )
            ControlButton(
                icon     = Icons.AutoMirrored.Filled.VolumeUp,
                label    = "ДИНАМИК",
                isActive = uiState.isSpeakerOn,
                onClick  = viewModel::toggleSpeaker,
                modifier = Modifier.weight(1f)
            )
            ControlButton(
                icon     = Icons.Default.RecordVoiceOver,
                label    = "ЗАПИСЬ",
                isActive = uiState.isRecording,
                onClick  = { /* TODO: start/stop recording */ },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Завершить звонок ──
        Button(
            onClick = {
                viewModel.endCall()
                onCallEnded()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DeafCallColors.RedDanger
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.PhoneDisabled, contentDescription = null,
                modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ЗАВЕРШИТЬ ЗВОНОК", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

