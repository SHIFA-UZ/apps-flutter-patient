package shifa.uz.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import shifa.uz.data.model.ApiError
import shifa.uz.data.model.TokenRefreshResponse
import shifa.uz.data.storage.TokenStorage

suspend inline fun <reified T> HttpClient.getWithRefresh(
    urlString: String,
    tokenStorage: TokenStorage,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): Result<T> {
    val response = get(urlString, block)
    return handleResponseWithRetry(response, tokenStorage) {
        get(urlString, block)
    }
}

suspend inline fun <reified T> HttpClient.postWithRefresh(
    urlString: String,
    tokenStorage: TokenStorage,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): Result<T> {
    val response = post(urlString, block)
    return handleResponseWithRetry(response, tokenStorage) {
        post(urlString, block)
    }
}

suspend inline fun <reified T> HttpClient.putWithRefresh(
    urlString: String,
    tokenStorage: TokenStorage,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): Result<T> {
    val response = put(urlString, block)
    return handleResponseWithRetry(response, tokenStorage) {
        put(urlString, block)
    }
}

suspend inline fun <reified T> HttpClient.patchWithRefresh(
    urlString: String,
    tokenStorage: TokenStorage,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): Result<T> {
    val response = patch(urlString, block)
    return handleResponseWithRetry(response, tokenStorage) {
        patch(urlString, block)
    }
}

suspend inline fun <reified T> HttpClient.deleteWithRefresh(
    urlString: String,
    tokenStorage: TokenStorage,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): Result<T> {
    val response = delete(urlString, block)
    return handleResponseWithRetry(response, tokenStorage) {
        delete(urlString, block)
    }
}

suspend inline fun <reified T> handleResponseWithRetry(
    response: HttpResponse,
    tokenStorage: TokenStorage,
    crossinline retryRequest: suspend () -> HttpResponse,
): Result<T> {
    try {
        when (response.status.value) {
            in 200..299 -> {
                val data = response.body<T>()
                return Result.success(data)
            }

            401 -> {
                val error =
                    try {
                        response.body<ApiError>()
                    } catch (e: Exception) {
                        null
                    }

                if (error?.code == "TOKEN_EXPIRED") {
                    val currentToken = tokenStorage.token
                    if (currentToken != null) {
                        val refreshResult = refreshTokenCall(currentToken, tokenStorage)

                        if (refreshResult.isSuccess) {
                            val retryResponse = retryRequest()

                            when (retryResponse.status.value) {
                                in 200..299 -> {
                                    val data = retryResponse.body<T>()
                                    return Result.success(data)
                                }
                                else -> {
                                    val retryErrorBody =
                                        try {
                                            retryResponse.bodyAsText()
                                        } catch (e: Exception) {
                                            "Retry failed"
                                        }
                                    return Result.failure(Exception(retryErrorBody))
                                }
                            }
                        } else {
                            tokenStorage.clear()
                            return Result.failure(Exception(error?.message ?: "Session expired"))
                        }
                    }
                } else if (error?.code == "TOKEN_INVALID" || error?.code == "TOKEN_ERROR") {
                    tokenStorage.clear()
                }

                val errorMessage = error?.message ?: "Unauthorized"
                return Result.failure(Exception(errorMessage))
            }

            else -> {
                val errorBody = response.bodyAsText()
                val error =
                    try {
                        response.body<ApiError>()
                    } catch (e: Exception) {
                        null
                    }

                return Result.failure(Exception(error?.message ?: errorBody))
            }
        }
    } catch (e: Exception) {
        return Result.failure(e)
    }
}

suspend fun refreshTokenCall(
    currentToken: String,
    tokenStorage: TokenStorage,
): Result<TokenRefreshResponse> {
    return try {
        val refreshClient = createHttpClient { currentToken }
        val response: HttpResponse =
            refreshClient.post(ApiConfig.Endpoints.REFRESH_TOKEN) {
                header("Authorization", "Bearer $currentToken")
            }

        when (response.status.value) {
            200 -> {
                val tokenResponse = response.body<TokenRefreshResponse>()
                tokenStorage.token = tokenResponse.token
                Result.success(tokenResponse)
            }
            else -> {
                val errorBody = response.bodyAsText()
                Result.failure(Exception("Token refresh failed"))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
