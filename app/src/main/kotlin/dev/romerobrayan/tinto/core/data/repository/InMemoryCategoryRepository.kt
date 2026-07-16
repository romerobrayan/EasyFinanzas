package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.common.MockData
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Demo-mode categories; also the system set [SyncedCategoryRepository] seeds
 * into a fresh account's `users/{uid}/categories` collection.
 */
@Singleton
class InMemoryCategoryRepository @Inject constructor() : CategoryRepository {

    private val categories = MutableStateFlow(MockData.categories)

    override fun observeCategories(): Flow<List<Category>> = categories.asStateFlow()
}
