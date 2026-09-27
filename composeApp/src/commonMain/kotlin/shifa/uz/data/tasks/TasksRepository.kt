package shifa.uz.data.tasks

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonElement
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_LOAD_TASKS
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.httpClient
import shifa.uz.data.api.items
import shifa.uz.data.model.RemoteCareTask

class TasksRepository(
    private val client: HttpClient = httpClient,
) {
    /** T1: an array or `{"data": [...]}`; items that don't parse are skipped (15 §5.11). */
    suspend fun getMyTasks(): Result<List<RemoteCareTask>> =
        apiCall<JsonElement>(FAILED_TO_LOAD_TASKS) { client.get(ApiConfig.Endpoints.MY_TASKS) }
            .map { it.items<RemoteCareTask>("data") }
}
