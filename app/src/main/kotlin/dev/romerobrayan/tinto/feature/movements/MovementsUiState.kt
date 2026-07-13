package dev.romerobrayan.tinto.feature.movements

import dev.romerobrayan.tinto.core.common.MovementUi
import dev.romerobrayan.tinto.core.designsystem.component.MonthOption
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Category
import kotlinx.datetime.LocalDate

sealed interface MovementsFilter {
    data object All : MovementsFilter
    data class ByCard(val cardId: String) : MovementsFilter
    data class ByCategory(val categoryId: String) : MovementsFilter
}

/** Statement rows for one calendar day. */
data class MovementsDayGroup(
    val date: LocalDate,
    val isToday: Boolean,
    val isYesterday: Boolean,
    val items: List<MovementUi>,
)

data class MovementsUiState(
    val monthLabel: String = "",
    val monthOptions: List<MonthOption> = emptyList(),
    val selectedMonthKey: String = "",
    val filter: MovementsFilter = MovementsFilter.All,
    val cards: List<Card> = emptyList(),
    val categories: List<Category> = emptyList(),
    val groups: List<MovementsDayGroup> = emptyList(),
)
