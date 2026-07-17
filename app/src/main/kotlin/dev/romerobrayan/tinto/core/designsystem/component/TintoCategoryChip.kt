package dev.romerobrayan.tinto.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.PillShape

/**
 * Category selector chip — the tile glyph plus the name, gold-bordered when
 * selected. The manual form and the pending review sheet pick categories with
 * the same chip.
 */
@Composable
fun TintoCategoryChip(
    name: String,
    iconKey: String,
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tinto = LocalTintoColors.current
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (selected) 1.dp else 0.5.dp,
                color = if (selected) tinto.gold else MaterialTheme.colorScheme.outline,
                shape = PillShape,
            )
            .clickable(onClick = onClick)
            .padding(start = 6.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(iconKey = iconKey, colorHex = colorHex, size = 24.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            style = LocalTintoTypography.current.caption,
            color = if (selected) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
