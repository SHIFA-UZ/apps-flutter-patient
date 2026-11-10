package shifa.uz.ui.bookings

import androidx.compose.runtime.Stable
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.paymentExpiredSlotReleased
import shifa.composeapp.generated.resources.paymentFailedSlotReleased
import shifa.composeapp.generated.resources.paymentRefundSlotTaken
import shifa.uz.data.model.Appointment
import shifa.uz.utils.parseInstant
import kotlin.time.Duration

/** [appointment] is B3's pay-now booking, refreshed by B4 while the page polls; it keeps B3's checkout. */
@Stable
data class PaymentUiState(
    val appointment: Appointment,
    val now: Instant,
    val isChecking: Boolean = false,
    val notice: StringResource? = null,
    /** The provider's success page was seen: the money may be on its way, so leaving never cancels the booking. */
    val providerSaidPaid: Boolean = false,
) {
    val checkoutUrl get() = appointment.payment?.checkoutUrl.orEmpty()

    /** Time left on the held slot (§2.3: always `holdExpiresAt`, never a fixed 15 minutes). */
    val holdLeft: Duration?
        get() =
            (parseInstant(appointment.holdExpiresAt) ?: parseInstant(appointment.payment?.expiresAt))
                ?.let { (it - now).coerceAtLeast(Duration.ZERO) }

    /** The booking ended without a visit (§2.4, §7); null while it's pending or once it's paid. */
    val failure
        get() =
            when {
                !appointment.isPaymentDead -> null
                appointment.isPaid || appointment.refundRequired -> Res.string.paymentRefundSlotTaken
                appointment.status == Appointment.EXPIRED -> Res.string.paymentExpiredSlotReleased
                else -> Res.string.paymentFailedSlotReleased
            }
}
