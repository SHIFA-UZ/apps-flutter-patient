package shifa.uz.data.model

import kotlinx.serialization.Serializable

/**
 * Error body (15 §1.6): `message` (or `error`), and since the backend's Phase 1 a stable `code` from its
 * `shared/ErrorCode.kt` (`VALIDATION`, `SESSION_EXPIRED`, `TOKEN_INVALID`, `RATE_LIMITED`, `ACCOUNT_DISABLED`, …).
 */
@Serializable
data class ApiError(
    val message: String? = null,
    val error: String? = null,
    val code: String? = null,
)

/**
 * [message] is display-safe: a canonical English string, localized by `toUserMessage()`. [code] is the backend's
 * error code when it sent one; it is the stable thing to branch on.
 */
sealed class ApiException(
    message: String,
    val code: String? = null,
) : Exception(message) {
    class ValidationError(message: String, code: String? = null) : ApiException(message, code)

    class Unauthorized(message: String, code: String? = null) : ApiException(message, code)

    class Forbidden(message: String, code: String? = null) : ApiException(message, code)

    class NotFound(message: String, code: String? = null) : ApiException(message, code)

    class Conflict(message: String, code: String? = null) : ApiException(message, code)

    class RateLimit(message: String, code: String? = null) : ApiException(message, code)

    class ServerError(message: String, code: String? = null) : ApiException(message, code)

    class NetworkError(message: String) : ApiException(message)

    companion object {
        fun of(
            status: Int,
            message: String,
            code: String? = null,
        ): ApiException =
            when (status) {
                400, 422 -> ValidationError(message, code)
                401 -> Unauthorized(message, code)
                403 -> Forbidden(message, code)
                404 -> NotFound(message, code)
                409 -> Conflict(message, code)
                429 -> RateLimit(message, code)
                else -> ServerError(message, code)
            }
    }
}
