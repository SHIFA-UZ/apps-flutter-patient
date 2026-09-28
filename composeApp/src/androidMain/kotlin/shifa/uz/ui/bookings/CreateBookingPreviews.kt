package shifa.uz.ui.bookings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import shifa.uz.data.model.AvailableSlot
import shifa.uz.data.model.DoctorService
import shifa.uz.data.model.PatientDocument
import shifa.uz.ui.doctors.LocalDeviceLocation
import shifa.uz.ui.widgets.PreviewTheme
import shifa.uz.utils.DeviceLocation
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
        CompositionLocalProvider(LocalDeviceLocation provides DeviceLocation.Found(41.3, 69.2)) {
            CreateBookingContent(state, now)
        }
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
