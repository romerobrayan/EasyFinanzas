package dev.romerobrayan.tinto.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.PillShape

/**
 * Single-choice selector pill (gasto/ingreso, efectivo/tarjeta, recurrencia).
 * Selected = gold fill with dark text; unselected = surface variant.
 */
@Composable
fun TintoSelectorPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tinto = LocalTintoColors.current
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(if (selected) tinto.gold else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            style = if (selected) {
                LocalTintoTypography.current.body.copy(fontWeight = FontWeight.Medium)
            } else {
                LocalTintoTypography.current.body
            },
            color = if (selected) {
                MaterialTheme.colorScheme.background
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
