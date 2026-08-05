package dev.romerobrayan.tinto.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.romerobrayan.tinto.core.data.local.CardDao
import dev.romerobrayan.tinto.core.data.local.MIGRATION_2_3
import dev.romerobrayan.tinto.core.data.local.PendingTransactionDao
import dev.romerobrayan.tinto.core.data.local.RecurringRuleDao
import dev.romerobrayan.tinto.core.data.local.ReminderDao
import dev.romerobrayan.tinto.core.data.local.TintoDatabase
import dev.romerobrayan.tinto.core.data.local.TransactionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Migrations are real from version 3 on, because that is where the
     * device-local ledger of a no-account user starts living here — rows that
     * exist nowhere else and that a schema change must never drop.
     *
     * The destructive fallback survives for version 1 and 2 only: those
     * databases held nothing but `pending_transactions`, capture staging the
     * user can re-scan. Dropping it costs a re-scan; crashing on launch costs
     * them the app.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TintoDatabase =
        Room.databaseBuilder(context, TintoDatabase::class.java, "tinto.db")
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
            .build()

    @Provides
    fun providePendingTransactionDao(database: TintoDatabase): PendingTransactionDao =
        database.pendingTransactionDao()

    @Provides
    fun provideTransactionDao(database: TintoDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideCardDao(database: TintoDatabase): CardDao = database.cardDao()

    @Provides
    fun provideReminderDao(database: TintoDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun provideRecurringRuleDao(database: TintoDatabase): RecurringRuleDao =
        database.recurringRuleDao()
}
