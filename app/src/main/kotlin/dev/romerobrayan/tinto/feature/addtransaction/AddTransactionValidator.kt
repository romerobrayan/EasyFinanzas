package dev.romerobrayan.tinto.feature.addtransaction

import dev.romerobrayan.tinto.core.domain.model.PaymentMethod

/**
 * Pure client-side validation for the manual form. The UI maps each [Error]
 * to a strings.xml message.
 */
object AddTransactionValidator {

    enum class Error { AMOUNT_REQUIRED, CATEGORY_REQUIRED, LAST4_INVALID }

    fun validate(
        amountPesos: Long?,
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
        if (method == PaymentMethod.CARD && (last4.length != 4 || last4.any { !it.isDigit() })) {
            errors += Error.LAST4_INVALID
        }
        return errors
    }
}
