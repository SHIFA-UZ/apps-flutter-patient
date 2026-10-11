package shifa.uz.ui.bookings

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.AvailableSlot
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.PatientDocument

@Stable
data class CreateBookingUiState(
    val doctor: Doctor,
    val isVideo: Boolean = false,
    val serviceId: String? = null, // video only
    val locationId: Int? = null, // in person only
    val date: LocalDate? = null,
    val slots: List<AvailableSlot>? = null, // null: loading
    val slot: AvailableSlot? = null,
    val reason: String = "",
    val documents: List<PatientDocument> = emptyList(),
    val isUploading: Boolean = false,
    val isBooking: Boolean = false,
    val error: StringResource? = null,
) {
    val canBook get() = slot != null && !isBooking && !isUploading

    // Clinic-only services can't be booked as video.
    val videoServices get() = doctor.serviceItems.filter { it.isOnline }

    val canAttach get() = documents.size < MAX_BOOKING_DOCUMENTS && !isUploading && !isBooking
}

// 06 §3.1 step 9.
const val MAX_BOOKING_DOCUMENTS = 5
