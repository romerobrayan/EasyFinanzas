package dev.romerobrayan.tinto.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.component.MoneyText
import dev.romerobrayan.tinto.core.designsystem.component.MonthPickerSheet
import dev.romerobrayan.tinto.core.designsystem.component.MonthSelector
import dev.romerobrayan.tinto.core.designsystem.component.MovementDetailSheet
import dev.romerobrayan.tinto.core.designsystem.component.PeriodSelector
import dev.romerobrayan.tinto.core.designsystem.component.StatementRow
import dev.romerobrayan.tinto.core.designsystem.component.TintoBarChart
import dev.romerobrayan.tinto.core.designsystem.component.TintoSelectorPill
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.PillShape
import dev.romerobrayan.tinto.core.designsystem.theme.TileShape
import dev.romerobrayan.tinto.core.domain.model.Period
import dev.romerobrayan.tinto.core.domain.model.TransactionType

@Composable
fun DashboardScreen(
    onSeeAll: () -> Unit,
    onEditMovement: (String) -> Unit,
    onReviewPending: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        state = state,
        onPeriodSelected = viewModel::onPeriodSelected,
        onTypeSelected = viewModel::onTypeSelected,
        onBarSelected = viewModel::onBarSelected,
        onMonthSelected = viewModel::onMonthSelected,
        onSeeAll = onSeeAll,
        onEditMovement = onEditMovement,
        onDeleteMovement = viewModel::onDeleteMovement,
        onReviewPending = onReviewPending,
        modifier = modifier,
    )
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onPeriodSelected: (Period) -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onBarSelected: (Int) -> Unit,
    onMonthSelected: (String) -> Unit,
    onSeeAll: () -> Unit,
    onEditMovement: (String) -> Unit,
    onDeleteMovement: (String) -> Unit,
    onReviewPending: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    var showMonthSheet by rememberSaveable { mutableStateOf(false) }
    var selectedMovementId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = type.screenTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (state.pendingCount > 0) {
            Spacer(Modifier.height(14.dp))
            PendingBanner(count = state.pendingCount, onReview = onReviewPending)
        }

        Spacer(Modifier.height(16.dp))
        MonthSelector(label = state.monthLabel, onClick = { showMonthSheet = true })

        Spacer(Modifier.height(16.dp))
        PeriodSelector(selected = state.selectedPeriod, onSelect = onPeriodSelected)

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TintoSelectorPill(
                label = stringResource(R.string.dashboard_type_expenses),
                selected = state.selectedType == TransactionType.EXPENSE,
                onClick = { onTypeSelected(TransactionType.EXPENSE) },
            )
            TintoSelectorPill(
                label = stringResource(R.string.dashboard_type_income),
                selected = state.selectedType == TransactionType.INCOME,
                onClick = { onTypeSelected(TransactionType.INCOME) },
            )
        }

        Spacer(Modifier.height(20.dp))
        TintoBarChart(
            bars = state.bars,
            selectedIndex = state.selectedBarIndex,
            onBarSelected = onBarSelected,
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(
                if (state.selectedType == TransactionType.EXPENSE) {
                    R.string.dashboard_expenses_of
                } else {
                    R.string.dashboard_income_of
                },
                heroLabelCore(state),
            ),
            style = type.caption,
            color = tinto.muted,
        )
        Spacer(Modifier.height(2.dp))
        MoneyText(amount = state.heroAmount, style = type.moneyHero)

        state.comparison?.let { comparison ->
            Spacer(Modifier.height(10.dp))
            ComparisonChip(comparison)
        }

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.dashboard_movements),
                style = type.sectionTitle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.dashboard_see_all),
                style = type.body.copy(fontWeight = FontWeight.Medium),
                color = tinto.gold,
                modifier = Modifier.clickable(onClick = onSeeAll),
            )
        }

        Spacer(Modifier.height(2.dp))
        if (state.preview.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_empty_period),
                    style = type.caption,
                    color = tinto.muted,
                )
            }
        } else {
            state.preview.forEachIndexed { index, item ->
                StatementRow(
                    item = item,
                    onClick = { selectedMovementId = item.id },
                )
                if (index != state.preview.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showMonthSheet) {
        MonthPickerSheet(
            months = state.monthOptions,
            selectedKey = state.selectedMonthKey,
            onSelect = { option ->
                onMonthSelected(option.key)
                showMonthSheet = false
            },
            onDismiss = { showMonthSheet = false },
        )
    }

    val selectedMovement = selectedMovementId?.let { id ->
        state.preview.firstOrNull { it.id == id }
    }
    if (selectedMovement != null) {
        MovementDetailSheet(
            item = selectedMovement,
            onEdit = {
                selectedMovementId = null
                onEditMovement(selectedMovement.id)
            },
            onDelete = {
                selectedMovementId = null
                onDeleteMovement(selectedMovement.id)
            },
            onDismiss = { selectedMovementId = null },
        )
    }
}

/** "Detectamos N movimientos nuevos" + Revisar — never raw capture text. */
@Composable
private fun PendingBanner(count: Int, onReview: () -> Unit) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TileShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onReview)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.MarkChatUnread,
            contentDescription = null,
            tint = tinto.gold,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = pluralStringResource(R.plurals.pending_banner, count, count),
            style = type.body,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.pending_review_action),
            style = type.body.copy(fontWeight = FontWeight.Medium),
            color = tinto.gold,
        )
    }
}

@Composable
private fun ComparisonChip(comparison: ComparisonUi) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val versusCore = if (comparison.versusPeriod == Period.WEEK) {
            stringResource(R.string.week_of, comparison.versusDateLabel)
        } else {
            comparison.versusDateLabel
        }
        if (comparison.percent == 0) {
            Text(
                text = stringResource(R.string.dashboard_comparison_equal, versusCore),
                style = type.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Icon(
                imageVector = if (comparison.isDecrease) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                contentDescription = null,
                tint = if (comparison.isPositiveChange) tinto.income else tinto.expense,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(
                    if (comparison.isDecrease) R.string.dashboard_comparison_down else R.string.dashboard_comparison_up,
                    comparison.percent,
                    versusCore,
                ),
                style = type.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun heroLabelCore(state: DashboardUiState): String =
    if (state.heroPeriod == Period.WEEK) {
        stringResource(R.string.week_of, state.heroDateLabel)
    } else {
        state.heroDateLabel
    }
