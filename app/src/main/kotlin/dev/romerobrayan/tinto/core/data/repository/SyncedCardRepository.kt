package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.data.firebase.listenAsList
import dev.romerobrayan.tinto.core.data.firebase.toCard
import dev.romerobrayan.tinto.core.data.firebase.userCollection
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.UserSession
import dev.romerobrayan.tinto.core.domain.repository.AuthRepository
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Registered cards routed by session. Real accounts start with none —
 * card management UI arrives with the capture sprint.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SyncedCardRepository @Inject constructor(
    private val auth: AuthRepository,
    private val demo: InMemoryCardRepository,
    private val analytics: TintoAnalytics,
) : CardRepository {

    override fun observeCards(): Flow<List<Card>> =
        auth.session.flatMapLatest { session ->
            when (session) {
                is UserSession.SignedIn ->
                    userCollection(session.user.uid, "cards")
                        .listenAsList(analytics)
                        .map { docs -> docs.mapNotNull { it.toCard() } }

                UserSession.Demo -> demo.observeCards()
                else -> flowOf(emptyList())
            }
        }
}
