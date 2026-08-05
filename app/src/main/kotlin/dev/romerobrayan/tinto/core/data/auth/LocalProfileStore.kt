package dev.romerobrayan.tinto.core.data.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The whole identity of a no-account user: a nickname in SharedPreferences.
 * Nothing here is ever uploaded — it exists so the ledger has a name to show
 * and so the session survives a process restart, which is what makes the
 * device-local mode a real account rather than a long demo.
 *
 * [activeName] and [rememberedName] are separate on purpose: leaving the local
 * profile ends the session but keeps the nickname (and the Room data behind
 * it), so coming back is one tap instead of a re-introduction.
 */
@Singleton
class LocalProfileStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _rememberedName = MutableStateFlow(prefs.getString(KEY_NAME, null))

    /** Last nickname used locally, kept across sign-outs for prefill. */
    val rememberedName: StateFlow<String?> = _rememberedName.asStateFlow()

    private val _activeName = MutableStateFlow(
        prefs.getString(KEY_NAME, null).takeIf { prefs.getBoolean(KEY_ACTIVE, false) },
    )

    /** Nickname of the running local session; null when it is not the active one. */
    val activeName: StateFlow<String?> = _activeName.asStateFlow()

    fun activate(displayName: String) {
        prefs.edit().putString(KEY_NAME, displayName).putBoolean(KEY_ACTIVE, true).apply()
        _rememberedName.value = displayName
        _activeName.value = displayName
    }

    /** Ends the local session; the nickname and the device data stay put. */
    fun deactivate() {
        prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
        _activeName.value = null
    }

    private companion object {
        const val PREFS_NAME = "local_profile"
        const val KEY_NAME = "display_name"
        const val KEY_ACTIVE = "active"
    }
}
