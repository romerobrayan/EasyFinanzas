package dev.romerobrayan.tinto.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.romerobrayan.tinto.core.common.TintoDispatchers
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Singleton
    fun provideDispatchers(): TintoDispatchers = TintoDispatchers(
        io = Dispatchers.IO,
        default = Dispatchers.Default,
    )

    /** App-lifetime scope for singletons that keep state flows alive (e.g. the auth session). */
    @Provides
    @Singleton
    fun provideApplicationScope(dispatchers: TintoDispatchers): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatchers.default)
}
