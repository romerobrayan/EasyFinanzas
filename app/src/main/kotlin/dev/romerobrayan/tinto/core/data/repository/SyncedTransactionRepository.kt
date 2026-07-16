package dev.romerobrayan.tinto.core.data.repository

import com.google.firebase.firestore.Query
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.data.firebase.listenAsList
import dev.romerobrayan.tinto.core.data.firebase.toFirestoreMap
import dev.romerobrayan.tinto.core.data.firebase.toTransaction
import dev.romerobrayan.tinto.core.data.firebase.userCollection
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.UserSession
import dev.romerobrayan.tinto.core.domain.repository.AuthRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * The user's ledger, routed by session: signed-in reads/writes
 * `users/{uid}/transactions` in Cloud Firestore (offline cache included),
 * demo mode falls back to the in-memory sample ledger.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SyncedTransactionRepository @Inject constructor(
    private val auth: AuthRepository,
    private val demo: InMemoryTransactionRepository,
    private val analytics: TintoAnalytics,
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        auth.session.flatMapLatest { session ->
            when (session) {
                is UserSession.SignedIn ->
                    userCollection(session.user.uid, "transactions")
                        .orderBy("occurredAt", Query.Direction.DESCENDING)
                        .listenAsList(analytics)
                        .map { docs -> docs.mapNotNull { it.toTransaction() } }

                UserSession.Demo -> demo.observeTransactions()
                else -> flowOf(emptyList())
            }
        }

    override suspend fun addTransaction(transaction: Transaction) {
        when (val session = auth.session.value) {
            is UserSession.SignedIn ->
                // Deliberately not awaited: Firestore applies the write to the
                // local cache immediately (listeners refire at once) and syncs
                // to the server when online — awaiting would hang offline.
                userCollection(session.user.uid, "transactions")
                    .document(transaction.id)
                    .set(transaction.toFirestoreMap())

            UserSession.Demo -> demo.addTransaction(transaction)
            else -> Unit
        }
    }
}
