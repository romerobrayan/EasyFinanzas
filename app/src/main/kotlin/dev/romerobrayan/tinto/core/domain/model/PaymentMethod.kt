package dev.romerobrayan.tinto.core.domain.model

/**
 * How a movement was paid/received. [TRANSFER] (Sprint 5) is offered only for
 * incomes — a bank transfer/deposit with no card. Additive: persisted rows
 * written before it exist only carry CARD/CASH.
 */
enum class PaymentMethod { CARD, CASH, TRANSFER }
