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
    val locationId: Int? = null,
    val locationLabel: String? = null,
    val locationAddress: String? = null,
    // B4 only: B2 leaves them out (seen 2026-10-11).
    val signatureRequested: Boolean = false,
    val alreadySigned: Boolean = false,
    // MOBILE_BOOKING_PAYMENT §5.
    val isVideoConsultation: Boolean? = null,
    val serviceId: String? = null,
    val serviceTitle: String? = null,
    val paymentType: String? = null, // PAY_NOW, PAY_LATER, null: free or old
    val paymentBadge: String? = null, // PAID, PAY_IN_CLINIC
    val paymentAmountMinor: Long? = null,
    val paymentCurrency: String? = null,
    val holdExpiresAt: String? = null, // while PENDING_PAYMENT
    val isLocked24h: Boolean? = null,
    val canCancel: Boolean? = null,
    val canReschedule: Boolean? = null,
    val refundRequired: Boolean = false,
    val payment: PaymentCheckout? = null, // B3's reply for pay now only
) {
    val needsSignature get() = signatureRequested && !alreadySigned

    val isVideo get() = isVideoConsultation ?: (videoFlag == true || location == "Video Consultation")

    val isPaid get() = paymentBadge == PAID || paymentStatus == PAID

    val isPayInClinic get() = paymentBadge == PAY_IN_CLINIC || paymentStatus == PAY_IN_CLINIC

    val isAwaitingPayment get() = status == PENDING_PAYMENT

    /** The pay-now booking died: its hold ran out, or the provider declined (the slot is free again). */
    val isPaymentDead get() = status == EXPIRED || status == PAYMENT_FAILED

    val price get() = paymentAmountMinor?.let { moneyText(it, paymentCurrency.orEmpty()) }

    // Null is CONFIRMED, and IN_PROGRESS (the visit has started) is past it. REQUESTED waits for the doctor:
    // B3 creates every booking as REQUESTED and nothing confirms it automatically.
    val isConfirmed get() = status == null || status == "CONFIRMED" || status == "IN_PROGRESS"

    // 06 quirk 12: Flutter ignored the status here. REQUESTED and PENDING are open (new bookings are either, 15 B2).
    val isOpen get() = status != "CANCELLED" && status != "COMPLETED" && !isPaymentDead

    val start get() = parseInstant(startAt)

    // 06: the video join window, start − 5 min to end + 15 min. Home also uses it for "Now".
    private val window get() = start?.let { (it - 5.minutes)..((parseInstant(endAt) ?: it) + 15.minutes) }

    fun isNow(now: Instant) = window?.contains(now) == true

    fun isOver(now: Instant) = window?.let { now > it.endInclusive } ?: true

    companion object {
        const val PAID = "PAID"
        const val PAY_IN_CLINIC = "PAY_IN_CLINIC"
        const val PENDING_PAYMENT = "PENDING_PAYMENT"
        const val EXPIRED = "EXPIRED"
        const val PAYMENT_FAILED = "PAYMENT_FAILED"
    }
}

/** B3's pay-now checkout (MOBILE_BOOKING_PAYMENT §2.2). */
@Serializable
data class PaymentCheckout(
    val id: String = "",
    val provider: String? = null,
    val checkoutUrl: String? = null,
    val expiresAt: String? = null,
)

/** `GET payments/methods` item: a provider the service can be paid with (§2.1). */
@Serializable
data class PaymentMethod(
    val provider: String = "",
    val amountMinor: Long = 0,
    val currency: String = "",
) {
    val price get() = moneyText(amountMinor, currency)

    /** CLICK → Click, STRIPE → Stripe. */
    val name get() = provider.lowercase().replaceFirstChar { it.uppercase() }
}

/** D2's [doctor], else the card's copy from the appointment (until D2 loads, or if it fails). */
fun Appointment.shownDoctor(doctor: Doctor?) =
    doctor ?: Doctor(
        id = doctorId.orEmpty(),
        fullName = doctorName,
        profession = doctorProfession,
        clinic = doctorClinic,
        photoUrl = doctorPhotoUrl,
    )

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
    val rescheduleAppointmentId: Long? = null, // 06 §3.5: a reschedule is a new B3 naming the old one
    val paymentProvider: String? = null, // pay now only, from payments/methods (§2.2)
)

/** B6 body: the PNG as plain base64, no `data:` prefix (06 §3.8). */
@Serializable
data class SignatureRequest(
    val signatureImageBase64: String,
)

/** V1 body. */
@Serializable
data class VideoTokenRequest(
    val appointmentId: Long,
)

/** V1: a Daily meeting token for [roomUrl] (06 §5.3). */
@Serializable
data class VideoToken(
    val token: String = "",
    val roomUrl: String = "",
    val roomName: String? = null,
)
