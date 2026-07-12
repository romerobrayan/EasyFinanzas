package dev.romerobrayan.tinto.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val TintoDarkColorScheme = darkColorScheme(
    primary = VtPrimary,
    onPrimary = VtOnBackground,
    primaryContainer = VtPrimaryContainer,
    onPrimaryContainer = VtOnBackground,
    secondary = VtSecondary,
    onSecondary = VtOnBackground,
    tertiary = VtAccentGold,
    onTertiary = VtBackground,
    background = VtBackground,
    onBackground = VtOnBackground,
    surface = VtSurface,
    onSurface = VtOnBackground,
    surfaceVariant = VtSurfaceVariant,
    onSurfaceVariant = VtOnSurfaceVariant,
    outline = VtOutline,
    outlineVariant = VtOutline,
    error = VtError,
    onError = VtOnBackground,
    // Flat design: surfaces separate by fill color, never by elevation tint.
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = VtBackground,
    surfaceContainerLow = VtSurface,
    surfaceContainer = VtSurface,
    surfaceContainerHigh = VtSurfaceVariant,
    surfaceContainerHighest = VtSurfaceVariant,
)

/** Dark-first Vino Tinto theme. There is no light theme in v1. */
@Composable
fun TintoTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTintoColors provides TintoColors(
            expense = VtExpense,
            income = VtIncome,
            gold = VtAccentGold,
            muted = VtMuted,
        ),
        LocalTintoTypography provides TintoType,
    ) {
        MaterialTheme(
            colorScheme = TintoDarkColorScheme,
            typography = TintoM3Typography,
            shapes = TintoShapes,
            content = content,
        )
    }
}
