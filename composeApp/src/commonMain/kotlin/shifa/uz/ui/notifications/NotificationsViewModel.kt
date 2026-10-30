package shifa.uz.ui.notifications

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
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.allNotificationsMarkedAsRead
import shifa.composeapp.generated.resources.checkInSubmittedSuccessfully
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.documents.DocumentsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.AppNotification
import shifa.uz.data.notifications.NotificationsRepository
import shifa.uz.data.tasks.TasksRepository
import shifa.uz.ui.notifications.NotificationLink.ToAppointment
import shifa.uz.ui.notifications.NotificationLink.ToBookings
import shifa.uz.ui.notifications.NotificationLink.ToChat
import shifa.uz.ui.notifications.NotificationLink.ToDocument
import shifa.uz.ui.notifications.NotificationLink.ToDocuments
import shifa.uz.ui.notifications.NotificationLink.ToMessages
import shifa.uz.ui.notifications.NotificationLink.ToSigning
import shifa.uz.ui.notifications.NotificationLink.ToTask
import shifa.uz.ui.notifications.NotificationLink.ToTasks

/** 13 (NOTIF-01): the inbox and its details tray. Loads on each open, like Flutter. */
class NotificationsViewModel(
    private val notifications: NotificationsRepository = NotificationsRepository(),
    bookings: BookingsRepository = BookingsRepository(),
    documents: DocumentsRepository = DocumentsRepository(),
    tasks: TasksRepository = TasksRepository(),
) : ViewModel() {
    private val links = NotificationLinks(bookings, documents, tasks)
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    /** N3 or N4 went through, so the bell's count is stale. */
    private val _read = Channel<Unit>(Channel.CONFLATED)
    val read: Flow<Unit> = _read.receiveAsFlow()

    private var nextPage: Job? = null

    init {
        load()
    }

    private fun update(transform: NotificationsUiState.() -> NotificationsUiState) {
        _state.value = _state.value.transform()
    }

    /** N1a's first page. The list stays on screen meanwhile; a page still on its way is dropped. */
    fun load() {
        if (_state.value.isLoading) return
        nextPage?.cancel()
        update { copy(isLoading = true, isLoadingMore = false) }
        viewModelScope.launch {
            notifications.getNotificationsPage(0).fold(
                onSuccess = {
                    update {
                        copy(
                            notifications = it.items,
                            page = 0,
                            hasMore = !it.last,
                            isLoadingMore = false,
                            isLoading = false,
                            error = null,
                        )
                    }
                },
                onFailure = { fail(it) },
            )
        }
    }

    /**
     * The next N1a page, when the list is scrolled to its end. An item already shown (a new one pushed the pages) is
     * skipped. A page that fails ends the loading (no retry loop); pull to refresh starts over.
     */
    fun loadMore() {
        val current = _state.value
        if (!current.hasMore || current.isLoadingMore || current.isLoading || current.notifications == null) return
        update { copy(isLoadingMore = true) }
        nextPage =
            viewModelScope.launch {
                notifications.getNotificationsPage(current.page + 1).fold(
                    onSuccess = { next ->
                        update {
                            val shown = notifications.orEmpty()
                            val ids = shown.map { it.id }.toSet()
                            copy(
                                notifications = shown + next.items.filter { it.id !in ids },
                                page = current.page + 1,
                                hasMore = !next.last,
                                isLoadingMore = false,
                            )
                        }
                    },
                    onFailure = {
                        update { copy(isLoadingMore = false, hasMore = false) }
                        fail(it)
                    },
                )
            }
    }

    /** 13 §4.1 tap: an unread one is marked read, best effort (quirk 11), and the tray opens. */
    fun open(notification: AppNotification) {
        update { copy(selected = notification, link = null) }
        if (!notification.isRead && notification.id > 0) {
            markedRead { it.id == notification.id }
            viewModelScope.launch { notifications.markRead(notification.id).onSuccess { _read.send(Unit) } }
        }
        viewModelScope.launch {
            val link = links.of(notification)
            if (_state.value.selected == notification) update { copy(link = link) }
        }
    }

    fun close() = update { copy(selected = null, link = null) }

    /** A task notification's check-in went through (T4 is the tray's own). */
    fun onCheckInSubmitted() = update { copy(error = null, notice = Res.string.checkInSubmittedSuccessfully) }

    /** N4. A page still on its way would bring items read before N4 as unread, so it is dropped. */
    fun markAllRead() {
        nextPage?.cancel()
        update { copy(isLoadingMore = false) }
        viewModelScope.launch {
            notifications.markAllRead().fold(
                onSuccess = {
                    markedRead { true }
                    update { copy(error = null, notice = Res.string.allNotificationsMarkedAsRead) }
                    _read.send(Unit)
                },
                onFailure = { fail(it) },
            )
        }
    }

    // Read locally; the next N1 brings the server's readAt.
    private fun markedRead(which: (AppNotification) -> Boolean) =
        update {
            copy(
                notifications = notifications?.map { if (which(it) && !it.isRead) it.copy(readAt = "") else it },
            )
        }

    private suspend fun fail(e: Throwable) {
        update { copy(isLoading = false, notice = null, error = e.toUserMessage()) }
        if (e is ApiException.Unauthorized) _signedOut.send(Unit)
    }
}

