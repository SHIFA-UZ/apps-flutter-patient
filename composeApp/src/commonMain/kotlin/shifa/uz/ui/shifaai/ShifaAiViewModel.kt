package shifa.uz.ui.shifaai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.copilotDailyLimit
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.copilot.CopilotRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.CopilotChat
import shifa.uz.data.model.CopilotMessage
import shifa.uz.data.storage.AppLanguage
import shifa.uz.data.storage.CopilotStorage
import shifa.uz.data.storage.languageStorage
import shifa.uz.utils.getCurrentInstant

/** 09 §5.1 (AI-01): the chat list and the open chat. Lives in the tab shell, so the list survives tab switches. */
class ShifaAiViewModel(
    private val copilot: CopilotRepository = CopilotRepository(),
    private val storage: CopilotStorage = CopilotStorage(),
    private val language: StateFlow<AppLanguage> = languageStorage.language,
    private val now: () -> Long = { getCurrentInstant().toEpochMilliseconds() },
) : ViewModel() {
    private val _state = MutableStateFlow(ShifaAiUiState(chats = storage.load()))
    val state: StateFlow<ShifaAiUiState> = _state.asStateFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private var reply: Job? = null

    private fun update(transform: ShifaAiUiState.() -> ShifaAiUiState) {
        _state.value = _state.value.transform()
    }

    fun search(query: String) = update { copy(query = query) }

    fun edit(input: String) = update { copy(input = input) }

    /** Opens a saved chat, or with no [id] a new one, saved once it has a message. Reopening lifts a rate limit. */
    fun open(id: String? = null) {
        val chat = _state.value.chats.firstOrNull { it.id == id } ?: CopilotChat(now().toString())
        update { copy(chat = chat, input = "", error = null, notice = null, rateLimited = false) }
    }

    /** Leaving stops the reply (09 §8); its question stays, unanswered. */
    fun close() {
        reply?.cancel()
        update { copy(chat = null, streaming = null, error = null, notice = null) }
    }

    fun delete() {
        val id = _state.value.chat?.id ?: return
        close()
        update { copy(chats = storage.save(chats.filterNot { it.id == id })) }
    }

    /** For what isn't built yet (voice, attachments). */
    fun showNotice(notice: StringResource) = update { copy(notice = notice, error = null) }

    /** X1: the reply streams into [ShifaAiUiState.streaming], then joins the chat. */
    fun send() {
        val state = _state.value
        val open = state.chat ?: return
        if (!state.canSend) return
        val asked = put(open.copy(messages = open.messages + CopilotMessage(CopilotMessage.USER, state.input.trim())))
        update { copy(input = "", streaming = "", error = null, notice = null) }
        reply =
            viewModelScope.launch {
                copilot.stream(
                    asked.messages,
                    language.value.copilotCode,
                ) { update { copy(streaming = streaming?.plus(it)) } }.fold(
                    onSuccess = {
                        update { copy(streaming = null) }
                        // 09 Q14: an empty reply isn't a message.
                        if (it.isNotEmpty()) {
                            put(
                                asked.copy(messages = asked.messages + CopilotMessage(CopilotMessage.ASSISTANT, it)),
                            )
                        }
                    },
                    onFailure = {
                        val limited = it is ApiException.RateLimit
                        update {
                            copy(
                                streaming = null,
                                rateLimited = limited,
                                error = if (limited) Res.string.copilotDailyLimit else it.toUserMessage(),
                            )
                        }
                        if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                    },
                )
            }
    }

    // Saves [chat] at the top of the list; returns it as stored.
    private fun put(chat: CopilotChat): CopilotChat {
        val saved =
            storage.save(
                listOf(chat.copy(updatedAt = now())) + _state.value.chats.filterNot { it.id == chat.id },
            )
        update { copy(chats = saved, chat = saved.first().takeIf { it.id == this.chat?.id } ?: this.chat) }
        return saved.first()
    }
}

// 09 §2: the backend's OutputLanguage; German gets English.
private val AppLanguage.copilotCode
    get() =
        when (this) {
            AppLanguage.Russian -> "RU"
            AppLanguage.Uzbek -> "UZ"
            else -> "EN"
        }
