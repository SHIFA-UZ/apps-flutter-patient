package shifa.uz.ui.notifications

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.documents.DocumentsRepository
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.PatientDocument
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.notifications.NotificationsRepository
import shifa.uz.data.tasks.TasksRepository

@OptIn(ExperimentalCoroutinesApi::class)
class PushViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val notifications = mock<NotificationsRepository>()
    private val bookings = mock<BookingsRepository>()
    private val documents = mock<DocumentsRepository>()
    private val tasks = mock<TasksRepository>()
    private val token = MutableStateFlow<String?>(null)
    private val opened = MutableStateFlow<Map<String, String>?>(null)
    private val arrived = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val appointment = Appointment(id = "a1")
    private val file = PatientDocument("d1", fileUrl = "https://f/1")
    private val care = RemoteCareTask(id = "t1", startDate = "2026-09-20")
    private lateinit var classToTest: PushViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { notifications.registerPushToken(any()) } doReturn Result.success(Unit)
        wheneverBlocking { notifications.markRead(any()) } doReturn Result.success(Unit)
        wheneverBlocking { bookings.getAppointments() } doReturn Result.success(listOf(appointment))
        wheneverBlocking { documents.getDocuments() } doReturn Result.success(listOf(file))
        wheneverBlocking { tasks.getMyTasks() } doReturn Result.success(listOf(care))
        classToTest = PushViewModel(notifications, bookings, documents, tasks, token, opened, arrived)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `P7 sends the token, and again when it refreshes`() =
        runTest {
            advanceUntilIdle()
            verifyBlocking(notifications, never()) { registerPushToken(any()) }

            token.value = "a"
            advanceUntilIdle()
            token.value = "b"
            advanceUntilIdle()

            verifyBlocking(notifications) { registerPushToken("a") }
            verifyBlocking(notifications) { registerPushToken("b") }
        }

    @Test
    fun `a tapped push is marked read and opens what it links, entityId included`() =
        runTest {
            classToTest.open.test {
                opened.value = mapOf("id" to "5", "type" to "APPOINTMENT_REMINDER", "appointmentId" to "a1")
                assertThat(awaitItem()).isEqualTo(NotificationLink.ToAppointment(appointment))
                opened.value = mapOf("notificationId" to "6", "type" to "new-document", "entityId" to "d1")
                assertThat(awaitItem()).isEqualTo(NotificationLink.ToDocument(file))
                opened.value = mapOf("type" to "TASK_ASSIGNED", "entityId" to "t1")
                assertThat(awaitItem()).isEqualTo(NotificationLink.ToTask(care))
            }
            advanceUntilIdle()

            verifyBlocking(notifications) { markRead(5) }
            verifyBlocking(notifications) { markRead(6) }
            verifyBlocking(notifications, times(2)) { markRead(any()) }
            assertThat(opened.value).isNull()
        }

    @Test
    fun `a chat push opens its chat, an unbuilt or untyped one the inbox, and an arrival refreshes the bell`() =
        runTest {
            classToTest.open.test {
                opened.value = mapOf("id" to "uuid", "type" to "CHAT_MESSAGE", "entityId" to "c1")
                assertThat(awaitItem()).isEqualTo(NotificationLink.ToChat("c1"))
                opened.value = mapOf("id" to "uuid2", "type" to "NEW_MESSAGE")
                assertThat(awaitItem()).isEqualTo(NotificationLink.ToMessages)
                opened.value = mapOf("id" to "uuid3", "type" to "TREATMENT_PLAN_CREATED", "entityId" to "p1")
                assertThat(awaitItem()).isNull()
                opened.value = mapOf("title" to "Hi")
                assertThat(awaitItem()).isNull()
            }
            verifyBlocking(notifications, never()) { markRead(any()) }

            classToTest.stale.test {
                arrived.tryEmit(Unit)
                awaitItem()
            }
        }
}
