package dev.romerobrayan.tinto.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Vino Tinto tokens — single source of truth in DESIGN_SYSTEM.md.
val VtBackground = Color(0xFF17090E)
val VtSurface = Color(0xFF24121A)
val VtSurfaceVariant = Color(0xFF301823)
val VtPrimary = Color(0xFFB23A5E)
val VtPrimaryContainer = Color(0xFF5A2A3C)
val VtSecondary = Color(0xFF8E2C4D)
val VtAccentGold = Color(0xFFC9A961)
val VtExpense = Color(0xFFD8567A)
val VtIncome = Color(0xFF5FB894)
val VtError = Color(0xFFE5484D)
val VtOnBackground = Color(0xFFF5E9EC)
val VtOnSurfaceVariant = Color(0xFFB99CA6)
val VtMuted = Color(0xFF7D6069)
val VtOutline = Color(0xFF3A2029)

/**
 * Semantic finance colors that deliberately live outside the Material scheme
 * (income/expense/gold accent/tertiary text). Read via [LocalTintoColors].
 */
@Immutable
data class TintoColors(
    val expense: Color,
    val income: Color,
    val gold: Color,
    val muted: Color,
)

val LocalTintoColors = staticCompositionLocalOf {
    TintoColors(
        expense = VtExpense,
        income = VtIncome,
        gold = VtAccentGold,
        muted = VtMuted,
    )
}

/** Parses a `#RRGGBB` category accent into a [Color]; falls back to secondary text. */
fun String.toAccentColor(): Color {
    val hex = removePrefix("#")
    return hex.toLongOrNull(16)?.let { Color(0xFF000000L or it) } ?: VtOnSurfaceVariant
}
