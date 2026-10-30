// JSON fixtures stay on one line, like the backend sends them.
@file:Suppress("ktlint:standard:max-line-length")

package shifa.uz.data

import com.google.common.truth.Truth.assertThat
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Test
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_LOAD_DOCTOR
import shifa.uz.data.api.createHttpClient
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsQuery
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.BookingRequest
import shifa.uz.data.model.CheckInRequest
import shifa.uz.data.notifications.NotificationsRepository
import shifa.uz.data.profile.ProfileRepository
import shifa.uz.data.storage.TokenStorage
import shifa.uz.data.tasks.TasksRepository

/** The Home, booking and task calls: B1, B2, B3, T1, T3, T4, N1-N4, D1, D2, D3, P1, P7. */
class HomeRepositoriesTest {
    private var reply: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respondOk() }
    private val engine = MockEngine { reply(it) }
    private val client = createHttpClient(TokenStorage(MapSettings()), engine)

    private fun replyJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) {
        reply = { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }
    }

    private val path get() = engine.requestHistory.last().url.encodedPath

    @Test
    fun `a client for the development backend sends there, with the token`() =
        runTest {
            val storage = TokenStorage(MapSettings()).apply { token = "t0k" }
            val dev = createHttpClient(storage, engine, ApiConfig.DEV_BASE_URL)
            replyJson("""{"count":1}""")

            NotificationsRepository(dev).unreadCount().getOrThrow()

            val request = engine.requestHistory.last()
            assertThat(request.url.host).isEqualTo("api-dev.shifa.uz")
            assertThat(request.url.encodedPath).isEqualTo("/api/notifications/unread/count")
            assertThat(request.headers["Authorization"]).isEqualTo("Bearer t0k")
        }

    @Test
    fun `B2 reads numeric ids and a zone suffix, and skips items that don't decode`() =
        runTest {
            replyJson(
                """[{"id":7,"doctorId":3,"startAt":"2026-09-27T10:00:00Z[UTC]","location":"Video Consultation"},""" +
                    """{"id":"bad","doctorName":{}},{"id":"y","startAt":"2026-09-28T09:00:00Z","isVideo":true}]""",
            )

            val list = BookingsRepository(client).getAppointments().getOrThrow()

            assertThat(list.map { it.id }).containsExactly("7", "y").inOrder()
            assertThat(list[0].doctorId).isEqualTo("3")
            assertThat(list[0].start).isEqualTo(Instant.parse("2026-09-27T10:00:00Z"))
            assertThat(list.map { it.isVideo }).containsExactly(true, true)
            assertThat(list[0].isConfirmed).isTrue() // no status
            assertThat(engine.requestHistory.single().url.parameters.isEmpty()).isTrue()
            assertThat(path).isEqualTo("/api/patients/me/appointments")
        }

    @Test
    fun `B2 that isn't an array is no appointments`() =
        runTest {
            replyJson("""{"appointments":[]}""")
            assertThat(BookingsRepository(client).getAppointments().getOrThrow()).isEmpty()
        }

    @Test
    fun `T1 reads the data key or a bare array and skips tasks without a start date`() =
        runTest {
            replyJson("""{"data":[{"id":1,"taskName":"Run","startDate":"2026-09-27"},{"id":2,"taskName":"No date"}]}""")
            assertThat(TasksRepository(client).getMyTasks().getOrThrow().map { it.id }).containsExactly("1")

            replyJson(
                """[{"id":3,"startDate":"2026-09-27","status":"EXPIRED"},{"startDate":"x","status":"COMPLETED"}]""",
            )
            assertThat(TasksRepository(client).getMyTasks().getOrThrow().map { it.isOpen }).containsExactly(true, false)
            assertThat(path).isEqualTo("/api/tasks/my-tasks")
        }

    @Test
    fun `T3 skips slots that don't decode, and an unknown status is pending`() =
        runTest {
            replyJson(
                """[{"id":1,"scheduledDate":"2026-09-28","scheduledTime":"08:00:00","status":"SKIPPED"},""" +
                    """{"id":"bad"},{"id":2,"scheduledDate":"2026-09-28","status":"MISSED"}]""",
            )

            val slots = TasksRepository(client).getCheckIns("7").getOrThrow()

            assertThat(slots.map { it.id }).containsExactly(1L, 2L).inOrder()
            assertThat(slots.map { it.isPending }).containsExactly(true, false).inOrder()
            assertThat(path).isEqualTo("/api/tasks/my-tasks/7/check-ins")
        }

    @Test
    fun `T4 posts only what is set`() =
        runTest {
            TasksRepository(
                client,
            ).submitCheckIn("7", CheckInRequest(9012, numericValue = 128.0, notes = "x")).getOrThrow()

            val request = engine.requestHistory.last()
            assertThat(request.method).isEqualTo(HttpMethod.Post)
            assertThat(path).isEqualTo("/api/tasks/my-tasks/7/check-in")
            assertThat(Json.parseToJsonElement((request.body as TextContent).text))
                .isEqualTo(Json.parseToJsonElement("""{"checkInId":9012,"numericValue":128.0,"notes":"x"}"""))
        }

    @Test
    fun `N2 reads the count`() =
        runTest {
            replyJson("""{"count":12}""")
            assertThat(NotificationsRepository(client).unreadCount().getOrThrow()).isEqualTo(12)
            assertThat(path).isEqualTo("/api/notifications/unread/count")
        }

    @Test
    fun `N1a reads a Spring page, and a bare array as the only page`() =
        runTest {
            replyJson(
                """{"content":[{"id":1,"title":"t","message":"m","type":"X","createdAt":"2026-09-28T08:00:00Z"}],""" +
                    """"number":2,"size":20,"last":false,"totalElements":41}""",
            )

            val page = NotificationsRepository(client).getNotificationsPage(2).getOrThrow()

            assertThat(page.items.map { it.id }).containsExactly(1)
            assertThat(page.last).isFalse()
            assertThat(path).isEqualTo("/api/notifications/page")
            assertThat(engine.requestHistory.last().url.parameters["page"]).isEqualTo("2")
            assertThat(engine.requestHistory.last().url.parameters["size"]).isEqualTo("20")

            replyJson("""[{"id":3,"title":"t","message":"m","type":"X","createdAt":"2026-09-28T08:00:00Z"}]""")
            val only = NotificationsRepository(client).getNotificationsPage(0).getOrThrow()
            assertThat(only.items.map { it.id }).containsExactly(3)
            assertThat(only.last).isTrue()
        }

    @Test
    fun `N1 reads linked ids from any spot, and skips items without a date`() =
        runTest {
            replyJson(
                """[{"id":1,"title":"t","message":"m","type":"TASK_ASSIGNED","createdAt":"2026-09-28T08:00:00Z",""" +
                    """"data":{"task_id":7}},{"id":2,"title":"t","message":"m","type":"X","createdAt":"soon"},""" +
                    """{"id":3,"title":"t","message":"m","type":"NEW_DOCUMENT","createdAt":"2026-09-28T09:00:00Z",""" +
                    """"document_id":"d","readAt":"2026-09-28T10:00:00Z"}]""",
            )

            val list = NotificationsRepository(client).getNotifications().getOrThrow()

            assertThat(list.map { it.id }).containsExactly(1, 3).inOrder()
            assertThat(list[0].task).isEqualTo("7")
            assertThat(list[0].createdAt).isEqualTo(Instant.parse("2026-09-28T08:00:00Z"))
            assertThat(list[1].document).isEqualTo("d")
            assertThat(list.map { it.isRead }).containsExactly(false, true).inOrder()
            assertThat(path).isEqualTo("/api/notifications")
        }

    @Test
    fun `N3, N4 and P7 are PUTs`() =
        runTest {
            NotificationsRepository(client).markRead(5).getOrThrow()
            assertThat(engine.requestHistory.last().method).isEqualTo(HttpMethod.Put)
            assertThat(path).isEqualTo("/api/notifications/5/read")

            NotificationsRepository(client).markAllRead().getOrThrow()
            assertThat(engine.requestHistory.last().method).isEqualTo(HttpMethod.Put)
            assertThat(path).isEqualTo("/api/notifications/read-all")

            NotificationsRepository(client).registerPushToken("fcm").getOrThrow()
            val request = engine.requestHistory.last()
            assertThat(request.method).isEqualTo(HttpMethod.Put)
            assertThat(path).isEqualTo("/api/patients/me/fcm-token")
            assertThat(Json.parseToJsonElement((request.body as TextContent).text))
                .isEqualTo(Json.parseToJsonElement("""{"fcmToken":"fcm"}"""))
        }

    @Test
    fun `D2 reads the doctor, with a rating only when there are reviews`() =
        runTest {
            replyJson(
                """{"id":5,"firstName":"Aziz","lastName":"Karimov","averageRating":4.75,"reviewCount":12,"certificates":["c"]}""",
            )

            val doctor = DoctorsRepository(client).getDoctor("5").getOrThrow()

            assertThat(doctor.name).isEqualTo("Aziz Karimov")
            assertThat(doctor.ratingText).isEqualTo("4.8(12)")
            assertThat(doctor.isVerified).isTrue()
            assertThat(doctor.copy(reviewCount = 0).ratingText).isNull()
            assertThat(path).isEqualTo("/api/public/doctors/5")
        }

    @Test
    fun `D1 reads every page, with the near me params`() =
        runTest {
            reply = {
                val body =
                    if (it.url.parameters["page"] == null) {
                        """{"doctors":[{"id":1},{"id":2}],"totalCount":3}"""
                    } else {
                        """{"doctors":[{"id":3,"certificates":null}],"totalCount":3}"""
                    }
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }

            val query = DoctorsQuery(latitude = 40.5, longitude = 72.0, radiusKm = 50.0)
            val found = DoctorsRepository(client).searchDoctors(query).getOrThrow()

            assertThat(found.map { it.id }).containsExactly("1", "2", "3").inOrder()
            assertThat(engine.requestHistory).hasSize(2)
            val last = engine.requestHistory.last().url.parameters
            assertThat(last["page"]).isEqualTo("2")
            assertThat(last["latitude"]).isEqualTo("40.5")
            assertThat(last["radiusKm"]).isEqualTo("50.0")
        }

    @Test
    fun `a missing doctor isn't reported as a missing account`() =
        runTest {
            replyJson("{}", HttpStatusCode.NotFound)

            val error = DoctorsRepository(client).getDoctor("5").exceptionOrNull()

            assertThat(error).isInstanceOf(ApiException.NotFound::class.java)
            assertThat(error!!.message).isEqualTo(FAILED_TO_LOAD_DOCTOR)
        }

    @Test
    fun `P1 reads the name and photo`() =
        runTest {
            replyJson("""{"id":1,"fullName":"Aziz Karimov","photoUrl":"https://x/p.jpg","email":"a@shifa.uz"}""")

            val profile = ProfileRepository(client).getProfile().getOrThrow()

            assertThat(profile.fullName).isEqualTo("Aziz Karimov")
            assertThat(profile.photoUrl).isEqualTo("https://x/p.jpg")
            assertThat(path).isEqualTo("/api/patients/me/profile")
        }

    @Test
    fun `B1 sends the local day and the location only when set`() =
        runTest {
            replyJson(
                """[{"startAt":"2026-09-30T05:00:00Z","endAt":"2026-09-30T05:30:00Z","slotMinutes":30,"locationId":3}]""",
            )

            val slots = BookingsRepository(client).getSlots("42", LocalDate(2026, 9, 30), 3).getOrThrow()

            assertThat(slots.single().locationId).isEqualTo(3)
            assertThat(path).isEqualTo("/api/patients/me/schedule/doctors/42/available")
            val url = engine.requestHistory.last().url
            assertThat(url.parameters["day"]).isEqualTo("2026-09-30")
            assertThat(url.parameters["locationId"]).isEqualTo("3")

            BookingsRepository(client).getSlots("42", LocalDate(2026, 9, 30), null)
            assertThat(engine.requestHistory.last().url.parameters.names()).containsExactly("day")
        }

    @Test
    fun `D3 drops placeholders, and an error is no locations`() =
        runTest {
            replyJson("""[{"id":0},{"id":4,"isPrimary":true}]""")
            assertThat(DoctorsRepository(client).getLocations("42").map { it.id }).containsExactly(4)
            assertThat(path).isEqualTo("/api/public/doctors/42/locations")

            replyJson("{}", HttpStatusCode.InternalServerError)
            assertThat(DoctorsRepository(client).getLocations("42")).isEmpty()
        }

    @Test
    fun `B3 sends ids as numbers and leaves out what isn't set`() =
        runTest {
            replyJson("""{"id":9,"startAt":"2026-09-30T05:00:00Z","paymentStatus":"PENDING"}""")

            val booked =
                BookingsRepository(client)
                    .book(
                        BookingRequest(
                            42,
                            "2026-09-30T05:00:00Z",
                            30,
                            false,
                            locationId = 3,
                            documentIds = listOf(101),
                        ),
                    )
                    .getOrThrow()

            assertThat(booked.paymentStatus).isEqualTo("PENDING")
            val request = engine.requestHistory.last()
            assertThat(request.method).isEqualTo(HttpMethod.Post)
            assertThat(path).isEqualTo("/api/patients/me/appointments")
            assertThat(Json.parseToJsonElement((request.body as TextContent).text)).isEqualTo(
                Json.parseToJsonElement(
                    """{"doctorId":42,"startAt":"2026-09-30T05:00:00Z","slotMinutes":30,"isVideo":false,""" +
                        """"locationId":3,"documentIds":[101]}""",
                ),
            )
        }
}
