package shifa.uz.ui.debug

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.wheneverBlocking
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.chat.ChatRepository
import shifa.uz.data.documents.DocumentsRepository
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.PatientDocument
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.tasks.TasksRepository

@OptIn(ExperimentalCoroutinesApi::class)
class DebugViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val bookings = mock<BookingsRepository>()
    private val tasks = mock<TasksRepository>()
    private val documents = mock<DocumentsRepository>()
    private val chat = mock<ChatRepository>()
    private val now = Instant.parse("2026-10-11T09:00:00Z")
    private var demo: Boolean? = null
    private var resets = 0

    private val classToTest by lazy {
        DebugViewModel(
            bookings,
            tasks,
            documents,
            chat,
            MutableStateFlow(true),
            setDemo = { demo = it },
            resetDemo = { resets++ },
            clock = { now },
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { bookings.getAppointments() } doReturn
            Result.success(
                listOf(
                    Appointment("past", startAt = "2026-10-01T09:00:00Z", status = "COMPLETED"),
                    Appointment("later", startAt = "2026-10-20T09:00:00Z"),
                    Appointment("next", startAt = "2026-10-12T09:00:00Z", status = "REQUESTED"),
                    Appointment("cancelled", startAt = "2026-10-11T12:00:00Z", status = "CANCELLED"),
                ),
            )
        wheneverBlocking { tasks.getMyTasks() } doReturn
            Result.success(
                listOf(
                    RemoteCareTask("done", startDate = "2026-10-01", status = "COMPLETED"),
                    RemoteCareTask("open", startDate = "2026-10-01"),
                ),
            )
        wheneverBlocking { documents.getDocuments() } doReturn Result.success(listOf(PatientDocument("d1")))
        wheneverBlocking { chat.conversations() } doReturn Result.failure(Exception())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fake pushes link the next open visit, an open task and the first document, without a notification id`() =
        runTest {
            classToTest.state.test {
                awaitItem()
                advanceUntilIdle()
                val state = expectMostRecentItem()

                assertThat(state.demo).isTrue()
                assertThat(state.appointmentId).isEqualTo("next")
                assertThat(state.taskId).isEqualTo("open")
                assertThat(state.documentId).isEqualTo("d1")
                assertThat(state.chatId).isNull()
                val chatPush = state.pushes.single { it.type == "NEW_MESSAGE" }.data
                assertThat(chatPush).doesNotContainKey("chatId")
                assertThat(state.pushes.first().data).containsExactly(
                    "type",
                    "APPOINTMENT_CONFIRMED",
                    "title",
                    "Appointment confirmed",
                    "message",
                    "Your visit is confirmed.",
                    "appointmentId",
                    "next",
                )
            }
        }

    @Test
    fun `the switch sets demo mode, reset reseeds, and a blocked push says why`() =
        runTest {
            classToTest.onDemoChange(false)
            assertThat(demo).isFalse()

            classToTest.resetDemoData()
            advanceUntilIdle()
            assertThat(resets).isEqualTo(1)
            assertThat(classToTest.state.value.noticeIsError).isFalse()

            classToTest.onPushed(false)
            assertThat(classToTest.state.value.noticeIsError).isTrue()
        }
}
