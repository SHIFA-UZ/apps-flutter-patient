package shifa.uz.data.api

import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import shifa.uz.data.model.ApiError
import shifa.uz.data.model.ApiException

/**
 * Sends [request] and reads a [T]. Failures are [ApiException]s with a display-safe message:
 * non-2xx goes through [sanitizeError] (falling back to [defaultMessage]), no response is a
 * [ApiException.NetworkError].
 */
suspend inline fun <reified T> apiCall(
    defaultMessage: String,
    request: () -> HttpResponse,
): Result<T> {
    val response =
        try {
            request()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(ApiException.NetworkError(networkErrorMessage(e)))
        }
    val status = response.status.value
    if (!response.status.isSuccess()) {
        return Result.failure(ApiException.of(status, sanitizeError(status, errorText(response), defaultMessage)))
    }
    return try {
        Result.success(response.body<T>())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(ApiException.ServerError(defaultMessage))
    }
}

@PublishedApi
internal fun networkErrorMessage(e: Exception) =
    when (e) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> CONNECTION_TIMED_OUT
        else -> NETWORK_ERROR
    }

private val errorJson = Json { ignoreUnknownKeys = true }

// 15 §1.6: `message`, else `error`, else the raw body.
@PublishedApi
internal suspend fun errorText(response: HttpResponse): String? {
    val text =
        try {
            response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
    val error = runCatching { errorJson.decodeFromString<ApiError>(text) }.getOrNull()
    return error?.message ?: error?.error ?: text
}
