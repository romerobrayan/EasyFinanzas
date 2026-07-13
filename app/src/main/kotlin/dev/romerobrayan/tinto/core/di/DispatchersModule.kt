package dev.romerobrayan.tinto.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.romerobrayan.tinto.core.common.TintoDispatchers
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Singleton
    fun provideDispatchers(): TintoDispatchers = TintoDispatchers(
        io = Dispatchers.IO,
        default = Dispatchers.Default,
    )
}
