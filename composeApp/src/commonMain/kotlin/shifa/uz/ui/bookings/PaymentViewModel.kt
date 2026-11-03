package shifa.uz.ui.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.paymentStillPendingConfirmBooking
import shifa.uz.data.analytics.Analytics
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.AnalyticsTracker
import shifa.uz.data.analytics.PaymentReports
import shifa.uz.data.analytics.amountBucket
import shifa.uz.data.analytics.daysAhead
import shifa.uz.data.analytics.hourOfDay
import shifa.uz.data.analytics.paymentFailureReason
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.utils.getCurrentInstant
import kotlin.time.Duration.Companion.seconds

/**
 * MOBILE_BOOKING_PAYMENT §2.3-2.4: the checkout page polls B4 every few seconds while it's open. Only the backend's
 * PAID confirms (never the provider's redirect); [done] then says PAID, or that the hold ran out or the payment failed.
 */
class PaymentViewModel(
    appointment: Appointment,
    private val bookings: BookingsRepository = BookingsRepository(),
    private val analytics: AnalyticsTracker = Analytics,
    private val clock: () -> Instant = ::getCurrentInstant,
) : ViewModel() {
    private val openedAt = clock()
    private var reported = false
    private val _state = MutableStateFlow(PaymentUiState(appointment, clock()))
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    private val _done = Channel<Appointment>(Channel.CONFLATED)
    val done: Flow<Appointment> = _done.receiveAsFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private val checkout = appointment.payment

    private fun update(transform: PaymentUiState.() -> PaymentUiState) {
        _state.value = _state.value.transform()
    }

    init {
        viewModelScope.launch {
            while (!finished) {
                delay(TICK)
                update { copy(now = clock()) }
            }
        }
        viewModelScope.launch {
            while (!finished) {
                delay(POLL)
                refresh("poll")
            }
        }
    }

    private val finished get() =
        _state.value.appointment.let { it.isPaymentDead || (it.isPaid && !it.isAwaitingPayment) }

    /** Complete Payment: asks now, and says so when the backend hasn't seen the payment yet. */
    fun check(via: String = "check") {
        if (_state.value.isChecking || finished) return
        update { copy(isChecking = true, notice = null) }
        viewModelScope.launch {
            refresh(via)
            val a = _state.value.appointment
            // The Complete Payment button only; a redirect (events.md §5) isn't a tap.
            if (via == "check") {
                val result =
                    when {
                        a.isPaid && !a.isPaymentDead -> "paid"
                        a.isPaymentDead -> "failed"
                        else -> "pending"
                    }
                analytics.track(AnalyticsEvent.PaymentCheckTapped(result, secondsOpen()))
                if (!finished) analytics.track(AnalyticsEvent.PaymentStillPendingShown)
            }
            update {
                copy(isChecking = false, notice = Res.string.paymentStillPendingConfirmBooking.takeIf { !finished })
            }
        }
    }

    /** The provider's redirect landed (`success` / `cancel`); success asks B4 at once (events.md §5). */
    fun onRedirect(page: String) {
        analytics.track(AnalyticsEvent.PaymentRedirectSeen(page, checkout?.provider?.lowercase()))
        if (page == "success") check("redirect")
    }

    private fun secondsOpen() = (clock() - openedAt).inWholeSeconds

    // A failed B4 just waits for the next one; a 401 signs out.
    private suspend fun refresh(via: String) {
        bookings.getAppointment(_state.value.appointment.id).fold(
            onSuccess = {
                update { copy(appointment = it.copy(payment = it.payment ?: checkout), now = clock()) }
                if (finished) {
                    report(_state.value.appointment, via)
                    _done.send(_state.value.appointment)
                }
            },
            onFailure = { if (it is ApiException.Unauthorized) _signedOut.send(Unit) },
        )
    }

    // Once: the poll and a check can both see the end.
    private fun report(
        a: Appointment,
        via: String,
    ) {
        if (reported) return
        reported = true
        val provider = a.payment?.provider?.lowercase()
        val failure = a.paymentFailureReason()
        if (failure == null && !PaymentReports.claim(a.id)) return // the booking page already reported it
        analytics.track(
            if (failure == null) {
                val now = clock()
                AnalyticsEvent.PaymentCompleted(
                    provider = provider,
                    secondsToPay = secondsOpen(),
                    via = via,
                    amountBucket = amountBucket(a.paymentAmountMinor, a.paymentCurrency),
                    currency = a.paymentCurrency?.uppercase(),
                    isVideo = a.isVideo,
                    daysAhead = daysAhead(a.start, now),
                    hourOfDay = hourOfDay(a.start, TimeZone.currentSystemDefault()),
                    at = now.toString(),
                )
            } else {
                AnalyticsEvent.PaymentFailed(failure, provider, secondsOpen())
            },
        )
    }

    private companion object {
        val TICK = 1.seconds
        val POLL = 3.seconds
    }
}
