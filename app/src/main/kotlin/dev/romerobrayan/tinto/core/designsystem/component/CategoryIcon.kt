package dev.romerobrayan.tinto.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.romerobrayan.tinto.core.designsystem.theme.TileShape
import dev.romerobrayan.tinto.core.designsystem.theme.TintoTheme
import dev.romerobrayan.tinto.core.designsystem.theme.toAccentColor

/**
 * Category glyph on a tinted rounded tile. The tile is always
 * `surfaceVariant`; only the glyph carries the category accent —
 * glyph-on-tint keeps the statement calm (never fill the tile).
 */
@Composable
fun CategoryIcon(
    iconKey: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(TileShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = glyphFor(iconKey),
            contentDescription = null,
            tint = colorHex.toAccentColor(),
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Maps `Category.iconKey` (Tabler-style keys) onto bundled Material glyphs. */
private fun glyphFor(iconKey: String): ImageVector = when (iconKey) {
    "tools-kitchen-2" -> Icons.Outlined.Restaurant
    "device-tv" -> Icons.Outlined.Tv
    "ant" -> Icons.Outlined.BugReport
    "heartbeat" -> Icons.Outlined.MonitorHeart
    "bus" -> Icons.Outlined.DirectionsBus
    "repeat" -> Icons.Outlined.Repeat
    "shopping-cart" -> Icons.Outlined.ShoppingCart
    // Gasto (Sprint 5)
    "home" -> Icons.Outlined.Home
    "alert" -> Icons.Outlined.Warning
    // Ingreso (Sprint 5)
    "payroll" -> Icons.Outlined.Payments
    "debt" -> Icons.Outlined.ReceiptLong
    "loan" -> Icons.Outlined.AccountBalance
    "transfer" -> Icons.Outlined.SwapHoriz
    "contribution" -> Icons.Outlined.Savings
    else -> Icons.Outlined.MoreHoriz
}

@Preview
@Composable
private fun CategoryIconPreview() {
    TintoTheme {
        CategoryIcon(iconKey = "tools-kitchen-2", colorHex = "#E08AA3")
    }
}
