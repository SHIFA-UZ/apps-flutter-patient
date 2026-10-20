package shifa.uz.ui.visit

import androidx.compose.runtime.Stable
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.shownDoctor

/** [appointment] starts as the list's copy; B4 replaces it with the signing flags. [doctor] is D2. */
@Stable
data class LiveAppointmentUiState(
    val appointment: Appointment,
    val now: Instant,
    val doctor: Doctor? = null,
    val detailsOpen: Boolean = true,
    val signing: Boolean = false,
    val isSubmitting: Boolean = false,
    val signError: StringResource? = null,
    val error: StringResource? = null,
    val notice: StringResource? = null,
) {
    val shownDoctor get() = appointment.shownDoctor(doctor)
}
