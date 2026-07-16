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
) : AuthRepository {

    private val firebaseAuth: FirebaseAuth get() = Firebase.auth

    private val demoMode = MutableStateFlow(false)

    override val session: StateFlow<UserSession> =
        combine(firebaseUsers(), demoMode) { user, demo ->
            when {
                user != null -> UserSession.SignedIn(user.toAuthUser())
                demo -> UserSession.Demo
                else -> UserSession.SignedOut
            }
        }
            // Keeps crash reports / analytics tied to the uid even when the
            // session was restored from disk rather than an explicit login.
            .onEach { analytics.setUser((it as? UserSession.SignedIn)?.user?.uid) }
            .stateIn(appScope, SharingStarted.Eagerly, UserSession.Loading)

    override suspend fun signInWithGoogle(idToken: String): AuthUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = requireNotNull(firebaseAuth.signInWithCredential(credential).await().user) {
            "Firebase returned a session without a user"
        }
        demoMode.value = false
        return user.toAuthUser()
    }

    override fun enterDemoMode() {
        demoMode.value = true
    }

    override fun signOut() {
        firebaseAuth.signOut()
        demoMode.value = false
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
