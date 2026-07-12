package dev.romerobrayan.tinto.core.domain.model

/**
 * A registered payment card. [last4] stores the last FOUR digits — Colombian
 * bank notifications reference cards as `****1234`, and matching captured
 * transactions to a card depends on having all four.
 */
data class Card(
    val id: String,
    val bank: String,
    val last4: String,
    val label: String?,
)
