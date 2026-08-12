package com.nuvio.tv.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the Stremio session (authKey + basic user identity).
 *
 * The authKey is a bearer session token returned by /api/login and sent on
 * every subsequent Stremio API call. It's the whole session — treat it like a
 * credential. Stored in its own DataStore file, separate from Nuvio's own
 * settings stores, so the two account systems never collide.
 */
@Singleton
class StremioAuthDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.stremioAuthDataStore by preferencesDataStore(name = "stremio_auth")

    private object Keys {
        val AUTH_KEY = stringPreferencesKey("stremio_auth_key")
        val USER_ID = stringPreferencesKey("stremio_user_id")
        val USER_EMAIL = stringPreferencesKey("stremio_user_email")
    }

    /** Current auth key, or null when logged out. */
    val authKey: Flow<String?> = context.stremioAuthDataStore.data
        .map { prefs -> prefs[Keys.AUTH_KEY] }

    val userEmail: Flow<String?> = context.stremioAuthDataStore.data
        .map { prefs -> prefs[Keys.USER_EMAIL] }

    /** True when a Stremio session is active. */
    val isLoggedIn: Flow<Boolean> = context.stremioAuthDataStore.data
        .map { prefs -> !prefs[Keys.AUTH_KEY].isNullOrBlank() }

    suspend fun saveSession(authKey: String, userId: String?, email: String?) {
        context.stremioAuthDataStore.edit { prefs ->
            prefs[Keys.AUTH_KEY] = authKey
            if (userId != null) prefs[Keys.USER_ID] = userId else prefs.remove(Keys.USER_ID)
            if (email != null) prefs[Keys.USER_EMAIL] = email else prefs.remove(Keys.USER_EMAIL)
        }
    }

    suspend fun clear() {
        context.stremioAuthDataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_KEY)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_EMAIL)
        }
    }
}
