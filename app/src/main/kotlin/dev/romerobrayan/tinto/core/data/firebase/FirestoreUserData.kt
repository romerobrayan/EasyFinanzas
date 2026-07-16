package dev.romerobrayan.tinto.core.data.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Per-user Firestore layout — everything hangs under the uid so the security
 * rules can isolate accounts with a single match block:
 *
 *   users/{uid}/transactions/{id}
 *   users/{uid}/categories/{id}
 *   users/{uid}/cards/{id}
 *   users/{uid}/reminders/{id}
 *
 * Firestore's on-disk cache is enabled by default on Android, so these
 * listeners emit immediately from local data and the SDK syncs writes in the
 * background when connectivity returns — the app keeps working offline.
 */
internal fun userCollection(uid: String, collection: String): CollectionReference =
    Firebase.firestore.collection("users").document(uid).collection(collection)

/**
 * The query as a cold flow of document lists. Listener failures are recorded
 * as non-fatals and surface as an empty list instead of crashing the UI.
 */
internal fun Query.listenAsList(analytics: TintoAnalytics): Flow<List<DocumentSnapshot>> =
    callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            when {
                error != null -> {
                    analytics.recordError(error)
                    trySend(emptyList())
                }

                snapshot != null -> trySend(snapshot.documents)
            }
        }
        awaitClose { registration.remove() }
    }
