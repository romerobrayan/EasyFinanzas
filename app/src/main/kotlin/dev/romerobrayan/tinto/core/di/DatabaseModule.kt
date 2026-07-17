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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TintoDatabase =
        Room.databaseBuilder(context, TintoDatabase::class.java, "tinto.db").build()

    @Provides
    fun providePendingTransactionDao(database: TintoDatabase): PendingTransactionDao =
        database.pendingTransactionDao()
}
