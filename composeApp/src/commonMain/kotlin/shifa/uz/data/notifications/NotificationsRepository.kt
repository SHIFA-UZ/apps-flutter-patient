package shifa.uz.data.notifications

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_LOAD_NOTIFICATIONS
import shifa.uz.data.api.FAILED_TO_MARK_READ
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.httpClient
import shifa.uz.data.api.items
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.AppNotification
import shifa.uz.data.model.FcmTokenRequest
import shifa.uz.data.model.Page
import shifa.uz.data.model.UnreadCount

class NotificationsRepository(
    private val client: HttpClient = httpClient,
) {
    /** N1. A non-array response is empty (15 §6.2). */
    suspend fun getNotifications(): Result<List<AppNotification>> =
        apiCall<JsonElement>(FAILED_TO_LOAD_NOTIFICATIONS) { client.get(ApiConfig.Endpoints.NOTIFICATIONS) }
            .map { it.items<AppNotification>() }

    /**
     * N1a, newest first, [size] per 0-based [page]. Spring's `Page` shape: `content` and `last`; a bare array (an
     * older backend) is one page. A backend without the endpoint (production until its next release answers 500
     * NoResourceFoundException, others 404) gets N1 as the only page. The sanitized error can't tell that 500 from
     * another, so any first-page server error degrades to N1's full list rather than an empty inbox.
     */
    suspend fun getNotificationsPage(
        page: Int,
        size: Int = PAGE_SIZE,
    ): Result<Page<AppNotification>> {
        val paged =
            apiCall<JsonElement>(FAILED_TO_LOAD_NOTIFICATIONS) {
                client.get(ApiConfig.Endpoints.NOTIFICATIONS_PAGE) {
                    parameter("page", page)
                    parameter("size", size)
                }
            }
        val missing = paged.exceptionOrNull().let { it is ApiException.NotFound || it is ApiException.ServerError }
        if (page == 0 && missing) return getNotifications().map { Page(it, last = true) }
        return paged.map { json ->
            val last = ((json as? JsonObject)?.get("last") as? JsonPrimitive)?.booleanOrNull ?: true
            Page(json.items<AppNotification>("content"), last)
        }
    }

    /** N2. */
    suspend fun unreadCount(): Result<Int> =
        apiCall<UnreadCount>(FAILED_TO_LOAD_NOTIFICATIONS) {
            client.get(ApiConfig.Endpoints.UNREAD_NOTIFICATIONS_COUNT)
        }.map { it.count }

    /** N3, no body. The response is ignored. */
    suspend fun markRead(id: Int): Result<Unit> =
        apiCall<String>(FAILED_TO_MARK_READ) { client.put(ApiConfig.Endpoints.notificationRead(id)) }.map {}

    /** N4, no body. */
    suspend fun markAllRead(): Result<Unit> =
        apiCall<String>(FAILED_TO_MARK_READ) { client.put(ApiConfig.Endpoints.NOTIFICATIONS_READ_ALL) }.map {}

    /** P7 (13 §3.1), best effort. Logout's `""` is in AuthRepository. */
    suspend fun registerPushToken(token: String): Result<Unit> =
        apiCall<String>("") {
            client.put(ApiConfig.Endpoints.FCM_TOKEN) {
                contentType(ContentType.Application.Json)
                setBody(FcmTokenRequest(token))
            }
        }.map {}
}

private const val PAGE_SIZE = 20
