package dev.romerobrayan.tinto.navigation

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes — one object per destination.

@Serializable
data object DashboardRoute

@Serializable
data object MovementsRoute

/** [transactionId] null = add a new movement; non-null = edit that movement. */
@Serializable
data class AddTransactionRoute(val transactionId: String? = null)

@Serializable
data object RemindersRoute

@Serializable
data object ProfileRoute
