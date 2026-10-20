package shifa.uz.data.tasks

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_LOAD_CHECK_INS
import shifa.uz.data.api.FAILED_TO_LOAD_TASKS
import shifa.uz.data.api.FAILED_TO_SUBMIT_CHECK_IN
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.httpClient
import shifa.uz.data.api.items
import shifa.uz.data.model.CheckInRequest
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.model.TaskCheckIn

class TasksRepository(
    private val client: HttpClient = httpClient,
) {
    /** T1: an array or `{"data": [...]}`; items that don't parse are skipped (15 §5.11). */
    suspend fun getMyTasks(): Result<List<RemoteCareTask>> =
        apiCall<JsonElement>(FAILED_TO_LOAD_TASKS) { client.get(ApiConfig.Endpoints.MY_TASKS) }
            .map { it.items<RemoteCareTask>("data") }

    /** T3, every slot of the task. Parsed per item, so one bad slot doesn't hide the rest (12 quirk 5). */
    suspend fun getCheckIns(taskId: String): Result<List<TaskCheckIn>> =
        apiCall<JsonElement>(FAILED_TO_LOAD_CHECK_INS) { client.get(ApiConfig.Endpoints.taskCheckIns(taskId)) }
            .map { it.items<TaskCheckIn>() }

    /** T4. Any 2xx is a success; the body is ignored. */
    suspend fun submitCheckIn(
        taskId: String,
        request: CheckInRequest,
    ): Result<Unit> =
        apiCall<String>(FAILED_TO_SUBMIT_CHECK_IN) {
            client.post(ApiConfig.Endpoints.taskCheckIn(taskId)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.map {}
}
