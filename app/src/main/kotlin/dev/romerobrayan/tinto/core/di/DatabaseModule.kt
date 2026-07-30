package dev.romerobrayan.tinto.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.romerobrayan.tinto.core.data.local.PendingTransactionDao
import dev.romerobrayan.tinto.core.data.local.TintoDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Destructive fallback on purpose: `pending_transactions` is capture staging
     * — parsed movements the user has not confirmed yet — never the committed
     * ledger, which lives in Cloud Firestore. Dropping unreviewed captures on a
     * schema change costs the user a re-scan; crashing on launch costs them the
     * app. Revisit if this database ever holds data that is not re-derivable.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TintoDatabase =
        Room.databaseBuilder(context, TintoDatabase::class.java, "tinto.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun providePendingTransactionDao(database: TintoDatabase): PendingTransactionDao =
        database.pendingTransactionDao()
}
