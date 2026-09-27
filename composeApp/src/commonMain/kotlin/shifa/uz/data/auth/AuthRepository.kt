package shifa.uz.data.auth

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.LOGIN_FAILED
import shifa.uz.data.api.NO_TOKEN_RECEIVED
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.httpClient
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.LoginRequest
import shifa.uz.data.model.LoginResponse
import shifa.uz.data.storage.TokenStorage

private const val KEY_HAS_LAUNCHED = "has_launched_before"

class AuthRepository(
    private val client: HttpClient = httpClient,
    private val tokenStorage: TokenStorage = TokenStorage(),
    private val settings: Settings = Settings(),
) {
    /** A1. Saves the token on success. */
    suspend fun login(
        username: String,
        password: String,
    ): Result<LoginResponse> =
        apiCall<LoginResponse>(LOGIN_FAILED) {
            client.post(ApiConfig.Endpoints.LOGIN) {
                parameter("app", "patient")
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username, password))
            }
        }.mapCatching {
            if (it.token.isBlank()) throw ApiException.ServerError(NO_TOKEN_RECEIVED)
            tokenStorage.token = it.token
            it
        }

    /**
     * 05 §2.1: true if a stored token is still good. Only a 401 from the profile call rejects it;
     * any other failure (offline, 5xx) keeps the session.
     */
    suspend fun restoreSession(): Boolean =
        withContext(Dispatchers.Default) {
            try {
                // The iOS Keychain survives a reinstall; drop its token on the first launch.
                if (!settings.getBoolean(KEY_HAS_LAUNCHED, false)) {
                    tokenStorage.clear()
                    settings[KEY_HAS_LAUNCHED] = true
                }
                if (tokenStorage.token == null) return@withContext false
                val rejected = apiCall<Unit>("") { client.get(ApiConfig.Endpoints.PROFILE) }.exceptionOrNull()
                if (rejected is ApiException.Unauthorized) tokenStorage.clear()
                rejected !is ApiException.Unauthorized
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
        }

    // No logout endpoint (05 §2.8).
    fun logout() = tokenStorage.clear()
}
