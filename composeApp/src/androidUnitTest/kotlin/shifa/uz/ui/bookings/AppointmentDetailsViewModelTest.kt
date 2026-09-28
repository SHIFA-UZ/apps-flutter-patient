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
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.errorCancellingAppointment
import shifa.uz.data.api.FAILED_TO_CANCEL_APPOINTMENT
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentDetailsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val doctors = mock<DoctorsRepository>()
    private val bookings = mock<BookingsRepository>()
    private val now = Instant.parse("2026-09-27T09:00:00Z")
    private val inThreeDays =
        Appointment(id = "1", doctorId = "d1", doctorName = "Ali", startAt = "2026-09-30T09:00:00Z")
    private val tomorrow = inThreeDays.copy(startAt = "2026-09-28T09:00:00Z")
    private lateinit var classToTest: AppointmentDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking {
            doctors.getDoctor("d1")
        } doReturn Result.success(Doctor(id = "d1", fullName = "Dr Ali", phone = "+998 90 123"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun create(appointment: Appointment = inThreeDays) {
        classToTest = AppointmentDetailsViewModel(appointment, doctors, bookings) { now }
    }

    private val state get() = classToTest.state.value

    @Test
    fun `it shows the appointment's doctor until D2 loads, then calls D2's phone`() =
        runTest {
            create()
            assertThat(state.shownDoctor.name).isEqualTo("Ali")
            assertThat(state.contactUri).isNull()
            advanceUntilIdle()

            assertThat(state.shownDoctor.name).isEqualTo("Dr Ali")
            assertThat(state.contactUri).isEqualTo("tel:+99890123")
        }

    @Test
    fun `within 48 hours it warns and does not cancel`() =
        runTest {
            create(tomorrow)
            assertThat(state.showWarning).isTrue()
            assertThat(state.canCancel).isFalse()

            classToTest.askCancel()
            assertThat(state.confirmCancel).isFalse()
        }

    @Test
    fun `cancelled and past appointments neither warn nor cancel`() {
        create(tomorrow.copy(status = "CANCELLED"))
        assertThat(state.showWarning).isFalse()
        assertThat(state.canCancel).isFalse()

        create(inThreeDays.copy(startAt = "2026-09-26T09:00:00Z"))
        assertThat(state.isPast).isTrue()
        assertThat(state.showWarning).isFalse()
        assertThat(state.canCancel).isFalse()
    }

    @Test
    fun `a confirmed cancel deletes it and closes`() =
        runTest {
            wheneverBlocking { bookings.cancel("1") } doReturn Result.success(Unit)
            create()

            classToTest.cancelled.test {
                classToTest.askCancel()
                assertThat(state.confirmCancel).isTrue()
                classToTest.cancel()
                assertThat(state.isCancelling).isTrue()
                advanceUntilIdle()
                awaitItem()
            }
        }

    @Test
    fun `a failed cancel shows the error`() =
        runTest {
            wheneverBlocking { bookings.cancel("1") } doReturn
                Result.failure(ApiException.ServerError(FAILED_TO_CANCEL_APPOINTMENT))
            create()
            classToTest.askCancel()

            classToTest.cancel()
            advanceUntilIdle()

            assertThat(state.isCancelling).isFalse()
            assertThat(state.confirmCancel).isFalse()
            assertThat(state.error).isEqualTo(Res.string.errorCancellingAppointment)
        }

    @Test
    fun `no doctor id loads no doctor`() =
        runTest {
            create(inThreeDays.copy(doctorId = null))
            advanceUntilIdle()

            verifyBlocking(doctors, never()) { getDoctor(any()) }
        }

    @Test
    fun `a 401 on cancel signs out`() =
        runTest {
            wheneverBlocking { bookings.cancel("1") } doReturn Result.failure(ApiException.Unauthorized("Unauthorized"))
            create()

            classToTest.signedOut.test {
                classToTest.askCancel()
                classToTest.cancel()
                advanceUntilIdle()
                awaitItem()
            }
        }
}
