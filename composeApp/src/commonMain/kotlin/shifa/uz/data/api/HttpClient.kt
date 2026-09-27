package shifa.uz.data.api

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import shifa.uz.utils.getAdjustedBaseUrl

expect fun createHttpEngine(): HttpClientEngine

fun createHttpClient(tokenProvider: () -> String?): HttpClient {
    return HttpClient(createHttpEngine()) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                    prettyPrint = true
                },
            )
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val token = tokenProvider()
                    if (token != null) {
                        val tokenPreview = if (token.length >= 10) token.take(10) else token
                        BearerTokens(accessToken = token, refreshToken = "")
                    } else {
                        null
                    }
                }
                refreshTokens {
                    val token = tokenProvider()
                    if (token != null) {
                        val tokenPreview = if (token.length >= 10) token.take(10) else token
                        BearerTokens(accessToken = token, refreshToken = "")
                    } else {
                        null
                    }
                }
                sendWithoutRequest { request ->
                    val shouldSend = request.url.encodedPath.startsWith("/api/")
                    shouldSend
                }
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = ApiConfig.TIMEOUT_MS
            connectTimeoutMillis = ApiConfig.TIMEOUT_MS
        }

        defaultRequest {
            val adjustedUrl = getAdjustedBaseUrl(ApiConfig.BASE_URL)
            url(adjustedUrl)
        }
    }
}
