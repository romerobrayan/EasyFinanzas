package dev.romerobrayan.tinto.feature.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.common.Dates
import dev.romerobrayan.tinto.core.designsystem.component.MoneyText
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.TileShape
import dev.romerobrayan.tinto.core.domain.model.CaptureChannel

/**
 * The pending inbox: every capture the parser staged, waiting for the user's
 * confirm-or-discard. Nothing here has touched the ledger yet.
 */
@Composable
fun PendingReviewScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PendingReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.pending_title),
                style = type.screenTitle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_close),
                    tint = tinto.muted,
                )
            }
        }

        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.pending_empty),
                    style = type.caption,
                    color = tinto.muted,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                    PendingRow(item = item, onClick = { viewModel.onItemClick(item.id) })
                    if (index != state.items.lastIndex) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }

    state.reviewSheet?.let { sheet ->
        PendingReviewSheet(
            sheet = sheet,
            categories = state.categories,
            cards = state.cards,
            onAmountChanged = viewModel::onAmountChanged,
            onTypeChanged = viewModel::onTypeChanged,
            onMethodChanged = viewModel::onMethodChanged,
            onLast4Changed = viewModel::onLast4Changed,
            onCategorySelected = viewModel::onCategorySelected,
            onDateChanged = viewModel::onDateChanged,
            onMerchantChanged = viewModel::onMerchantChanged,
            onConfirm = viewModel::onConfirm,
            onDiscard = viewModel::onDiscard,
            onDismiss = viewModel::onSheetDismiss,
        )
    }
}

@Composable
private fun PendingRow(
    item: PendingItemUi,
    onClick: () -> Unit,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(TileShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Sms,
                contentDescription = null,
                tint = tinto.gold,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = type.body,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.isDuplicate) {
                    Spacer(Modifier.width(6.dp))
                    DuplicateBadge()
                }
            }
            Spacer(Modifier.height(2.dp))
            val cardLabel = item.last4?.let { stringResource(R.string.card_mask, it) }
                ?: stringResource(R.string.pending_no_card)
            Text(
                text = "${captureChannelLabel(item.channel)} · ${item.issuer} · $cardLabel",
                style = type.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(amount = item.amount, type = item.type)
            Spacer(Modifier.height(2.dp))
            Text(
                text = Dates.dayMonthLabel(item.date),
                style = type.caption,
                color = tinto.muted,
            )
        }
    }
}

@Composable
internal fun captureChannelLabel(channel: CaptureChannel): String = stringResource(
    when (channel) {
        CaptureChannel.SMS -> R.string.source_sms
        CaptureChannel.NOTIFICATION -> R.string.source_notification
    },
)
