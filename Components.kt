package com.deafcall.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deafcall.ui.theme.DeafCallColors

// ─────────────────────────────────────────────
//  Пульсирующий аватар звонящего
// ─────────────────────────────────────────────
@Composable
fun CallerAvatar(
    emoji: String,
    size: Dp = 96.dp,
    isPulsing: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_pulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size + 48.dp)
    ) {
        // Outer ring
        if (isPulsing) {
            Box(
                modifier = Modifier
                    .size(size + 44.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        DeafCallColors.CyanAccent.copy(alpha = 0.08f)
                    )
                    .border(1.dp, DeafCallColors.CyanAccent.copy(alpha = 0.3f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(size + 24.dp)
                    .clip(CircleShape)
                    .background(DeafCallColors.CyanAccent.copy(alpha = 0.1f))
                    .border(1.dp, DeafCallColors.CyanAccent.copy(alpha = 0.4f), CircleShape)
            )
        }

        // Avatar circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(DeafCallColors.PurpleAccent, DeafCallColors.CyanAccent)
                    )
                )
        ) {
            Text(text = emoji, fontSize = (size.value * 0.4f).sp)
        }
    }
}

// ─────────────────────────────────────────────
//  Кнопка принять / отклонить звонок
// ─────────────────────────────────────────────
@Composable
fun CallActionButton(
    isAccept: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = if (isAccept)
        Brush.linearGradient(listOf(Color(0xFF00C853), DeafCallColors.GreenSuccess))
    else
        Brush.linearGradient(listOf(Color(0xFFB71C1C), DeafCallColors.RedDanger))

    val icon  = if (isAccept) Icons.Default.Call else Icons.Default.CallEnd
    val label = if (isAccept) "ПРИНЯТЬ" else "ОТКЛОНИТЬ"

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label,
                tint = if (isAccept) Color.Black else Color.White,
                modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (isAccept) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Карточка настройки с тогглом
// ─────────────────────────────────────────────
@Composable
fun SettingsToggleRow(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DeafCallColors.PurpleAccent
            )
        )
    }
}

// ─────────────────────────────────────────────
//  Bubble транскрипции
// ─────────────────────────────────────────────
@Composable
fun TranscriptBubble(
    text: String,
    timestamp: String,
    isPartial: Boolean,
    isUserMessage: Boolean,
    fontSize: TextUnit = 16.sp,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { it / 2 }
    ) {
        val bgColor = if (isUserMessage)
            DeafCallColors.PurpleAccent.copy(alpha = 0.15f)
        else
            DeafCallColors.CyanAccent.copy(alpha = 0.06f)

        val borderColor = if (isUserMessage)
            DeafCallColors.PurpleAccent.copy(alpha = if (isPartial) 0.6f else 0.2f)
        else
            DeafCallColors.CyanAccent.copy(alpha = if (isPartial) 0.8f else 0.2f)

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(
                    if (isUserMessage)
                        RoundedCornerShape(12.dp, 12.dp, 4.dp, 12.dp)
                    else
                        RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp)
                )
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isUserMessage) "ВЫ" else "СОБЕСЕДНИК",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUserMessage)
                        DeafCallColors.PurpleAccent
                    else
                        DeafCallColors.CyanAccent
                )
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isPartial) "$text..." else text,
                fontSize = fontSize,
                lineHeight = (fontSize.value * 1.5f).sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isPartial) FontWeight.Normal else FontWeight.Medium
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Чип быстрой фразы
// ─────────────────────────────────────────────
@Composable
fun QuickPhraseChip(
    emoji: String,
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Кнопка управления звонком (Mute/Speaker/Record)
// ─────────────────────────────────────────────
@Composable
fun ControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive)
        DeafCallColors.CyanAccent.copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.surface

    val borderColor = if (isActive)
        DeafCallColors.CyanAccent
    else
        MaterialTheme.colorScheme.outline

    val iconColor = if (isActive)
        DeafCallColors.CyanAccent
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 9.sp, color = iconColor, letterSpacing = 0.5.sp)
    }
}

// ─────────────────────────────────────────────
//  Волна звука (анимация)
// ─────────────────────────────────────────────
@Composable
fun SoundWaveIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        repeat(5) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 4f,
                targetValue = if (isActive) (8f + index * 4f) else 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300 + index * 60, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isActive) DeafCallColors.CyanAccent
                        else DeafCallColors.DarkMuted
                    )
            )
        }
    }
}
