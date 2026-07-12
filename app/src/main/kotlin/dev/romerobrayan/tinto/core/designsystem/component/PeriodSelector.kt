package dev.romerobrayan.tinto.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.PillShape
import dev.romerobrayan.tinto.core.designsystem.theme.TintoTheme
import dev.romerobrayan.tinto.core.domain.model.Period

/**
 * Día / Semana / Mes / Año pill row. The selected pill fills gold with dark
 * text; unselected periods are text-only in muted.
 */
@Composable
fun PeriodSelector(
    selected: Period,
    onSelect: (Period) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tinto = LocalTintoColors.current
    val type = LocalTintoTypography.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Period.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (isSelected) tinto.gold else Color.Transparent)
                    .clickable { onSelect(period) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(period.labelRes),
                    style = if (isSelected) {
                        type.body.copy(fontWeight = FontWeight.Medium)
                    } else {
                        type.body
                    },
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.background
                    } else {
                        tinto.muted
                    },
                )
            }
        }
    }
}

private val Period.labelRes: Int
    @StringRes get() = when (this) {
        Period.DAY -> R.string.period_day
        Period.WEEK -> R.string.period_week
        Period.MONTH -> R.string.period_month
        Period.YEAR -> R.string.period_year
    }

@Preview
@Composable
private fun PeriodSelectorPreview() {
    TintoTheme {
        PeriodSelector(selected = Period.MONTH, onSelect = {})
    }
}
