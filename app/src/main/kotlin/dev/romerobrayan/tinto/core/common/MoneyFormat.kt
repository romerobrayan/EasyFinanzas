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
        return "$$grouped"
    }

    private const val CENTS_PER_PESO = 100L
    private const val GROUP_SIZE = 3
}
