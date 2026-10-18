package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FlarialClientTheme(
    accentColor: Color = NeonCyan,
    secondaryAccent: Color = NeonViolet,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = accentColor,
        onPrimary = ObsidianBg,
        primaryContainer = accentColor.copy(alpha = 0.16f),
        onPrimaryContainer = accentColor,
        secondary = secondaryAccent,
        onSecondary = TextPrimary,
        secondaryContainer = secondaryAccent.copy(alpha = 0.18f),
        onSecondaryContainer = secondaryAccent,
        tertiary = NeonRose,
        onTertiary = ObsidianBg,
        tertiaryContainer = NeonRose.copy(alpha = 0.18f),
        onTertiaryContainer = NeonRose,
        background = ObsidianBg,
        onBackground = TextPrimary,
        surface = ObsidianSurface,
        onSurface = TextPrimary,
        surfaceVariant = ObsidianCard,
        onSurfaceVariant = TextSecondary,
        surfaceContainerHighest = ObsidianElevated,
        outline = ObsidianBorder,
        outlineVariant = ObsidianBorder.copy(alpha = 0.6f),
        error = DangerRed,
        onError = TextPrimary
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
