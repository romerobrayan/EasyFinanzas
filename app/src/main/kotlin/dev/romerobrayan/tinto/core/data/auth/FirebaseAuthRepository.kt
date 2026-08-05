package dev.romerobrayan.tinto.core.data.auth

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.AuthUser
import dev.romerobrayan.tinto.core.domain.model.UserSession
import dev.romerobrayan.tinto.core.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseAuthRepository @Inject constructor(
    appScope: CoroutineScope,
    private val analytics: TintoAnalytics,
    private val localProfile: LocalProfileStore,
) : AuthRepository {

    private val firebaseAuth: FirebaseAuth get() = Firebase.auth

    private val demoMode = MutableStateFlow(false)

    init {
        // Applied synchronously at construction (this runs from
        // Application.onCreate, before any screen composes) so relaunching
        // into a local profile never has a window where collection is on.
        analytics.setCollectionEnabled(localProfile.activeName.value == null)
    }

    override val localProfileName: StateFlow<String?> = localProfile.rememberedName

    override val session: StateFlow<UserSession> =
        combine(firebaseUsers(), localProfile.activeName, demoMode) { user, localName, demo ->
            when {
                // A Firebase user wins: signing in from a local profile moves
                // the user to their cloud ledger (the device data stays put).
                user != null -> UserSession.SignedIn(user.toAuthUser())
                localName != null -> UserSession.Local(localName)
                demo -> UserSession.Demo
                else -> UserSession.SignedOut
            }
        }
            // Keeps crash reports / analytics tied to the uid even when the
            // session was restored from disk rather than an explicit login —
            // and keeps a local profile out of both entirely.
            .onEach { session ->
                analytics.setUser((session as? UserSession.SignedIn)?.user?.uid)
                analytics.setCollectionEnabled(session !is UserSession.Local)
            }
            .stateIn(appScope, SharingStarted.Eagerly, UserSession.Loading)

    override suspend fun signInWithGoogle(idToken: String): AuthUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = requireNotNull(firebaseAuth.signInWithCredential(credential).await().user) {
            "Firebase returned a session without a user"
        }
        demoMode.value = false
        localProfile.deactivate()
        return user.toAuthUser()
    }

    override fun enterDemoMode() {
        demoMode.value = true
    }

    override fun continueLocally(displayName: String) {
        // Collection goes off *before* the session flips. The promise of this
        // mode is that nothing about it reaches Google — including the screen
        // views that fire the instant the shell composes, and including any
        // "someone chose local mode" event, which is why none is logged here.
        analytics.setCollectionEnabled(false)
        demoMode.value = false
        localProfile.activate(displayName)
    }

    override fun signOut() {
        firebaseAuth.signOut()
        demoMode.value = false
        // Leaving is not deleting: the nickname and the device-local ledger
        // survive so the user can come back into the same data.
        localProfile.deactivate()
    }

    /** Auth state as a flow; the listener fires immediately with the persisted user. */
    private fun firebaseUsers() = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    private fun FirebaseUser.toAuthUser(): AuthUser =
        AuthUser(uid = uid, displayName = displayName, email = email)
}
