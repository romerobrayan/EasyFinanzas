package dev.romerobrayan.tinto.feature.pending

import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.Instant

/**
 * One detected movement in the review inbox. Carries everything the batch
 * confirm needs (occurredAt/bank/cardId travel hidden through the UI model)
 * so promoting never re-reads the store.
 */
data class PendingItemUi(
    val id: String,
    /** Merchant when parsed, issuer otherwise — never raw message text. */
    val title: String,
    val merchant: String?,
    val amount: Money,
    val type: TransactionType,
    val dateLabel: String,
    val channel: CaptureChannel,
    val issuer: String,
    /** Mask digits (registered-card match first); null renders "Sin tarjeta". */
    val cardLast4: String?,
    val cardId: String?,
    val bank: String?,
    /** Category this item will be committed under (user-changeable per row). */
    val categoryId: String,
    val categoryName: String,
    val categoryIconKey: String,
    val categoryColorHex: String,
    /** Same money seen twice (vs the ledger or another pending item). */
    val isDuplicate: Boolean,
    /** Included in the next batch action (add or discard). */
    val isSelected: Boolean,
    val occurredAt: Instant,
)

data class PendingReviewUiState(
    val items: List<PendingItemUi> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCount: Int = 0,
    val allSelected: Boolean = false,
    /** Pending id whose category picker sheet is open; null = closed. */
    val categoryPickerFor: String? = null,
    val showDiscardConfirm: Boolean = false,
)
