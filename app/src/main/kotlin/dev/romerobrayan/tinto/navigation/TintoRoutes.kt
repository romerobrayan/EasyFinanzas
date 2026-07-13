package dev.romerobrayan.tinto.navigation

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes — one object per destination.

@Serializable
data object DashboardRoute

@Serializable
data object MovementsRoute

@Serializable
data object AddTransactionRoute

@Serializable
data object RemindersRoute

@Serializable
data object ProfileRoute
