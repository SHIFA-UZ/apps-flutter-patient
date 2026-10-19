package shifa.uz.ui.shifaai

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.CopilotChat

/**
 * [chat] is the open one (null: the list shows); [streaming] is its reply so far, "" before the first token.
 * [recording] and [isTranscribing]: the mic's voice input (AI-02).
 */
@Stable
data class ShifaAiUiState(
    val chats: List<CopilotChat> = emptyList(),
    val query: String = "",
    val chat: CopilotChat? = null,
    val input: String = "",
    val streaming: String? = null,
    val error: StringResource? = null,
    val notice: StringResource? = null,
    val rateLimited: Boolean = false,
    val recording: Boolean = false,
    val isTranscribing: Boolean = false,
) {
    /** Chats with a message matching [query]. */
    val shown get() =
        query.trim().let {
                q ->
            chats.filter { chat -> chat.messages.any { it.content.contains(q, ignoreCase = true) } }
        }

    val isSending get() = streaming != null

    // 09 §5.1: a rate limit keeps the input off until the chat is reopened.
    val canSend get() = input.isNotBlank() && !isSending && !rateLimited && !recording && !isTranscribing
}
