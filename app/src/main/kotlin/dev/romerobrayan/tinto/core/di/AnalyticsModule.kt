package dev.romerobrayan.tinto.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.data.analytics.FirebaseTintoAnalytics

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    abstract fun bindAnalytics(impl: FirebaseTintoAnalytics): TintoAnalytics
}
