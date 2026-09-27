package shifa.uz.data.notifications

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_LOAD_NOTIFICATIONS
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.httpClient
import shifa.uz.data.model.UnreadCount

class NotificationsRepository(
    private val client: HttpClient = httpClient,
) {
    /** N2. */
    suspend fun unreadCount(): Result<Int> =
        apiCall<UnreadCount>(FAILED_TO_LOAD_NOTIFICATIONS) {
            client.get(ApiConfig.Endpoints.UNREAD_NOTIFICATIONS_COUNT)
        }.map { it.count }
}
