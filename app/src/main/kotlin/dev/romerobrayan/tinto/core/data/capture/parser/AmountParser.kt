package dev.romerobrayan.tinto.core.data.capture.parser

import dev.romerobrayan.tinto.core.domain.model.Money

/**
 * Which character groups thousands and which marks decimals in an issuer's
 * amounts. Colombian bank messages are NOT in a single locale — the
 * convention is a property of each issuer rule, never a global setting.
 */
enum class DecimalConvention {
    /** US style — `,` groups, `.` decimals: Bancolombia (`$15,000.00`, `$10,000`). */
    DOT_DECIMAL,

    /** Colombian style — `.` groups, `,` decimals: 1CERO1, Nu (`$3.900,00`). */
    COMMA_DECIMAL,
}

/**
 * Money amounts from message text with integer math only — an amount is never
 * floated through Double on its way to [Money] (CLAUDE.md guardrail).
 */
object AmountParser {

    private val amountToken = Regex("""\$\s?\d[\d.,]*""")

    private const val CENTS_PER_PESO = 100L

    /** Parses the first `$…` token in [body], or null when there is none. */
    fun findAmount(body: String, convention: DecimalConvention?): Money? =
        amountToken.find(body)?.let { parse(it.value, convention) }

    /**
     * Parses one amount token. A null [convention] (unknown sender) falls back
     * to the heuristic "the separator with exactly two trailing digits is the
     * decimal, all others are grouping" — COP is displayed in whole pesos, so
     * two decimals are the only decimals that occur.
     */
    fun parse(text: String, convention: DecimalConvention?): Money? {
        val token = text.trim().removePrefix("$").replace(" ", "").trimEnd('.', ',')
        if (token.isEmpty() || !token.all { it.isDigit() || it == '.' || it == ',' }) return null

        val decimalSeparator: Char? = when (convention) {
            DecimalConvention.DOT_DECIMAL -> '.'
            DecimalConvention.COMMA_DECIMAL -> ','
            null -> heuristicDecimalSeparator(token)
        }
        val groupSeparators = when (decimalSeparator) {
            '.' -> ","
            ',' -> "."
            else -> ".," // no decimals present — every separator groups
        }

        val ungrouped = token.filterNot { it in groupSeparators }
        val parts = ungrouped.split(decimalSeparator ?: return wholePesos(ungrouped))
        return when (parts.size) {
            1 -> wholePesos(parts[0])
            2 -> {
                val pesos = parts[0].toLongOrNull() ?: return null
                val centavos = parts[1].takeIf { it.length == 2 }?.toLongOrNull() ?: return null
                Money(pesos * CENTS_PER_PESO + centavos)
            }
            else -> null
        }
    }

    private fun wholePesos(digits: String): Money? =
        digits.toLongOrNull()?.let { Money(it * CENTS_PER_PESO) }

    /** Null means "no decimal part" — strip every separator as grouping. */
    private fun heuristicDecimalSeparator(token: String): Char? {
        val lastSeparator = token.indexOfLast { it == '.' || it == ',' }
        if (lastSeparator == -1) return null
        val trailingDigits = token.length - lastSeparator - 1
        return if (trailingDigits == 2) token[lastSeparator] else null
    }
}
