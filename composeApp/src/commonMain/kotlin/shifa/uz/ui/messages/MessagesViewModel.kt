package shifa.uz.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.failedToStartChat
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.chat.ChatRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.ChatUser

/** 08 §5.3 (CHAT-01): the Messages tab, the chats with doctors. Lives in the tab shell. */
class MessagesViewModel(
    private val chat: ChatRepository = ChatRepository(),
    private val debounceMs: Long = 400,
) : ViewModel() {
    private val _state = MutableStateFlow(MessagesUiState())
    val state: StateFlow<MessagesUiState> = _state.asStateFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private val _open = Channel<ChatTarget>(Channel.BUFFERED)

    /** A doctor picked from the search, as their chat or a new one. */
    val open: Flow<ChatTarget> = _open.receiveAsFlow()

    private var searching: Job? = null

    private fun update(transform: MessagesUiState.() -> MessagesUiState) {
        _state.value = _state.value.transform()
    }

    /** C1, on first open, pull to refresh and when a chat closes. The list stays on screen meanwhile. */
    fun load() {
        if (_state.value.isLoading) return
        update { copy(isLoading = true) }
        viewModelScope.launch {
            chat.conversations().fold(
                onSuccess = { update { copy(isLoading = false, conversations = it, error = null) } },
                onFailure = { fail(it) { copy(isLoading = false) } },
            )
        }
    }

    /** C5, after a pause in typing (Flutter called it on every key). A blank query shows the chats again. */
    fun search(query: String) {
        searching?.cancel()
        update { copy(query = query, results = null, isSearching = query.isNotBlank()) }
        if (query.isBlank()) return
        searching =
            viewModelScope.launch {
                delay(debounceMs)
                chat.search(query.trim()).fold(
                    onSuccess = { update { copy(isSearching = false, results = it, error = null) } },
                    onFailure = { fail(it) { copy(isSearching = false) } },
                )
            }
    }

    /** 08 §5.3 `_startConversation`: the list is refetched for [user]'s chat; without one a new chat opens. */
    fun start(user: ChatUser) {
        if (_state.value.isStarting) return
        update { copy(isStarting = true) }
        viewModelScope.launch {
            chat.conversations().fold(
                onSuccess = { list ->
                    update { copy(isStarting = false, conversations = list, error = null) }
                    val found = list.find { it.participantId == user.id }
                    _open.send(ChatTarget(found?.id, user.id.takeIf { found == null }, user.name, user.photoUrl))
                },
                onFailure = {
                    update { copy(isStarting = false, error = Res.string.failedToStartChat) }
                    if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                },
            )
        }
    }

    private suspend fun fail(
        e: Throwable,
        done: MessagesUiState.() -> MessagesUiState,
    ) {
        update { done().copy(error = e.toUserMessage()) }
        if (e is ApiException.Unauthorized) _signedOut.send(Unit)
    }
}
