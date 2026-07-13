package dev.romerobrayan.tinto.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.romerobrayan.tinto.core.data.repository.InMemoryCardRepository
import dev.romerobrayan.tinto.core.data.repository.InMemoryCategoryRepository
import dev.romerobrayan.tinto.core.data.repository.InMemoryReminderRepository
import dev.romerobrayan.tinto.core.data.repository.InMemoryTransactionRepository
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import dev.romerobrayan.tinto.core.domain.repository.ReminderRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository

/**
 * Binds the domain repository contracts to the Sprint-1 in-memory stubs.
 * TODO(sprint-2): point these at the Room-backed implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindTransactionRepository(impl: InMemoryTransactionRepository): TransactionRepository

    @Binds
    abstract fun bindCardRepository(impl: InMemoryCardRepository): CardRepository

    @Binds
    abstract fun bindCategoryRepository(impl: InMemoryCategoryRepository): CategoryRepository

    @Binds
    abstract fun bindReminderRepository(impl: InMemoryReminderRepository): ReminderRepository
}
