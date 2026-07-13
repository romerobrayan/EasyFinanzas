package dev.romerobrayan.tinto.feature.profile

import dev.romerobrayan.tinto.core.domain.model.Card

data class ProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val cards: List<Card> = emptyList(),
)
