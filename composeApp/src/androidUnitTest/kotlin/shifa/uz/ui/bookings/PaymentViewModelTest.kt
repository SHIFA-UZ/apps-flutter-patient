package shifa.uz.ui.bookings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.paymentExpiredSlotReleased
import shifa.composeapp.generated.resources.paymentRefundSlotTaken
import shifa.composeapp.generated.resources.paymentStillPendingConfirmBooking
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.PaymentReports
import shifa.uz.data.analytics.RecordingTracker
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.PaymentCheckout
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val bookings = mock<BookingsRepository>()
    private val tracker = RecordingTracker()
    private val now = Instant.parse("2026-09-28T09:00:00Z")
    private val held =
        Appointment(
            id = "50",
            status = Appointment.PENDING_PAYMENT,
            paymentStatus = "PENDING",
            holdExpiresAt = "2026-09-28T09:15:00Z",
            payment = PaymentCheckout("880", "CLICK", "https://pay/880"),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        PaymentReports.clearForTests()
        wheneverBlocking { bookings.getAppointment("50") } doReturn Result.success(held.copy(payment = null))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the provider's success redirect is tracked as a redirect, not as a button tap`() =
        runTest {
            wheneverBlocking { bookings.getAppointment("50") } doReturn
                Result.success(held.copy(status = "CONFIRMED", paymentStatus = Appointment.PAID, payment = null))
            val classToTest = PaymentViewModel(held, bookings, tracker) { now }
            classToTest.done.test {
                classToTest.onRedirect("success")
                awaitItem()
            }

            assertThat(tracker.events.map { it.name })
                .containsExactly("payment_redirect_seen", "payment_completed")
                .inOrder()
            assertThat(tracker.events.filterIsInstance<AnalyticsEvent.PaymentCompleted>().single().via)
                .isEqualTo("redirect")
        }

    @Test
    fun `the page tracks a check, the poll that confirms, and a dead booking`() =
        runTest {
            val classToTest = PaymentViewModel(held, bookings, tracker) { now }
            classToTest.check()
            runCurrent()
            wheneverBlocking { bookings.getAppointment("50") } doReturn
                Result.success(held.copy(status = "CONFIRMED", paymentStatus = Appointment.PAID, payment = null))
            classToTest.done.test {
                advanceTimeBy(3_100)
                awaitItem()
            }

            assertThat(tracker.events.take(2))
                .containsExactly(
                    AnalyticsEvent.PaymentCheckTapped("pending", secondsOpen = 0),
                    AnalyticsEvent.PaymentStillPendingShown,
                ).inOrder()
            val paid = tracker.events.filterIsInstance<AnalyticsEvent.PaymentCompleted>().single()
            assertThat(paid.via).isEqualTo("poll")
            assertThat(paid.provider).isEqualTo("click")
            assertThat(paid.userProperties).containsEntry("has_paid", true)

            tracker.events.clear()
            wheneverBlocking { bookings.getAppointment("50") } doReturn
                Result.success(held.copy(status = Appointment.EXPIRED, paymentStatus = "FAILED"))
            val expired = PaymentViewModel(held, bookings, tracker) { now }
            expired.done.test {
                advanceTimeBy(3_100)
                awaitItem()
            }
            assertThat(tracker.events).containsExactly(AnalyticsEvent.PaymentFailed("expired", "click", 0))
        }

    @Test
    fun `it polls until the backend says PAID, keeping the checkout and the countdown`() =
        runTest {
            val classToTest = PaymentViewModel(held, bookings, tracker) { now }
            assertThat(classToTest.state.value.holdLeft).isEqualTo(15.minutes)

            classToTest.check()
            runCurrent()
            assertThat(classToTest.state.value.notice).isEqualTo(Res.string.paymentStillPendingConfirmBooking)
            assertThat(classToTest.state.value.checkoutUrl).isEqualTo("https://pay/880")

            wheneverBlocking { bookings.getAppointment("50") } doReturn
                Result.success(held.copy(status = "CONFIRMED", paymentStatus = Appointment.PAID, payment = null))
            classToTest.done.test {
                advanceTimeBy(3_100)
                assertThat(awaitItem().isPaid).isTrue()
            }
        }

    @Test
    fun `an expired hold says the slot is gone, and a late payment says it's refunded`() =
        runTest {
            wheneverBlocking { bookings.getAppointment("50") } doReturn
                Result.success(held.copy(status = Appointment.EXPIRED, paymentStatus = "FAILED"))
            val classToTest = PaymentViewModel(held, bookings, tracker) { now }
            classToTest.done.test {
                advanceTimeBy(3_100)
                awaitItem()
            }
            assertThat(classToTest.state.value.failure).isEqualTo(Res.string.paymentExpiredSlotReleased)

            val late =
                PaymentUiState(
                    held.copy(status = Appointment.EXPIRED, paymentStatus = "PAID", refundRequired = true),
                    now,
                )
            assertThat(late.failure).isEqualTo(Res.string.paymentRefundSlotTaken)
        }
}
