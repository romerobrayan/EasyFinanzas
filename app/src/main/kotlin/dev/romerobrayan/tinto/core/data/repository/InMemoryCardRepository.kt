package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.common.MockData
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Demo-mode sample cards; served by [SyncedCardRepository] during the demo session. */
@Singleton
class InMemoryCardRepository @Inject constructor() : CardRepository {

    private val cards = MutableStateFlow(MockData.cards)

    override fun observeCards(): Flow<List<Card>> = cards.asStateFlow()

    override suspend fun addCard(card: Card) {
        cards.update { it + card }
    }

    override suspend fun updateCard(card: Card) {
        cards.update { list -> list.map { if (it.id == card.id) card else it } }
    }

    override suspend fun deleteCard(cardId: String) {
        cards.update { list -> list.filterNot { it.id == cardId } }
    }
}
