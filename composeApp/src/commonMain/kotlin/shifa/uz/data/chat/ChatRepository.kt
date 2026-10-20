package shifa.uz.data.chat

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_LOAD_CONVERSATION
import shifa.uz.data.api.FAILED_TO_LOAD_CONVERSATIONS
import shifa.uz.data.api.FAILED_TO_MARK_CONVERSATION_READ
import shifa.uz.data.api.FAILED_TO_SEARCH_USERS
import shifa.uz.data.api.FAILED_TO_SEND_MESSAGE
import shifa.uz.data.api.FAILED_TO_UPLOAD_ATTACHMENT
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.apiJson
import shifa.uz.data.api.appendFile
import shifa.uz.data.api.httpClient
import shifa.uz.data.api.items
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.ChatContact
import shifa.uz.data.model.ChatMessage
import shifa.uz.data.model.ChatUser
import shifa.uz.data.model.Conversation
import shifa.uz.data.model.SendMessageRequest
import shifa.uz.data.model.UploadResponse

/** 08 §2: the patient's chats with doctors. */
class ChatRepository(
    private val client: HttpClient = httpClient,
) {
    /** C1: items that don't parse are skipped. */
    suspend fun conversations(): Result<List<ChatContact>> =
        apiCall<JsonElement>(FAILED_TO_LOAD_CONVERSATIONS) { client.get(ApiConfig.Endpoints.CONVERSATIONS) }
            .map { it.items<ChatContact>() }

    /** C2. A missing `messages` is empty. */
    suspend fun conversation(id: String): Result<Conversation> =
        apiCall<JsonElement>(FAILED_TO_LOAD_CONVERSATION) { client.get(ApiConfig.Endpoints.conversation(id)) }
            .map { json ->
                Conversation(
                    (json as? JsonObject)?.get("conversation")?.let {
                        runCatching { apiJson.decodeFromJsonElement<ChatContact>(it) }.getOrNull()
                    },
                    json.items("messages"),
                )
            }

    /** C3. The sent message, or null when it went but its echo doesn't parse (the next poll brings it). */
    suspend fun send(request: SendMessageRequest): Result<ChatMessage?> =
        apiCall<JsonElement>(FAILED_TO_SEND_MESSAGE) {
            client.post(ApiConfig.Endpoints.SEND_MESSAGE) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.map { runCatching { apiJson.decodeFromJsonElement<ChatMessage>(it) }.getOrNull() }

    /** C4, no body. The response is ignored. */
    suspend fun markRead(id: String): Result<Unit> =
        apiCall<String>(FAILED_TO_MARK_CONVERSATION_READ) { client.post(ApiConfig.Endpoints.conversationRead(id)) }
            .map {}

    /** C5. */
    suspend fun search(query: String): Result<List<ChatUser>> =
        apiCall<JsonElement>(FAILED_TO_SEARCH_USERS) {
            client.get(ApiConfig.Endpoints.SEARCH_USERS) { parameter("q", query) }
        }.map { it.items<ChatUser>() }

    /** C7: the file's URL. No URL is a failure. */
    suspend fun upload(
        bytes: ByteArray,
        fileName: String,
    ): Result<String> =
        apiCall<UploadResponse>(FAILED_TO_UPLOAD_ATTACHMENT) {
            client.submitFormWithBinaryData(
                ApiConfig.Endpoints.UPLOAD_ATTACHMENT,
                formData { appendFile("file", bytes, fileName) },
            )
        }.mapCatching { it.url.ifBlank { throw ApiException.ServerError(FAILED_TO_UPLOAD_ATTACHMENT) } }
}
