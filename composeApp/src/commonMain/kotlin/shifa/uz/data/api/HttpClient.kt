package shifa.uz.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.bearerAuth
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import shifa.uz.data.storage.TokenStorage
import shifa.uz.utils.getAdjustedBaseUrl

expect fun createHttpEngine(): HttpClientEngine

// isLenient also reads numeric IDs into String fields (15 §1.9).
internal val apiJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false // optional keys are omitted, not sent as null (A2, P2)
        coerceInputValues = true // a null list (D1 `certificates`) reads as the default, not a dropped item
        prettyPrint = true
    }

/** The app-wide client. */
val httpClient: HttpClient by lazy { createHttpClient(TokenStorage()) }

fun createHttpClient(
    tokenStorage: TokenStorage,
    engine: HttpClientEngine = createHttpEngine(),
): HttpClient {
    val baseUrl = getAdjustedBaseUrl(ApiConfig.BASE_URL)
    val apiHost = Url(baseUrl).host
    return HttpClient(engine) {
        install(ContentNegotiation) { json(apiJson) }

        // INFO logs the request line and status only, never headers or bodies (token, password).
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        // 01 §4.1: 30 s connect and receive, no send timeout.
        install(HttpTimeout) {
            connectTimeoutMillis = ApiConfig.TIMEOUT_MS
            socketTimeoutMillis = ApiConfig.TIMEOUT_MS
        }

        defaultRequest { url(baseUrl) }
    }.apply {
        // 01 §4.2: the token goes on every backend request except the public auth paths, and never to
        // another host (00 bug 7). A plain interceptor, because Ktor's Auth plugin caches the token.
        plugin(HttpSend).intercept { request ->
            val path = request.url.encodedPath
            if (request.url.host == apiHost && ApiConfig.PUBLIC_AUTH_PATHS.none(path::endsWith)) {
                tokenStorage.token?.let(request::bearerAuth)
            }
            execute(request)
        }
    }
}
