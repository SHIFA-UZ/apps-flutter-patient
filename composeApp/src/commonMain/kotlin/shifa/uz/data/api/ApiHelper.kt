package shifa.uz.data.api

import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.defaultForFilePath
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
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
        val auth = "/auth/" in response.call.request.url.encodedPath
        return Result.failure(ApiException.of(status, sanitizeError(status, errorText(response), defaultMessage, auth)))
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

// 15 §1.6: `message`, else `error`, else the raw body. Lenient: the backend sends `error` as the status number.
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
    val error = runCatching { apiJson.decodeFromString<ApiError>(text) }.getOrNull()
    return error?.message ?: error?.error ?: text
}

/** A JSON array, or the array under [key]. Items that don't decode are skipped; any other shape is empty (B2, T1). */
internal inline fun <reified T> JsonElement.items(key: String? = null): List<T> {
    val array = this as? JsonArray ?: key?.let { (this as? JsonObject)?.get(it) } as? JsonArray ?: return emptyList()
    return array.mapNotNull { runCatching { apiJson.decodeFromJsonElement<T>(it) }.getOrNull() }
}

/** A multipart file part with a real Content-Type (11 §9). Quotes and line breaks can't break out of the header. */
internal fun FormBuilder.appendFile(
    key: String,
    bytes: ByteArray,
    fileName: String,
) {
    val name = fileName.replace(Regex("[\"\\\\/\r\n]"), "_")
    append(
        key,
        bytes,
        Headers.build {
            append(HttpHeaders.ContentType, ContentType.defaultForFilePath(name).toString())
            append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
        },
    )
}
