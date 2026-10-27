package com.fabiancg.bottomappbar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme

private val DarkScheme = darkColorScheme(
    primary = MpazNavy,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFBBF7D0),
    secondary = MpazGold,
    onSecondary = Color.White,
    secondaryContainer = MpazGoldLight,
    onSecondaryContainer = Color(0xFFFCA5A5),
    tertiary = MpazNavyLight,
    background = Color(0xFF0B0F0E),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF0B0F0E),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = Color(0xFF9CA3AF),
    surfaceContainer = SurfaceSofter,
    surfaceContainerHigh = SurfaceSoft,
    outline = Outline,
    outlineVariant = Color(0xFF2F3A35),
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = DangerBg,
    onErrorContainer = DangerFg
)

@Composable
fun BottomAppBarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, typography = Typography, content = content)
}
