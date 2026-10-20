package shifa.uz.data.model

import kotlinx.serialization.Serializable

/** Error body. The envelope is unverified; the backend sends `message` or `error` (15 §1.6). */
@Serializable
data class ApiError(
    val message: String? = null,
    val error: String? = null,
)

/** [message] is display-safe: a canonical English string, localized by `toUserMessage()`. */
sealed class ApiException(message: String) : Exception(message) {
    class ValidationError(message: String) : ApiException(message)

    class Unauthorized(message: String) : ApiException(message)

    class Forbidden(message: String) : ApiException(message)

    class NotFound(message: String) : ApiException(message)

    class Conflict(message: String) : ApiException(message)

    class RateLimit(message: String) : ApiException(message)

    class ServerError(message: String) : ApiException(message)

    class NetworkError(message: String) : ApiException(message)

    companion object {
        fun of(
            status: Int,
            message: String,
        ): ApiException =
            when (status) {
                400, 422 -> ValidationError(message)
                401 -> Unauthorized(message)
                403 -> Forbidden(message)
                404 -> NotFound(message)
                409 -> Conflict(message)
                429 -> RateLimit(message)
                else -> ServerError(message)
            }
    }
}
