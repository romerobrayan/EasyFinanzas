package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.data.local.CardDao
import dev.romerobrayan.tinto.core.data.local.toDomain
import dev.romerobrayan.tinto.core.data.local.toEntity
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Registered cards of a no-account user; device-local, never uploaded. */
@Singleton
class LocalCardRepository @Inject constructor(
    private val dao: CardDao,
) : CardRepository {

    override fun observeCards(): Flow<List<Card>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun addCard(card: Card) {
        dao.upsert(card.toEntity())
    }

    override suspend fun updateCard(card: Card) {
        dao.upsert(card.toEntity())
    }

    override suspend fun deleteCard(cardId: String) {
        dao.delete(cardId)
    }
}
