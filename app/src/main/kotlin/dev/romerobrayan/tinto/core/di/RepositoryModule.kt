package dev.romerobrayan.tinto.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.romerobrayan.tinto.core.data.auth.FirebaseAuthRepository
import dev.romerobrayan.tinto.core.data.capture.SmsCaptureManager
import dev.romerobrayan.tinto.core.data.capture.parser.RuleBasedTransactionParser
import dev.romerobrayan.tinto.core.data.repository.RoomPendingTransactionRepository
import dev.romerobrayan.tinto.core.data.repository.SyncedCardRepository
import dev.romerobrayan.tinto.core.data.repository.SyncedCategoryRepository
import dev.romerobrayan.tinto.core.data.repository.SyncedReminderRepository
import dev.romerobrayan.tinto.core.data.repository.SyncedTransactionRepository
import dev.romerobrayan.tinto.core.domain.repository.AuthRepository
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import dev.romerobrayan.tinto.core.domain.repository.PendingTransactionRepository
import dev.romerobrayan.tinto.core.domain.repository.ReminderRepository
import dev.romerobrayan.tinto.core.domain.repository.SmsCapture
import dev.romerobrayan.tinto.core.domain.repository.TransactionParser
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository

/**
 * Binds the domain contracts to the session-routed implementations: Cloud
 * Firestore under `users/{uid}` when signed in, the in-memory sample data in
 * demo mode. The InMemory* repositories stay as the demo sources inside the
 * Synced* ones.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    abstract fun bindTransactionRepository(impl: SyncedTransactionRepository): TransactionRepository

    @Binds
    abstract fun bindCardRepository(impl: SyncedCardRepository): CardRepository

    @Binds
    abstract fun bindCategoryRepository(impl: SyncedCategoryRepository): CategoryRepository

    @Binds
    abstract fun bindReminderRepository(impl: SyncedReminderRepository): ReminderRepository

    // Capture pipeline: the staging store is device-local (Room) on purpose —
    // no Synced*/InMemory* split, see PendingTransactionRepository docs.
    @Binds
    abstract fun bindPendingTransactionRepository(
        impl: RoomPendingTransactionRepository,
    ): PendingTransactionRepository

    @Binds
    abstract fun bindTransactionParser(impl: RuleBasedTransactionParser): TransactionParser

    @Binds
    abstract fun bindSmsCapture(impl: SmsCaptureManager): SmsCapture
}
