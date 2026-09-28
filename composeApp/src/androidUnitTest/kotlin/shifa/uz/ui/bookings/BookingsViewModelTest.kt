package shifa.uz.ui.bookings

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
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.appointmentCancelledSuccessfully
import shifa.composeapp.generated.resources.locationPermissionDeniedForever
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsQuery
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.utils.DeviceLocation

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
    fun `near me searches around the fix and drops doctors without coordinates`() =
        runTest {
            val fix = DeviceLocation.Found(40.63, 72.50)
            val near = Doctor(id = "n", distanceKm = 0.1)
            val located = Doctor(id = "l", latitude = 40.7, longitude = 72.5)
            val nowhere = Doctor(id = "x")
            val query =
                DoctorsQuery(search = "", sortBy = "distance", latitude = 40.63, longitude = 72.50, radiusKm = 50.0)
            wheneverBlocking { doctors.searchDoctors(query) } doReturn Result.success(listOf(near, located, nowhere))

            classToTest.locating()
            assertThat(state.filter).isEqualTo(DoctorFilter.NearMe)
            assertThat(state.recommended).isNull()
            classToTest.located(fix)
            advanceUntilIdle()

            assertThat(state.recommended).containsExactly(near, located).inOrder()
            assertThat(state.location).isEqualTo(fix)
            assertThat(state.isLocating).isFalse()
            assertThat(located.distanceFrom(fix)).isWithin(0.01).of(7.78) // Haversine
        }

    @Test
    fun `no fix goes back to All and says why`() =
        runTest {
            classToTest.locating()
            classToTest.located(DeviceLocation.DeniedForever)
            advanceUntilIdle()

            assertThat(state.filter).isEqualTo(DoctorFilter.All)
            assertThat(state.error).isEqualTo(Res.string.locationPermissionDeniedForever)
            assertThat(state.recommended).containsExactly(vali)
        }
}
