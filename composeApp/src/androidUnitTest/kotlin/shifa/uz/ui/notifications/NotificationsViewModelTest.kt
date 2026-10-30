package shifa.uz.ui.notifications

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.allNotificationsMarkedAsRead
import shifa.composeapp.generated.resources.errorNetworkConnection
import shifa.uz.data.api.NETWORK_ERROR
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.documents.DocumentsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.AppNotification
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Page
import shifa.uz.data.model.PatientDocument
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.notifications.NotificationsRepository
import shifa.uz.data.tasks.TasksRepository

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val notifications = mock<NotificationsRepository>()
    private val bookings = mock<BookingsRepository>()
    private val documents = mock<DocumentsRepository>()
    private val tasks = mock<TasksRepository>()

    private val reminder =
        AppNotification(1, "", "", "appointment_reminder", "2026-09-28T08:00:00Z", appointmentId = "a1")
    private val document = AppNotification(2, "", "", "NEW_DOCUMENT", "2026-09-28T07:00:00Z", documentId = "d1")
    private val task = AppNotification(3, "", "", "TASK_ASSIGNED", "2026-09-27T07:00:00Z", taskId = "t1")
    private val read = AppNotification(4, "", "", "TASK_COMPLETED", "2026-09-26T07:00:00Z", readAt = "x", taskId = "t1")
    private val all = listOf(read, task, reminder, document)
    private val appointment = Appointment(id = "a1")
    private val file = PatientDocument("d1", fileUrl = "https://f/1")
    private val care = RemoteCareTask(id = "t1", startDate = "2026-09-20")
    private lateinit var classToTest: NotificationsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { notifications.getNotificationsPage(any(), any()) } doReturn Result.success(Page(all, true))
        wheneverBlocking { notifications.markRead(any()) } doReturn Result.success(Unit)
        wheneverBlocking { notifications.markAllRead() } doReturn Result.success(Unit)
        wheneverBlocking { bookings.getAppointments() } doReturn Result.success(listOf(appointment))
        wheneverBlocking { documents.getDocuments() } doReturn Result.success(listOf(file))
        wheneverBlocking { tasks.getMyTasks() } doReturn Result.success(listOf(care))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = classToTest.state.value

    private fun TestScope.open() {
        classToTest = NotificationsViewModel(notifications, bookings, documents, tasks)
        advanceUntilIdle()
    }

    private fun TestScope.tap(notification: AppNotification): NotificationLink? {
        classToTest.open(notification)
        advanceUntilIdle()
        return state.link
    }

    @Test
    fun `N1 loads newest first, and a filter keeps its types`() =
        runTest {
            open()

            assertThat(state.shownIn(NotificationFilter.All)).containsExactly(reminder, document, task, read).inOrder()
            assertThat(state.shownIn(NotificationFilter.Appointments)).containsExactly(reminder)
            assertThat(state.shownIn(NotificationFilter.Tasks)).containsExactly(task, read).inOrder()
            listOf("CONSULTATION_PAYMENT_DUE_1H", "INSTALLMENT_OVERDUE").forEach { kind ->
                assertThat(NotificationFilter.entries.filter { it.matches(kind) })
                    .containsExactly(NotificationFilter.All, NotificationFilter.Payments)
            }
        }

    @Test
    fun `a tap marks an unread one read once, and tells the bell`() =
        runTest {
            open()

            classToTest.read.test {
                tap(reminder)
                awaitItem()
            }
            assertThat(state.notifications!!.first { it.id == 1 }.isRead).isTrue()
            assertThat(state.selected).isEqualTo(reminder)

            tap(read)
            verifyBlocking(notifications, times(1)) { markRead(any()) }
        }

    @Test
    fun `the tray links the item found in its list`() =
        runTest {
            open()

            assertThat(tap(reminder)).isEqualTo(NotificationLink.ToAppointment(appointment))
            assertThat(tap(document)).isEqualTo(NotificationLink.ToDocument(file))
            assertThat(tap(task)).isEqualTo(NotificationLink.ToTask(care))
        }

    @Test
    fun `an access request shows its document, and a signature request opens the visit to sign`() =
        runTest {
            open()

            val access =
                AppNotification(5, "", "", "DOCUMENT_ACCESS_REQUEST", "2026-09-28T07:00:00Z", documentId = "d1")
            assertThat(tap(access)).isEqualTo(NotificationLink.ToDocument(file))
            val sign = AppNotification(6, "", "", "SIGNATURE_REQUESTED", "2026-09-28T07:00:00Z", appointmentId = "a1")
            assertThat(tap(sign)).isEqualTo(NotificationLink.ToSigning(appointment))
            // A 025-2 form's signing isn't built.
            assertThat(tap(sign.copy(id = 7, patientFormId = "f1"))).isNull()
        }

    @Test
    fun `a visit summary opens the Documents tab`() =
        runTest {
            open()

            val ready =
                AppNotification(8, "", "", "AI_VISIT_SUMMARY_READY", "2026-09-28T07:00:00Z", appointmentId = "a1")
            assertThat(tap(ready)).isEqualTo(NotificationLink.ToDocuments)
        }

    @Test
    fun `an item not in its list opens the list, and a completed task opens My Tasks`() =
        runTest {
            wheneverBlocking { bookings.getAppointments() } doReturn Result.success(emptyList())
            wheneverBlocking { documents.getDocuments() } doReturn
                Result.failure(ApiException.NetworkError(NETWORK_ERROR))
            open()

            assertThat(tap(reminder)).isEqualTo(NotificationLink.ToBookings)
            assertThat(tap(document)).isEqualTo(NotificationLink.ToDocuments)
            assertThat(tap(read)).isEqualTo(NotificationLink.ToTasks)
        }

    @Test
    fun `unbuilt routes get no button`() =
        runTest {
            open()

            val payment =
                AppNotification(9, "", "", "CONSULTATION_PAYMENT_DUE_1H", "2026-09-28T08:00:00Z", appointmentId = "a1")
            assertThat(tap(payment)).isNull()
            verifyBlocking(bookings, never()) { getAppointments() }

            classToTest.close()
            assertThat(state.selected).isNull()
        }

    @Test
    fun `mark all reads everything with the notice`() =
        runTest {
            open()

            classToTest.read.test {
                classToTest.markAllRead()
                advanceUntilIdle()
                awaitItem()
            }
            assertThat(state.notifications!!.all { it.isRead }).isTrue()
            assertThat(state.notice).isEqualTo(Res.string.allNotificationsMarkedAsRead)
        }

    @Test
    fun `the next page is appended once, without repeating an item, until the last one`() =
        runTest {
            val older = AppNotification(9, "", "", "NEW_DOCUMENT", "2026-09-25T07:00:00Z")
            wheneverBlocking { notifications.getNotificationsPage(eq(0), any()) } doReturn
                Result.success(Page(all, false))
            wheneverBlocking { notifications.getNotificationsPage(eq(1), any()) } doReturn
                Result.success(Page(listOf(read, older), true))
            open()
            assertThat(state.hasMore).isTrue()

            classToTest.loadMore()
            classToTest.loadMore() // ignored while the first is in flight
            advanceUntilIdle()

            assertThat(state.notifications).isEqualTo(all + older)
            assertThat(state.page).isEqualTo(1)
            assertThat(state.hasMore).isFalse()
            verifyBlocking(notifications, times(1)) { getNotificationsPage(eq(1), any()) }

            classToTest.loadMore()
            advanceUntilIdle()
            verifyBlocking(notifications, times(1)) { getNotificationsPage(eq(1), any()) }

            // A reload starts over from the first page.
            classToTest.load()
            advanceUntilIdle()
            assertThat(state.notifications).isEqualTo(all)
            assertThat(state.hasMore).isTrue()
        }

    @Test
    fun `a failed next page stops the loading, and a refresh drops a page still on its way`() =
        runTest {
            wheneverBlocking { notifications.getNotificationsPage(eq(0), any()) } doReturn
                Result.success(Page(all, false))
            wheneverBlocking { notifications.getNotificationsPage(eq(1), any()) } doReturn
                Result.failure(ApiException.NetworkError(NETWORK_ERROR))
            open()

            classToTest.loadMore()
            advanceUntilIdle()
            assertThat(state.hasMore).isFalse()
            assertThat(state.isLoadingMore).isFalse()
            assertThat(state.error).isEqualTo(Res.string.errorNetworkConnection)
            assertThat(state.notifications).isEqualTo(all)

            // Pull to refresh starts over; a page 1 that is still loading when it happens is dropped.
            val gate = CompletableDeferred<Unit>()
            val older = AppNotification(9, "", "", "NEW_DOCUMENT", "2026-09-25T07:00:00Z")
            wheneverBlocking { notifications.getNotificationsPage(eq(1), any()) } doSuspendableAnswer {
                gate.await()
                Result.success(Page(listOf(older), true))
            }
            classToTest.load()
            advanceUntilIdle()
            assertThat(state.hasMore).isTrue()
            classToTest.loadMore()
            runCurrent() // the request is out
            verifyBlocking(notifications, times(2)) { getNotificationsPage(eq(1), any()) }
            classToTest.load()
            gate.complete(Unit)
            advanceUntilIdle()

            assertThat(state.notifications).isEqualTo(all)
            assertThat(state.page).isEqualTo(0)
            assertThat(state.hasMore).isTrue()
            assertThat(state.isLoadingMore).isFalse()
        }

    @Test
    fun `Mark all read drops a page on its way and blocks the next until it is done`() =
        runTest {
            wheneverBlocking { notifications.getNotificationsPage(eq(0), any()) } doReturn
                Result.success(Page(all, false))
            val gate = CompletableDeferred<Unit>()
            val unreadOlder = AppNotification(9, "", "", "NEW_DOCUMENT", "2026-09-25T07:00:00Z")
            wheneverBlocking { notifications.getNotificationsPage(eq(1), any()) } doSuspendableAnswer {
                gate.await()
                Result.success(Page(listOf(unreadOlder), true))
            }
            val marked = CompletableDeferred<Unit>()
            wheneverBlocking { notifications.markAllRead() } doSuspendableAnswer {
                marked.await()
                Result.success(Unit)
            }
            open()
            classToTest.loadMore()
            runCurrent()

            classToTest.markAllRead()
            runCurrent()
            assertThat(state.isMarkingRead).isTrue()
            classToTest.loadMore() // refused while N4 runs
            classToTest.load() // so is a refresh
            classToTest.markAllRead() // and a second N4
            gate.complete(Unit)
            marked.complete(Unit)
            advanceUntilIdle()

            assertThat(state.isMarkingRead).isFalse()
            assertThat(state.notifications!!.map { it.id }).containsExactly(4, 3, 1, 2).inOrder()
            assertThat(state.notifications!!.all { it.isRead }).isTrue()
            verifyBlocking(notifications, times(1)) { getNotificationsPage(eq(1), any()) }
            verifyBlocking(notifications, times(1)) { getNotificationsPage(eq(0), any()) }
            verifyBlocking(notifications, times(1)) { markAllRead() }
        }

    @Test
    fun `Mark all read waits for the first list`() =
        runTest {
            classToTest = NotificationsViewModel(notifications, bookings, documents, tasks)
            classToTest.markAllRead() // before the first page is in: ignored, the list still loads
            advanceUntilIdle()

            assertThat(state.notifications).isEqualTo(all)
            assertThat(state.isMarkingRead).isFalse()
            verifyBlocking(notifications, never()) { markAllRead() }
        }

    @Test
    fun `a failed reload keeps the list and shows the error`() =
        runTest {
            open()
            wheneverBlocking { notifications.getNotificationsPage(any(), any()) } doReturn
                Result.failure(ApiException.NetworkError(NETWORK_ERROR))

            classToTest.load()
            advanceUntilIdle()

            assertThat(state.notifications).isEqualTo(all)
            assertThat(state.error).isEqualTo(Res.string.errorNetworkConnection)
        }

    @Test
    fun `a 401 signs out`() =
        runTest {
            wheneverBlocking { notifications.getNotificationsPage(any(), any()) } doReturn
                Result.failure(ApiException.Unauthorized("Unauthorized"))
            classToTest = NotificationsViewModel(notifications, bookings, documents, tasks)

            classToTest.signedOut.test {
                advanceUntilIdle()
                awaitItem()
            }
        }

    @Test
    fun `an access request message splits into its parts`() {
        assertThat(accessRequestParts("""Dr. Aziz requested access to "MRI scan" for Ali Valiyev."""))
            .containsExactly("Dr. Aziz", "MRI scan", "Ali Valiyev")
            .inOrder()
        assertThat(accessRequestParts("Something else")).isNull()
    }
}
