package com.lawliet.project

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

val Orbitron = FontFamily(Font(R.font.orbitron_regular))
val Rajdhani = FontFamily(Font(R.font.rajdhani_regular))

// Warna mengikuti mode tampilan: original (terang) atau LawBlox (gelap ala Roblox).
val Ink: Color get() = if (LbTheme.on) Color(0xFFF2F2F2) else Color(0xFF0A0A0A)
val Paper: Color get() = if (LbTheme.on) Color(0xFF232527) else Color.White
val Soft: Color get() = if (LbTheme.on) Color(0xFF2F3133) else Color(0xFFF4F4F5)
val Line: Color get() = if (LbTheme.on) Color(0xFF45474A) else Color(0xFFE4E4E7)
val Muted: Color get() = if (LbTheme.on) Color(0xFFB8B8B8) else Color(0xFF71717A)
val WaGreen = Color(0xFF25D366)

private fun Typography.withFont(f: FontFamily, heading: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = heading), displayMedium = displayMedium.copy(fontFamily = heading),
    displaySmall = displaySmall.copy(fontFamily = heading), headlineLarge = headlineLarge.copy(fontFamily = heading),
    headlineMedium = headlineMedium.copy(fontFamily = heading), headlineSmall = headlineSmall.copy(fontFamily = heading),
    titleLarge = titleLarge.copy(fontFamily = heading), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = if (LbTheme.on) darkColorScheme(
        primary = Ink, onPrimary = Paper, background = Paper, onBackground = Ink,
        surface = Paper, onSurface = Ink, surfaceVariant = Soft, onSurfaceVariant = Muted, outline = Line,
    ) else lightColorScheme(
        primary = Ink, onPrimary = Paper, background = Paper, onBackground = Ink,
        surface = Paper, onSurface = Ink, surfaceVariant = Soft, outline = Line,
    ),
    typography = if (LbTheme.on) Typography() else Typography().withFont(Rajdhani, Orbitron),
    content = content,
)
