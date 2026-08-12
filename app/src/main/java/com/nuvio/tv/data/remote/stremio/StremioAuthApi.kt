package com.nuvio.tv.data.remote.stremio

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Stremio account API.
 *
 * Wire format confirmed from Stremio/stremio-api-client (apiClient.js):
 *   POST https://api.strem.io/api/{method}
 *   body: JSON of { authKey, ...params }   (authKey null before login)
 *   response: { result, error }            (error non-null OR HTTP != 200 => failure)
 *
 * This is a public API with no client keys, so it works on any sideloaded /
 * self-signed build — unlike Nuvio Sync's Supabase-backed auth.
 *
 * Base URL for the Retrofit instance must be "https://api.strem.io/".
 */
interface StremioAuthApi {

    @POST("api/login")
    suspend fun login(@Body body: StremioAuthRequest): StremioAuthEnvelope

    @POST("api/register")
    suspend fun register(@Body body: StremioAuthRequest): StremioAuthEnvelope

    @POST("api/logout")
    suspend fun logout(@Body body: StremioLogoutRequest): StremioLogoutEnvelope
}

/**
 * Login/register request. authKey is always sent (null pre-auth) to mirror
 * the reference client's `Object.assign({ authKey }, params)`.
 */
@JsonClass(generateAdapter = true)
data class StremioAuthRequest(
    val email: String,
    val password: String,
    val authKey: String? = null
)

@JsonClass(generateAdapter = true)
data class StremioLogoutRequest(
    val authKey: String?
)

/**
 * Standard Stremio envelope. On success `result` is populated; on failure
 * `error` is populated (and the repository throws). HTTP non-200 is surfaced
 * by Retrofit as an HttpException before we ever parse this.
 */
@JsonClass(generateAdapter = true)
data class StremioAuthEnvelope(
    val result: StremioAuthResult? = null,
    val error: StremioApiError? = null
)

@JsonClass(generateAdapter = true)
data class StremioLogoutEnvelope(
    val result: Any? = null,
    val error: StremioApiError? = null
)

@JsonClass(generateAdapter = true)
data class StremioAuthResult(
    val authKey: String,
    val user: StremioUser? = null
)

@JsonClass(generateAdapter = true)
data class StremioUser(
    val _id: String? = null,
    val email: String? = null,
    val avatar: String? = null
)

@JsonClass(generateAdapter = true)
data class StremioApiError(
    val code: Int? = null,
    val message: String? = null
)
