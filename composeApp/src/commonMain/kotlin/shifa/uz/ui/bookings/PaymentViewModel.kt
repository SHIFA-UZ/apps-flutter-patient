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
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.paymentStillPendingConfirmBooking
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
    private val clock: () -> Instant = ::getCurrentInstant,
) : ViewModel() {
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
                refresh()
            }
        }
    }

    private val finished get() = _state.value.appointment.let { it.isPaymentDead || (it.isPaid && !it.isAwaitingPayment) }

    /** Complete Payment: asks now, and says so when the backend hasn't seen the payment yet. */
    fun check() {
        if (_state.value.isChecking || finished) return
        update { copy(isChecking = true, notice = null) }
        viewModelScope.launch {
            refresh()
            update {
                copy(isChecking = false, notice = Res.string.paymentStillPendingConfirmBooking.takeIf { !finished })
            }
        }
    }

    // A failed B4 just waits for the next one; a 401 signs out.
    private suspend fun refresh() {
        bookings.getAppointment(_state.value.appointment.id).fold(
            onSuccess = {
                update { copy(appointment = it.copy(payment = it.payment ?: checkout), now = clock()) }
                if (finished) _done.send(_state.value.appointment)
            },
            onFailure = { if (it is ApiException.Unauthorized) _signedOut.send(Unit) },
        )
    }

    private companion object {
        val TICK = 1.seconds
        val POLL = 3.seconds
    }
}
