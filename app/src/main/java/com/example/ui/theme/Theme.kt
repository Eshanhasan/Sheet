package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF041C13),
    primaryContainer = EmeraldDim,
    onPrimaryContainer = IvoryText,
    secondary = AmberUnitAccent,
    onSecondary = Color(0xFF1F1200),
    secondaryContainer = Color(0xFF3E2906),
    onSecondaryContainer = AmberUnitSoft,
    tertiary = CyanCurrencyAccent,
    onTertiary = Color(0xFF002026),
    tertiaryContainer = Color(0xFF0B3B46),
    onTertiaryContainer = Color(0xFFCFFAFE),
    background = ForestObsidian,
    onBackground = IvoryText,
    surface = ForestSurface,
    onSurface = IvoryText,
    surfaceVariant = ForestCard,
    onSurfaceVariant = SageMutedText,
    outline = ForestBorder,
    error = RoseAlertAccent,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDeepPrimary,
    onPrimary = Color.White,
    primaryContainer = LedgerSubtleSurface,
    onPrimaryContainer = LedgerInkText,
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = AmberUnitSoft,
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = Color(0xFF0891B2),
    onTertiary = Color.White,
    background = LedgerPaperBg,
    onBackground = LedgerInkText,
    surface = LedgerCardSurface,
    onSurface = LedgerInkText,
    surfaceVariant = Color(0xFFE5F0EB),
    onSurfaceVariant = LedgerMutedInk,
    outline = Color(0xFFBDD4C9),
    error = RoseAlertAccent,
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SheetSnapTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
