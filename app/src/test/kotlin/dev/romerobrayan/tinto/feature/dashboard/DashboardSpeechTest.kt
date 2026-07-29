package dev.romerobrayan.tinto.feature.dashboard

import androidx.lifecycle.ViewModel
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.MonthSummary
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.SpeechFailure
import dev.romerobrayan.tinto.core.domain.model.SpeechOutcome
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import dev.romerobrayan.tinto.core.domain.repository.MonthSummaryNarrator
import dev.romerobrayan.tinto.core.domain.repository.PendingTransactionRepository
import dev.romerobrayan.tinto.core.domain.repository.SpeechSynthesizer
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import dev.romerobrayan.tinto.core.domain.usecase.AggregateSpendUseCase
import dev.romerobrayan.tinto.core.domain.usecase.SpeakMonthSummaryUseCase
import dev.romerobrayan.tinto.core.domain.usecase.SummarizeMonthUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins the spoken-summary state machine on the dashboard. The fake synthesizer
 * suspends on a gate the test controls, so Preparing and Speaking are separately
 * observable — the whole point of modelling them apart is that engine init is
 * asynchronous and the UI must not pretend otherwise.
 */
class DashboardSpeechTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var synthesizer: ControllableSpeechSynthesizer
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        synthesizer = ControllableSpeechSynthesizer()
        viewModel = DashboardViewModel(
            transactionRepository = FakeTransactionRepository(TRANSACTIONS),
            categoryRepository = FakeCategoryRepository(CATEGORIES),
            cardRepository = FakeCardRepository(),
            pendingTransactionRepository = FakePendingTransactionRepository(),
            aggregateSpend = AggregateSpendUseCase(),
            summarizeMonth = SummarizeMonthUseCase(),
            speakMonthSummary = SpeakMonthSummaryUseCase(TotalNarrator(), synthesizer),
            analytics = NoOpAnalytics(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a tap prepares, then speaks, then returns to idle`() = runTest(dispatcher) {
        observeState()

        viewModel.onSpeakToggled()
        advanceUntilIdle()
        assertEquals(SpeechUiState.Preparing, viewModel.uiState.value.speech)

        synthesizer.reportStarted()
        advanceUntilIdle()
        assertEquals(SpeechUiState.Speaking, viewModel.uiState.value.speech)

        synthesizer.finishUtterance()
        advanceUntilIdle()
        assertEquals(SpeechUiState.Idle, viewModel.uiState.value.speech)
    }

    @Test
    fun `tapping while speaking stops instead of queueing a second utterance`() =
        runTest(dispatcher) {
            observeState()
            viewModel.onSpeakToggled()
            advanceUntilIdle()
            synthesizer.reportStarted()
            advanceUntilIdle()

            viewModel.onSpeakToggled()
            advanceUntilIdle()

            assertEquals(SpeechUiState.Idle, viewModel.uiState.value.speech)
            assertEquals(1, synthesizer.stopCount)
            assertEquals(1, synthesizer.speakCount)
        }

    @Test
    fun `double tapping while preparing does not start a second utterance`() =
        runTest(dispatcher) {
            observeState()

            viewModel.onSpeakToggled()
            advanceUntilIdle()
            viewModel.onSpeakToggled()
            advanceUntilIdle()

            assertEquals(1, synthesizer.speakCount)
            assertEquals(SpeechUiState.Idle, viewModel.uiState.value.speech)
        }

    @Test
    fun `a cancelled utterance is not reported as an error`() = runTest(dispatcher) {
        observeState()
        synthesizer.outcome = SpeechOutcome.Cancelled

        viewModel.onSpeakToggled()
        advanceUntilIdle()
        synthesizer.finishUtterance()
        advanceUntilIdle()

        assertEquals(SpeechUiState.Idle, viewModel.uiState.value.speech)
    }

    @Test
    fun `each failure reason surfaces its own message`() = runTest(dispatcher) {
        observeState()
        val messages = mutableSetOf<Int>()

        for (reason in SpeechFailure.entries) {
            synthesizer.outcome = SpeechOutcome.Failed(reason)
            viewModel.onSpeakToggled()
            advanceUntilIdle()
            synthesizer.finishUtterance()
            advanceUntilIdle()

            val speech = viewModel.uiState.value.speech
            assertTrue("$reason should surface an error", speech is SpeechUiState.Error)
            messages += (speech as SpeechUiState.Error).messageRes
        }

        assertEquals(
            "every failure reason needs a distinct message",
            SpeechFailure.entries.size,
            messages.size,
        )
    }

    @Test
    fun `the engine-unavailable message is the one users see when no engine exists`() =
        runTest(dispatcher) {
            observeState()
            synthesizer.outcome = SpeechOutcome.Failed(SpeechFailure.ENGINE_UNAVAILABLE)

            viewModel.onSpeakToggled()
            advanceUntilIdle()
            synthesizer.finishUtterance()
            advanceUntilIdle()

            assertEquals(
                SpeechUiState.Error(R.string.tts_error_engine_unavailable),
                viewModel.uiState.value.speech,
            )
            assertNotEquals(
                R.string.tts_error_missing_voice_data,
                (viewModel.uiState.value.speech as SpeechUiState.Error).messageRes,
            )
        }

    @Test
    fun `a new attempt clears a previous error`() = runTest(dispatcher) {
        observeState()
        synthesizer.outcome = SpeechOutcome.Failed(SpeechFailure.SYNTHESIS_FAILED)
        viewModel.onSpeakToggled()
        advanceUntilIdle()
        synthesizer.finishUtterance()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.speech is SpeechUiState.Error)

        synthesizer.outcome = SpeechOutcome.Completed
        viewModel.onSpeakToggled()
        advanceUntilIdle()

        assertEquals(SpeechUiState.Preparing, viewModel.uiState.value.speech)
    }

    @Test
    fun `it narrates the selected month's expenses`() = runTest(dispatcher) {
        observeState()

        // June is the older statement; July is the latest data month.
        viewModel.onMonthSelected("2026-06-01")
        advanceUntilIdle()
        viewModel.onSpeakToggled()
        advanceUntilIdle()

        assertEquals("2026-06-01/100000", synthesizer.spokenText)
    }

    @Test
    fun `it narrates expenses even while the income toggle is showing`() = runTest(dispatcher) {
        observeState()

        viewModel.onTypeSelected(TransactionType.INCOME)
        advanceUntilIdle()
        viewModel.onSpeakToggled()
        advanceUntilIdle()

        // July expenses are 88_000; the 900_000 of income is not spoken.
        assertEquals("2026-07-01/88000", synthesizer.spokenText)
    }

    @Test
    fun `clearing the view model releases the engine`() = runTest(dispatcher) {
        observeState()
        viewModel.onSpeakToggled()
        advanceUntilIdle()

        viewModel.clearForTest()
        advanceUntilIdle()

        assertEquals(1, synthesizer.stopCount)
        assertEquals(1, synthesizer.shutdownCount)
    }

    /** uiState is `WhileSubscribed`, so it only computes with a live collector. */
    private fun TestScope.observeState() {
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
    }

    private companion object {
        val ZONE = TimeZone.of("America/Bogota")

        val CATEGORIES = listOf(
            Category("mercado", "Mercado", "mercado", "#B23A5E", true),
        )

        val TRANSACTIONS = listOf(
            expense(LocalDate(2026, 6, 10), 100_000),
            expense(LocalDate(2026, 7, 10), 88_000),
            expense(LocalDate(2026, 7, 12), 900_000, TransactionType.INCOME),
        )

        fun expense(
            date: LocalDate,
            pesos: Long,
            type: TransactionType = TransactionType.EXPENSE,
        ): Transaction {
            val instant = date.atTime(12, 0).toInstant(ZONE)
            return Transaction(
                id = "$date-$pesos-$type",
                type = type,
                amount = Money.ofPesos(pesos),
                method = PaymentMethod.CASH,
                cardId = null,
                bank = null,
                categoryId = "mercado",
                merchant = null,
                occurredAt = instant,
                source = TransactionSource.MANUAL,
                createdAt = instant,
                updatedAt = instant,
            )
        }
    }
}

