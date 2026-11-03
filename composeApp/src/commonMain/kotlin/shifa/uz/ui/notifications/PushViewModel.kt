package shifa.uz.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import shifa.uz.data.analytics.Analytics
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.AnalyticsTracker
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.documents.DocumentsRepository
import shifa.uz.data.model.AppNotification
import shifa.uz.data.notifications.NotificationsRepository
import shifa.uz.data.notifications.Push
import shifa.uz.data.tasks.TasksRepository

/**
 * 13 §3.1-3.3 (NOTIF-03, NOTIF-04), for the signed-in shell: sends the FCM token (P7), and routes a tapped push
 * once signed in, cold start included. The tap marks it read (N3, best effort).
 */
class PushViewModel(
    private val notifications: NotificationsRepository = NotificationsRepository(),
    bookings: BookingsRepository = BookingsRepository(),
    documents: DocumentsRepository = DocumentsRepository(),
    tasks: TasksRepository = TasksRepository(),
    token: Flow<String?> = Push.token,
    private val opened: MutableStateFlow<Map<String, String>?> = Push.opened,
    arrived: Flow<String?> = Push.arrived,
    private val analytics: AnalyticsTracker = Analytics,
) : ViewModel() {
    private val links = NotificationLinks(bookings, documents, tasks)

    /** Where a tapped push leads; null is the inbox (13 §3.3). */
    private val _open = Channel<NotificationLink?>(Channel.BUFFERED)
    val open: Flow<NotificationLink?> = _open.receiveAsFlow()

    /** A push arrived or N3 went through, so the bell's count is stale. */
    private val _stale = Channel<Unit>(Channel.CONFLATED)
    val stale: Flow<Unit> = _stale.receiveAsFlow()

    init {
        viewModelScope.launch {
            token.filterNotNull().collect {
                notifications.registerPushToken(it).onSuccess { analytics.track(AnalyticsEvent.PushTokenRegistered) }
            }
        }
        viewModelScope.launch {
            arrived.collect {
                analytics.track(AnalyticsEvent.PushReceived(it))
                _stale.send(Unit)
            }
        }
        viewModelScope.launch {
            opened.filterNotNull().collect { data ->
                opened.value = null
                val n = AppNotification.fromPush(data)
                // ponytail: a string id isn't marked read; N3 takes an Int (13 §3.3).
                if (n.id > 0) launch { notifications.markRead(n.id).onSuccess { _stale.send(Unit) } }
                val link = links.of(n)
                analytics.track(AnalyticsEvent.PushOpened(data["type"], link?.routedTo() ?: "inbox"))
                _open.send(link)
            }
        }
    }
}
