package dev.romerobrayan.tinto.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.R
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
import dev.romerobrayan.tinto.core.domain.model.SpeechFailure
import dev.romerobrayan.tinto.core.domain.model.SpeechOutcome
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import dev.romerobrayan.tinto.core.domain.repository.PendingTransactionRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import dev.romerobrayan.tinto.core.domain.usecase.AggregateSpendUseCase
import dev.romerobrayan.tinto.core.domain.usecase.SpeakMonthSummaryUseCase
import dev.romerobrayan.tinto.core.domain.usecase.SummarizeMonthUseCase
import dev.romerobrayan.tinto.core.domain.usecase.startOfMonth
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    private val categoryRepository: CategoryRepository,
    cardRepository: CardRepository,
    pendingTransactionRepository: PendingTransactionRepository,
    private val aggregateSpend: AggregateSpendUseCase,
    private val summarizeMonth: SummarizeMonthUseCase,
    private val speakMonthSummary: SpeakMonthSummaryUseCase,
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

    private val speechState = MutableStateFlow<SpeechUiState>(SpeechUiState.Idle)

    /** The in-flight utterance, so a second tap can stop it. */
    private var speechJob: Job? = null

    // Speech is folded in as a second stage rather than a sixth argument: the
    // typed `combine` overloads stop at five, and the vararg one erases types.
    val uiState: StateFlow<DashboardUiState> = combine(
        combine(
            transactionRepository.observeTransactions(),
            categoryRepository.observeCategories(),
            cardRepository.observeCards(),
            pendingTransactionRepository.observePending().map { it.size },
            selection,
        ) { transactions, categories, cards, pendingCount, currentSelection ->
            buildState(transactions, categories, cards, currentSelection).copy(pendingCount = pendingCount)
        },
        speechState,
    ) { state, speech ->
        state.copy(speech = speech)
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

    /**
     * Play/stop for the spoken month summary. Tapping while preparing or speaking
     * stops instead of queueing a second utterance — the control is a toggle, and
     * the engine would otherwise flush mid-sentence and read it again.
     *
     * Always narrates the selected month's expenses, deliberately ignoring the
     * period and type toggles.
     */
    fun onSpeakToggled() {
        speechJob?.takeIf { it.isActive }?.let { running ->
            speakMonthSummary.stop()
            running.cancel()
            speechJob = null
            speechState.value = SpeechUiState.Idle
            return
        }

        speechState.value = SpeechUiState.Preparing
        speechJob = viewModelScope.launch {
            val transactions = transactionRepository.observeTransactions().first()
            val categories = categoryRepository.observeCategories().first()
            val month = resolveMonthStart(selection.value.monthKey, latestMovementDate(transactions))
            val summary = summarizeMonth(transactions, categories, month, timeZone)

            // Preparing holds until the engine reports audio actually started.
            val outcome = speakMonthSummary(summary) {
                speechState.value = SpeechUiState.Speaking
            }
            speechState.value = when (outcome) {
                SpeechOutcome.Completed, SpeechOutcome.Cancelled -> SpeechUiState.Idle
                is SpeechOutcome.Failed -> SpeechUiState.Error(outcome.reason.messageRes())
            }
            // Deliberately not clearing speechJob here: viewModelScope dispatches
            // eagerly on the main thread, so this can run before the assignment
            // below lands. The isActive check is what gates a restart anyway.
        }
    }

    override fun onCleared() {
        // Hilt singletons get no destroy callback, so the consuming scope releases
        // the engine — see docs/tts.md for why this lives here.
        speechJob?.cancel()
        speechJob = null
        speakMonthSummary.stop()
        speakMonthSummary.release()
        super.onCleared()
    }

    private fun SpeechFailure.messageRes(): Int = when (this) {
        SpeechFailure.ENGINE_UNAVAILABLE -> R.string.tts_error_engine_unavailable
        SpeechFailure.LANGUAGE_NOT_SUPPORTED -> R.string.tts_error_language_not_supported
        SpeechFailure.MISSING_VOICE_DATA -> R.string.tts_error_missing_voice_data
        SpeechFailure.SYNTHESIS_FAILED -> R.string.tts_error_synthesis_failed
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
        val latestDate = latestMovementDate(transactions)

        val monthOptions = dates
            .map { it.startOfMonth() }
            .distinct()
            .sortedDescending()
            .map { MonthOption(key = it.toString(), label = Dates.monthYearLabel(it)) }

        val selectedMonthStart = resolveMonthStart(currentSelection.monthKey, latestDate)

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

    /** Newest movement's date, or today when there are no movements yet. */
    private fun latestMovementDate(transactions: List<Transaction>): LocalDate =
        transactions.map { it.occurredAt.toLocalDateTime(timeZone).date }.maxOrNull()
            ?: Clock.System.todayIn(timeZone)

    /** The browsed month, or the latest data month when nothing is selected. */
    private fun resolveMonthStart(monthKey: String?, latestDate: LocalDate): LocalDate =
        monthKey?.let { key -> runCatching { LocalDate.parse(key) }.getOrNull() }
            ?: latestDate.startOfMonth()

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
