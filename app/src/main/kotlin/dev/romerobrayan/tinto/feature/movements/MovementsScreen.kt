package dev.romerobrayan.tinto.feature.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.common.Dates
import dev.romerobrayan.tinto.core.designsystem.component.MonthPickerSheet
import dev.romerobrayan.tinto.core.designsystem.component.MonthSelector
import dev.romerobrayan.tinto.core.designsystem.component.StatementRow
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.PillShape

@Composable
fun MovementsScreen(
    modifier: Modifier = Modifier,
    viewModel: MovementsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MovementsContent(
        state = state,
        onFilterSelected = viewModel::onFilterSelected,
        onMonthSelected = viewModel::onMonthSelected,
        modifier = modifier,
    )
}

@Composable
private fun MovementsContent(
    state: MovementsUiState,
    onFilterSelected: (MovementsFilter) -> Unit,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    var showMonthSheet by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.movements_title),
                style = type.screenTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))
            MonthSelector(label = state.monthLabel, onClick = { showMonthSheet = true })
            Spacer(Modifier.height(12.dp))
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 18.dp),
        ) {
            item {
                TintoFilterChip(
                    label = stringResource(R.string.filter_all),
                    selected = state.filter == MovementsFilter.All,
                    onClick = { onFilterSelected(MovementsFilter.All) },
                )
            }
            items(state.cards, key = { "card-${it.id}" }) { card ->
                TintoFilterChip(
                    label = "${card.bank} ${stringResource(R.string.card_mask, card.last4)}",
                    selected = (state.filter as? MovementsFilter.ByCard)?.cardId == card.id,
                    onClick = { onFilterSelected(MovementsFilter.ByCard(card.id)) },
                )
            }
            items(state.categories, key = { "cat-${it.id}" }) { category ->
                TintoFilterChip(
                    label = category.name,
                    selected = (state.filter as? MovementsFilter.ByCategory)?.categoryId == category.id,
                    onClick = { onFilterSelected(MovementsFilter.ByCategory(category.id)) },
                )
            }
        }

        if (state.groups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.movements_empty),
                    style = type.caption,
                    color = tinto.muted,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 16.dp),
            ) {
                state.groups.forEach { group ->
                    item(key = "header-${group.date}") {
                        Text(
                            text = dayLabel(group),
                            style = type.caption.copy(fontWeight = FontWeight.Medium),
                            color = tinto.muted,
                            modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),
                        )
                    }
                    group.items.forEachIndexed { index, item ->
                        item(key = item.id) {
                            Column {
                                StatementRow(item = item)
                                if (index != group.items.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
}

@Composable
private fun dayLabel(group: MovementsDayGroup): String = when {
    group.isToday -> stringResource(R.string.date_today)
    group.isYesterday -> stringResource(R.string.date_yesterday)
    else -> Dates.dayOfMonthName(group.date)
}

@Composable
private fun TintoFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tinto = LocalTintoColors.current
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = LocalTintoTypography.current.caption,
            color = if (selected) tinto.gold else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
