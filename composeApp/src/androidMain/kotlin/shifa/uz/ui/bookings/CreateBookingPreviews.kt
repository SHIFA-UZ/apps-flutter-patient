package shifa.uz.ui.bookings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.AvailableSlot
import shifa.uz.data.model.DoctorLocation
import shifa.uz.data.model.DoctorPrice
import shifa.uz.data.model.DoctorService
import shifa.uz.data.model.PatientDocument
import shifa.uz.data.model.PaymentCheckout
import shifa.uz.data.model.PaymentMethod
import shifa.uz.ui.widgets.PreviewTheme
import kotlin.time.Duration.Companion.minutes

private val now = Instant.parse("2026-09-27T09:00:00Z")

// 09:00 to 14:30 Tashkent, every 30 minutes.
private val slots =
    (0 until 12).map {
        val start = Instant.parse("2026-10-20T04:00:00Z") + (30 * it).minutes
        AvailableSlot(start.toString(), (start + 30.minutes).toString())
    }

private val doctor =
    previewDoctor.copy(
        distanceKm = 1.2,
        supportsInPerson = true,
        serviceItems = listOf(DoctorService("7", "Video consultation", isFreeConsultation = true)),
    )

@Composable
private fun Booking(state: CreateBookingUiState) =
    PreviewTheme {
        CreateBookingContent(state, now)
    }

// Figma 1662:18783: still looking for the next free day.
@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun CreateBookingIncompletePreview() = Booking(CreateBookingUiState(doctor))

// Figma 1662:19457.
@Preview(widthDp = 393, heightDp = 1000)
@Composable
internal fun CreateBookingCompletePreview() =
    Booking(
        CreateBookingUiState(
            doctor,
            isVideo = true,
            serviceId = "7",
            date = LocalDate(2026, 10, 20),
            slots = slots,
            slot = slots.first(),
            reason = "New appointment regarding my health issues",
            documents = listOf(PatientDocument("101", "blood-test.pdf")),
        ),
    )

// Figma 1921:25675.
@Preview(widthDp = 393, heightDp = 1000)
@Composable
internal fun ReschedulePreview() =
    PreviewTheme {
        RescheduleContent(
            previewAppointment.copy(startAt = "2026-10-05T07:00:00Z", endAt = "2026-10-05T07:30:00Z"),
            CreateBookingUiState(doctor, date = LocalDate(2026, 10, 20), slots = slots, slot = slots[2]),
            now,
            detailsOpen = true,
        )
    }

private val paying =
    doctor.copy(
        serviceItems =
            listOf(
                DoctorService(
                    "8",
                    "Check up",
                    prices = listOf(DoctorPrice(30_000_000, "UZS")),
                    paymentType = "PAY_NOW",
                ),
                DoctorService(
                    "9",
                    "Cleaning",
                    prices = listOf(DoctorPrice(9_000_000, "UZS", locationId = 1)),
                    paymentType = "PAY_LATER",
                ),
            ),
    )

private val clinics =
    listOf(DoctorLocation(1, true, clinic = "Sog'lom Hayot Klinikasi"), DoctorLocation(2, clinic = "SEBAMED"))

// Figma 1823:16296: pay now, so Complete Payment.
@Preview(widthDp = 393, heightDp = 1100)
@Composable
internal fun CreateBookingPayNowPreview() =
    Booking(
        CreateBookingUiState(
            paying,
            locations = clinics,
            locationId = 1,
            serviceId = "8",
            date = LocalDate(2026, 10, 20),
            slots = slots,
            slot = slots.first(),
        ),
    )

// Figma 1823:16350: pay in clinic, so no video and a note under the service.
@Preview(widthDp = 393, heightDp = 1000)
@Composable
internal fun CreateBookingPayInClinicPreview() =
    Booking(
        CreateBookingUiState(
            paying,
            locationId = 1,
            serviceId = "9",
            date = LocalDate(2026, 10, 20),
            slots = slots,
            slot = slots.first(),
            reason = "New appointment regarding my health issues",
        ),
    )

// Figma 1876:18609.
@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ConfirmPaymentTrayPreview() =
    PreviewTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            ConfirmPaymentTray(
                CreateBookingUiState(
                    paying,
                    methods = listOf(PaymentMethod("STRIPE", 30_000_000, "UZS")),
                    provider = "STRIPE",
                ),
            )
        }
    }

private val held =
    Appointment(
        id = "50",
        status = Appointment.PENDING_PAYMENT,
        holdExpiresAt = "2026-09-27T09:12:34Z",
        payment = PaymentCheckout("880", "STRIPE", "https://pay/880"),
    )

// Figma 1825:17388 (the provider's page is empty in previews).
@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun PaymentPreview() = PreviewTheme { PaymentContent(PaymentUiState(held, now)) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun PaymentExpiredPreview() =
    PreviewTheme { PaymentContent(PaymentUiState(held.copy(status = Appointment.EXPIRED), now)) }
