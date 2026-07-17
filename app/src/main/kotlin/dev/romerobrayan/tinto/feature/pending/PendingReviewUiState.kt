package dev.romerobrayan.tinto.feature.pending

import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import dev.romerobrayan.tinto.feature.addtransaction.AddTransactionValidator
import kotlinx.datetime.LocalDate

/** One row of the pending inbox: the parse as proposed, awaiting review. */
data class PendingItemUi(
    val id: String,
    /** Proposed merchant, falling back to the issuer name. */
    val title: String,
    val issuer: String,
    val channel: CaptureChannel,
    val amount: Money,
    val type: TransactionType,
    val date: LocalDate,
    /** Matched registered-card digits, else the raw parsed mask, else null. */
    val last4: String?,
    /** Likely duplicate of a committed movement or another pending item. */
    val isDuplicate: Boolean,
)

/** The editable review sheet; non-null while open. Category starts unset — it is required to confirm. */
data class PendingReviewSheetUiState(
    val pendingId: String,
    val issuer: String,
    val channel: CaptureChannel,
    /** Original message shown for trust; never leaves the device. */
    val rawBody: String,
    val isDuplicate: Boolean,
    val amountDigits: String,
    val type: TransactionType,
    val method: PaymentMethod,
    val last4: String,
    val categoryId: String?,
    val date: LocalDate,
    val merchant: String,
    /** Only populated after a confirm attempt, so the sheet starts clean. */
    val errors: Set<AddTransactionValidator.Error>,
)

data class PendingReviewUiState(
    val items: List<PendingItemUi> = emptyList(),
    val categories: List<Category> = emptyList(),
    val cards: List<Card> = emptyList(),
    val reviewSheet: PendingReviewSheetUiState? = null,
)