/**
 * 13 §4.2's sheet routes that exist here, for the tray and for a tapped push. There's no get-by-id, so the linked
 * item is found in its list; when it isn't there (or the list fails) the list opens instead.
 * ponytail: payment, the 025-2 form and treatment plans aren't built, so those get no link.
 */
internal class NotificationLinks(
    private val bookings: BookingsRepository,
    private val documents: DocumentsRepository,
    private val tasks: TasksRepository,
) {
    suspend fun of(n: AppNotification): NotificationLink? {
        val kind = n.kind
        return when {
            UNBUILT.any { kind.startsWith(it) } -> null
            // 08 §5.1: Flutter opened the list whatever the id; with one, the chat opens here.
            kind == "CHAT_MESSAGE" || kind == "NEW_MESSAGE" -> n.chat?.let(::ToChat) ?: ToMessages
            "TASK" in kind ->
                n.task?.takeIf { kind != "TASK_COMPLETED" }
                    ?.let { id -> tasks.getMyTasks().getOrNull()?.find { it.id == id } }
                    ?.let(::ToTask) ?: ToTasks
            kind == "PROPHYLAXIS_REMINDER" -> ToBookings
            // The user's call: no summary page; the visit's documentation is on the Documents tab.
            kind in VISIT_SUMMARY -> ToDocuments
            // 13 §6.3: a 025-2 form (patientFormId) has its own signing, not built; else the visit's.
            kind == "SIGNATURE_REQUESTED" -> if (n.form == null) appointment(n.appointment)?.let(::ToSigning) else null
            // A request also shows the document asked for (approve / reject is NOTIF-02, not built).
            kind.startsWith("DOCUMENT_ACCESS_") -> document(n.document) ?: ToDocuments
            n.appointment != null && n.document == null ->
                appointment(
                    n.appointment,
                )?.let(::ToAppointment) ?: ToBookings
            else -> document(n.document) ?: n.document?.let { ToDocuments }
        }
    }

    private suspend fun appointment(id: String?) =
        id?.let { bookings.getAppointments().getOrNull()?.find { it.id == id } }

    private suspend fun document(id: String?) =
        id?.let { documents.getDocuments().getOrNull()?.find { it.id == id && it.file != null } }?.let(::ToDocument)

    private companion object {
        val UNBUILT =
            listOf(
                "CONSULTATION_PAYMENT_",
                "TREATMENT_PLAN_",
                "INSTALLMENT_",
            )
        val VISIT_SUMMARY = setOf("AI_VISIT_SUMMARY_READY", "AI_SCRIBE_READY")
    }
}
