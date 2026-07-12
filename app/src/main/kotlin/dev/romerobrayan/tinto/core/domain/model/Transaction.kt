package dev.romerobrayan.tinto.core.domain.model

import kotlinx.datetime.Instant

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Money,
    val method: PaymentMethod,
    /** Registered card this movement belongs to; null when [method] is CASH. */
    val cardId: String?,
    val bank: String?,
    val categoryId: String,
    /** Free-text description / merchant name. */
    val merchant: String?,
    /** When the transaction actually happened. */
    val occurredAt: Instant,
    val source: TransactionSource,
    val createdAt: Instant,
    val updatedAt: Instant,
)
