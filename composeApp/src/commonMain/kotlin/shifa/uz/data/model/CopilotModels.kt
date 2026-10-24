package shifa.uz.data.model

import kotlinx.serialization.Serializable

/** A turn (09 §2): [role] is `user` or `assistant`. [sentAt] (epoch ms) is ours, never sent to X1; older chats have none. */
@Serializable
data class CopilotMessage(
    val role: String,
    val content: String,
    val sentAt: Long? = null,
) {
    val isUser get() = role == USER

    companion object {
        const val USER = "user"
        const val ASSISTANT = "assistant"
    }
}

/** A Shifa AI chat, kept on this device only (the backend has no chat list). [id] is its creation time, in ms. */
@Serializable
data class CopilotChat(
    val id: String,
    val messages: List<CopilotMessage> = emptyList(),
    val updatedAt: Long = 0,
) {
    val title get() = messages.firstOrNull { it.isUser }?.content.orEmpty()
    val preview get() = messages.lastOrNull()?.content.orEmpty()
}

/** X1. [language] is `EN`, `RU` or `UZ`. */
@Serializable
data class CopilotStreamRequest(
    val messages: List<CopilotMessage>,
    val language: String,
)

/** An X1 error: the non-200 body, or an SSE `error` event's data (09 §1.1). */
@Serializable
data class TranscribeResponse(
    val text: String = "",
)

@Serializable
data class CopilotError(
    val code: String? = null,
    val message: String? = null,
)
