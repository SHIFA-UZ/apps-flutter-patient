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
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
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
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.documents.DocumentsRepository
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
                    DoctorService("7", "Video visit", prices = listOf(DoctorPrice(100_000, "UZS"))),
                    DoctorService("8", "Filling", prices = listOf(DoctorPrice(9_000_000, "UZS", locationId = 14))),
                    DoctorService("3", "Free video visit", isFreeConsultation = true),
                    DoctorService(
                        "9",
                        "Cleaning",
                        prices = listOf(DoctorPrice(90_000, "UZS", locationId = 5)),
                        paymentType = "PAY_LATER",
                    ),
                ),
        )

    // 14:00 in Tashkent.
    private val now = Instant.parse("2026-09-28T09:00:00Z")
    private val tashkent = TimeZone.of("Asia/Tashkent")
    private val today = LocalDate(2026, 9, 28)
    private val gone = AvailableSlot("2026-09-28T08:00:00Z", "2026-09-28T08:30:00Z")
    private val first = AvailableSlot("2026-09-30T05:00:00Z", "2026-09-30T05:30:00Z", 30, locationId = 3)
    private val second = AvailableSlot("2026-09-30T05:30:00Z", "2026-09-30T06:00:00Z")

    private val classToTest by lazy { CreateBookingViewModel(card, bookings, doctors, documents, { now }, tashkent) }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
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
            val none = CreateBookingViewModel(card, bookings, doctors, documents, { now }, tashkent)
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
            val reschedule = CreateBookingViewModel(card, bookings, doctors, documents, { now }, tashkent, old)
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
                Result.success(listOf(PaymentMethod("CLICK", 150_000, "UZS"), PaymentMethod("STRIPE", 150_000, "UZS")))
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

            // Back on the booking page the slot stays held, and Complete Payment goes back to the same checkout.
            classToTest.onPaymentClosed()
            assertThat(state.isHeld(now)).isTrue()
            classToTest.book()
            assertThat(state.paying).isTrue()
            verifyBlocking(bookings, times(1)) { book(any()) }

            classToTest.paid.test {
                classToTest.onPaymentDone(held.copy(status = "CONFIRMED", paymentStatus = Appointment.PAID))
                assertThat(awaitItem().id).isEqualTo("50")
            }
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
}
