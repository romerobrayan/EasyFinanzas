package dev.romerobrayan.tinto.feature.addtransaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.toCategoryScope
import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import dev.romerobrayan.tinto.core.domain.model.asTransactionSource
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import dev.romerobrayan.tinto.core.domain.repository.PendingTransactionRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import dev.romerobrayan.tinto.navigation.AddTransactionRoute
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val cardRepository: CardRepository,
    categoryRepository: CategoryRepository,
    private val pendingRepository: PendingTransactionRepository,
    private val analytics: TintoAnalytics,
) : ViewModel() {

    private val timeZone = TimeZone.currentSystemDefault()

    /** Non-null when this screen was opened to edit an existing movement. */
    private val editingId: String? = savedStateHandle.toRoute<AddTransactionRoute>().transactionId

    /** Non-null when reviewing a captured pending item (confirm mode). */
    private val pendingId: String? = savedStateHandle.toRoute<AddTransactionRoute>().pendingId

    /** The movement being edited, once loaded; add mode keeps it null. */
    private var original: Transaction? = null

    /** The pending capture being confirmed, once loaded. */
    private var originalPending: PendingTransaction? = null

    /** Latest full (unscoped) category list, cached so type changes can
     *  re-validate the selected category against the new scope. */
    @Volatile
    private var allCategories: List<Category> = emptyList()

    private data class Form(
        val amountDigits: String = "",
        val type: TransactionType = TransactionType.EXPENSE,
        val method: PaymentMethod = PaymentMethod.CASH,
        val last4: String = "",
        val categoryId: String? = null,
        /** null = today (kept relative so the default follows the clock). */
        val date: LocalDate? = null,
        val merchant: String = "",
        val submitAttempted: Boolean = false,
    )

    private val form = MutableStateFlow(Form())

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits once after the movement is persisted; the screen then closes. */
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    init {
        editingId?.let { id -> viewModelScope.launch { prefillFrom(id) } }
        pendingId?.let { id -> viewModelScope.launch { prefillFromPending(id) } }
    }

    val uiState: StateFlow<AddTransactionUiState> = combine(
        form,
        categoryRepository.observeCategories(),
        cardRepository.observeCards(),
    ) { currentForm, categories, cards ->
        allCategories = categories
        val today = Clock.System.todayIn(timeZone)
        val date = currentForm.date ?: today
        // Only the categories that match the selected movement type: expenses
        // and incomes draw from disjoint sets.
        val scope = currentForm.type.toCategoryScope()
        val scopedCategories = categories.filter { it.scope == scope }
        AddTransactionUiState(
            isEditing = editingId != null,
            isConfirmingCapture = pendingId != null,
            amountDigits = currentForm.amountDigits,
            type = currentForm.type,
            method = currentForm.method,
            last4 = currentForm.last4,
            categoryId = currentForm.categoryId,
            date = date,
            isDateToday = date == today,
            merchant = currentForm.merchant,
            categories = scopedCategories,
            cards = cards,
            errors = if (currentForm.submitAttempted) validate(currentForm) else emptySet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddTransactionUiState())

    fun onAmountChanged(raw: String) {
        form.update {
            it.copy(amountDigits = raw.filter(Char::isDigit).trimStart('0').take(MAX_AMOUNT_DIGITS))
        }
    }

    fun onTypeChanged(type: TransactionType) = form.update { current ->
        // Reset the category if the previously chosen one belongs to the other
        // scope (expense vs income), so the form never keeps an off-scope pick.
        val targetScope = type.toCategoryScope()
        val categoryStillValid = current.categoryId?.let { id ->
            allCategories.firstOrNull { it.id == id }?.scope == targetScope
        } ?: false
        current.copy(
            type = type,
            categoryId = if (categoryStillValid) current.categoryId else null,
        )
    }

    fun onMethodChanged(method: PaymentMethod) = form.update { it.copy(method = method) }

    fun onLast4Changed(value: String) {
        form.update { it.copy(last4 = value.filter(Char::isDigit).take(4)) }
    }

    fun onCategorySelected(categoryId: String) = form.update { it.copy(categoryId = categoryId) }

    fun onDateChanged(date: LocalDate) = form.update { it.copy(date = date) }

    fun onMerchantChanged(value: String) = form.update { it.copy(merchant = value) }

    fun onSubmit() {
        val currentForm = form.value
        if (validate(currentForm).isNotEmpty()) {
            form.update { it.copy(submitAttempted = true) }
            return
        }
        // Editing but the original hasn't loaded yet — saving now would fork a
        // duplicate movement, so ignore the tap (the load resolves in ms).
        if (editingId != null && original == null) return
        if (pendingId != null && originalPending == null) return
        viewModelScope.launch {
            val now = Clock.System.now()
            val today = Clock.System.todayIn(timeZone)
            val date = currentForm.date ?: today
            val matchedCard = if (currentForm.method == PaymentMethod.CARD) {
                cardRepository.observeCards().first().firstOrNull { it.last4 == currentForm.last4 }
            } else {
                null
            }
            val confirming = originalPending
            if (confirming != null) {
                val pendingDate = confirming.occurredAt.toLocalDateTime(timeZone).date
                transactionRepository.addTransaction(
                    Transaction(
                        id = UUID.randomUUID().toString(),
                        type = currentForm.type,
                        amount = Money.ofPesos(currentForm.amountDigits.toLong()),
                        method = currentForm.method,
                        cardId = matchedCard?.id,
                        bank = if (currentForm.method == PaymentMethod.CARD) {
                            matchedCard?.bank ?: confirming.bank
                        } else {
                            null
                        },
                        categoryId = requireNotNull(currentForm.categoryId),
                        merchant = currentForm.merchant.trim().ifEmpty { null },
                        // The parsed instant is the truth for an unchanged day;
                        // a moved date lands at noon (or now, when today).
                        occurredAt = when (date) {
                            pendingDate -> confirming.occurredAt
                            today -> now
                            else -> date.atTime(12, 0).toInstant(timeZone)
                        },
                        // Provenance preserved — never rewritten to MANUAL.
                        source = confirming.channel.asTransactionSource(),
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                pendingRepository.markConfirmed(confirming.id)
                analytics.logPendingConfirmed(1)
                _saved.tryEmit(Unit)
                return@launch
            }
            val editing = original
            if (editing != null) {
                val originalDate = editing.occurredAt.toLocalDateTime(timeZone).date
                transactionRepository.updateTransaction(
                    editing.copy(
                        type = currentForm.type,
                        amount = Money.ofPesos(currentForm.amountDigits.toLong()),
                        method = currentForm.method,
                        cardId = matchedCard?.id,
                        bank = matchedCard?.bank,
                        categoryId = requireNotNull(currentForm.categoryId),
                        merchant = currentForm.merchant.trim().ifEmpty { null },
                        // Unchanged day keeps the original instant; a moved date
                        // lands at noon (or now, when moved to today). createdAt
                        // and source ride along untouched via copy().
                        occurredAt = when (date) {
                            originalDate -> editing.occurredAt
                            today -> now
                            else -> date.atTime(12, 0).toInstant(timeZone)
                        },
                        updatedAt = now,
                    ),
                )
                analytics.logEditTransaction(currentForm.type.name, currentForm.method.name)
            } else {
                transactionRepository.addTransaction(
                    Transaction(
                        id = UUID.randomUUID().toString(),
                        type = currentForm.type,
                        amount = Money.ofPesos(currentForm.amountDigits.toLong()),
                        method = currentForm.method,
                        cardId = matchedCard?.id,
                        bank = matchedCard?.bank,
                        categoryId = requireNotNull(currentForm.categoryId),
                        merchant = currentForm.merchant.trim().ifEmpty { null },
                        occurredAt = if (date == today) now else date.atTime(12, 0).toInstant(timeZone),
                        source = TransactionSource.MANUAL,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                analytics.logAddTransaction(currentForm.type.name, currentForm.method.name)
            }
            _saved.tryEmit(Unit)
        }
    }

    /** Removes the pending capture without a ledger write (confirm mode only). */
    fun onDiscardPending() {
        val pending = originalPending ?: return
        viewModelScope.launch {
            pendingRepository.markDiscarded(pending.id)
            analytics.logPendingDiscarded(1)
            _saved.tryEmit(Unit)
        }
    }

    private suspend fun prefillFromPending(id: String) {
        val pending = pendingRepository.observePending().first()
            .firstOrNull { it.id == id } ?: return
        originalPending = pending
        val cards = cardRepository.observeCards().first()
        val matchedLast4 = pending.cardId
            ?.let { cardId -> cards.firstOrNull { it.id == cardId }?.last4 }
            ?: pending.last4
        form.update {
            it.copy(
                amountDigits = (pending.amount.cents / CENTS_PER_PESO).toString(),
                type = pending.type,
                // Cash never notifies; the user can still switch.
                method = PaymentMethod.CARD,
                last4 = matchedLast4.orEmpty(),
                date = pending.occurredAt.toLocalDateTime(timeZone).date,
                merchant = pending.merchant.orEmpty(),
            )
        }
    }

    private suspend fun prefillFrom(transactionId: String) {
        val transaction = transactionRepository.observeTransactions().first()
            .firstOrNull { it.id == transactionId } ?: return
        original = transaction
        val cards = cardRepository.observeCards().first()
        form.update {
            it.copy(
                amountDigits = (transaction.amount.cents / CENTS_PER_PESO).toString(),
                type = transaction.type,
                method = transaction.method,
                last4 = transaction.cardId
                    ?.let { cardId -> cards.firstOrNull { card -> card.id == cardId }?.last4 }
                    .orEmpty(),
                categoryId = transaction.categoryId,
                date = transaction.occurredAt.toLocalDateTime(timeZone).date,
                merchant = transaction.merchant.orEmpty(),
            )
        }
    }

    private fun validate(currentForm: Form) = AddTransactionValidator.validate(
        amountPesos = currentForm.amountDigits.toLongOrNull(),
        method = currentForm.method,
        last4 = currentForm.last4,
        categoryId = currentForm.categoryId,
    )

    private companion object {
        const val MAX_AMOUNT_DIGITS = 10
        const val CENTS_PER_PESO = 100L
    }
}
