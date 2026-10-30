package shifa.uz.data.debug

import com.google.common.truth.Truth.assertThat
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Test
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.createHttpClient
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.chat.ChatRepository
import shifa.uz.data.copilot.CopilotRepository
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.BookingRequest
import shifa.uz.data.model.CheckInRequest
import shifa.uz.data.model.CopilotMessage
import shifa.uz.data.model.SendMessageRequest
import shifa.uz.data.notifications.NotificationsRepository
import shifa.uz.data.profile.ProfileRepository
import shifa.uz.data.storage.TokenStorage
import shifa.uz.data.tasks.TasksRepository
import kotlin.time.Duration.Companion.seconds

// The real repositories over the demo engine, so the demo data has to parse like production's.
class DemoBackendTest {
    private var now = Instant.parse("2026-10-11T09:00:00Z")
    private val tashkent = TimeZone.of("Asia/Tashkent")
    private val real =
        MockEngine {
            respond(
                """{"id":16,"fullName":"Nigora Omarova"}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
    private val client =
        createHttpClient(
            TokenStorage(MapSettings()).apply { token = "t0k" },
            DemoBackend({ now }, tashkent, passthrough = { HttpClient(real) }).engine(),
        )

    @Test
    fun `the demo answers on the development host too`() =
        runTest {
            val dev =
                createHttpClient(
                    TokenStorage(MapSettings()).apply { token = "t0k" },
                    DemoBackend({ now }, tashkent, passthrough = { HttpClient(real) }).engine(),
                    ApiConfig.DEV_BASE_URL,
                )

            assertThat(NotificationsRepository(dev).unreadCount().getOrThrow()).isEqualTo(7)
            assertThat(real.requestHistory).isEmpty()
        }

    @Test
    fun `the seed has a visit to join now, a pending one and an unpaid one`() =
        runTest {
            val visits = BookingsRepository(client).getAppointments().getOrThrow()

            assertThat(visits.single { it.isNow(now) }.isVideo).isTrue()
            assertThat(visits.map { it.status }).contains("REQUESTED")
            assertThat(visits.map { it.paymentStatus }).contains("PENDING")
        }

    @Test
    fun `a video booking needs an online price, as on production, and a booking takes its slot`() =
        runTest {
            val bookings = BookingsRepository(client)
            val day = (now.toLocalDateTime(tashkent).date)
            val slot = bookings.getSlots("9001", day, null).getOrThrow().first()

            fun request(service: Long) = BookingRequest(9001, slot.startAt, 30, isVideo = true, serviceId = service)

            assertThat(
                bookings.book(request(9203)).exceptionOrNull(),
            ).isInstanceOf(ApiException.ValidationError::class.java)
            val booked = bookings.book(request(9201)).getOrThrow()

            // MOBILE_BOOKING_PAYMENT §1: a free booking is confirmed at once.
            assertThat(booked.status).isEqualTo("CONFIRMED")
            assertThat(booked.paymentStatus).isEqualTo("NOT_REQUIRED")
            assertThat(bookings.getAppointments().getOrThrow().map { it.id }).contains(booked.id)
            assertThat(
                bookings.getSlots("9001", day, null).getOrThrow().map { it.startAt },
            ).doesNotContain(slot.startAt)
        }

    @Test
    fun `a check-in is due now and submitting completes it`() =
        runTest {
            val tasks = TasksRepository(client)
            assertThat(tasks.getMyTasks().getOrThrow().count { it.isOpen }).isEqualTo(3)
            val due =
                tasks.getCheckIns(
                    "9401",
                ).getOrThrow().filter { it.isPending }.minBy { it.scheduledAt(tashkent)!! }
            assertThat(due.scheduledAt(tashkent)!!).isLessThan(now)

            tasks.submitCheckIn("9401", CheckInRequest(due.id, numericValue = 120.0)).getOrThrow()

            assertThat(tasks.getCheckIns("9401").getOrThrow().single { it.id == due.id }.isPending).isFalse()
        }

    @Test
    fun `reading notifications lowers the count`() =
        runTest {
            val notifications = NotificationsRepository(client)
            assertThat(notifications.unreadCount().getOrThrow()).isEqualTo(7)

            notifications.markRead(9601).getOrThrow()
            assertThat(notifications.unreadCount().getOrThrow()).isEqualTo(6)
            notifications.markAllRead().getOrThrow()
            assertThat(notifications.unreadCount().getOrThrow()).isEqualTo(0)
            assertThat(notifications.getNotifications().getOrThrow()).hasSize(8)
            val first = notifications.getNotificationsPage(0, 5).getOrThrow()
            val second = notifications.getNotificationsPage(1, 5).getOrThrow()
            assertThat(first.items).hasSize(5)
            assertThat(first.last).isFalse()
            assertThat(second.items).hasSize(3)
            assertThat(second.last).isTrue()
        }

    @Test
    fun `a sent message gets a reply a few seconds later`() =
        runTest {
            val chat = ChatRepository(client)
            chat.send(SendMessageRequest(conversationId = 9801, text = "Hi")).getOrThrow()
            assertThat(chat.conversation("9801").getOrThrow().messages.last().isMine).isTrue()

            now += 5.seconds
            val reply = chat.conversation("9801").getOrThrow().messages.last()
            assertThat(reply.isMine).isFalse()
            assertThat(chat.conversations().getOrThrow().single { it.id == "9801" }.unreadCount).isEqualTo(3)
        }

    @Test
    fun `Shifa AI streams a demo reply`() =
        runTest {
            val reply = CopilotRepository(client = client).stream(listOf(CopilotMessage("user", "Hello")), "en") {}

            assertThat(reply.getOrThrow()).startsWith("This is a demo reply.")
        }

    @Test
    fun `GETs it doesn't fake go to the real backend, other writes are refused`() =
        runTest {
            assertThat(DoctorsRepository(client).getDoctor("16").getOrThrow().fullName).isEqualTo("Nigora Omarova")
            assertThat(real.requestHistory.single().headers[HttpHeaders.Authorization]).isEqualTo("Bearer t0k")

            assertThat(ProfileRepository(client).requestDeletion().isFailure).isTrue()
            assertThat(real.requestHistory).hasSize(1)
        }
}
