package dev.romerobrayan.tinto.feature.addtransaction

import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.TransactionType

/**
 * Pure client-side validation for the manual form. The UI maps each [Error]
 * to a strings.xml message.
 */
object AddTransactionValidator {

    enum class Error { AMOUNT_REQUIRED, CATEGORY_REQUIRED, LAST4_INVALID }

    fun validate(
        amountPesos: Long?,
        type: TransactionType,
        method: PaymentMethod,
        last4: String,
        categoryId: String?,
    ): Set<Error> {
        val errors = mutableSetOf<Error>()
        if (amountPesos == null || amountPesos <= 0L) {
            errors += Error.AMOUNT_REQUIRED
        }
        if (categoryId.isNullOrBlank()) {
            errors += Error.CATEGORY_REQUIRED
        }
        // The manual 4-digit field only exists for card expenses. Income picks a
        // registered card (last4 comes pre-filled) or uses cash/transfer, and
        // cash/transfer never carry a card — so last4 isn't required there.
        val requiresLast4 = type == TransactionType.EXPENSE && method == PaymentMethod.CARD
        if (requiresLast4 && (last4.length != 4 || last4.any { !it.isDigit() })) {
            errors += Error.LAST4_INVALID
        }
        return errors
    }
}
