package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.data.firebase.listenAsList
import dev.romerobrayan.tinto.core.data.firebase.toCard
import dev.romerobrayan.tinto.core.data.firebase.toFirestoreMap
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
 * Registered cards routed by session: signed-in reads/writes
 * `users/{uid}/cards`, a local profile reads/writes this device's Room
 * database, demo mode uses the in-memory samples. Cloud writes are
 * fire-and-forget so card management keeps working offline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SyncedCardRepository @Inject constructor(
    private val auth: AuthRepository,
    private val local: LocalCardRepository,
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

                is UserSession.Local -> local.observeCards()
                UserSession.Demo -> demo.observeCards()
                else -> flowOf(emptyList())
            }
        }

    override suspend fun addCard(card: Card) {
        when (val session = auth.session.value) {
            is UserSession.SignedIn ->
                userCollection(session.user.uid, "cards")
                    .document(card.id)
                    .set(card.toFirestoreMap())

            is UserSession.Local -> local.addCard(card)
            UserSession.Demo -> demo.addCard(card)
            else -> Unit
        }
    }

    override suspend fun updateCard(card: Card) {
        when (val session = auth.session.value) {
            is UserSession.SignedIn ->
                userCollection(session.user.uid, "cards")
                    .document(card.id)
                    .set(card.toFirestoreMap())

            is UserSession.Local -> local.updateCard(card)
            UserSession.Demo -> demo.updateCard(card)
            else -> Unit
        }
    }

    override suspend fun deleteCard(cardId: String) {
        when (val session = auth.session.value) {
            is UserSession.SignedIn ->
                userCollection(session.user.uid, "cards")
                    .document(cardId)
                    .delete()

            is UserSession.Local -> local.deleteCard(cardId)
            UserSession.Demo -> demo.deleteCard(cardId)
            else -> Unit
        }
    }
}
