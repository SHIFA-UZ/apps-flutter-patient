package shifa.uz.ui.messages

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.ChatContact
import shifa.uz.data.model.ChatUser

/** The chat to open: [conversationId], or with only [doctorId] a new one, made by its first message (08 §6.4). */
data class ChatTarget(
    val conversationId: String?,
    val doctorId: String? = null,
    val name: String = "",
    val photoUrl: String? = null,
)

/**
 * [conversations] is null until the first load; a failed reload keeps the previous list.
 * A non-blank [query] shows the doctors found ([results], null until the search answers) instead.
 */
@Stable
data class MessagesUiState(
    val conversations: List<ChatContact>? = null,
    val isLoading: Boolean = false,
    val query: String = "",
    val results: List<ChatUser>? = null,
    val isSearching: Boolean = false,
    val isStarting: Boolean = false,
    val error: StringResource? = null,
) {
    val isSearch get() = query.isNotBlank()

    /** Newest first; chats without a message last. */
    val sorted get() = conversations?.sortedByDescending { it.lastMessageAt }
}
