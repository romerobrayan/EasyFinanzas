package dev.romerobrayan.tinto.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.romerobrayan.tinto.R

// Two families only: Fraunces for display/brand, Inter for body and numbers.
val Fraunces = FontFamily(
    Font(R.font.fraunces_variable, weight = FontWeight.Normal),
    Font(R.font.fraunces_variable, weight = FontWeight.Medium),
)

val Inter = FontFamily(
    Font(R.font.inter_variable, weight = FontWeight.Normal),
    Font(R.font.inter_variable, weight = FontWeight.Medium),
)

/** Tabular figures so aligned digits never jitter where money appears. */
private const val TABULAR_FIGURES = "tnum"

/** The Tinto type scale (DESIGN_SYSTEM.md). Read via [LocalTintoTypography]. */
@Immutable
data class TintoTypography(
    val moneyHero: TextStyle,
    val moneyRow: TextStyle,
    val screenTitle: TextStyle,
    val sectionTitle: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val meta: TextStyle,
)

val TintoType = TintoTypography(
    moneyHero = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    moneyRow = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    screenTitle = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    sectionTitle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    body = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    caption = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    meta = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

val LocalTintoTypography = staticCompositionLocalOf { TintoType }

/** Material slots mapped onto the Tinto scale so M3 components stay on-brand. */
val TintoM3Typography = Typography(
    headlineMedium = TintoType.moneyHero,
    titleLarge = TintoType.screenTitle,
    titleMedium = TintoType.sectionTitle,
    bodyLarge = TintoType.body,
    bodyMedium = TintoType.body,
    bodySmall = TintoType.caption,
    labelLarge = TintoType.body.copy(fontWeight = FontWeight.Medium),
    labelMedium = TintoType.caption.copy(fontWeight = FontWeight.Medium),
    labelSmall = TintoType.meta,
)
