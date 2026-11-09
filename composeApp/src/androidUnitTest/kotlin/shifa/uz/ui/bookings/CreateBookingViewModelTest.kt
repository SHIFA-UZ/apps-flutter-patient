package shifa.uz.ui.bookings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.bookingSelectService
import shifa.composeapp.generated.resources.bookingSelectServiceForVideo
import shifa.composeapp.generated.resources.bookingSlotTaken
import shifa.composeapp.generated.resources.bookingVideoNoServicesOffered
import shifa.composeapp.generated.resources.paymentCancelledSlotReleased
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.PaymentReports
import shifa.uz.data.analytics.RecordingTracker
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.documents.DocumentsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.AvailableSlot
import shifa.uz.data.model.BookingRequest
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.DoctorLocation
import shifa.uz.data.model.DoctorPrice
import shifa.uz.data.model.DoctorService
import shifa.uz.data.model.PatientDocument
import shifa.uz.data.model.PaymentCheckout
import shifa.uz.data.model.PaymentMethod

@OptIn(ExperimentalCoroutinesApi::class)
class CreateBookingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val bookings = mock<BookingsRepository>()
    private val doctors = mock<DoctorsRepository>()
    private val documents = mock<DocumentsRepository>()
    private val card = Doctor(id = "42", fullName = "Ali")
    private val full =
        card.copy(
            serviceItems =
                listOf(
                    DoctorService("7", "Video visit", prices = listOf(DoctorPrice(10_000_000, "UZS"))),
                    DoctorService("8", "Filling", prices = listOf(DoctorPrice(900_000_000, "UZS", locationId = 14))),
                    DoctorService("3", "Free video visit", isFreeConsultation = true),
                    DoctorService(
                        "9",
                        "Cleaning",
                        prices = listOf(DoctorPrice(9_000_000, "UZS", locationId = 5)),
                        paymentType = "PAY_LATER",
                    ),
                ),
        )

    // 14:00 in Tashkent.
    private val now = Instant.parse("2026-09-28T09:00:00Z")
    private val today = LocalDate(2026, 9, 28)
    private val gone = AvailableSlot("2026-09-28T08:00:00Z", "2026-09-28T08:30:00Z")
    private val first = AvailableSlot("2026-09-30T05:00:00Z", "2026-09-30T05:30:00Z", 30, locationId = 3)
    private val second = AvailableSlot("2026-09-30T05:30:00Z", "2026-09-30T06:00:00Z")

    private val tracker = RecordingTracker()
    private val classToTest by lazy {
        CreateBookingViewModel(card, bookings, doctors, documents, { now }, analytics = tracker)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        PaymentReports.clearForTests()
        wheneverBlocking { doctors.getDoctor("42") } doReturn Result.success(full)
        wheneverBlocking {
            doctors.getLocations(
                "42",
            )
        } doReturn listOf(DoctorLocation(4), DoctorLocation(5, isPrimary = true))
        wheneverBlocking { bookings.getSlots(any(), any(), anyOrNull()) } doReturn Result.success(emptyList())
        wheneverBlocking { bookings.getSlots("42", today, 5) } doReturn Result.success(listOf(gone))
        wheneverBlocking {
            bookings.getSlots(
                "42",
                LocalDate(2026, 9, 30),
                5,
            )
        } doReturn Result.success(listOf(first, second))
        wheneverBlocking { bookings.getAppointments() } doReturn Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a payment the page already reported is not reported again on close`() =
        runTest {
            PaymentReports.claim("9") // the payment page's poll saw PAID first
            wheneverBlocking { bookings.getPaymentMethods(any(), anyOrNull()) } doReturn
                Result.success(listOf(PaymentMethod(provider = "CLICK", amountMinor = 10_000_000, currency = "UZS")))
            val paid =
                Appointment(id = "9", startAt = first.startAt, status = "CONFIRMED", paymentStatus = Appointment.PAID)
            wheneverBlocking { bookings.book(any()) } doReturn
                Result.success(
                    paid.copy(
                        status = Appointment.PENDING_PAYMENT,
                        paymentStatus = "PENDING",
                        payment = PaymentCheckout("880", "CLICK", "https://pay/880"),
                    ),
                )
            wheneverBlocking { bookings.getAppointment("9") } doReturn Result.success(paid)
            open()
            classToTest.onServiceChange("7")
            classToTest.book()
            advanceUntilIdle()
            classToTest.pay()
            advanceUntilIdle()
            classToTest.onPaymentClosed()
            advanceUntilIdle()

            assertThat(tracker.events.filterIsInstance<AnalyticsEvent.PaymentCompleted>()).isEmpty()
        }

    @Test
    fun `a reschedule that fails its checks is a reschedule failure`() =
        runTest {
            wheneverBlocking { bookings.getSlots("42", LocalDate(2026, 9, 30), null) } doReturn
                Result.success(listOf(first))
            val old = Appointment(id = "77", startAt = first.startAt, isVideoConsultation = true)
            val reschedule =
                CreateBookingViewModel(card, bookings, doctors, documents, { now }, old, analytics = tracker)
            advanceUntilIdle()
            reschedule.onVideoChange(true)
            advanceUntilIdle()
            reschedule.book()
            advanceUntilIdle()

            assertThat(tracker.events.first()).isInstanceOf(AnalyticsEvent.RescheduleStarted::class.java)
            assertThat(tracker.events.filterIsInstance<AnalyticsEvent.BookingFailed>()).isEmpty()
            assertThat(tracker.events.filterIsInstance<AnalyticsEvent.RescheduleFailed>()).isNotEmpty()
        }

    @Test
    fun `a free booking is tracked from the scan to the outcome`() =
        runTest {
            wheneverBlocking { bookings.book(any()) } doReturn
                Result.success(Appointment(id = "9", startAt = first.startAt, status = "CONFIRMED"))
            open()
            classToTest.onServiceChange("3")
            classToTest.book()
            advanceUntilIdle()

            assertThat(tracker.events.take(2))
                .containsExactly(
                    AnalyticsEvent.BookingStarted,
                    AnalyticsEvent.BookingSlotsLoaded(daysToFirstSlot = 2, isVideo = false, scanStart = "today"),
                ).inOrder()
            val service = tracker.events.filterIsInstance<AnalyticsEvent.BookingServiceSelected>().single()
            assertThat(service.paymentKind).isEqualTo("free")
            assertThat(service.servicesCount).isEqualTo(3)
            assertThat(tracker.events.last())
                .isEqualTo(
                    AnalyticsEvent.BookingCompleted(
                        isVideo = false,
                        daysAhead = 1,
                        hourOfDay = 10,
                        paymentKind = "free",
                        hasReason = false,
                        attachments = 0,
                        at = now.toString(),
                    ),
                )
            assertThat(tracker.events.last().userProperties).containsEntry("has_booked", true)
        }

    @Test
    fun `pay now tracks the providers, the checkout, and the back button as an abandonment`() =
        runTest {
            wheneverBlocking { bookings.getPaymentMethods(any(), anyOrNull()) } doReturn
                Result.success(listOf(PaymentMethod(provider = "CLICK", amountMinor = 10_000_000, currency = "UZS")))
            wheneverBlocking { bookings.book(any()) } doReturn
                Result.success(
                    Appointment(
                        id = "9",
                        startAt = first.startAt,
                        status = Appointment.PENDING_PAYMENT,
                        paymentStatus = "PENDING",
                        holdExpiresAt = "2026-09-28T09:15:00Z",
                        paymentAmountMinor = 10_000_000,
                        paymentCurrency = "UZS",
                        payment = PaymentCheckout("880", "CLICK", "https://pay/880"),
                    ),
                )
            open()
            classToTest.onServiceChange("7")
            classToTest.book()
            advanceUntilIdle()
            classToTest.pay()
            advanceUntilIdle()
            classToTest.abandon()

            assertThat(tracker.events)
                .containsAtLeast(
                    AnalyticsEvent.PaymentMethodsLoaded(1, listOf("click"), "50k-150k", "UZS"),
                    AnalyticsEvent.PaymentCheckoutOpened("click", 900, "50k-150k", "UZS", isVideo = false),
                    AnalyticsEvent.BookingAbandoned("checkout", isReschedule = false),
                ).inOrder()
        }

    private val state get() = classToTest.state.value

    /** Opens the screen: D2, D3, then the scan. */
    private fun TestScope.open() {
        classToTest
        advanceUntilIdle()
    }

    @Test
    fun `the scan skips past slots and lands on the first free day at the primary location`() =
        runTest {
            open()

            assertThat(state.doctor).isEqualTo(full)
            assertThat(state.locationId).isEqualTo(5)
            assertThat(state.date).isEqualTo(LocalDate(2026, 9, 30))
            assertThat(state.slots).containsExactly(first, second).inOrder()
            assertThat(state.slot).isEqualTo(first)
            assertThat(state.canBook).isTrue()
        }

    @Test
    fun `D2's next available day is asked first, and alone`() =
        runTest {
            wheneverBlocking { doctors.getDoctor("42") } doReturn
                Result.success(full.copy(nextAvailableStartAt = "2026-09-30T05:00:00Z"))
            open()

            assertThat(state.date).isEqualTo(LocalDate(2026, 9, 30))
            assertThat(state.slots).containsExactly(first, second).inOrder()
            verifyBlocking(bookings, times(1)) { getSlots(any(), any(), anyOrNull()) }
            verifyBlocking(bookings, never()) { getSlots("42", today, 5) }
        }

    @Test
    fun `a next available day with nothing at this clinic goes on scanning from it`() =
        runTest {
            wheneverBlocking { doctors.getDoctor("42") } doReturn
                Result.success(full.copy(nextAvailableStartAt = "2026-09-29T05:00:00Z"))
            open()

            assertThat(state.date).isEqualTo(LocalDate(2026, 9, 30))
            assertThat(state.slot).isEqualTo(first)
            // The hinted day alone, then the next 7 in one batch.
            verifyBlocking(bookings, times(8)) { getSlots(any(), any(), anyOrNull()) }
        }

    @Test
    fun `no free slot in 30 days ends on the last day, empty`() =
        runTest {
            wheneverBlocking { bookings.getSlots("42", LocalDate(2026, 9, 30), 5) } doReturn Result.success(emptyList())
            open()

            assertThat(state.date).isEqualTo(LocalDate(2026, 10, 27))
            assertThat(state.slots).isEmpty()
            assertThat(state.canBook).isFalse()
        }

    @Test
    fun `video scans without a location and keeps a service that allows it`() =
        runTest {
            val video = AvailableSlot("2026-09-29T05:00:00Z", "2026-09-29T05:30:00Z")
            wheneverBlocking { bookings.getSlots(any(), any(), isNull()) } doReturn Result.success(listOf(video))
            open()
            classToTest.onVideoChange(true)
            classToTest.onServiceChange("7")
            classToTest.onVideoChange(false)
            classToTest.onVideoChange(true)
            advanceUntilIdle()
            assertThat(state.serviceId).isEqualTo("7")

            // Pay later is in person only (MOBILE_BOOKING_PAYMENT §1).
            classToTest.onVideoChange(false)
            classToTest.onServiceChange("9")
            assertThat(state.canSwitchVideo).isFalse()
            classToTest.onVideoChange(true)
            advanceUntilIdle()
            assertThat(state.serviceId).isNull()
            assertThat(state.date).isEqualTo(today)
            assertThat(state.slot).isEqualTo(video)
        }

    @Test
    fun `video offers only services with an online price or free, needs one, and a doctor who lists some`() =
        runTest {
            wheneverBlocking { bookings.getSlots(any(), any(), isNull()) } doReturn Result.success(listOf(first))
            open()
            classToTest.onVideoChange(true)
            advanceUntilIdle()
            // The clinic-only one is what B3 rejects with "Selected service has no price configured".
            assertThat(state.videoServices.map { it.id }).containsExactly("7", "3").inOrder()
            classToTest.book()
            assertThat(state.error).isEqualTo(Res.string.bookingSelectServiceForVideo)

            wheneverBlocking { doctors.getDoctor("42") } doReturn Result.failure(Exception())
            val none = CreateBookingViewModel(card, bookings, doctors, documents, { now })
            advanceUntilIdle()
            none.onVideoChange(true)
            advanceUntilIdle()
            none.book()
            assertThat(none.state.value.error).isEqualTo(Res.string.bookingVideoNoServicesOffered)
            verifyBlocking(bookings, never()) { book(any()) }
        }

    @Test
    fun `a visit already at that start blocks the booking`() =
        runTest {
            wheneverBlocking { bookings.getAppointments() } doReturn
                Result.success(
                    listOf(Appointment(id = "1", startAt = "2026-09-30T10:00:00+05:00", status = "CONFIRMED")),
                )
            open()
            classToTest.book()
            // In person needs a service too, once the doctor lists some.
            assertThat(state.error).isEqualTo(Res.string.bookingSelectService)
            classToTest.onServiceChange("3")
            classToTest.book()
            advanceUntilIdle()

            assertThat(state.error).isEqualTo(Res.string.bookingSlotTaken)
            assertThat(state.isBooking).isFalse()
            verifyBlocking(bookings, never()) { book(any()) }
        }

    @Test
    fun `booking sends B3 with the uploaded PDFs and reports a pending payment`() =
        runTest {
            val bytes = byteArrayOf(1)
            wheneverBlocking { documents.upload(bytes, "a.pdf", "a.pdf", "PRE_APPOINTMENT") } doReturn
                Result.success(PatientDocument("101", "a.pdf"))
            val request =
                BookingRequest(42, first.startAt, 30, false, "Pain", 3, locationId = 3, documentIds = listOf(101))
            wheneverBlocking {
                bookings.book(
                    request,
                )
            } doReturn Result.success(Appointment(id = "9", paymentStatus = "pending"))
            open()
            classToTest.attach("a.pdf", bytes)
            classToTest.onReasonChange(" Pain ")
            classToTest.onServiceChange("3")
            advanceUntilIdle()
            assertThat(state.documents.map { it.id }).containsExactly("101")

            classToTest.booked.test {
                classToTest.book()
                assertThat(awaitItem()).isTrue()
            }
            verifyBlocking(bookings) { book(request) }
        }

    @Test
    fun `a reschedule keeps the visit's kind, reason and location, and names it in B3`() =
        runTest {
            val old =
                Appointment(id = "77", startAt = first.startAt, reason = "Pain", locationId = 4, status = "CONFIRMED")
            wheneverBlocking { bookings.getAppointments() } doReturn Result.success(listOf(old))
            wheneverBlocking { bookings.getSlots("42", LocalDate(2026, 9, 30), 4) } doReturn
                Result.success(listOf(first))
            val request =
                BookingRequest(42, first.startAt, 30, false, "Pain", locationId = 3, rescheduleAppointmentId = 77)
            wheneverBlocking { bookings.book(request) } doReturn Result.success(Appointment(id = "78"))
            val reschedule = CreateBookingViewModel(card, bookings, doctors, documents, { now }, old)
            advanceUntilIdle()
            assertThat(reschedule.state.value.locationId).isEqualTo(4)
            assertThat(reschedule.state.value.isVideo).isFalse()

            // Its own old slot doesn't count as taken.
            reschedule.booked.test {
                reschedule.book()
                awaitItem()
            }
            verifyBlocking(bookings) { book(request) }
        }

    @Test
    fun `pay now asks for a provider, books with it and opens the held checkout`() =
        runTest {
            val held =
                Appointment(
                    id = "50",
                    status = Appointment.PENDING_PAYMENT,
                    holdExpiresAt = "2026-09-28T09:15:00Z",
                    payment = PaymentCheckout("880", "STRIPE", "https://pay/880", "2026-09-28T09:15:00Z"),
                )
            val request =
                BookingRequest(42, first.startAt, 30, false, serviceId = 7, locationId = 3, paymentProvider = "STRIPE")
            wheneverBlocking { bookings.getPaymentMethods("7", 5) } doReturn
                Result.success(
                    listOf(PaymentMethod("CLICK", 15_000_000, "UZS"), PaymentMethod("STRIPE", 15_000_000, "UZS")),
                )
            wheneverBlocking { bookings.book(request) } doReturn Result.success(held)
            open()
            classToTest.onServiceChange("7")
            assertThat(state.isPayNow).isTrue()

            classToTest.book()
            advanceUntilIdle()
            assertThat(state.methods?.map { it.provider }).containsExactly("CLICK", "STRIPE").inOrder()
            assertThat(state.provider).isEqualTo("CLICK")
            verifyBlocking(bookings, never()) { book(any()) }

            classToTest.onProviderSelect("STRIPE")
            classToTest.pay()
            advanceUntilIdle()
            assertThat(state.methods).isNull()
            assertThat(state.pending).isEqualTo(held)
            assertThat(state.paying).isTrue()

            // Leaving the page while unpaid releases the held booking (B5) and rescans the day; the slot is free again.
            wheneverBlocking { bookings.getAppointment("50") } doReturn Result.success(held)
            wheneverBlocking { bookings.cancel("50") } doReturn Result.success(Unit)
            classToTest.onPaymentClosed()
            advanceUntilIdle()
            assertThat(state.pending).isNull()
            assertThat(state.paying).isFalse()
            assertThat(state.notice).isEqualTo(Res.string.paymentCancelledSlotReleased)
            verifyBlocking(bookings) { cancel("50") }
            verifyBlocking(bookings, times(1)) { book(any()) }
            verifyBlocking(bookings, atLeast(2)) { getSlots("42", LocalDate(2026, 9, 30), 5) }

            // Booked again and paid between the page's last poll and the back press: paid, never cancelled.
            classToTest.onSlotSelect(first)
            classToTest.book()
            advanceUntilIdle()
            classToTest.onProviderSelect("STRIPE")
            classToTest.pay()
            advanceUntilIdle()
            assertThat(state.pending).isEqualTo(held)
            val paid = held.copy(status = "CONFIRMED", paymentStatus = Appointment.PAID)
            wheneverBlocking { bookings.getAppointment("50") } doReturn Result.success(paid)
            classToTest.paid.test {
                classToTest.onPaymentClosed()
                advanceUntilIdle()
                assertThat(awaitItem().id).isEqualTo("50")
                // The page's own late PAID report is not a second one.
                classToTest.onPaymentDone(paid)
                expectNoEvents()
            }
            assertThat(state.pending).isNull()
            verifyBlocking(bookings, times(1)) { cancel("50") }

            // When B4 can't answer, nothing is cancelled: a payment may have landed, and the hold expires by itself.
            classToTest.onSlotSelect(first)
            classToTest.book()
            advanceUntilIdle()
            classToTest.onProviderSelect("STRIPE")
            classToTest.pay()
            advanceUntilIdle()
            wheneverBlocking { bookings.getAppointment("50") } doReturn Result.failure(ApiException.NetworkError("x"))
            classToTest.onPaymentClosed()
            classToTest.onPaymentClosed()
            // A changed choice while the close resolves has no hold left to drop.
            classToTest.onSlotSelect(second)
            advanceUntilIdle()
            assertThat(state.pending).isNull()
            assertThat(state.paying).isFalse()
            verifyBlocking(bookings, times(1)) { cancel("50") }
        }

    @Test
    fun `pay in clinic and free book at once, and a failed or expired payment frees the page`() =
        runTest {
            val clinic = BookingRequest(42, first.startAt, 30, false, serviceId = 9, locationId = 3)
            wheneverBlocking { bookings.book(clinic) } doReturn
                Result.success(Appointment(id = "60", status = "CONFIRMED", paymentStatus = Appointment.PAY_IN_CLINIC))
            open()
            classToTest.onServiceChange("9")
            assertThat(state.isPayInClinic).isTrue()
            assertThat(state.price).isEqualTo("90000.00 UZS")
            classToTest.booked.test {
                classToTest.book()
                assertThat(awaitItem()).isFalse()
            }
            verifyBlocking(bookings, never()) { getPaymentMethods(any(), anyOrNull()) }

            val dead = Appointment(id = "61", status = Appointment.EXPIRED)
            classToTest.onPaymentDone(dead)
            assertThat(state.pending).isNull()
            assertThat(state.paying).isFalse()
        }

    @Test
    fun `without a zone given, the slots are asked for the doctor's date, not the phone's`() =
        runTest {
            // 20:30 UTC on 27.09 is already 01:30 on 28.09 in Tashkent, the doctor's practice zone.
            val late = Instant.parse("2026-09-27T20:30:00Z")
            CreateBookingViewModel(card.copy(timeZone = "Asia/Tashkent"), bookings, doctors, documents, { late })
            advanceUntilIdle()

            verifyBlocking(bookings, atLeast(1)) { getSlots(eq("42"), eq(LocalDate(2026, 9, 28)), anyOrNull()) }
            verifyBlocking(bookings, never()) { getSlots(any(), eq(LocalDate(2026, 9, 27)), anyOrNull()) }
        }

    @Test
    fun `the slot day follows the zone the doctor's detail sends, not the card's copy`() =
        runTest {
            // 16:30 UTC on 27.09: 21:30 on the 27th in Tashkent (the card's fallback), 01:30 on the 28th in Tokyo.
            val late = Instant.parse("2026-09-27T16:30:00Z")
            wheneverBlocking { doctors.getDoctor("42") } doReturn Result.success(full.copy(timeZone = "Asia/Tokyo"))
            CreateBookingViewModel(card.copy(timeZone = null), bookings, doctors, documents, { late })
            advanceUntilIdle()

            verifyBlocking(bookings, atLeast(1)) { getSlots(eq("42"), eq(LocalDate(2026, 9, 28)), anyOrNull()) }
            verifyBlocking(bookings, never()) { getSlots(any(), eq(LocalDate(2026, 9, 27)), anyOrNull()) }
        }
}
