package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.Card
import kotlinx.coroutines.flow.Flow

interface CardRepository {

    fun observeCards(): Flow<List<Card>>

    suspend fun addCard(card: Card)

    suspend fun updateCard(card: Card)

    /** Removes the card only — transactions referencing it stay untouched. */
    suspend fun deleteCard(cardId: String)
}
