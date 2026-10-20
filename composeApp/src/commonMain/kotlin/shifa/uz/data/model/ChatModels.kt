package shifa.uz.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonNames
import shifa.uz.utils.parseInstant

/** C1 item and C2 `conversation` (15 §5.8). Without an `id` the item is skipped. */
@Serializable
data class ChatContact(
    val id: String,
    val participantId: String = "",
    val participantName: String = "",
    val participantPhotoUrl: String? = null,
    val lastMessage: String? = null,
    @SerialName("lastMessageAt") private val lastAt: String? = null,
    val unreadCount: Int = 0,
) {
    @Transient
    val lastMessageAt: Instant? = parseInstant(lastAt)
}

enum class MessageType { Text, Image, Voice, Document, System }

/** The new format's `content` (08 §3.3). Sizes and durations may come as decimals. */
@Serializable
data class MessageContent(
    val text: String? = null,
    private val fileUrl: String? = null,
    private val url: String? = null,
    private val attachmentUrl: String? = null,
    val thumbnailUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Double? = null,
    val duration: Double? = null,
) {
    val file get() = fileUrl ?: url ?: attachmentUrl
}

/**
 * C2 `messages[]` and C3, in either format (15 §5.9): the new one has `content` and `type`, the legacy one is flat
 * and its type comes from the attachment. Without an `id` or an ISO `createdAt` the item is skipped.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatMessage(
    val id: String,
    @JsonNames("conversationId") val chatId: String? = null,
    @SerialName("type") private val rawType: String? = null,
    private val content: MessageContent? = null,
    private val text: String? = null,
    private val attachmentUrl: String? = null,
    private val attachmentName: String? = null,
    @SerialName("createdAt") private val created: String,
    val isMine: Boolean = false,
    val isRead: Boolean = false,
) {
    @Transient
    val createdAt: Instant = parseInstant(created) ?: throw SerializationException("createdAt: $created")

    private val isNew get() = content != null && rawType != null

    val type: MessageType
        get() =
            when {
                isNew ->
                    MessageType.entries.firstOrNull {
                        it.name.equals(
                            rawType,
                            ignoreCase = true,
                        )
                    } ?: MessageType.Text
                attachmentUrl == null -> MessageType.Text
                (attachmentName ?: attachmentUrl).lowercase().let { n -> DOCUMENT_EXTENSIONS.any { n.endsWith(it) } } ->
                    MessageType.Document
                else -> MessageType.Image
            }

    val body get() = (if (isNew) content?.text else text)?.ifBlank { null }
    val fileUrl get() = if (isNew) content?.file ?: content?.thumbnailUrl else attachmentUrl
    val fileName get() = if (isNew) content?.fileName else attachmentName
    val durationSeconds get() = content?.duration?.toInt()
}

// Not in a private companion: the serializer lives there and the repository couldn't reach it.
private val DOCUMENT_EXTENSIONS = listOf(".pdf", ".doc", ".docx")

/** C5 item; the rest of its keys aren't shown. */
@Serializable
data class ChatUser(
    val id: String,
    val name: String = "",
    val photoUrl: String? = null,
)

/** C2. [contact] is null when `conversation` doesn't parse. */
data class Conversation(
    val contact: ChatContact?,
    val messages: List<ChatMessage>,
)

/** C3 body. Ids go as numbers; nulls are left out. */
@Serializable
data class SendMessageRequest(
    val conversationId: Long? = null,
    val recipientDoctorId: Long? = null,
    val text: String? = null,
    val type: String? = null,
    val attachmentUrl: String? = null,
    val attachmentName: String? = null,
)

/** C7. */
@Serializable
data class UploadResponse(
    val url: String = "",
)
