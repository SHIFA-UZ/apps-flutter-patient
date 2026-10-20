package shifa.uz.ui.bookings

import androidx.compose.runtime.Stable
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.AvailableSlot
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.DoctorLocation
import shifa.uz.data.model.PatientDocument
import shifa.uz.data.model.PaymentMethod
import shifa.uz.utils.parseInstant

/**
 * The booking page. The chosen [service] decides the payment (MOBILE_BOOKING_PAYMENT): free and pay later book at
 * once; pay now asks for a provider ([methods], the Confirm Payment tray), then books and pays ([pending], [paying]).
 */
@Stable
data class CreateBookingUiState(
    val doctor: Doctor,
    val locations: List<DoctorLocation> = emptyList(),
    val locationId: Int? = null, // in person only
    val isVideo: Boolean = false,
    val serviceId: String? = null,
    val date: LocalDate? = null,
    val slots: List<AvailableSlot>? = null, // null: loading
    val slot: AvailableSlot? = null,
    val reason: String = "",
    val documents: List<PatientDocument> = emptyList(),
    val isUploading: Boolean = false,
    val isBooking: Boolean = false,
    val error: StringResource? = null,
    val methods: List<PaymentMethod>? = null, // non-null: the Confirm Payment tray is open
    val provider: String? = null,
    val pending: Appointment? = null, // the PENDING_PAYMENT booking, held until its holdExpiresAt
    val paying: Boolean = false, // the payment page is open
) {
    val canBook get() = slot != null && !isBooking && !isUploading

    /** The services for this visit: video ones for video, else the ones bookable at the clinic. */
    val services get() = doctor.serviceItems.filter { if (isVideo) it.canVideo else it.bookableAt(locationId) }

    val service get() = services.firstOrNull { it.id == serviceId }

    // Whether video is possible at all, before a service is picked.
    val videoServices get() = doctor.serviceItems.filter { it.canVideo }

    /** No video for pay later (§1); without services the switch stays, and video then says why it can't book. */
    val canSwitchVideo get() = service?.canVideo ?: (doctor.serviceItems.isEmpty() || videoServices.isNotEmpty())

    val isPayNow get() = service?.isPayNow == true

    val isPayInClinic get() = service?.isPayLater == true

    val price get() = service?.price(locationId.takeUnless { isVideo })?.text

    val canAttach get() = documents.size < MAX_BOOKING_DOCUMENTS && !isUploading && !isBooking

    /** The held slot's end; the payment can go on until then. */
    val holdEnd get() = pending?.let { parseInstant(it.holdExpiresAt) ?: parseInstant(it.payment?.expiresAt) }

    fun isHeld(now: Instant) = pending?.payment?.checkoutUrl != null && holdEnd?.let { now < it } ?: false
}

// 06 §3.1 step 9.
const val MAX_BOOKING_DOCUMENTS = 5