/**
 * `onCleared` is protected on [ViewModel]; reflection keeps the production API
 * unchanged rather than widening it just so a test can reach it.
 */
private fun DashboardViewModel.clearForTest() {
    ViewModel::class.java.getDeclaredMethod("onCleared")
        .apply { isAccessible = true }
        .invoke(this)
}

/** Renders the summary as `month/pesos`, so tests can assert what was narrated. */
private class TotalNarrator : MonthSummaryNarrator {
    override fun narrate(summary: MonthSummary): String =
        "${summary.month}/${summary.total.cents / 100}"
}

private class ControllableSpeechSynthesizer : SpeechSynthesizer {

    var outcome: SpeechOutcome = SpeechOutcome.Completed
    var spokenText: String? = null
    var speakCount = 0
    var stopCount = 0
    var shutdownCount = 0

    private var gate = CompletableDeferred<Unit>()
    private var onStarted: (() -> Unit)? = null

    override suspend fun speak(text: String, onStarted: () -> Unit): SpeechOutcome {
        speakCount++
        spokenText = text
        this.onStarted = onStarted
        val currentGate = CompletableDeferred<Unit>()
        gate = currentGate
        currentGate.await()
        return outcome
    }

    /** The engine reporting that audio actually began. */
    fun reportStarted() {
        onStarted?.invoke()
    }

