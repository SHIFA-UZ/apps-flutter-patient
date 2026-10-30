package shifa.uz.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import shifa.uz.utils.getCurrentInstant
import shifa.uz.utils.parseInstant

/** One page of a list (N1a and the backend's other `/page` siblings): the items and whether it is the last one. */
data class Page<T>(
    val items: List<T>,
    val last: Boolean,
)

/** N2. */
@Serializable
data class UnreadCount(
    val count: Int = 0,
)

/**
 * N1 item, the keys the app reads (15 §5.10). Without `id`, `title`, `message`, `type` or an ISO `createdAt` the
 * item is skipped (13 quirk 9). Linked ids come camelCase, snake_case or under `data`.
 * ponytail: no plan or access-request ids; their screens aren't built.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AppNotification(
    val id: Int,
    val title: String,
    val message: String,
    val type: String,
    @SerialName("createdAt") private val created: String,
    val readAt: String? = null,
    @JsonNames("appointment_id") private val appointmentId: String? = null,
    @JsonNames("document_id") private val documentId: String? = null,
    @JsonNames("task_id") private val taskId: String? = null,
    @JsonNames("chat_id") private val chatId: String? = null,
    @JsonNames("patient_form_id") private val patientFormId: String? = null,
    @JsonNames("doctor_name") private val doctorName: String? = null,
    private val data: JsonObject? = null,
) {
    // kotlinx-datetime 0.5 has no Instant serializer on iOS. A bad date throws, so items() skips the item.
    @Transient
    val createdAt: Instant = parseInstant(created) ?: throw SerializationException("createdAt: $created")
    val isRead get() = readAt != null
    val kind get() = type.uppercase()
    val appointment get() = appointmentId ?: data.string("appointmentId", "appointment_id")
    val document get() = documentId ?: data.string("documentId", "document_id")
    val task get() = taskId ?: data.string("taskId", "task_id")
    val chat get() = chatId ?: data.string("chatId", "chat_id")
    val form get() = patientFormId ?: data.string("patientFormId", "patient_form_id")
    val doctor get() = doctorName ?: data.string("doctorName", "doctor_name")

    companion object {
        /**
         * 13 §6.2: a push's data as an item, to route its tap. The id is `notificationId` or `id` (0 when it isn't a
         * number); `entityId` stands in for the type's own id, and `createdAt` defaults to now.
         * ponytail: plan ids aren't read; their screen isn't built.
         */
        fun fromPush(data: Map<String, String>): AppNotification {
            val type = data["type"]?.replace('-', '_') ?: "GENERAL"
            val entity =
                ENTITY_KEYS.entries.firstOrNull { it.key in type.uppercase() }
                    ?.let { key -> data["entityId"]?.let { mapOf(key.value to it) } }
                    .orEmpty()
            return AppNotification(
                id = (data["notificationId"] ?: data["id"])?.toIntOrNull() ?: 0,
                title = data["title"].orEmpty(),
                message = data["message"].orEmpty(),
                type = type,
                created = data["createdAt"]?.takeIf { parseInstant(it) != null } ?: getCurrentInstant().toString(),
                data = JsonObject((entity + data).mapValues { JsonPrimitive(it.value) }),
            )
        }

        private val ENTITY_KEYS =
            mapOf(
                "APPOINTMENT" to "appointmentId",
                "DOCUMENT" to "documentId",
                "TASK" to "taskId",
                "MESSAGE" to "chatId",
                "CHAT" to "chatId",
                "SIGNATURE" to "appointmentId",
            )
    }
}

private fun JsonObject?.string(vararg keys: String) =
    keys.firstNotNullOfOrNull { (this?.get(it) as? JsonPrimitive)?.contentOrNull?.ifBlank { null } }
