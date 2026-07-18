package dev.romerobrayan.tinto.feature.profile

import dev.romerobrayan.tinto.core.domain.model.Card

/** The card bottom-sheet form; non-null while the sheet is open. */
data class CardFormUiState(
    /** null = adding a new card; non-null = editing that card. */
    val editingCardId: String? = null,
    val bank: String = "",
    val last4: String = "",
    val label: String = "",
    /** Only populated after a submit attempt, so the form starts clean. */
    val errors: Set<CardFormValidator.Error> = emptySet(),
)

data class ProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val cards: List<Card> = emptyList(),
    /** True while exploring with sample data (no cloud persistence). */
    val isDemo: Boolean = false,
    /** Whether the user opted in to SMS capture (Sprint 3). */
    val smsCaptureEnabled: Boolean = false,
    val cardForm: CardFormUiState? = null,
)
