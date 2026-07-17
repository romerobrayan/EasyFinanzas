package dev.romerobrayan.tinto.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.common.Dates
import dev.romerobrayan.tinto.core.common.MockData
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.common.toMovementUi
import dev.romerobrayan.tinto.core.designsystem.component.ChartBarUi
import dev.romerobrayan.tinto.core.designsystem.component.MonthOption
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.ChartBucket
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.Period
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import dev.romerobrayan.tinto.core.domain.usecase.AggregateSpendUseCase
import dev.romerobrayan.tinto.core.domain.usecase.startOfMonth
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
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
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    cardRepository: CardRepository,
    private val aggregateSpend: AggregateSpendUseCase,
    private val analytics: TintoAnalytics,
) : ViewModel() {

    private val timeZone = TimeZone.currentSystemDefault()

    private data class Selection(
        val period: Period = Period.MONTH,
        val type: TransactionType = TransactionType.EXPENSE,
        /** ISO date of the selected month's first day; null = latest data month. */
        val monthKey: String? = null,
        /** Tapped chart bucket; null = the last (current) bucket. */
        val bucketIndex: Int? = null,
    )

    private val selection = MutableStateFlow(Selection())

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.observeTransactions(),
        categoryRepository.observeCategories(),
        cardRepository.observeCards(),
        selection,
    ) { transactions, categories, cards, currentSelection ->
        buildState(transactions, categories, cards, currentSelection)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun onPeriodSelected(period: Period) {
        selection.update { it.copy(period = period, bucketIndex = null) }
    }

    fun onTypeSelected(type: TransactionType) {
        selection.update { it.copy(type = type, bucketIndex = null) }
    }

    fun onBarSelected(index: Int) {
        selection.update { it.copy(bucketIndex = index) }
    }

    fun onMonthSelected(monthKey: String) {
        selection.update { it.copy(monthKey = monthKey, bucketIndex = null) }
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
    ): DashboardUiState {
        val dates = transactions.map { it.occurredAt.toLocalDateTime(timeZone).date }
        val latestDate = dates.maxOrNull() ?: Clock.System.todayIn(timeZone)

        val monthOptions = dates
            .map { it.startOfMonth() }
            .distinct()
            .sortedDescending()
            .map { MonthOption(key = it.toString(), label = Dates.monthYearLabel(it)) }

        val selectedMonthStart = currentSelection.monthKey
            ?.let { key -> runCatching { LocalDate.parse(key) }.getOrNull() }
            ?: latestDate.startOfMonth()

        // The chart ends at "today" for the current month, or at the month's
        // last day when browsing an older statement.
        val anchor = if (selectedMonthStart == latestDate.startOfMonth()) {
            latestDate
        } else {
            selectedMonthStart.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        }

        val buckets = aggregateSpend(
            transactions,
            currentSelection.period,
            anchor,
            timeZone,
            currentSelection.type,
        )
        val selectedIndex = (currentSelection.bucketIndex ?: buckets.lastIndex)
            .coerceIn(0, buckets.lastIndex.coerceAtLeast(0))
        val selectedBucket = buckets.getOrNull(selectedIndex)

        val categoriesById = categories.associateBy { it.id }
        val cardsById = cards.associateBy { it.id }
        val preview = selectedBucket?.let { bucket ->
            transactions
                .filter {
                    val date = it.occurredAt.toLocalDateTime(timeZone).date
                    date >= bucket.start && date < bucket.endExclusive
                }
                .sortedByDescending { it.occurredAt }
                .take(PREVIEW_SIZE)
                .map { it.toMovementUi(categoriesById, cardsById, MockData.recurringMerchants, timeZone) }
        }.orEmpty()

        val comparison = selectedBucket?.let { bucket ->
            buckets.getOrNull(selectedIndex - 1)?.takeIf { it.total.cents > 0 }?.let { previous ->
                val delta = bucket.total.cents - previous.total.cents
                ComparisonUi(
                    percent = (abs(delta) * 100.0 / previous.total.cents).roundToInt(),
                    isDecrease = delta < 0,
                    isPositiveChange = when (currentSelection.type) {
                        TransactionType.EXPENSE -> delta < 0
                        TransactionType.INCOME -> delta > 0
                    },
                    versusPeriod = currentSelection.period,
                    versusDateLabel = heroDateLabel(previous, currentSelection.period),
                )
            }
        }

        return DashboardUiState(
            monthLabel = Dates.monthYearLabel(selectedMonthStart),
            monthOptions = monthOptions,
            selectedMonthKey = selectedMonthStart.toString(),
            selectedPeriod = currentSelection.period,
            selectedType = currentSelection.type,
            bars = buckets.map { ChartBarUi(label = axisLabel(it, currentSelection.period), value = it.total) },
            selectedBarIndex = selectedIndex,
            heroAmount = selectedBucket?.total ?: Money.Zero,
            heroPeriod = currentSelection.period,
            heroDateLabel = selectedBucket?.let { heroDateLabel(it, currentSelection.period) }.orEmpty(),
            comparison = comparison,
            preview = preview,
        )
    }

    private fun axisLabel(bucket: ChartBucket, period: Period): String = when (period) {
        Period.DAY -> bucket.start.dayOfMonth.toString()
        Period.WEEK -> Dates.dayMonthLabel(bucket.start)
        Period.MONTH -> Dates.shortMonth(bucket.start)
        Period.YEAR -> bucket.start.year.toString()
    }

    private fun heroDateLabel(bucket: ChartBucket, period: Period): String = when (period) {
        Period.DAY -> Dates.dayMonthLabel(bucket.start)
        Period.WEEK -> Dates.dayMonthLabel(bucket.start)
        Period.MONTH -> Dates.monthName(bucket.start)
        Period.YEAR -> bucket.start.year.toString()
    }

    private companion object {
        const val PREVIEW_SIZE = 4
    }
}
