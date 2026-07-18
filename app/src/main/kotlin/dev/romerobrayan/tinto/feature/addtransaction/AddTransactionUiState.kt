package dev.romerobrayan.tinto.feature.addtransaction

import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class AddTransactionUiState(
    /** True when editing an existing movement (title/CTA change accordingly). */
    val isEditing: Boolean = false,
    /** True when confirming a captured pending item (adds Descartar, CTA changes). */
    val isConfirmingCapture: Boolean = false,
    /** Raw peso digits as typed; formatting happens in the amount field. */
    val amountDigits: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val method: PaymentMethod = PaymentMethod.CASH,
    val last4: String = "",
    val categoryId: String? = null,
    val date: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val isDateToday: Boolean = true,
    val merchant: String = "",
    val categories: List<Category> = emptyList(),
    val cards: List<Card> = emptyList(),
    /** Only populated after a submit attempt, so the form starts clean. */
    val errors: Set<AddTransactionValidator.Error> = emptySet(),
)
