package shifa.uz.ui.messages

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.ChatMessage

/**
 * One chat. [messages] are oldest first, null until the first load (a new chat, without [conversationId], starts
 * empty). Until its first message a new chat is addressed by [doctorId].
 */
@Stable
data class ConversationUiState(
    val name: String = "",
    val photoUrl: String? = null,
    val conversationId: String? = null,
    val doctorId: String? = null,
    val messages: List<ChatMessage>? = null,
    val input: String = "",
    val isSending: Boolean = false,
    val error: StringResource? = null,
    val notice: StringResource? = null,
) {
    val canAddress get() = conversationId != null || doctorId != null

    val canSend get() = input.isNotBlank() && !isSending && canAddress
}
