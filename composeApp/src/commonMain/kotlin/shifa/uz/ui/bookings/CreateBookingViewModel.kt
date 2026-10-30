package shifa.uz.ui.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.bookingDocUnreadableSkipped
import shifa.composeapp.generated.resources.bookingSelectService
import shifa.composeapp.generated.resources.bookingSelectServiceForVideo
import shifa.composeapp.generated.resources.bookingSlotTaken
import shifa.composeapp.generated.resources.bookingVideoNoServicesOffered
import shifa.composeapp.generated.resources.failedToBookAppointment
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.documents.DocumentsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.AvailableSlot
import shifa.uz.data.model.BookingRequest
import shifa.uz.data.model.Doctor
import shifa.uz.utils.getCurrentInstant
import shifa.uz.utils.parseInstant

/**
 * 06 §3.1 (BOOK-01/03/04): B1 slots, B3 to book. With [reschedule], B3 books its new time (06 §3.5).
 * MOBILE_BOOKING_PAYMENT: a pay-now service gets its providers first (§2.1), B3 names the chosen one and returns a
 * checkout held until `holdExpiresAt` ([paid] once the backend says PAID); free and pay later book at once (§3).
 */
class CreateBookingViewModel(
    doctor: Doctor,
    private val bookings: BookingsRepository = BookingsRepository(),
    private val doctors: DoctorsRepository = DoctorsRepository(),
    private val documents: DocumentsRepository = DocumentsRepository(),
    private val clock: () -> Instant = ::getCurrentInstant,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
    private val reschedule: Appointment? = null,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            CreateBookingUiState(
                doctor,
                isVideo = reschedule?.isVideo == true,
                serviceId = reschedule?.serviceId,
                reason = reschedule?.reason.orEmpty(),
            ),
        )
    val state: StateFlow<CreateBookingUiState> = _state.asStateFlow()

    // true: the payment is still PENDING (the legacy flow, without a provider).
    private val _booked = Channel<Boolean>(Channel.BUFFERED)
    val booked: Flow<Boolean> = _booked.receiveAsFlow()

    /** A pay-now booking the backend confirmed as PAID. */
    private val _paid = Channel<Appointment>(Channel.BUFFERED)
    val paid: Flow<Appointment> = _paid.receiveAsFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private var slotsJob: Job? = null

    private fun update(transform: CreateBookingUiState.() -> CreateBookingUiState) {
        _state.value = _state.value.transform()
    }

    init {
        viewModelScope.launch {
            // D2 for the services and `nextAvailableStartAt` (the scan's starting day); on failure the card's copy stays.
            val detail = async { doctors.getDoctor(doctor.id) }
            // The primary location, else the first; Select Clinic changes it when there are several (Figma 1823:16296).
            val locations = doctors.getLocations(doctor.id)
            val primary = locations.firstOrNull { it.isPrimary } ?: locations.firstOrNull()
            detail.await().onSuccess { update { copy(doctor = it) } }
            update { copy(locations = locations, locationId = reschedule?.locationId ?: primary?.id) }
            loadSlots(null)
        }
    }

    fun onLocationChange(id: Int) {
        discardPending()
        update { copy(locationId = id, error = null).let { s -> s.copy(serviceId = s.service?.id) } }
        loadSlots(null)
    }

    /** Video keeps a service that allows it; pay later never does (§1). */
    fun onVideoChange(on: Boolean) {
        discardPending()
        update { copy(isVideo = on, error = null).let { s -> s.copy(serviceId = s.service?.id) } }
        loadSlots(null)
    }

    fun onServiceChange(id: String) {
        discardPending()
        update { copy(serviceId = id, error = null) }
        // A pay-later service is in person only.
        if (_state.value.isVideo && _state.value.service?.canVideo == false) onVideoChange(false)
    }

    fun onDateChange(date: LocalDate) {
        discardPending()
        loadSlots(date)
    }

    fun onSlotSelect(slot: AvailableSlot) {
        discardPending()
        update { copy(slot = slot) }
    }

    fun onReasonChange(reason: String) = update { copy(reason = reason) }

    /** F2 at once, as a pre-appointment document (06 §3.1 step 9). */
    fun attach(
        fileName: String,
        bytes: ByteArray,
    ) {
        if (!_state.value.canAttach) return
        if (bytes.isEmpty()) return update { copy(error = Res.string.bookingDocUnreadableSkipped) }
        update { copy(isUploading = true, error = null) }
        viewModelScope.launch {
            documents.upload(bytes, fileName, fileName, PRE_APPOINTMENT).fold(
                onSuccess = { update { copy(isUploading = false, documents = documents + it) } },
                onFailure = {
                    val error = fail(it)
                    update { copy(isUploading = false, error = error) }
                },
            )
        }
    }

    /** 06 §3.1 step 11. A pay-now service opens the Confirm Payment tray instead; a held one opens its payment. */
    fun book() {
        val state = _state.value
        val slot = state.slot ?: return
        if (!state.canBook) return
        if (state.isHeld(clock())) return update { copy(paying = true, error = null) }
        val missing =
            when {
                state.isVideo && state.services.isEmpty() -> Res.string.bookingVideoNoServicesOffered
                state.isVideo && state.service == null -> Res.string.bookingSelectServiceForVideo
                reschedule == null && state.services.isNotEmpty() && state.service == null ->
                    Res.string.bookingSelectService
                else -> null
            }
        if (missing != null) return update { copy(error = missing) }
        val doctorId =
            state.doctor.id.toLongOrNull() ?: return update {
                copy(
                    error = Res.string.failedToBookAppointment,
                )
            }
        update { copy(isBooking = true, error = null) }
        viewModelScope.launch {
            // No second visit at the same start; if B2 fails, the backend decides.
            val taken =
                bookings.getAppointments().getOrNull().orEmpty().any {
                    it.isOpen && it.start != null && it.start == slot.start && it.id != reschedule?.id
                }
            if (taken) return@launch update { copy(isBooking = false, error = Res.string.bookingSlotTaken) }
            // Reschedule keeps the visit's payment, so it never asks for one.
            val service = state.service
            if (service == null || !state.isPayNow || reschedule != null) {
                return@launch submit(request(state, slot, doctorId, null))
            }
            bookings.getPaymentMethods(service.id, state.locationId.takeUnless { state.isVideo }).fold(
                onSuccess = { methods ->
                    // None: the backend treats it as free or pay later after all (§2.1).
                    if (methods.isEmpty()) {
                        submit(request(state, slot, doctorId, null))
                    } else {
                        update { copy(isBooking = false, methods = methods, provider = methods.first().provider) }
                    }
                },
                onFailure = {
                    val error = fail(it)
                    update { copy(isBooking = false, error = error) }
                },
            )
        }
    }

    fun onProviderSelect(provider: String) = update { copy(provider = provider) }

    fun closeMethods() {
        if (!_state.value.isBooking) update { copy(methods = null) }
    }

    /** Pay Now: B3 with the provider (§2.2). A failure books nothing, so the tray stays to try again. */
    fun pay() {
        val state = _state.value
        val slot = state.slot ?: return
        val provider = state.provider ?: return
        val doctorId = state.doctor.id.toLongOrNull() ?: return
        if (state.isBooking) return
        update { copy(isBooking = true, error = null) }
        viewModelScope.launch { submit(request(state, slot, doctorId, provider)) }
    }

    /** The payment page closed before an answer: the slot stays held, Complete Payment goes back to it. */
    fun onPaymentClosed() = update { copy(paying = false) }

    /** The payment page saw the booking end: PAID, or the hold / payment died and the slot is free again. */
    fun onPaymentDone(appointment: Appointment) {
        if (appointment.isPaid && !appointment.isPaymentDead) {
            update { copy(paying = false) }
            _paid.trySend(appointment)
            return
        }
        update { copy(pending = null, paying = false) }
        loadSlots(_state.value.date)
    }

    private fun request(
        state: CreateBookingUiState,
        slot: AvailableSlot,
        doctorId: Long,
        provider: String?,
    ) = BookingRequest(
        doctorId = doctorId,
        startAt = slot.startAt,
        slotMinutes = slot.slotMinutes,
        isVideo = state.isVideo,
        reason = state.reason.trim().ifEmpty { null },
        serviceId = state.service?.id?.toLongOrNull(),
        locationId = if (state.isVideo) null else slot.locationId ?: state.locationId,
        documentIds = state.documents.mapNotNull { it.id.toLongOrNull() }.ifEmpty { null },
        rescheduleAppointmentId = reschedule?.id?.toLongOrNull(),
        paymentProvider = provider,
    )

    private suspend fun submit(request: BookingRequest) {
        bookings.book(request).fold(
            onSuccess = {
                when {
                    it.isAwaitingPayment && it.payment?.checkoutUrl != null ->
                        update { copy(isBooking = false, methods = null, pending = it, paying = true) }
                    request.paymentProvider != null && it.isPaid -> {
                        update { copy(isBooking = false, methods = null) }
                        _paid.send(it)
                    }
                    else -> _booked.send(it.paymentStatus.equals("PENDING", ignoreCase = true))
                }
            },
            onFailure = {
                val error = fail(it)
                update { copy(isBooking = false, error = error) }
            },
        )
    }

    /**
     * A changed choice drops the held booking: B5 frees its slot (§6), best effort. If it was paid meanwhile, the
     * backend marks it for a refund.
     */
    private fun discardPending() {
        val pending = _state.value.pending ?: return
        update { copy(pending = null) }
        viewModelScope.launch { bookings.cancel(pending.id) }
    }

    /**
     * [day] null: 06 §3.1 step 3, the first of the next 30 days with a free slot, else the last of them.
     * The scan starts on the day of D2's `nextAvailableStartAt` when there is one (one B1 call in the usual case;
     * it ignores the location, so the scan goes on from there when that day has nothing here), else today.
     * Days go out in batches of [BATCH] in parallel (Flutter went one by one) and are read in order.
     */
    private fun loadSlots(day: LocalDate?) {
        slotsJob?.cancel()
        update { copy(date = day, slots = null, slot = null) }
        val state = _state.value
        val location = state.locationId.takeUnless { state.isVideo }
        slotsJob =
            viewModelScope.launch {
                val now = clock()
                val today = now.toLocalDateTime(zone).date
                val hint =
                    parseInstant(state.doctor.nextAvailableStartAt)?.toLocalDateTime(zone)?.date?.takeIf { it >= today }
                val from = hint ?: today
                val days = day?.let(::listOf) ?: (0 until SCAN_DAYS).map { from.plus(it, DateTimeUnit.DAY) }
                val batches =
                    if (day == null && hint != null) {
                        listOf(days.take(1)) + days.drop(1).chunked(BATCH)
                    } else {
                        days.chunked(BATCH)
                    }
                for (batch in batches) {
                    val results = batch.map { async { bookings.getSlots(state.doctor.id, it, location) } }.awaitAll()
                    batch.zip(results).forEach { (d, result) ->
                        result.exceptionOrNull()?.let {
                            val error = fail(it)
                            return@launch update { copy(slots = emptyList(), error = error) }
                        }
                        // Today: only slots still ahead; the filter holds for any day.
                        val free = result.getOrThrow().filter { slot -> slot.start?.let { it > now } == true }
                        if (free.isNotEmpty() || d == days.last()) {
                            return@launch update { copy(date = d, slots = free, slot = free.firstOrNull()) }
                        }
                    }
                }
            }
    }

    private suspend fun fail(e: Throwable): StringResource {
        if (e is ApiException.Unauthorized) _signedOut.send(Unit)
        return e.toUserMessage()
    }

    private companion object {
        const val SCAN_DAYS = 30
        const val BATCH = 7
        const val PRE_APPOINTMENT = "PRE_APPOINTMENT"
    }
}
