package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun observeCategories(): Flow<List<Category>>
}