    /** The engine finishing the utterance with [outcome]. */
    fun finishUtterance() {
        gate.complete(Unit)
    }

    override fun stop() {
        stopCount++
    }

    override fun shutdown() {
        shutdownCount++
    }
}

private class FakeTransactionRepository(
    transactions: List<Transaction>,
) : TransactionRepository {
    private val state = MutableStateFlow(transactions)
    override fun observeTransactions(): Flow<List<Transaction>> = state
    override suspend fun addTransaction(transaction: Transaction) = Unit
    override suspend fun updateTransaction(transaction: Transaction) = Unit
    override suspend fun deleteTransaction(transactionId: String) = Unit
}

private class FakeCategoryRepository(categories: List<Category>) : CategoryRepository {
    private val state = MutableStateFlow(categories)
    override fun observeCategories(): Flow<List<Category>> = state
}

private class FakeCardRepository : CardRepository {
    private val state = MutableStateFlow(emptyList<Card>())
    override fun observeCards(): Flow<List<Card>> = state
    override suspend fun addCard(card: Card) = Unit
    override suspend fun updateCard(card: Card) = Unit
    override suspend fun deleteCard(cardId: String) = Unit
}

private class FakePendingTransactionRepository : PendingTransactionRepository {
    private val state = MutableStateFlow(emptyList<PendingTransaction>())
    override fun observePending(): Flow<List<PendingTransaction>> = state
    override suspend fun stage(pending: PendingTransaction) = Unit
    override suspend fun markConfirmed(pendingId: String) = Unit
    override suspend fun markDiscarded(pendingId: String) = Unit
}

private class NoOpAnalytics : TintoAnalytics {
    override fun setUser(userId: String?) = Unit
    override fun logScreenView(screenName: String) = Unit
    override fun logLogin(method: String) = Unit
    override fun logDemoMode() = Unit
    override fun logSignOut() = Unit
    override fun logAddTransaction(type: String, method: String) = Unit
    override fun logEditTransaction(type: String, method: String) = Unit
    override fun logDeleteTransaction(type: String, method: String) = Unit
    override fun logAddCard() = Unit
    override fun logDeleteCard() = Unit
    override fun logAddReminder(recurrence: String) = Unit
    override fun logReminderPaid(recurrence: String) = Unit
    override fun logReminderNotificationShown(recurrence: String) = Unit
    override fun logCapturePermissionGranted(channel: String) = Unit
    override fun logCaptureDetected(channel: String, issuer: String) = Unit
    override fun logPendingConfirmed(count: Int) = Unit
    override fun logPendingDiscarded(count: Int) = Unit
    override fun logPendingDuplicateShown() = Unit
    override fun recordError(error: Throwable) = Unit
}
