package dev.romerobrayan.tinto.core.common

import kotlinx.coroutines.CoroutineDispatcher

/** Injected dispatchers so callers never hardcode `Dispatchers.*`. */
data class TintoDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
)
