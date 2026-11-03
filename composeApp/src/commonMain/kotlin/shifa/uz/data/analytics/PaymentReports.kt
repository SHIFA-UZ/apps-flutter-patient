package shifa.uz.data.analytics

/**
 * Which pay-now bookings already sent `payment_completed` (events.md §5). The payment page's poll and the booking
 * page's close-time check can both see PAID, in either order, so the two ViewModels share this guard.
 */
object PaymentReports {
    private val reported = mutableSetOf<String>()

    /** True the first time [appointmentId] is claimed. */
    fun claim(appointmentId: String): Boolean = reported.add(appointmentId)

    internal fun clearForTests() = reported.clear()
}
