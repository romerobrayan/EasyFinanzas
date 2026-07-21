package dev.romerobrayan.tinto.navigation

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes — one object per destination.

@Serializable
data object DashboardRoute

@Serializable
data object MovementsRoute

/**
 * [transactionId] null = add a new movement; non-null = edit that movement.
 * [pendingId] non-null = review/confirm that captured pending item (the form
 * pre-fills from the parse and Confirmar promotes it to the ledger).
 */
@Serializable
data class AddTransactionRoute(
    val transactionId: String? = null,
    val pendingId: String? = null,
)

/** The pending-capture inbox (Sprint 3): review, confirm or discard in batch. */
@Serializable
data object PendingRoute

/** The automation-rules manager (Sprint 5): pause/resume or delete rules. */
@Serializable
data object RecurringRulesRoute

@Serializable
data object RemindersRoute

@Serializable
data object ProfileRoute
