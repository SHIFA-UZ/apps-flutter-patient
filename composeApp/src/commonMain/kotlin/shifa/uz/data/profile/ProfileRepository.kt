package shifa.uz.data.profile

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_DELETE_ACCOUNT
import shifa.uz.data.api.FAILED_TO_LOAD_PROFILE
import shifa.uz.data.api.FAILED_TO_UPDATE_PROFILE
import shifa.uz.data.api.FAILED_TO_UPLOAD_PHOTO
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.appendFile
import shifa.uz.data.api.httpClient
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.DeleteAccountChallenge
import shifa.uz.data.model.DeleteAccountConfirmRequest
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.model.ProfileUpdate

private const val DELETION_TIMEOUT_MS = 10_000L

class ProfileRepository(
    private val client: HttpClient = httpClient,
) {
    /** P1. */
    suspend fun getProfile(): Result<PatientProfile> =
        apiCall(FAILED_TO_LOAD_PROFILE) { client.get(ApiConfig.Endpoints.PROFILE) }

    /** P2. Returns the updated profile. */
    suspend fun updateProfile(update: ProfileUpdate): Result<PatientProfile> =
        apiCall(FAILED_TO_UPDATE_PROFILE) {
            client.patch(ApiConfig.Endpoints.PROFILE) {
                contentType(ContentType.Application.Json)
                setBody(update)
            }
        }

    /** P5, 10 s timeout (14 §3.1.3). */
    suspend fun requestDeletion(): Result<DeleteAccountChallenge> =
        apiCall<DeleteAccountChallenge>(FAILED_TO_DELETE_ACCOUNT) {
            client.post(
                ApiConfig.Endpoints.DELETE_ACCOUNT_REQUEST,
            ) { timeout { requestTimeoutMillis = DELETION_TIMEOUT_MS } }
        }.mapCatching { if (it.challengeId.isBlank()) throw ApiException.ServerError(FAILED_TO_DELETE_ACCOUNT) else it }

    /** P6. The response body is not read. */
    suspend fun confirmDeletion(
        challengeId: String,
        email: String,
        emailOtp: String,
    ): Result<Unit> =
        apiCall<String>(FAILED_TO_DELETE_ACCOUNT) {
            client.post(ApiConfig.Endpoints.DELETE_ACCOUNT_CONFIRM) {
                timeout { requestTimeoutMillis = DELETION_TIMEOUT_MS }
                contentType(ContentType.Application.Json)
                setBody(DeleteAccountConfirmRequest(challengeId, email, emailOtp))
            }
        }.map {}

    /** P3. Returns the new photo URL. P4 `/profile/photo` is unverified and not used as a fallback. */
    suspend fun uploadPhoto(
        bytes: ByteArray,
        fileName: String,
    ): Result<String> =
        apiCall<String>(FAILED_TO_UPLOAD_PHOTO) {
            client.submitFormWithBinaryData(
                ApiConfig.Endpoints.PATIENT_PHOTO,
                formData { appendFile("file", bytes, fileName) },
            )
        }.mapCatching { photoUrl(it) ?: throw ApiException.ServerError(FAILED_TO_UPLOAD_PHOTO) }

    /** P3, then P2 with the new URL (14 §2.3). Returns the updated profile. */
    suspend fun changePhoto(
        bytes: ByteArray,
        fileName: String,
    ): Result<PatientProfile> {
        val url = uploadPhoto(bytes, fileName).getOrElse { return Result.failure(it) }
        return updateProfile(ProfileUpdate(photoUrl = url))
    }
}

/** P3 answers `{"photoUrl": "…"}` or a bare URL, quoted or not. */
internal fun photoUrl(body: String): String? {
    val json = runCatching { Json.parseToJsonElement(body) }.getOrElse { return body.trim().ifEmpty { null } }
    val url = if (json is JsonObject) json["photoUrl"] else json
    return (url as? JsonPrimitive)?.contentOrNull?.ifBlank { null }
}
