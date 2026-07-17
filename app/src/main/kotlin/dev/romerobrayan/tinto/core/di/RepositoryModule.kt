package dev.romerobrayan.tinto.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.romerobrayan.tinto.core.data.auth.FirebaseAuthRepository
import dev.romerobrayan.tinto.core.data.capture.SharedPreferencesCaptureSettings
import dev.romerobrayan.tinto.core.data.repository.LocalPendingTransactionRepository
import dev.romerobrayan.tinto.core.data.repository.SyncedCardRepository
import dev.romerobrayan.tinto.core.data.repository.SyncedCategoryRepository
import dev.romerobrayan.tinto.core.data.repository.SyncedReminderRepository
import dev.romerobrayan.tinto.core.data.repository.SyncedTransactionRepository
import dev.romerobrayan.tinto.core.domain.repository.AuthRepository
import dev.romerobrayan.tinto.core.domain.repository.CaptureSettingsRepository
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import dev.romerobrayan.tinto.core.domain.repository.PendingTransactionRepository
import dev.romerobrayan.tinto.core.domain.repository.ReminderRepository
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

    // Capture staging is deliberately NOT session-routed: device-local Room,
    // shared by demo and signed-in sessions (TASK_SPRINT_3_CAPTURE.md).
    @Binds
    abstract fun bindPendingTransactionRepository(
        impl: LocalPendingTransactionRepository,
    ): PendingTransactionRepository

    @Binds
    abstract fun bindCaptureSettingsRepository(
        impl: SharedPreferencesCaptureSettings,
    ): CaptureSettingsRepository
}
