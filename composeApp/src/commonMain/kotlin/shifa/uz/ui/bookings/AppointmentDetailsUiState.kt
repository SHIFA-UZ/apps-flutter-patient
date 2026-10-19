package shifa.uz.ui.bookings

import androidx.compose.runtime.Stable
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.Review
import shifa.uz.data.model.shownDoctor

// 06 §3.5: cancel only 48 h or more ahead. Figma's copy says 24 h; the spec wins.
private const val CANCEL_HOURS = 48

/** [doctor] is D2, null until it loads (or if it fails): the card then shows the appointment's copy. */
@Stable
data class AppointmentDetailsUiState(
    val appointment: Appointment,
    val now: Instant,
    val doctor: Doctor? = null,
    val confirmCancel: Boolean = false,
    val isCancelling: Boolean = false,
    val error: StringResource? = null,
    val detailsOpen: Boolean = true,
    val review: Review? = null, // B7
    val reviewChecked: Boolean = false,
    val reviewOpen: Boolean = false,
    val rating: Int = 0,
    val comment: String = "",
    val isReviewing: Boolean = false,
    val reviewError: StringResource? = null,
    val notice: StringResource? = null,
) {
    private val hoursUntil get() = appointment.start?.let { (it - now).inWholeHours }

    val isPast get() = hoursUntil?.let { it < 0 } ?: true

    val canCancel get() = appointment.isOpen && !isPast && hoursUntil!! >= CANCEL_HOURS && appointment.id.isNotBlank()

    val showWarning get() = appointment.isOpen && !isPast && hoursUntil!! < CANCEL_HOURS

    /** Upcoming has the info cards, Reschedule and Cancel; anything else is past: the summary card and a review. */
    val isUpcoming get() = appointment.isOpen && !isPast

    // 06 §3.9: a past visit that wasn't cancelled, with a doctor, once B7 found no review.
    val canReview get() =
        isPast && appointment.status != "CANCELLED" && !appointment.doctorId.isNullOrBlank() &&
            reviewChecked && review == null

    val shownDoctor get() = appointment.shownDoctor(doctor)

    /** Contact: call, else email. D2 has them; Flutter read them from B2, which never sends them (06 quirk 13). */
    val contactUri
        get() =
            shownDoctor.phone?.filterNot(Char::isWhitespace)?.ifEmpty { null }?.let { "tel:$it" }
                ?: shownDoctor.email?.trim()?.ifEmpty { null }?.let { "mailto:$it" }
}
