package shifa.uz.ui.visit

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
import shifa.composeapp.generated.resources.pleaseSignAbove
import shifa.composeapp.generated.resources.signatureSubmittedSuccess
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor

@OptIn(ExperimentalCoroutinesApi::class)
class LiveAppointmentViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val doctors = mock<DoctorsRepository>()
    private val bookings = mock<BookingsRepository>()
    private val listed = Appointment("7", doctorId = "3", startAt = "2026-10-19T09:00:00Z")
    private val detailed = listed.copy(signatureRequested = true)
    private val png = byteArrayOf(1)

    private val classToTest by lazy {
        LiveAppointmentViewModel(listed, doctors, bookings) { Instant.parse("2026-10-19T09:05:00Z") }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { doctors.getDoctor("3") } doReturn Result.success(Doctor(id = "3", fullName = "Aziz"))
        wheneverBlocking { bookings.getAppointment("7") } doReturn Result.success(detailed)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `B4 brings the signature request and D2 the doctor`() =
        runTest {
            classToTest.state.test {
                assertThat(awaitItem().appointment.needsSignature).isFalse()
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertThat(state.appointment.needsSignature).isTrue()
                assertThat(state.shownDoctor.name).isEqualTo("Aziz")
            }
        }

    @Test
    fun `an empty pad asks to sign, and a signature goes to B6 and closes the request`() =
        runTest {
            wheneverBlocking { bookings.submitSignature(any(), any()) } doReturn Result.success(Unit)
            advanceUntilIdle()
            classToTest.openSign()

            classToTest.sign(null)
            assertThat(classToTest.state.value.signError).isEqualTo(Res.string.pleaseSignAbove)
            verifyBlocking(bookings, never()) { submitSignature(any(), any()) }

            wheneverBlocking {
                bookings.getAppointment(
                    "7",
                )
            } doReturn Result.success(detailed.copy(alreadySigned = true))
            classToTest.sign(png)
            advanceUntilIdle()

            verifyBlocking(bookings) { submitSignature("7", png) }
            val state = classToTest.state.value
            assertThat(state.signing).isFalse()
            assertThat(state.notice).isEqualTo(Res.string.signatureSubmittedSuccess)
            assertThat(state.appointment.needsSignature).isFalse()
        }

    @Test
    fun `a failed signature keeps the tray open with the error`() =
        runTest {
            wheneverBlocking {
                bookings.submitSignature(
                    any(),
                    any(),
                )
            } doReturn Result.failure(ApiException.ServerError("x"))
            advanceUntilIdle()
            classToTest.openSign()

            classToTest.sign(png)
            advanceUntilIdle()

            assertThat(classToTest.state.value.signing).isTrue()
            assertThat(classToTest.state.value.signError).isNotNull()
            assertThat(classToTest.state.value.isSubmitting).isFalse()
        }

    @Test
    fun `a 401 signs out`() =
        runTest {
            wheneverBlocking { bookings.getAppointment("7") } doReturn Result.failure(ApiException.Unauthorized("x"))

            classToTest.signedOut.test {
                advanceUntilIdle()
                awaitItem()
            }
        }
}
