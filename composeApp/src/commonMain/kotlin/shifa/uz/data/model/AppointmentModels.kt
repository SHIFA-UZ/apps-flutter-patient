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

    // Null is CONFIRMED; unknown values (REQUESTED…) count as PENDING.
    val isConfirmed get() = status == null || status == "CONFIRMED"

    val start get() = parseInstant(startAt)

    // 06: the video join window, start − 5 min to end + 15 min. Home also uses it for "Now".
    private val window get() = start?.let { (it - 5.minutes)..((parseInstant(endAt) ?: it) + 15.minutes) }

    fun isNow(now: Instant) = window?.contains(now) == true

    fun isOver(now: Instant) = window?.let { now > it.endInclusive } ?: true
}
