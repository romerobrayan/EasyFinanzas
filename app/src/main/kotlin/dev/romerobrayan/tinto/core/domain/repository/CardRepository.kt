package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.Card
import kotlinx.coroutines.flow.Flow

interface CardRepository {

    fun observeCards(): Flow<List<Card>>
}
