package dev.romerobrayan.tinto.core.common

import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Row model the statement UI renders. Kept as plain data (no Compose types)
 * so dashboard and movements share one mapping from the domain.
 */
data class MovementUi(
    val id: String,
    val title: String,
    val categoryName: String,
    val categoryIconKey: String,
    val categoryColorHex: String,
    /** Last four digits of the matched registered card; null when unmatched. */
    val cardLast4: String?,
    val isCash: Boolean,
    val amount: Money,
    val type: TransactionType,
    val isRecurring: Boolean,
    val date: LocalDate,
    /** Free-text description as stored; null when the movement has none. */
    val merchant: String?,
    val source: TransactionSource,
)

fun Transaction.toMovementUi(
    categoriesById: Map<String, Category>,
    cardsById: Map<String, Card>,
    recurringMerchants: Set<String>,
    timeZone: TimeZone,
): MovementUi {
    val category = categoriesById[categoryId]
    return MovementUi(
        id = id,
        title = merchant ?: category?.name.orEmpty(),
        categoryName = category?.name.orEmpty(),
        categoryIconKey = category?.iconKey ?: "dots",
        categoryColorHex = category?.colorHex ?: "#B99CA6",
        cardLast4 = cardId?.let { cardsById[it]?.last4 },
        isCash = method == PaymentMethod.CASH,
        amount = amount,
        type = type,
        // TODO(sprint-4): replace with real recurrence detection; mock-driven for now.
        isRecurring = merchant != null && merchant in recurringMerchants,
        date = occurredAt.toLocalDateTime(timeZone).date,
        merchant = merchant,
        source = source,
    )
}
