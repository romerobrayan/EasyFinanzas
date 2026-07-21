package dev.romerobrayan.tinto.core.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import dev.romerobrayan.tinto.core.common.MockData
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.data.firebase.listenAsList
import dev.romerobrayan.tinto.core.data.firebase.toCategory
import dev.romerobrayan.tinto.core.data.firebase.toFirestoreMap
import dev.romerobrayan.tinto.core.data.firebase.userCollection
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.UserSession
import dev.romerobrayan.tinto.core.domain.repository.AuthRepository
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Categories routed by session. A fresh account is seeded once with the
 * system category set (fixed ids, so re-seeding is idempotent); until the
 * seed lands the same set is served locally so pickers are never empty.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SyncedCategoryRepository @Inject constructor(
    private val auth: AuthRepository,
    private val demo: InMemoryCategoryRepository,
    private val analytics: TintoAnalytics,
) : CategoryRepository {

    private val seededUids = ConcurrentHashMap.newKeySet<String>()
    private val backfilledUids = ConcurrentHashMap.newKeySet<String>()

    override fun observeCategories(): Flow<List<Category>> =
        auth.session.flatMapLatest { session ->
            when (session) {
                is UserSession.SignedIn -> cloudCategories(session.user.uid)
                UserSession.Demo -> demo.observeCategories()
                else -> flowOf(emptyList())
            }
        }

    private fun cloudCategories(uid: String): Flow<List<Category>> =
        userCollection(uid, "categories")
            .listenAsList(analytics)
            .map { docs -> docs.mapNotNull { it.toCategory() } }
            .onEach { categories ->
                if (categories.isEmpty()) {
                    seedSystemCategories(uid)
                } else {
                    backfillSystemCategories(uid, categories)
                }
            }
            .map { categories -> categories.ifEmpty { MockData.categories } }

    private fun seedSystemCategories(uid: String) {
        if (!seededUids.add(uid)) return
        val batch = Firebase.firestore.batch()
        MockData.categories.forEach { category ->
            batch.set(userCollection(uid, "categories").document(category.id), category.toFirestoreMap())
        }
        batch.commit()
    }

    /**
     * Idempotent reconciliation for accounts seeded before Sprint 5: the seed
     * only runs on an empty collection, so existing accounts never receive the
     * categories added later (Hogar/Emergencias + the income set). Upsert the
     * missing system categories by their fixed id — user-created categories
     * (different ids) are left untouched, and once written they stop being
     * "missing", so this converges after one write and is safe to run every
     * launch. Guarded per-uid so a burst of emissions writes at most once.
     */
    private fun backfillSystemCategories(uid: String, existing: List<Category>) {
        if (!backfilledUids.add(uid)) return
        val existingIds = existing.mapTo(mutableSetOf()) { it.id }
        val missing = MockData.categories.filter { it.isSystem && it.id !in existingIds }
        if (missing.isEmpty()) return
        val batch = Firebase.firestore.batch()
        missing.forEach { category ->
            batch.set(userCollection(uid, "categories").document(category.id), category.toFirestoreMap())
        }
        batch.commit()
    }
}
