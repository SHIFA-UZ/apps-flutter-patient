package shifa.uz.data.profile

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.defaultForFilePath
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_LOAD_PROFILE
import shifa.uz.data.api.FAILED_TO_UPDATE_PROFILE
import shifa.uz.data.api.FAILED_TO_UPLOAD_PHOTO
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.httpClient
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.model.ProfileUpdate

class ProfileRepository(
    private val client: HttpClient = httpClient,
) {
    /** P1. */
    suspend fun getProfile(): Result<PatientProfile> =
        apiCall(FAILED_TO_LOAD_PROFILE) { client.get(ApiConfig.Endpoints.PROFILE) }

    /** P2. The response isn't read yet; the profile screen will parse it. */
    suspend fun updateProfile(update: ProfileUpdate): Result<Unit> =
        apiCall<String>(FAILED_TO_UPDATE_PROFILE) {
            client.patch(ApiConfig.Endpoints.PROFILE) {
                contentType(ContentType.Application.Json)
                setBody(update)
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
                formData {
                    append(
                        "file",
                        bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, ContentType.defaultForFilePath(fileName).toString())
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        },
                    )
                },
            )
        }.mapCatching { photoUrl(it) ?: throw ApiException.ServerError(FAILED_TO_UPLOAD_PHOTO) }
}

/** P3 answers `{"photoUrl": "…"}` or a bare URL, quoted or not. */
internal fun photoUrl(body: String): String? {
    val json = runCatching { Json.parseToJsonElement(body) }.getOrElse { return body.trim().ifEmpty { null } }
    val url = if (json is JsonObject) json["photoUrl"] else json
    return (url as? JsonPrimitive)?.contentOrNull?.ifBlank { null }
}
