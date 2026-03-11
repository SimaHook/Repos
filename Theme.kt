package com.deafcall.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
//  Colors
// ─────────────────────────────────────────────
object DeafCallColors {
    val CyanAccent    = Color(0xFF00E5FF)
    val PurpleAccent  = Color(0xFF7C4DFF)
    val GreenSuccess  = Color(0xFF00E676)
    val RedDanger     = Color(0xFFFF1744)
    val YellowWarn    = Color(0xFFFFAB00)

    val DarkBg        = Color(0xFF0A0A0F)
    val DarkSurface   = Color(0xFF12121A)
    val DarkSurface2  = Color(0xFF1A1A28)
    val DarkBorder    = Color(0xFF2A2A40)
    val DarkMuted     = Color(0xFF5C6080)
    val DarkText      = Color(0xFFE8EAF6)

    val LightBg       = Color(0xFFF5F5FF)
    val LightSurface  = Color(0xFFFFFFFF)
    val LightText     = Color(0xFF1A1A2E)
}

private val DarkColorScheme = darkColorScheme(
    primary          = DeafCallColors.CyanAccent,
    onPrimary        = Color.Black,
    secondary        = DeafCallColors.PurpleAccent,
    onSecondary      = Color.White,
    tertiary         = DeafCallColors.GreenSuccess,
    background       = DeafCallColors.DarkBg,
    surface          = DeafCallColors.DarkSurface,
    onBackground     = DeafCallColors.DarkText,
    onSurface        = DeafCallColors.DarkText,
    error            = DeafCallColors.RedDanger,
    outline          = DeafCallColors.DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF006EDA),
    onPrimary        = Color.White,
    secondary        = DeafCallColors.PurpleAccent,
    onSecondary      = Color.White,
    tertiary         = Color(0xFF00897B),
    background       = DeafCallColors.LightBg,
    surface          = DeafCallColors.LightSurface,
    onBackground     = DeafCallColors.LightText,
    onSurface        = DeafCallColors.LightText,
    error            = DeafCallColors.RedDanger
)

// ─────────────────────────────────────────────
//  Theme
// ─────────────────────────────────────────────
@Composable
fun DeafCallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast && darkTheme -> DarkColorScheme.copy(
            background = Color.Black,
            surface    = Color(0xFF0D0D0D),
            onBackground = Color.White,
            onSurface    = Color.White
        )
        highContrast -> LightColorScheme.copy(
            background = Color.White,
            onBackground = Color.Black,
            onSurface    = Color.Black
        )
        darkTheme  -> DarkColorScheme
        else       -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = DeafCallTypography,
        content     = content
    )
}

val DeafCallTypography = androidx.compose.material3.Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 22.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 10.sp,
        letterSpacing = 1.sp
    )
)
