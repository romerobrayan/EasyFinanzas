package dev.romerobrayan.tinto.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.PillShape
import dev.romerobrayan.tinto.core.designsystem.theme.TintoTheme

/** Small gold-on-tint pill marking detected recurring charges. */
@Composable
fun RecurringBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = stringResource(R.string.recurring_badge),
            style = LocalTintoTypography.current.meta,
            color = LocalTintoColors.current.gold,
        )
    }
}

@Preview
@Composable
private fun RecurringBadgePreview() {
    TintoTheme {
        RecurringBadge()
    }
}
