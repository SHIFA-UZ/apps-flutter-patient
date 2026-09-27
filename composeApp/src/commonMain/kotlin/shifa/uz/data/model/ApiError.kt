package shifa.uz.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val timestamp: String,
    val status: Int,
    val code: String,
    val message: String,
    val path: String,
    val retryAfterSeconds: Int? = null,
    val refreshTokenUrl: String? = null,
)

sealed class ApiException(message: String) : Exception(message) {
    data class ValidationError(val error: ApiError) : ApiException(error.message)

    data class Unauthorized(val error: ApiError) : ApiException(error.message)

    data class TokenExpired(val error: ApiError) : ApiException(error.message)

    data class TokenInvalid(val error: ApiError) : ApiException(error.message)

    data class Forbidden(val error: ApiError) : ApiException(error.message)

    data class NotFound(val error: ApiError) : ApiException(error.message)

    data class Conflict(val error: ApiError) : ApiException(error.message)

    data class RateLimit(val error: ApiError, val retryAfter: Int) : ApiException(error.message)

    data class ServerError(val error: ApiError) : ApiException(error.message)

    data class NetworkError(override val message: String) : ApiException(message)
}
