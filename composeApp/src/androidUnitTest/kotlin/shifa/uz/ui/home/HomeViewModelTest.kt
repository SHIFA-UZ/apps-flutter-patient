package shifa.uz.ui.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.errorNetworkConnection
import shifa.composeapp.generated.resources.errorSomethingWentWrong
import shifa.uz.data.api.FAILED_TO_LOAD_DOCTOR
import shifa.uz.data.api.NETWORK_ERROR
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.notifications.NotificationsRepository
import shifa.uz.data.profile.ProfileRepository
import shifa.uz.data.tasks.TasksRepository

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val profile = mock<ProfileRepository>()
    private val bookings = mock<BookingsRepository>()
    private val doctors = mock<DoctorsRepository>()
    private val tasks = mock<TasksRepository>()
    private val notifications = mock<NotificationsRepository>()
    private val now = Instant.parse("2026-09-27T09:00:00Z")
    private lateinit var classToTest: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { profile.getProfile() } doReturn Result.success(PatientProfile(fullName = "  Aziz  Karimov "))
        wheneverBlocking { bookings.getAppointments() } doReturn Result.success(emptyList())
        wheneverBlocking { tasks.getMyTasks() } doReturn Result.success(emptyList())
        wheneverBlocking { notifications.unreadCount() } doReturn Result.success(4)
        wheneverBlocking { doctors.getDoctor(any()) } doReturn Result.success(Doctor())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun create() {
        classToTest = HomeViewModel(profile, bookings, doctors, tasks, notifications) { now }
    }

    private val state get() = classToTest.state.value

    private fun appointment(
        id: String,
        startAt: String,
        endAt: String? = null,
        status: String? = "CONFIRMED",
        doctorId: String? = null,
    ) = Appointment(id = id, doctorId = doctorId, startAt = startAt, endAt = endAt, status = status)

    private fun task(
        id: String,
        status: String,
    ) = RemoteCareTask(id = id, status = status, startDate = "2026-09-27")

    @Test
    fun `the first load shows the spinner, then the greeting, badge and open tasks`() =
        runTest {
            wheneverBlocking { tasks.getMyTasks() } doReturn
                Result.success(
                    listOf(task("1", "ACTIVE"), task("2", "COMPLETED"), task("3", "CANCELLED"), task("4", "EXPIRED")),
                )
            create()
            assertThat(state.isLoading).isTrue()

            advanceUntilIdle()

            assertThat(state.isLoading).isFalse()
            assertThat(state.firstName).isEqualTo("Aziz")
            assertThat(state.unreadNotifications).isEqualTo(4)
            assertThat(state.tasks!!.map { it.id }).containsExactly("1", "4").inOrder()
            assertThat(state.error).isNull()
        }

    @Test
    fun `upcoming keeps open visits, requested ones too, until their join window closes, soonest first`() =
        runTest {
            wheneverBlocking { bookings.getAppointments() } doReturn
                Result.success(
                    listOf(
                        appointment("tomorrow", "2026-09-28T09:00:00Z"),
                        appointment("ended", "2026-09-27T08:00:00Z", endAt = "2026-09-27T08:40:00Z"),
                        appointment(
                            "inProgress",
                            "2026-09-27T08:30:00Z",
                            endAt = "2026-09-27T08:50:00Z",
                            status = "IN_PROGRESS",
                        ),
                        appointment("noStatus", "2026-09-27T12:00:00Z", status = null),
                        appointment("requested", "2026-09-27T11:00:00Z", status = "REQUESTED"),
                        appointment("cancelled", "2026-09-27T11:00:00Z", status = "CANCELLED"),
                        appointment("completed", "2026-09-27T13:00:00Z", status = "COMPLETED"),
                        appointment("noDate", "soon"),
                    ),
                )
            create()
            advanceUntilIdle()

            assertThat(
                state.upcoming!!.map { it.id },
            ).containsExactly("inProgress", "requested", "noStatus", "tomorrow").inOrder()
            assertThat(state.upcoming!!.first().isNow(now)).isTrue()
            assertThat(state.upcoming!!.first().isConfirmed).isTrue() // IN_PROGRESS keeps Join
            assertThat(state.upcoming!!.single { it.id == "requested" }.isConfirmed).isFalse()
        }

    @Test
    fun `my doctors are the latest bookings' doctors, once each, leaving out the ones that fail`() =
        runTest {
            wheneverBlocking { bookings.getAppointments() } doReturn
                Result.success(
                    listOf(
                        appointment("1", "2026-09-01T09:00:00Z", doctorId = "d4"),
                        appointment("2", "2026-09-20T09:00:00Z", doctorId = "d1"),
                        appointment("3", "2026-09-10T09:00:00Z", doctorId = "d3"),
                        appointment("4", "2026-09-15T09:00:00Z", doctorId = "d2"),
                        appointment("5", "2026-09-12T09:00:00Z", doctorId = "d1"),
                        appointment("6", "2026-09-25T09:00:00Z", doctorId = " "),
                    ),
                )
            for (id in listOf(
                "d1",
                "d3",
            )) wheneverBlocking { doctors.getDoctor(id) } doReturn Result.success(Doctor(id = id))
            wheneverBlocking {
                doctors.getDoctor(
                    "d2",
                )
            } doReturn Result.failure(ApiException.NotFound(FAILED_TO_LOAD_DOCTOR))
            create()
            advanceUntilIdle()

            assertThat(state.doctors!!.map { it.id }).containsExactly("d1", "d3").inOrder()
            assertThat(state.error).isNull()
            verifyBlocking(doctors, never()) { getDoctor("d4") }
        }

    @Test
    fun `doctors that all fail are an error`() =
        runTest {
            wheneverBlocking { bookings.getAppointments() } doReturn
                Result.success(listOf(appointment("1", "2026-09-01T09:00:00Z", doctorId = "d1")))
            wheneverBlocking { doctors.getDoctor(any()) } doReturn
                Result.failure(ApiException.NetworkError(NETWORK_ERROR))
            create()
            advanceUntilIdle()

            assertThat(state.doctors).isNull()
            assertThat(state.error).isEqualTo(Res.string.errorNetworkConnection)
        }

    @Test
    fun `a failed refresh keeps the lists and reports the error`() =
        runTest {
            wheneverBlocking { tasks.getMyTasks() } doReturn Result.success(listOf(task("1", "ACTIVE")))
            create()
            advanceUntilIdle()
            wheneverBlocking { tasks.getMyTasks() } doReturn Result.failure(ApiException.ServerError("boom"))

            classToTest.refresh()
            assertThat(state.isRefreshing).isTrue()
            assertThat(state.isLoading).isFalse()
            advanceUntilIdle()

            assertThat(state.isRefreshing).isFalse()
            assertThat(state.tasks!!.map { it.id }).containsExactly("1")
            assertThat(state.error).isEqualTo(Res.string.errorSomethingWentWrong)
        }

    @Test
    fun `a failed profile or count only costs the name and badge`() =
        runTest {
            wheneverBlocking { profile.getProfile() } doReturn Result.failure(ApiException.ServerError("boom"))
            wheneverBlocking { notifications.unreadCount() } doReturn Result.failure(ApiException.ServerError("boom"))
            create()
            advanceUntilIdle()

            assertThat(state.firstName).isNull()
            assertThat(state.unreadNotifications).isEqualTo(0)
            assertThat(state.error).isNull()
        }

    @Test
    fun `a 401 on the profile signs out`() =
        runTest {
            wheneverBlocking { profile.getProfile() } doReturn Result.failure(ApiException.Unauthorized("401"))
            create()

            classToTest.signedOut.test {
                advanceUntilIdle()
                awaitItem()
            }
        }

    @Test
    fun `a saved profile change shows in the greeting without a refetch`() =
        runTest {
            create()
            advanceUntilIdle()

            classToTest.onProfileChanged(PatientProfile(fullName = "Sardor Aliyev", photoUrl = "https://x/p.jpg"))

            assertThat(state.firstName).isEqualTo("Sardor")
            assertThat(state.fullName).isEqualTo("Sardor Aliyev")
            assertThat(state.photoUrl).isEqualTo("https://x/p.jpg")
            verifyBlocking(profile) { getProfile() }
        }
}
