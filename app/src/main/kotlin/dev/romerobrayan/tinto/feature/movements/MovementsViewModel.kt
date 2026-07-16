package dev.romerobrayan.tinto.feature.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.common.Dates
import dev.romerobrayan.tinto.core.common.MockData
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.common.toMovementUi
import dev.romerobrayan.tinto.core.designsystem.component.MonthOption
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import dev.romerobrayan.tinto.core.domain.usecase.startOfMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

@HiltViewModel
class MovementsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    cardRepository: CardRepository,
    private val analytics: TintoAnalytics,
) : ViewModel() {

    private val timeZone = TimeZone.currentSystemDefault()

    private data class Selection(
        /** ISO date of the selected month's first day; null = latest data month. */
        val monthKey: String? = null,
        val filter: MovementsFilter = MovementsFilter.All,
    )

    private val selection = MutableStateFlow(Selection())

    val uiState: StateFlow<MovementsUiState> = combine(
        transactionRepository.observeTransactions(),
        categoryRepository.observeCategories(),
        cardRepository.observeCards(),
        selection,
    ) { transactions, categories, cards, currentSelection ->
        buildState(transactions, categories, cards, currentSelection)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovementsUiState())

    fun onFilterSelected(filter: MovementsFilter) {
        selection.update { it.copy(filter = filter) }
    }

    fun onMonthSelected(monthKey: String) {
        selection.update { it.copy(monthKey = monthKey) }
    }

    fun onDeleteMovement(transactionId: String) {
        viewModelScope.launch {
            val transaction = transactionRepository.observeTransactions().first()
                .firstOrNull { it.id == transactionId } ?: return@launch
            transactionRepository.deleteTransaction(transactionId)
            analytics.logDeleteTransaction(transaction.type.name, transaction.method.name)
        }
    }

    private fun buildState(
        transactions: List<Transaction>,
        categories: List<Category>,
        cards: List<Card>,
        currentSelection: Selection,
    ): MovementsUiState {
        val dates = transactions.map { it.occurredAt.toLocalDateTime(timeZone).date }
        val latestDate = dates.maxOrNull() ?: Clock.System.todayIn(timeZone)

        val monthOptions = dates
            .map { it.startOfMonth() }
            .distinct()
            .sortedDescending()
            .map { MonthOption(key = it.toString(), label = Dates.monthYearLabel(it)) }

        val monthStart = currentSelection.monthKey
            ?.let { key -> runCatching { LocalDate.parse(key) }.getOrNull() }
            ?: latestDate.startOfMonth()
        val monthEnd = monthStart.plus(1, DateTimeUnit.MONTH)

        val inMonth = transactions.filter {
            val date = it.occurredAt.toLocalDateTime(timeZone).date
            date >= monthStart && date < monthEnd
        }
        val filtered = when (val filter = currentSelection.filter) {
            MovementsFilter.All -> inMonth
            is MovementsFilter.ByCard -> inMonth.filter { it.cardId == filter.cardId }
            is MovementsFilter.ByCategory -> inMonth.filter { it.categoryId == filter.categoryId }
        }

        val categoriesById = categories.associateBy { it.id }
        val cardsById = cards.associateBy { it.id }
        val today = Clock.System.todayIn(timeZone)
        val yesterday = today.minus(1, DateTimeUnit.DAY)

        val groups = filtered
            .sortedByDescending { it.occurredAt }
            .map { it.toMovementUi(categoriesById, cardsById, MockData.recurringMerchants, timeZone) }
            .groupBy { it.date }
            .map { (date, items) ->
                MovementsDayGroup(
                    date = date,
                    isToday = date == today,
                    isYesterday = date == yesterday,
                    items = items,
                )
            }
            .sortedByDescending { it.date }

        return MovementsUiState(
            monthLabel = Dates.monthYearLabel(monthStart),
            monthOptions = monthOptions,
            selectedMonthKey = monthStart.toString(),
            filter = currentSelection.filter,
            cards = cards,
            categories = categories,
            groups = groups,
        )
    }
}
