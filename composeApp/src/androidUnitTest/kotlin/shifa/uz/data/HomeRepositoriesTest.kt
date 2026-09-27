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
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Test
import shifa.uz.data.api.FAILED_TO_LOAD_DOCTOR
import shifa.uz.data.api.createHttpClient
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.notifications.NotificationsRepository
import shifa.uz.data.profile.ProfileRepository
import shifa.uz.data.storage.TokenStorage
import shifa.uz.data.tasks.TasksRepository

/** The Home calls: B2, T1, N2, D2, P1. */
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
    fun `N2 reads the count`() =
        runTest {
            replyJson("""{"count":12}""")
            assertThat(NotificationsRepository(client).unreadCount().getOrThrow()).isEqualTo(12)
            assertThat(path).isEqualTo("/api/notifications/unread/count")
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
}
