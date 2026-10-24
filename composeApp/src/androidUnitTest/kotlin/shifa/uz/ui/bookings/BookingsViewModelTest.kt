package shifa.uz.ui.bookings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.appointmentCancelledSuccessfully
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsQuery
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor

@OptIn(ExperimentalCoroutinesApi::class)
class BookingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val bookings = mock<BookingsRepository>()
    private val doctors = mock<DoctorsRepository>()
    private val now = Instant.parse("2026-09-27T09:00:00Z")
    private val old = Appointment(id = "1", doctorId = "d1", startAt = "2026-09-01T09:00:00Z")
    private val older = Appointment(id = "2", doctorId = "d2", startAt = "2026-08-01T09:00:00Z")
    private val cancelled =
        Appointment(id = "3", doctorId = "d1", startAt = "2026-10-20T09:00:00Z", status = "CANCELLED")
    private val upcoming = Appointment(id = "4", doctorId = "d1", startAt = "2026-10-01T09:00:00Z")
    private val ali = Doctor(id = "d1", fullName = "Ali")
    private val vali = Doctor(id = "d2", fullName = "Vali")
    private val classToTest by lazy { BookingsViewModel(bookings, doctors) { now } }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { bookings.getAppointments() } doReturn Result.success(listOf(older, upcoming, cancelled, old))
        wheneverBlocking { doctors.getDoctor("d1") } doReturn Result.success(ali)
        wheneverBlocking { doctors.getDoctor("d2") } doReturn Result.success(vali)
        wheneverBlocking { doctors.searchDoctors(any()) } doReturn Result.success(listOf(vali))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = classToTest.state.value

    @Test
    fun `it loads past visits newest first, their doctors and the recommended ones`() =
        runTest {
            classToTest.load()
            assertThat(state.isLoading).isTrue()
            advanceUntilIdle()

            assertThat(state.past).containsExactly(cancelled, old, older).inOrder()
            assertThat(state.myDoctors).containsExactly(ali, vali).inOrder()
            assertThat(state.lastVisit(ali)).isEqualTo(old.start) // not the cancelled one
            assertThat(state.lastVisit(vali)).isEqualTo(older.start)
            assertThat(state.recommended).containsExactly(vali)
            assertThat(state.isLoading).isFalse()
            verifyBlocking(doctors) { searchDoctors(DoctorsQuery(search = "")) }
        }

    @Test
    fun `typing searches once, after the last keystroke, with the filter`() =
        runTest {
            classToTest.filterBy(DoctorFilter.Remote)
            advanceUntilIdle()

            classToTest.search("a")
            classToTest.search("al")
            classToTest.search("ali")
            advanceUntilIdle()

            verifyBlocking(doctors, never()) { searchDoctors(DoctorsQuery(search = "a", consultationType = "online")) }
            verifyBlocking(
                doctors,
                times(1),
            ) { searchDoctors(DoctorsQuery(search = "ali", consultationType = "online")) }
            assertThat(state.query).isEqualTo("ali")
        }

    @Test
    fun `a cancel reloads with a notice`() =
        runTest {
            classToTest.onCancelled()
            advanceUntilIdle()

            assertThat(state.notice).isEqualTo(Res.string.appointmentCancelledSuccessfully)
            verifyBlocking(bookings) { getAppointments() }
        }

    @Test
    fun `a 401 signs out`() =
        runTest {
            wheneverBlocking { bookings.getAppointments() } doReturn
                Result.failure(
                    ApiException.Unauthorized("Unauthorized"),
                )

            classToTest.signedOut.test {
                classToTest.load()
                advanceUntilIdle()
                awaitItem()
            }
            assertThat(state.error).isNotNull()
            assertThat(state.recommended).containsExactly(vali)
        }

    @Test
    fun `after a load every chip is fetched once, so switching shows it without asking again`() =
        runTest {
            val remote = DoctorsQuery(search = "", consultationType = "online")
            wheneverBlocking { doctors.searchDoctors(remote) } doReturn Result.success(listOf(ali))
            classToTest.load()
            advanceUntilIdle()
            verifyBlocking(doctors, times(1)) { searchDoctors(remote) } // prefetched

            classToTest.filterBy(DoctorFilter.Remote)
            assertThat(state.isSearching).isFalse()
            advanceUntilIdle()

            assertThat(state.recommended).containsExactly(ali)
            verifyBlocking(doctors, times(1)) { searchDoctors(remote) }
            verifyBlocking(doctors, times(DoctorFilter.entries.size)) { searchDoctors(any()) }
        }

    @Test
    fun `a chip not fetched yet shows skeletons until D1 answers`() =
        runTest {
            val today = DoctorsQuery(search = "", availableWithin = "today")
            wheneverBlocking { doctors.searchDoctors(today) } doSuspendableAnswer {
                delay(1_000) // D1 is slow
                Result.success(listOf(vali))
            }
            classToTest.filterBy(DoctorFilter.Today)
            dispatcher.scheduler.advanceTimeBy(500)

            assertThat(state.isSearching).isTrue()
            assertThat(state.recommended).isNull()
            advanceUntilIdle()
            assertThat(state.isSearching).isFalse()
            assertThat(state.recommended).containsExactly(vali)
        }
}
