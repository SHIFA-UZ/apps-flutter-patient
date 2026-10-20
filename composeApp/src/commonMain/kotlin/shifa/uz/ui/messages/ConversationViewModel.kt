package shifa.uz.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.chat.ChatRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.ChatMessage
import shifa.uz.data.model.SendMessageRequest

/** 08 §5.4 (CHAT-02): one chat. Received messages are marked read on open and as new ones come in. */
class ConversationViewModel(
    target: ChatTarget,
    private val chat: ChatRepository = ChatRepository(),
    private val pollMs: Long = 5_000,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            ConversationUiState(
                target.name,
                target.photoUrl,
                target.conversationId,
                target.doctorId,
                messages = if (target.conversationId == null) emptyList() else null,
            ),
        )
    val state: StateFlow<ConversationUiState> = _state.asStateFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    // The last fetch, and what was sent since (08 §5.4: kept until a fetch has them).
    private var server = emptyList<ChatMessage>()
    private var sent = emptyList<ChatMessage>()
    private var opened = false
    private var readUpTo: String? = null

    private fun update(transform: ConversationUiState.() -> ConversationUiState) {
        _state.value = _state.value.transform()
    }

    private fun shown() = (server + sent.filter { s -> server.none { it.id == s.id } }).sortedBy { it.createdAt }

    /** C2. A failed poll shows its error until the next one works. */
    private suspend fun refresh(id: String) {
        chat.conversation(id).fold(
            onSuccess = { c ->
                server = c.messages
                sent = sent.filter { s -> server.none { it.id == s.id } }
                update {
                    copy(
                        name = c.contact?.participantName?.ifBlank { null } ?: name,
                        photoUrl = c.contact?.participantPhotoUrl ?: photoUrl,
                        messages = shown(),
                        error = null,
                    )
                }
                // C4, its errors swallowed (08 §2.4).
                val newest = shown().lastOrNull { !it.isMine }?.id
                if (!opened || newest != readUpTo) {
                    opened = true
                    readUpTo = newest
                    chat.markRead(id)
                }
            },
            onFailure = { fail(it) },
        )
    }

    /** C2 every [pollMs], until cancelled: the screen runs it while it's shown. */
    suspend fun poll() {
        while (true) {
            _state.value.conversationId?.let { refresh(it) }
            delay(pollMs)
        }
    }

    fun edit(input: String) = update { copy(input = input) }

    /** For what isn't built yet (voice, files). */
    fun showNotice(notice: StringResource) = update { copy(notice = notice, error = null) }

    fun showError(error: StringResource) = update { copy(error = error, notice = null) }

    /** C3, one at a time. A failed send keeps the text for a retry. */
    fun send() {
        val state = _state.value
        if (!state.canSend) return
        update { copy(isSending = true, error = null, notice = null) }
        viewModelScope.launch { deliver(state.input.trim()) }
    }

    /** 08 §6.2: C7, then C3 as an image with the typed text as its caption. */
    fun pick(
        fileName: String,
        bytes: ByteArray,
    ) {
        val state = _state.value
        if (state.isSending || !state.canAddress) return
        update { copy(isSending = true, error = null, notice = null) }
        viewModelScope.launch {
            chat.upload(bytes, fileName).fold(
                onSuccess = { deliver(_state.value.input.trim(), it, fileName) },
                onFailure = { fail(it) { copy(isSending = false) } },
            )
        }
    }

    /**
     * The input clears once it's sent, unless it was edited meanwhile. A new chat's first message switches it to
     * the chat it made (Flutter's never did, 08 §6.4). ponytail: an echo without a `chatId` keeps the doctor id,
     * so no polling; refetch C1 for the doctor's chat if the backend does that.
     */
    private suspend fun deliver(
        text: String,
        imageUrl: String? = null,
        imageName: String? = null,
    ) {
        val state = _state.value
        chat
            .send(
                SendMessageRequest(
                    conversationId = state.conversationId?.toLongOrNull(),
                    recipientDoctorId = state.doctorId?.toLongOrNull()?.takeIf { state.conversationId == null },
                    text = text.ifBlank { null },
                    type = imageUrl?.let { "image" },
                    attachmentUrl = imageUrl,
                    attachmentName = imageName,
                ),
            ).fold(
                onSuccess = { message ->
                    message?.let { sent = sent + it }
                    val id = state.conversationId ?: message?.chatId
                    update {
                        copy(
                            conversationId = id,
                            doctorId = doctorId.takeIf { id == null },
                            messages = shown(),
                            input = if (input.trim() == text) "" else input,
                            isSending = false,
                        )
                    }
                    // Its echo didn't parse: the fetch brings it.
                    if (message == null) id?.let { refresh(it) }
                },
                onFailure = { fail(it) { copy(isSending = false) } },
            )
    }

    private suspend fun fail(
        e: Throwable,
        done: ConversationUiState.() -> ConversationUiState = { this },
    ) {
        update { done().copy(error = e.toUserMessage()) }
        if (e is ApiException.Unauthorized) _signedOut.send(Unit)
    }
}
