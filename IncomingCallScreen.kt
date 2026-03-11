package com.deafcall.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deafcall.model.CallState
import com.deafcall.ui.components.CallActionButton
import com.deafcall.ui.components.CallerAvatar
import com.deafcall.ui.components.SoundWaveIndicator
import com.deafcall.ui.theme.DeafCallColors
import com.deafcall.viewmodel.MainViewModel

@Composable
fun IncomingCallScreen(
    viewModel: MainViewModel,
    onCallAccepted: () -> Unit,
    onCallRejected: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val callInfo = (uiState.callState as? CallState.Incoming)?.callInfo

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {

            // Метка "ВХОДЯЩИЙ ЗВОНОК"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DeafCallColors.CyanAccent.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ВХОДЯЩИЙ ЗВОНОК",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeafCallColors.CyanAccent,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Аватар с пульсацией
            CallerAvatar(
                emoji = "👤",
                size = 100.dp,
                isPulsing = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Имя / номер
            Text(
                text = callInfo?.contactName ?: callInfo?.phoneNumber ?: "Неизвестный",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (callInfo?.contactName != null) {
                Text(
                    text = callInfo.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Виброиндикатор
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeafCallColors.CyanAccent.copy(alpha = 0.06f))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SoundWaveIndicator(isActive = true, modifier = Modifier.height(20.dp))
                    Text(
                        text = "📳  ВИБРАЦИЯ АКТИВНА",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeafCallColors.CyanAccent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "После ответа голос автоматически\nпреобразуется в текст на экране",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Кнопки принять / отклонить
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CallActionButton(
                    isAccept = true,
                    onClick = {
                        viewModel.acceptCall()
                        onCallAccepted()
                    },
                    modifier = Modifier.weight(1f)
                )
                CallActionButton(
                    isAccept = false,
                    onClick = {
                        viewModel.rejectCall()
                        onCallRejected()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
