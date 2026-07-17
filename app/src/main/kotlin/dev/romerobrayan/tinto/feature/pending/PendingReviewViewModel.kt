package dev.romerobrayan.tinto.feature.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import dev.romerobrayan.tinto.core.domain.repository.PendingTransactionRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import dev.romerobrayan.tinto.core.domain.usecase.DetectPendingDuplicates
import dev.romerobrayan.tinto.feature.addtransaction.AddTransactionValidator
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * The pending inbox. Confirm promotes through the session-routed
 * [TransactionRepository] (Firestore signed-in, in-memory in demo) with the
 * capture provenance preserved — never rewritten to MANUAL. Discard deletes
 * from the staging store without any ledger write.
 */
@HiltViewModel
class PendingReviewViewModel @Inject constructor(
    private val pendingRepository: PendingTransactionRepository,
    private val transactionRepository: TransactionRepository,
    private val cardRepository: CardRepository,
    categoryRepository: CategoryRepository,
    private val analytics: TintoAnalytics,
) : ViewModel() {

    private val timeZone = TimeZone.currentSystemDefault()

    private data class ReviewForm(
        val pendingId: String,
        val amountDigits: String,
        val type: TransactionType,
        val method: PaymentMethod,
        val last4: String,
        val categoryId: String? = null,
        val date: LocalDate,
        val merchant: String,
        val submitAttempted: Boolean = false,
    )

    /** Non-null while the review sheet is open. */
    private val form = MutableStateFlow<ReviewForm?>(null)

    val uiState: StateFlow<PendingReviewUiState> = combine(
        pendingRepository.observePending(),
        transactionRepository.observeTransactions(),
        cardRepository.observeCards(),
        categoryRepository.observeCategories(),
        form,
    ) { pending, transactions, cards, categories, currentForm ->
        buildState(pending, transactions, cards, categories, currentForm)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingReviewUiState())

    fun onItemClick(pendingId: String) {
        viewModelScope.launch {
            val pending = pendingRepository.observePending().first()
            val item = pending.firstOrNull { it.id == pendingId } ?: return@launch
            val committed = transactionRepository.observeTransactions().first()
            if (item.id in DetectPendingDuplicates.duplicateIds(pending, committed)) {
                analytics.logPendingDuplicateShown()
            }
            val cards = cardRepository.observeCards().first()
            form.value = ReviewForm(
                pendingId = item.id,
                amountDigits = (item.amount.cents / CENTS_PER_PESO).toString(),
                type = item.type,
                // Cash never notifies a bank — captures are card movements.
                method = PaymentMethod.CARD,
                last4 = resolvedLast4(item, cards).orEmpty(),
                categoryId = null,
                date = item.occurredAt.toLocalDateTime(timeZone).date,
                merchant = item.merchant.orEmpty(),
            )
        }
    }

    fun onSheetDismiss() {
        form.value = null
    }

    fun onAmountChanged(raw: String) {
        form.update {
            it?.copy(amountDigits = raw.filter(Char::isDigit).trimStart('0').take(MAX_AMOUNT_DIGITS))
        }
    }

    fun onTypeChanged(type: TransactionType) = form.update { it?.copy(type = type) }

    fun onMethodChanged(method: PaymentMethod) = form.update { it?.copy(method = method) }

    fun onLast4Changed(value: String) {
        form.update { it?.copy(last4 = value.filter(Char::isDigit).take(4)) }
    }

    fun onCategorySelected(categoryId: String) = form.update { it?.copy(categoryId = categoryId) }

    fun onDateChanged(date: LocalDate) = form.update { it?.copy(date = date) }

    fun onMerchantChanged(value: String) = form.update { it?.copy(merchant = value) }

    /** Promotes the reviewed capture into the ledger, then unstages it. */
    fun onConfirm() {
        val currentForm = form.value ?: return
        if (validate(currentForm).isNotEmpty()) {
            form.update { it?.copy(submitAttempted = true) }
            return
        }
        viewModelScope.launch {
            val pending = pendingRepository.observePending().first()
                .firstOrNull { it.id == currentForm.pendingId } ?: run {
                form.value = null
                return@launch
            }
            val now = Clock.System.now()
            val matchedCard = if (currentForm.method == PaymentMethod.CARD) {
                cardRepository.observeCards().first().firstOrNull { it.last4 == currentForm.last4 }
            } else {
                null
            }
            val originalDate = pending.occurredAt.toLocalDateTime(timeZone).date
            transactionRepository.addTransaction(
                Transaction(
                    // The staged UUID rides along, so a double-tap can't fork
                    // two ledger entries.
                    id = pending.id,
                    type = currentForm.type,
                    amount = Money.ofPesos(currentForm.amountDigits.toLong()),
                    method = currentForm.method,
                    cardId = matchedCard?.id,
                    bank = matchedCard?.bank ?: pending.bank,
                    categoryId = requireNotNull(currentForm.categoryId),
                    merchant = currentForm.merchant.trim().ifEmpty { null },
                    // The parsed instant is the real transaction time; keep it
                    // unless the user moved the date (then noon, like the
                    // manual form's edit mode).
                    occurredAt = if (currentForm.date == originalDate) {
                        pending.occurredAt
                    } else {
                        currentForm.date.atTime(12, 0).toInstant(timeZone)
                    },
                    // Provenance preserved: SMS stays SMS (never MANUAL).
                    source = when (pending.channel) {
                        CaptureChannel.SMS -> TransactionSource.SMS
                        CaptureChannel.NOTIFICATION -> TransactionSource.NOTIFICATION
                    },
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            pendingRepository.remove(pending.id)
            analytics.logPendingConfirmed()
            form.value = null
        }
    }

    /** Removes the capture from the staging store. No ledger write. */
    fun onDiscard() {
        val currentForm = form.value ?: return
        viewModelScope.launch {
            pendingRepository.remove(currentForm.pendingId)
            analytics.logPendingDiscarded()
            form.value = null
        }
    }

    private fun buildState(
        pending: List<PendingTransaction>,
        transactions: List<Transaction>,
        cards: List<Card>,
        categories: List<Category>,
        currentForm: ReviewForm?,
    ): PendingReviewUiState {
        val duplicates = DetectPendingDuplicates.duplicateIds(pending, transactions)
        val items = pending.map { item ->
            PendingItemUi(
                id = item.id,
                title = item.merchant ?: item.issuer,
                issuer = item.issuer,
                channel = item.channel,
                amount = item.amount,
                type = item.type,
                date = item.occurredAt.toLocalDateTime(timeZone).date,
                last4 = resolvedLast4(item, cards),
                isDuplicate = item.id in duplicates,
            )
        }
        // The sheet closes itself when its item leaves the store (e.g. it was
        // just confirmed): no matching pending item → no sheet state.
        val sheet = currentForm?.let { formState ->
            pending.firstOrNull { it.id == formState.pendingId }?.let { item ->
                PendingReviewSheetUiState(
                    pendingId = item.id,
                    issuer = item.issuer,
                    channel = item.channel,
                    rawBody = item.rawBody,
                    isDuplicate = item.id in duplicates,
                    amountDigits = formState.amountDigits,
                    type = formState.type,
                    method = formState.method,
                    last4 = formState.last4,
                    categoryId = formState.categoryId,
                    date = formState.date,
                    merchant = formState.merchant,
                    errors = if (formState.submitAttempted) validate(formState) else emptySet(),
                )
            }
        }
        return PendingReviewUiState(
            items = items,
            categories = categories,
            cards = cards,
            reviewSheet = sheet,
        )
    }

    /** Registered-card digits when matched; otherwise the raw parsed mask. */
    private fun resolvedLast4(item: PendingTransaction, cards: List<Card>): String? =
        item.cardId?.let { cardId -> cards.firstOrNull { it.id == cardId }?.last4 } ?: item.last4

    private fun validate(currentForm: ReviewForm) = AddTransactionValidator.validate(
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
