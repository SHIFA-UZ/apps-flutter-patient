package shifa.uz.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.allNotificationsMarkedAsRead
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

    init {
        load()
    }

    private fun update(transform: NotificationsUiState.() -> NotificationsUiState) {
        _state.value = _state.value.transform()
    }

    /** N1. The list stays on screen meanwhile. */
    fun load() {
        if (_state.value.isLoading) return
        update { copy(isLoading = true) }
        viewModelScope.launch {
            notifications.getNotifications().fold(
                onSuccess = { update { copy(notifications = it, isLoading = false, error = null) } },
                onFailure = { fail(it) },
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

    /** N4. */
    fun markAllRead() {
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
 * ponytail: payment, sign form, treatment plan and visit summary aren't built, so those get no link.
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
            kind.startsWith("DOCUMENT_ACCESS_") -> document(n.document) ?: ToDocuments
            n.appointment != null && n.document == null ->
                bookings.getAppointments().getOrNull()?.find { it.id == n.appointment }?.let(::ToAppointment)
                    ?: ToBookings
            else -> document(n.document) ?: n.document?.let { ToDocuments }
        }
    }

    private suspend fun document(id: String?) =
        id?.let { documents.getDocuments().getOrNull()?.find { it.id == id && it.file != null } }?.let(::ToDocument)

    private companion object {
        // DOCUMENT_ACCESS_REQUEST is approved or rejected inline (NOTIF-02, not built).
        val UNBUILT =
            listOf(
                "CONSULTATION_PAYMENT_",
                "TREATMENT_PLAN_",
                "INSTALLMENT_",
                "SIGNATURE_REQUESTED",
                "AI_",
                "DOCUMENT_ACCESS_REQUEST",
            )
    }
}
