package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.common.MockData
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** TODO(sprint-2): replace with the Room-backed implementation. */
@Singleton
class InMemoryCardRepository @Inject constructor() : CardRepository {

    private val cards = MutableStateFlow(MockData.cards)

    override fun observeCards(): Flow<List<Card>> = cards.asStateFlow()
}
