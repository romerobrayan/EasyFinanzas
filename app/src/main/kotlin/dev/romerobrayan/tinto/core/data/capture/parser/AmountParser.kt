package dev.romerobrayan.tinto.core.data.capture.parser

import dev.romerobrayan.tinto.core.domain.model.Money

/**
 * How an issuer writes amounts. The separator convention is a property of
 * each issuer rule, never a global setting: Bancolombia writes US style
 * (`$152,372.00`), 1CERO1 and Nu write Colombian style (`$152.372,00`).
 */
enum class AmountConvention {
    /** `,` groups thousands, `.` starts decimals — Bancolombia. */
    DOT_DECIMAL,

    /** `.` groups thousands, `,` starts decimals — 1CERO1, Nu. */
    COMMA_DECIMAL,
}

/**
 * Normalizes an issuer-formatted amount string into [Money] with integer
 * math only — never a Double. Handles the no-decimal forms (`$10,000`,
 * `$1,487,941`) in both conventions.
 */
object AmountParser {

    /**
     * @param raw the amount as matched, with or without `$`/spaces.
     * @param convention the issuer's separator convention; null falls back to
     * the heuristic "a separator followed by exactly two trailing digits is
     * the decimal, everything else is grouping" (COP displays whole pesos).
     */
    fun parse(raw: String, convention: AmountConvention?): Money? {
        val cleaned = raw.replace("$", "").replace(" ", "").trim()
        if (cleaned.isEmpty() || cleaned.any { !it.isDigit() && it != '.' && it != ',' }) return null

        val decimalSeparator = when (convention) {
            AmountConvention.DOT_DECIMAL -> '.'
            AmountConvention.COMMA_DECIMAL -> ','
            null -> guessDecimalSeparator(cleaned)
        }
        val groupSeparator = if (decimalSeparator == '.') ',' else '.'

        val withoutGroups = cleaned.replace(groupSeparator.toString(), "")
        val parts = withoutGroups.split(decimalSeparator)
        if (parts.size > 2 || parts.any { it.isEmpty() }) return null

        val pesos = parts[0].toLongOrNull() ?: return null
        val centavos = when (parts.size) {
            2 -> parts[1].takeIf { it.length <= 2 }?.padEnd(2, '0')?.toLongOrNull() ?: return null
            else -> 0L
        }
        return Money(pesos * CENTS_PER_PESO + centavos)
    }

    /** A separator with exactly two trailing digits reads as the decimal. */
    private fun guessDecimalSeparator(cleaned: String): Char {
        val lastSeparatorIndex = cleaned.indexOfLast { it == '.' || it == ',' }
        if (lastSeparatorIndex < 0) return '.'
        val trailing = cleaned.length - lastSeparatorIndex - 1
        return if (trailing == 2) cleaned[lastSeparatorIndex] else if (cleaned[lastSeparatorIndex] == '.') ',' else '.'
    }

    private const val CENTS_PER_PESO = 100L
}
