package dev.romerobrayan.tinto.feature.profile

/**
 * Pure client-side validation for the card form. The UI maps each [Error]
 * to a strings.xml message. Same last-4 rule as the add-transaction form —
 * exactly four digits, because capture matching depends on them.
 */
object CardFormValidator {

    enum class Error { BANK_REQUIRED, LAST4_INVALID }

    fun validate(bank: String, last4: String): Set<Error> {
        val errors = mutableSetOf<Error>()
        if (bank.isBlank()) {
            errors += Error.BANK_REQUIRED
        }
        if (last4.length != 4 || last4.any { !it.isDigit() }) {
            errors += Error.LAST4_INVALID
        }
        return errors
    }
}
