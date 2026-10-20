package shifa.uz.data.documents

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonElement
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_DELETE_DOCUMENT
import shifa.uz.data.api.FAILED_TO_LOAD_DOCUMENT
import shifa.uz.data.api.FAILED_TO_LOAD_DOCUMENTS
import shifa.uz.data.api.FAILED_TO_UPLOAD_DOCUMENT
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.appendFile
import shifa.uz.data.api.httpClient
import shifa.uz.data.api.items
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.PatientDocument

class DocumentsRepository(
    private val client: HttpClient = httpClient,
) {
    /** F1: an array; items that don't parse are skipped. */
    suspend fun getDocuments(): Result<List<PatientDocument>> =
        apiCall<JsonElement>(FAILED_TO_LOAD_DOCUMENTS) { client.get(ApiConfig.Endpoints.DOCUMENTS) }
            .map { it.items<PatientDocument>() }

    /** F2. Returns the new document. */
    suspend fun upload(
        bytes: ByteArray,
        fileName: String,
        title: String,
        category: String?,
    ): Result<PatientDocument> =
        apiCall(FAILED_TO_UPLOAD_DOCUMENT) {
            client.submitFormWithBinaryData(
                ApiConfig.Endpoints.DOCUMENTS,
                formData {
                    appendFile("file", bytes, fileName)
                    append("title", title)
                    append("isChatAttachment", "false")
                    category?.let { append("category", it) }
                },
            )
        }

    /** F3. The response body is not read. */
    suspend fun delete(id: String): Result<Unit> =
        apiCall<String>(FAILED_TO_DELETE_DOCUMENT) { client.delete(ApiConfig.Endpoints.document(id)) }.map {}

    /** F4: the file's bytes. The token only goes to the API host, so a file host's 401 is just an error. */
    suspend fun getFile(url: String): Result<ByteArray> =
        apiCall<ByteArray>(FAILED_TO_LOAD_DOCUMENT) { client.get(url) }
            .mapCatching { it.takeIf { it.isNotEmpty() } ?: throw ApiException.ServerError(FAILED_TO_LOAD_DOCUMENT) }
}
