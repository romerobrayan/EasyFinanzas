package dev.romerobrayan.tinto.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.romerobrayan.tinto.core.data.capture.parser.RuleBasedTransactionParser
import dev.romerobrayan.tinto.core.data.capture.parser.TintoIssuerRules
import dev.romerobrayan.tinto.core.domain.repository.TransactionParser
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CaptureModule {

    /** Rule sets are data: the parser is just the engine over [TintoIssuerRules]. */
    @Provides
    @Singleton
    fun provideTransactionParser(): TransactionParser =
        RuleBasedTransactionParser(TintoIssuerRules.all)
}
