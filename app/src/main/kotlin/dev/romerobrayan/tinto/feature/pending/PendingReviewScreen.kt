package dev.romerobrayan.tinto.feature.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.component.CategoryIcon
import dev.romerobrayan.tinto.core.designsystem.component.MoneyText
import dev.romerobrayan.tinto.core.designsystem.component.TintoConfirmDialog
import dev.romerobrayan.tinto.core.designsystem.theme.ButtonShape
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.PillShape
import dev.romerobrayan.tinto.core.designsystem.theme.SheetShape
import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.Category

/**
 * The pending inbox: everything the SMS capture detected, reviewed in batch.
 * Checkboxes pick which movements get added (select-all included), each row
 * carries its own category chip, and likely duplicates arrive badged and
 * unselected — discarding them is the same selection + Descartar gesture.
 * Tapping a row opens the full editable confirm form.
 */
@Composable
fun PendingReviewScreen(
    onClose: () -> Unit,
    onReviewItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PendingReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PendingReviewContent(
        state = state,
        onClose = onClose,
        onReviewItem = onReviewItem,
        onToggleItem = viewModel::onToggleItem,
        onToggleSelectAll = viewModel::onToggleSelectAll,
        onCategoryChipClick = viewModel::onCategoryChipClick,
        onConfirmSelected = viewModel::onConfirmSelected,
        onDiscardSelectedClick = viewModel::onDiscardSelectedClick,
        modifier = modifier,
    )

    state.categoryPickerFor?.let { pendingId ->
        val current = state.items.firstOrNull { it.id == pendingId }
        CategoryPickerSheet(
            categories = state.categories,
            selectedCategoryId = current?.categoryId,
            onPick = viewModel::onCategoryPicked,
            onDismiss = viewModel::onCategoryPickerDismiss,
        )
    }

    if (state.showDiscardConfirm) {
        TintoConfirmDialog(
            title = stringResource(R.string.pending_discard_confirm_title),
            message = pluralStringResource(
                R.plurals.pending_discard_confirm_message,
                state.selectedCount,
                state.selectedCount,
            ),
            confirmLabel = stringResource(R.string.action_discard),
            onConfirm = viewModel::onDiscardSelectedConfirmed,
            onDismiss = viewModel::onDiscardConfirmDismiss,
        )
    }
}

@Composable
private fun PendingReviewContent(
    state: PendingReviewUiState,
    onClose: () -> Unit,
    onReviewItem: (String) -> Unit,
    onToggleItem: (String) -> Unit,
    onToggleSelectAll: () -> Unit,
    onCategoryChipClick: (String) -> Unit,
    onConfirmSelected: () -> Unit,
    onDiscardSelectedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.pending_empty),
                    style = type.caption,
                    color = tinto.muted,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleSelectAll),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.allSelected,
                    onCheckedChange = { onToggleSelectAll() },
                    colors = tintoCheckboxColors(),
                )
                Text(
                    text = stringResource(R.string.pending_select_all),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.pending_selected_count,
                        state.selectedCount,
                        state.selectedCount,
                    ),
                    style = type.caption,
                    color = tinto.muted,
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                    PendingRow(
                        item = item,
                        onToggle = { onToggleItem(item.id) },
                        onCategoryClick = { onCategoryChipClick(item.id) },
                        onClick = { onReviewItem(item.id) },
                    )
                    if (index != state.items.lastIndex) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onConfirmSelected,
                enabled = state.selectedCount > 0,
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = tinto.muted,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.pending_confirm_button,
                        state.selectedCount,
                        state.selectedCount,
                    ),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                )
            }
            TextButton(
                onClick = onDiscardSelectedClick,
                enabled = state.selectedCount > 0,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(R.string.pending_discard_selected),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                    color = if (state.selectedCount > 0) tinto.expense else tinto.muted,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PendingRow(
    item: PendingItemUi,
    onToggle: () -> Unit,
    onCategoryClick: () -> Unit,
    onClick: () -> Unit,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.isSelected,
            onCheckedChange = { onToggle() },
            colors = tintoCheckboxColors(),
        )
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
            Text(
                text = listOf(
                    item.dateLabel,
                    sourceChipLabel(item),
                    item.cardLast4?.let { stringResource(R.string.card_mask, it) }
                        ?: stringResource(R.string.pending_no_card),
                ).joinToString(" · "),
                style = type.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            // Per-row category — tap to reassign before confirming.
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onCategoryClick)
                    .padding(start = 4.dp, top = 4.dp, end = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryIcon(
                    iconKey = item.categoryIconKey,
                    colorHex = item.categoryColorHex,
                    size = 20.dp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = item.categoryName,
                    style = type.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        MoneyText(amount = item.amount, type = item.type)
    }
}

@Composable
private fun DuplicateBadge() {
    val tinto = LocalTintoColors.current
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = stringResource(R.string.pending_duplicate_badge),
            style = LocalTintoTypography.current.meta,
            color = tinto.expense,
            maxLines = 1,
        )
    }
}

@Composable
private fun sourceChipLabel(item: PendingItemUi): String = when (item.channel) {
    CaptureChannel.SMS -> stringResource(R.string.pending_source_sms, item.issuer)
    CaptureChannel.NOTIFICATION -> stringResource(R.string.pending_source_notification, item.issuer)
}

@Composable
private fun tintoCheckboxColors() = CheckboxDefaults.colors(
    checkedColor = LocalTintoColors.current.gold,
    checkmarkColor = MaterialTheme.colorScheme.background,
    uncheckedColor = MaterialTheme.colorScheme.outline,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryPickerSheet(
    categories: List<Category>,
    selectedCategoryId: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val type = LocalTintoTypography.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = SheetShape,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.pending_category_picker_title),
                style = type.sectionTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    val selected = category.id == selectedCategoryId
                    Row(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (selected) 1.dp else 0.5.dp,
                                color = if (selected) {
                                    LocalTintoColors.current.gold
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                shape = PillShape,
                            )
                            .clickable { onPick(category.id) }
                            .padding(start = 6.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryIcon(iconKey = category.iconKey, colorHex = category.colorHex, size = 24.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = category.name,
                            style = type.caption,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}
