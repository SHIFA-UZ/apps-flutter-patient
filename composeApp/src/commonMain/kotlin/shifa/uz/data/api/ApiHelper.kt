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
import io.ktor.http.Url
import io.ktor.http.defaultForFilePath
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import shifa.uz.data.analytics.Analytics
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.AnalyticsTracker
import shifa.uz.data.model.ApiError
import shifa.uz.data.model.ApiException
import shifa.uz.utils.getAdjustedBaseUrl
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Sends [request] and reads a [T]. Failures are [ApiException]s with a display-safe message:
 * non-2xx goes through [sanitizeError] (falling back to [defaultMessage]), no response is a
 * [ApiException.NetworkError].
 */
suspend inline fun <reified T> apiCall(
    defaultMessage: String,
    request: () -> HttpResponse,
): Result<T> {
    val started = TimeSource.Monotonic.markNow()
    val response =
        try {
            request()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            httpLogger.log("FAILED $defaultMessage, no response: $e")
            // events.md §13: no request object here, so no endpoint; the exception's class says what happened.
            apiAnalytics.track(AnalyticsEvent.ApiError(null, null, 0, e::class.simpleName ?: "Exception", null))
            return Result.failure(ApiException.NetworkError(networkErrorMessage(e)))
        }
    val status = response.status.value
    val isBackend = response.call.request.url.host in BACKEND_HOSTS
    val endpoint = if (isBackend) endpointTemplate(response.call.request.url.encodedPath) else "external"
    val method = response.call.request.method.value
    if (!response.status.isSuccess()) {
        val auth = "/auth/" in response.call.request.url.encodedPath
        val error = errorBody(response)
        val raw = error?.message ?: error?.error ?: errorRawText(response)
        logFailure(response, raw)
        val code = error?.code
        val failure = ApiException.of(status, sanitizeError(status, raw, defaultMessage, auth, code), code)
        apiAnalytics.track(
            AnalyticsEvent.ApiError(
                endpoint,
                method,
                status,
                failure::class.simpleName ?: "ApiException",
                code?.takeIf { isBackend && BACKEND_CODE.matches(it) },
            ),
        )
        return Result.failure(failure)
    }
    return try {
        val body = response.body<T>()
        val elapsed = started.elapsedNow()
        if (elapsed > SLOW_CALL) {
            apiAnalytics.track(
                AnalyticsEvent.ApiSlow(endpoint, method, elapsed.inWholeMilliseconds),
            )
        }
        Result.success(body)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logFailure(response, "unreadable body: ${e.message}")
        apiAnalytics.track(AnalyticsEvent.ApiError(endpoint, method, status, "UnreadableBody", null))
        Result.failure(ApiException.ServerError(defaultMessage))
    }
}

/** events.md §13: a call that took longer than this, and still succeeded, is `api_slow`. */
@PublishedApi
internal val SLOW_CALL = 5.seconds

/** Tests swap it for a recording one; the app uses [Analytics]. */
@PublishedApi
internal var apiAnalytics: AnalyticsTracker = Analytics

/** Our backend's hosts; a call anywhere else (a file host) is reported as `external`, with no path or code. */
@PublishedApi
internal val BACKEND_HOSTS: Set<String> =
    setOf(
        Url(ApiConfig.BASE_URL).host,
        Url(ApiConfig.DEV_BASE_URL).host,
        Url(getAdjustedBaseUrl(ApiConfig.BASE_URL)).host,
    )

/** The backend's error codes look like `APPOINTMENT_LOCKED_24H`; anything else isn't sent (privacy.md §3). */
@PublishedApi
internal val BACKEND_CODE = Regex("[A-Z][A-Z0-9_]{1,39}")

/**
 * `/api/patients/me/appointments/123` → `patients/me/appointments/{id}` (events.md §13). Only the path words the
 * API is made of survive; every other segment (an id, a file name, anything unexpected) becomes `{id}`, so a path
 * can never carry data (privacy.md §3).
 */
@PublishedApi
internal fun endpointTemplate(path: String): String =
    path
        .removePrefix("/api/")
        .removePrefix("/")
        .split('/')
        .joinToString("/") { segment -> if (segment in ENDPOINT_WORDS) segment else "{id}" }

// The literal segments of ApiConfig.Endpoints. A new endpoint's words go here, or its path shows as {id}s.
private val ENDPOINT_WORDS =
    setOf(
        "auth", "login", "check-identifier", "send-email-otp", "send-sms-otp", "register-patient",
        "create-patient-for-doctor", "send-forgot-password-otp", "forgot-password-reset", "reset-password",
        "change-password", "patients", "me", "profile", "photo", "delete-account", "request", "confirm", "fcm-token",
        "appointments", "submit-signature", "review", "tasks", "my-tasks", "check-ins", "check-in", "notifications",
        "page", "unread", "count", "read-all", "read", "documents", "public", "doctors", "reviews", "locations",
        "schedule", "available", "copilot", "stream", "transcribe", "messages", "conversations", "send", "search",
        "upload-attachment", "video", "token", "payments", "methods",
    )

// The backend's own reason, as the UI shows only the sanitized text. 01 §7: 300 characters at most.
// ponytail: the reason is logged as sent, not key-redacted; only debug builds log (httpLogger).
@PublishedApi
internal fun logFailure(
    response: HttpResponse,
    reason: String?,
) {
    val request = response.call.request
    httpLogger.log(
        "FAILED ${response.status.value} ${request.method.value} ${request.url.encodedPath}: ${reason?.take(300)}",
    )
}

@PublishedApi
internal fun networkErrorMessage(e: Exception) =
    when (e) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> CONNECTION_TIMED_OUT
        else -> NETWORK_ERROR
    }

// 15 §1.6: the body as `{message, error, code}`, or null when it isn't that JSON. Lenient: `error` may be the status number.
@PublishedApi
internal suspend fun errorBody(response: HttpResponse): ApiError? =
    errorRawText(response)?.let { text -> runCatching { apiJson.decodeFromString<ApiError>(text) }.getOrNull() }

@PublishedApi
internal suspend fun errorRawText(response: HttpResponse): String? =
    try {
        response.bodyAsText()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
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
