package shifa.uz.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import shifa.uz.utils.parseInstant
import kotlin.time.Duration.Companion.minutes

/** B2 item (15 §5.6). `location` is optional: Flutter required it and crashed on video appointments without one. */
@Serializable
data class Appointment(
    val id: String = "",
    val doctorId: String? = null,
    val doctorName: String? = null,
    val doctorProfession: String? = null,
    val doctorClinic: String? = null,
    val doctorPhotoUrl: String? = null,
    val startAt: String = "",
    val endAt: String? = null,
    val location: String? = null,
    @SerialName("isVideo") val videoFlag: Boolean? = null,
    val reason: String? = null,
    val status: String? = null,
    val paymentStatus: String = "NOT_REQUIRED",
) {
    val isVideo get() = videoFlag == true || location == "Video Consultation"

    // Null is CONFIRMED, and IN_PROGRESS (the visit has started) is past it. REQUESTED waits for the doctor:
    // B3 creates every booking as REQUESTED and nothing confirms it automatically.
    val isConfirmed get() = status == null || status == "CONFIRMED" || status == "IN_PROGRESS"

    // 06 quirk 12: Flutter ignored the status here. REQUESTED and PENDING are open (new bookings are either, 15 B2).
    val isOpen get() = status != "CANCELLED" && status != "COMPLETED"

    val start get() = parseInstant(startAt)

    // 06: the video join window, start − 5 min to end + 15 min. Home also uses it for "Now".
    private val window get() = start?.let { (it - 5.minutes)..((parseInstant(endAt) ?: it) + 15.minutes) }

    fun isNow(now: Instant) = window?.contains(now) == true

    fun isOver(now: Instant) = window?.let { now > it.endInclusive } ?: true
}

/** B1 item. Only in-person slots carry `locationId`. */
@Serializable
data class AvailableSlot(
    val startAt: String,
    val endAt: String = "",
    val slotMinutes: Int = 30,
    val locationId: Int? = null,
) {
    val start get() = parseInstant(startAt)
}

/** B3 body; nulls are left out (15 B3). */
@Serializable
data class BookingRequest(
    val doctorId: Long,
    val startAt: String,
    val slotMinutes: Int,
    val isVideo: Boolean,
    val reason: String? = null,
    val serviceId: Long? = null,
    val locationId: Int? = null,
    val documentIds: List<Long>? = null,
)
