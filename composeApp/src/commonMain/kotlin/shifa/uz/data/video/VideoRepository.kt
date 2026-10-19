package shifa.uz.data.video

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_START_VIDEO_CALL
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.httpClient
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.VideoToken
import shifa.uz.data.model.VideoTokenRequest

class VideoRepository(
    private val client: HttpClient = httpClient,
) {
    /** V1: refused outside the join window and for unpaid consultations (06 §3.6, §7.3). */
    suspend fun token(appointmentId: String): Result<VideoToken> {
        val id =
            appointmentId.toLongOrNull() ?: return Result.failure(
                ApiException.ServerError(FAILED_TO_START_VIDEO_CALL),
            )
        return apiCall<VideoToken>(FAILED_TO_START_VIDEO_CALL) {
            client.post(ApiConfig.Endpoints.VIDEO_TOKEN) {
                contentType(ContentType.Application.Json)
                setBody(VideoTokenRequest(id))
            }
        }.mapCatching {
            it.takeIf {
                it.token.isNotBlank() && it.roomUrl.isNotBlank()
            } ?: throw ApiException.ServerError(FAILED_TO_START_VIDEO_CALL)
        }
    }
}
