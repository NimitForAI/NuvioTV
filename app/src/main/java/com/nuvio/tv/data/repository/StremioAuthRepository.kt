package com.nuvio.tv.data.repository

import com.nuvio.tv.data.local.StremioAuthDataStore
import com.nuvio.tv.data.remote.stremio.StremioAuthApi
import com.nuvio.tv.data.remote.stremio.StremioAuthRequest
import com.nuvio.tv.data.remote.stremio.StremioLogoutRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Result of an auth attempt, mapped to something the UI can show directly. */
sealed interface StremioAuthResultState {
    data object Success : StremioAuthResultState
    data class Failure(val message: String) : StremioAuthResultState
}

/**
 * Orchestrates Stremio account login/register/logout and persists the session.
 *
 * Error mapping is deliberate: the Stremio envelope carries a human-readable
 * `error.message` on bad credentials, which we surface as-is; network and HTTP
 * failures get generic, non-leaky messages.
 */
@Singleton
class StremioAuthRepository @Inject constructor(
    private val api: StremioAuthApi,
    private val authStore: StremioAuthDataStore
) {
    val isLoggedIn: Flow<Boolean> = authStore.isLoggedIn
    val userEmail: Flow<String?> = authStore.userEmail

    suspend fun login(email: String, password: String): StremioAuthResultState =
        authenticate(email, password, register = false)

    suspend fun register(email: String, password: String): StremioAuthResultState =
        authenticate(email, password, register = true)

    private suspend fun authenticate(
        email: String,
        password: String,
        register: Boolean
    ): StremioAuthResultState {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            return StremioAuthResultState.Failure("Enter your email and password.")
        }
        // Cheap sanity check so an obviously malformed address doesn't cost a
        // network round-trip to discover.
        if (!trimmedEmail.contains('@') || !trimmedEmail.substringAfterLast('@').contains('.')) {
            return StremioAuthResultState.Failure("Enter a valid email address.")
        }
        return try {
            val request = StremioAuthRequest(email = trimmedEmail, password = password)
            val envelope = if (register) api.register(request) else api.login(request)

            val error = envelope.error
            if (error != null) {
                return StremioAuthResultState.Failure(
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "Stremio rejected the request."
                )
            }
            val result = envelope.result
                ?: return StremioAuthResultState.Failure("Stremio returned no session.")

            authStore.saveSession(
                authKey = result.authKey,
                userId = result.user?._id,
                email = result.user?.email ?: trimmedEmail
            )
            StremioAuthResultState.Success
        } catch (e: HttpException) {
            // Stremio usually returns errors as HTTP 200 + { error }, but if a
            // 4xx/5xx ever carries an error body, surface its message rather
            // than a bare status code.
            val bodyMessage = runCatching {
                e.response()?.errorBody()?.string()
                    ?.let { Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(it)?.groupValues?.get(1) }
            }.getOrNull()
            StremioAuthResultState.Failure(
                bodyMessage?.takeIf { it.isNotBlank() }
                    ?: "Sign-in failed (HTTP ${e.code()}). Check your details and try again."
            )
        } catch (e: IOException) {
            StremioAuthResultState.Failure("Can't reach Stremio. Check your connection.")
        } catch (e: Exception) {
            StremioAuthResultState.Failure("Something went wrong signing in.")
        }
    }

    /**
     * Logs out. Best-effort remote invalidation, but the local session is
     * always cleared so the user isn't stuck "logged in" if the call fails.
     */
    suspend fun logout() {
        val key = authStore.authKey.first()
        try {
            api.logout(StremioLogoutRequest(authKey = key))
        } catch (_: Exception) {
            // Ignore — clear locally regardless.
        }
        authStore.clear()
    }
}
