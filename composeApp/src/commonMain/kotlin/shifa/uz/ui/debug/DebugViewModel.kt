package shifa.uz.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import shifa.uz.data.auth.AuthRepository
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.chat.ChatRepository
import shifa.uz.data.debug.Backend
import shifa.uz.data.debug.BackendChoice
import shifa.uz.data.debug.DemoBackend
import shifa.uz.data.debug.DemoMode
import shifa.uz.data.documents.DocumentsRepository
import shifa.uz.data.tasks.TasksRepository
import shifa.uz.utils.getCurrentInstant

/** Debug builds only (Profile → Debug menu). Turning demo data on or off restarts the screens (App keys on it). */
class DebugViewModel(
    private val bookings: BookingsRepository = BookingsRepository(),
    private val tasks: TasksRepository = TasksRepository(),
    private val documents: DocumentsRepository = DocumentsRepository(),
    private val chat: ChatRepository = ChatRepository(),
    demo: StateFlow<Boolean> = DemoMode.enabled,
    private val setDemo: (Boolean) -> Unit = DemoMode::set,
    private val resetDemo: suspend () -> Unit = { DemoBackend.shared.reset() },
    backend: StateFlow<Backend> = BackendChoice.current,
    private val setBackend: (Backend) -> Unit = BackendChoice::set,
    private val signOut: suspend () -> Unit = { AuthRepository().logout() },
    private val clock: () -> Instant = ::getCurrentInstant,
) : ViewModel() {
    private val _state =
        MutableStateFlow(DebugUiState(demo = demo.value, development = backend.value == Backend.Development))
    val state: StateFlow<DebugUiState> = _state.asStateFlow()

    init {
        load()
    }

    // The link targets for the fake pushes: the next open visit, an open task, the newest document and chat.
    private fun load() {
        viewModelScope.launch {
            val appointments = async { bookings.getAppointments().getOrNull().orEmpty() }
            val openTasks = async { tasks.getMyTasks().getOrNull().orEmpty().filter { it.isOpen } }
            val docs = async { documents.getDocuments().getOrNull().orEmpty() }
            val chats = async { chat.conversations().getOrNull().orEmpty() }
            val now = clock()
            val visit =
                appointments.await().filter { it.isOpen && !it.isOver(now) }.minByOrNull { it.startAt }
                    ?: appointments.await().firstOrNull()
            _state.value =
                _state.value.copy(
                    appointmentId = visit?.id,
                    taskId = openTasks.await().firstOrNull()?.id,
                    documentId = docs.await().firstOrNull()?.id,
                    chatId = chats.await().firstOrNull()?.id,
                )
        }
    }

    fun onDemoChange(on: Boolean) = setDemo(on)

    /** A token belongs to one backend, so the session ends first (P7 `""` on the old one), then App restarts. */
    fun onDevelopmentChange(on: Boolean) {
        viewModelScope.launch {
            signOut()
            setBackend(if (on) Backend.Development else Backend.Production)
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            resetDemo()
            _state.value = _state.value.copy(notice = "Demo data reset.", noticeIsError = false)
            load()
        }
    }

    fun onPushed(shown: Boolean) {
        _state.value =
            _state.value.copy(
                notice =
                    if (shown) {
                        "Sent: tap it in the notification shade."
                    } else {
                        "Notifications are off for Shifa. Turn them on in the system settings."
                    },
                noticeIsError = !shown,
            )
    }
}
