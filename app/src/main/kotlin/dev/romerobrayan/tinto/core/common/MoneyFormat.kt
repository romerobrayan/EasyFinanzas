package dev.romerobrayan.tinto.core.common

import dev.romerobrayan.tinto.core.domain.model.Money

/**
 * Single source of COP formatting: `$1.842.500` — dot-grouped, no decimals
 * (whole pesos for display). Sign and color are applied by `MoneyText`; this
 * always formats the absolute value.
 */
object MoneyFormat {

    fun format(money: Money): String {
        val pesos = money.abs().cents / CENTS_PER_PESO
        val grouped = pesos.toString()
            .reversed()
            .chunked(GROUP_SIZE)
            .joinToString(".")
            .reversed()
        return "$" + grouped
    }

    /**
     * Bare digits for text-to-speech: `1842500`, which a Spanish engine reads as
     * "un millón ochocientos cuarenta y dos mil quinientos". [format]'s output is
     * built for the eye — an engine reads its `$` and dot grouping as literal
     * tokens or as decimals. The word "pesos" comes from the string resource.
     */
    fun spokenPesos(money: Money): String =
        (money.abs().cents / CENTS_PER_PESO).toString()

    private const val CENTS_PER_PESO = 100L
    private const val GROUP_SIZE = 3
}
