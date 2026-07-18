package dev.romerobrayan.tinto.feature.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.common.Dates
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.asTransactionSource
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import dev.romerobrayan.tinto.core.domain.repository.PendingTransactionRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import dev.romerobrayan.tinto.core.domain.usecase.detectDuplicates
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The pending inbox: multi-select review of captured movements. Confirm
 * promotes the *selected* items to the ledger (source preserved), discard
 * removes them locally — item by item is never required, but each row keeps
 * its own category. Duplicates start unselected so a bulk "add everything"
 * never doubles money silently; the user decides what to do with them.
 */
@HiltViewModel
class PendingReviewViewModel @Inject constructor(
    private val pendingRepository: PendingTransactionRepository,
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    cardRepository: CardRepository,
    private val analytics: TintoAnalytics,
) : ViewModel() {

    private val timeZone = TimeZone.currentSystemDefault()

    private data class Review(
        /** null until the user touches the selection → default (non-duplicates). */
        val selectedIds: Set<String>? = null,
        /** Per-item category choice; unset rows commit under "Otros". */
        val categoryOverrides: Map<String, String> = emptyMap(),
        val categoryPickerFor: String? = null,
        val showDiscardConfirm: Boolean = false,
    )

    private val review = MutableStateFlow(Review())

    /** Default selection of the latest emission, for first-touch toggles. */
    @Volatile
    private var lastDefaultSelection: Set<String> = emptySet()

    private var duplicateEventLogged = false

    val uiState: StateFlow<PendingReviewUiState> = combine(
        pendingRepository.observePending(),
        transactionRepository.observeTransactions(),
        categoryRepository.observeCategories(),
        cardRepository.observeCards(),
        review,
    ) { pending, transactions, categories, cards, currentReview ->
        buildState(pending, transactions, categories, cards, currentReview)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingReviewUiState())

    fun onToggleItem(pendingId: String) {
        review.update { current ->
            val selected = current.selectedIds ?: lastDefaultSelection
            current.copy(
                selectedIds = if (pendingId in selected) selected - pendingId else selected + pendingId,
            )
        }
    }

    fun onToggleSelectAll() {
        val items = uiState.value.items
        review.update { current ->
            current.copy(
                selectedIds = if (items.all { it.isSelected }) {
                    emptySet()
                } else {
                    items.mapTo(mutableSetOf()) { it.id }
                },
            )
        }
    }

    fun onCategoryChipClick(pendingId: String) {
        review.update { it.copy(categoryPickerFor = pendingId) }
    }

    fun onCategoryPicked(categoryId: String) {
        review.update { current ->
            val target = current.categoryPickerFor ?: return@update current
            current.copy(
                categoryOverrides = current.categoryOverrides + (target to categoryId),
                categoryPickerFor = null,
            )
        }
    }

    fun onCategoryPickerDismiss() {
        review.update { it.copy(categoryPickerFor = null) }
    }

    /** Promotes every selected item to the ledger with its chosen category. */
    fun onConfirmSelected() {
        val selected = uiState.value.items.filter { it.isSelected }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val now = Clock.System.now()
            selected.forEach { item ->
                transactionRepository.addTransaction(
                    Transaction(
                        id = UUID.randomUUID().toString(),
                        type = item.type,
                        amount = item.amount,
                        // Cash never notifies — captures are card movements.
                        method = PaymentMethod.CARD,
                        cardId = item.cardId,
                        bank = item.bank,
                        categoryId = item.categoryId,
                        merchant = item.merchant,
                        occurredAt = item.occurredAt,
                        // Provenance is preserved — never rewritten to MANUAL.
                        source = item.channel.asTransactionSource(),
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                pendingRepository.markConfirmed(item.id)
            }
            analytics.logPendingConfirmed(selected.size)
            clearResolvedSelection(selected)
        }
    }

    fun onDiscardSelectedClick() {
        if (uiState.value.selectedCount == 0) return
        review.update { it.copy(showDiscardConfirm = true) }
    }

    fun onDiscardConfirmDismiss() {
        review.update { it.copy(showDiscardConfirm = false) }
    }

    /** Removes the selected items from the inbox — no ledger write. */
    fun onDiscardSelectedConfirmed() {
        val selected = uiState.value.items.filter { it.isSelected }
        review.update { it.copy(showDiscardConfirm = false) }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            selected.forEach { pendingRepository.markDiscarded(it.id) }
            analytics.logPendingDiscarded(selected.size)
            clearResolvedSelection(selected)
        }
    }

    private fun clearResolvedSelection(resolved: List<PendingItemUi>) {
        val resolvedIds = resolved.mapTo(mutableSetOf()) { it.id }
        review.update { current ->
            val selected = current.selectedIds ?: lastDefaultSelection
            current.copy(
                selectedIds = selected - resolvedIds,
                categoryOverrides = current.categoryOverrides - resolvedIds,
            )
        }
    }

    private fun buildState(
        pending: List<PendingTransaction>,
        transactions: List<Transaction>,
        categories: List<Category>,
        cards: List<Card>,
        currentReview: Review,
    ): PendingReviewUiState {
        val duplicates = detectDuplicates(pending, transactions)
        if (duplicates.isNotEmpty() && !duplicateEventLogged) {
            duplicateEventLogged = true
            analytics.logPendingDuplicateShown()
        }

        // Duplicates start unselected: a straight "add everything" must not
        // double money. Selecting them stays one tap away.
        val defaultSelection = pending
            .filterNot { it.id in duplicates }
            .mapTo(mutableSetOf()) { it.id }
        lastDefaultSelection = defaultSelection
        val selected = currentReview.selectedIds ?: defaultSelection

        val categoriesById = categories.associateBy { it.id }
        val cardsById = cards.associateBy { it.id }
        val fallbackCategory = categoriesById[DEFAULT_CATEGORY_ID] ?: categories.lastOrNull()

        val items = pending.map { item ->
            val category = currentReview.categoryOverrides[item.id]
                ?.let(categoriesById::get)
                ?: fallbackCategory
            // Staging matched the card once; re-match here so a card
            // registered after the capture still lines up at review time.
            val matchedCard = item.cardId?.let(cardsById::get)
                ?: item.last4?.let { last4 -> cards.firstOrNull { it.last4 == last4 } }
            PendingItemUi(
                id = item.id,
                title = item.merchant ?: item.issuer,
                merchant = item.merchant,
                amount = item.amount,
                type = item.type,
                dateLabel = Dates.dayMonthLabel(item.occurredAt.toLocalDateTime(timeZone).date),
                channel = item.channel,
                issuer = item.issuer,
                cardLast4 = matchedCard?.last4 ?: item.last4,
                cardId = matchedCard?.id,
                bank = item.bank,
                categoryId = category?.id ?: DEFAULT_CATEGORY_ID,
                categoryName = category?.name.orEmpty(),
                categoryIconKey = category?.iconKey ?: "dots",
                categoryColorHex = category?.colorHex ?: "#B99CA6",
                isDuplicate = item.id in duplicates,
                isSelected = item.id in selected,
                occurredAt = item.occurredAt,
            )
        }

        return PendingReviewUiState(
            items = items,
            categories = categories,
            selectedCount = items.count { it.isSelected },
            allSelected = items.isNotEmpty() && items.all { it.isSelected },
            categoryPickerFor = currentReview.categoryPickerFor,
            showDiscardConfirm = currentReview.showDiscardConfirm,
        )
    }

    private companion object {
        /** Seeded in demo and cloud alike; captures stage as "Otros" by default. */
        const val DEFAULT_CATEGORY_ID = "cat-otros"
    }
}
