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
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.errorRecordingVoice
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.chat.ChatRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.ChatMessage
import shifa.uz.data.model.MessageType
import shifa.uz.data.model.SendMessageRequest
import shifa.uz.utils.getCurrentInstant

/** 08 §5.4 (CHAT-02): one chat. Received messages are marked read on open and as new ones come in. */
class ConversationViewModel(
    target: ChatTarget,
    private val chat: ChatRepository = ChatRepository(),
    private val pollMs: Long = 5_000,
    private val clock: () -> Instant = ::getCurrentInstant,
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
    private var recordingSince: Instant? = null

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

    fun showNotice(notice: StringResource) = update { copy(notice = notice, error = null) }

    fun showError(error: StringResource) = update { copy(error = error, notice = null) }

    /** C3, one at a time. A failed send keeps the text for a retry. */
    fun send() {
        val state = _state.value
        if (!state.canSend) return
        update { copy(isSending = true, error = null, notice = null) }
        viewModelScope.launch { deliver(state.input.trim()) }
    }

    fun onRecording() {
        recordingSince = clock()
        update { copy(recording = true, error = null, notice = null) }
    }

    /** 08 §6.3: the recording goes as `voice_<epochMs>.m4a` with its size and length; none recorded is an error. */
    fun sendVoice(audio: ByteArray?) {
        val seconds = recordingSince?.let { (clock() - it).inWholeSeconds.toInt() } ?: 0
        recordingSince = null
        update { copy(recording = false) }
        if (audio == null) return showError(Res.string.errorRecordingVoice)
        attach("voice_${clock().toEpochMilliseconds()}.m4a", audio, MessageType.Voice, seconds)
    }

    /**
     * 08 §6.2: C7, then C3 as an image, voice or document message with the typed text as its caption.
     * Documents weren't sent by Flutter (08 §1); the backend takes `type: document` (15 §5.9).
     */
    fun attach(
        fileName: String,
        bytes: ByteArray,
        type: MessageType = MessageType.Image,
        duration: Int? = null,
    ) {
        val state = _state.value
        if (state.isSending || !state.canAddress) return
        update { copy(isSending = true, error = null, notice = null) }
        viewModelScope.launch {
            chat.upload(bytes, fileName).fold(
                onSuccess = {
                    deliver(_state.value.input.trim(), Attachment(it, fileName, type, bytes.size.toLong(), duration))
                },
                onFailure = { fail(it) { copy(isSending = false) } },
            )
        }
    }

    private data class Attachment(
        val url: String,
        val name: String,
        val type: MessageType,
        val size: Long,
        val duration: Int?,
    )

    /**
     * The input clears once it's sent, unless it was edited meanwhile. A new chat's first message switches it to
     * the chat it made (Flutter's never did, 08 §6.4). ponytail: an echo without a `chatId` keeps the doctor id,
     * so no polling; refetch C1 for the doctor's chat if the backend does that.
     */
    private suspend fun deliver(
        text: String,
        attachment: Attachment? = null,
    ) {
        val state = _state.value
        chat
            .send(
                SendMessageRequest(
                    conversationId = state.conversationId?.toLongOrNull(),
                    recipientDoctorId = state.doctorId?.toLongOrNull()?.takeIf { state.conversationId == null },
                    text = text.ifBlank { null },
                    type = attachment?.type?.name?.lowercase(),
                    attachmentUrl = attachment?.url,
                    attachmentName = attachment?.name,
                    fileSize = attachment?.size,
                    duration = attachment?.duration,
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
