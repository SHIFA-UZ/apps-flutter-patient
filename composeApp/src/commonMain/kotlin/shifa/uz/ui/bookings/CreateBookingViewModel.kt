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

/** 06 §3.1 (BOOK-01/03): B1 slots, B3 to book. With [reschedule], B3 books its new time (06 §3.5). */
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
                reason = reschedule?.reason.orEmpty(),
            ),
        )
    val state: StateFlow<CreateBookingUiState> = _state.asStateFlow()

    // true: the payment is still PENDING.
    private val _booked = Channel<Boolean>(Channel.BUFFERED)
    val booked: Flow<Boolean> = _booked.receiveAsFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private var slotsJob: Job? = null

    private fun update(transform: CreateBookingUiState.() -> CreateBookingUiState) {
        _state.value = _state.value.transform()
    }

    init {
        // D2 for the services; on failure the card's copy stays.
        viewModelScope.launch { doctors.getDoctor(doctor.id).onSuccess { update { copy(doctor = it) } } }
        // ponytail: no location step (not in Figma): the primary location, else the first. Add the picker with its design.
        viewModelScope.launch {
            val locations = doctors.getLocations(doctor.id)
            val primary = locations.firstOrNull { it.isPrimary } ?: locations.firstOrNull()
            update { copy(locationId = reschedule?.locationId ?: primary?.id) }
            loadSlots(null)
        }
    }

    fun onVideoChange(on: Boolean) {
        update { copy(isVideo = on, serviceId = null) }
        loadSlots(null)
    }

    fun onServiceChange(id: String) = update { copy(serviceId = id, error = null) }

    fun onDateChange(date: LocalDate) = loadSlots(date)

    fun onSlotSelect(slot: AvailableSlot) = update { copy(slot = slot) }

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

    /** 06 §3.1 step 11. */
    fun book() {
        val state = _state.value
        val slot = state.slot ?: return
        if (!state.canBook) return
        val missing =
            when {
                !state.isVideo -> null
                state.videoServices.isEmpty() -> Res.string.bookingVideoNoServicesOffered
                state.serviceId == null -> Res.string.bookingSelectServiceForVideo
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
                    it.status != "CANCELLED" && it.start != null && it.start == slot.start && it.id != reschedule?.id
                }
            if (taken) return@launch update { copy(isBooking = false, error = Res.string.bookingSlotTaken) }
            val request =
                BookingRequest(
                    doctorId = doctorId,
                    startAt = slot.startAt,
                    slotMinutes = slot.slotMinutes,
                    isVideo = state.isVideo,
                    reason = state.reason.trim().ifEmpty { null },
                    serviceId = state.serviceId?.toLongOrNull()?.takeIf { state.isVideo },
                    locationId = if (state.isVideo) null else slot.locationId ?: state.locationId,
                    documentIds = state.documents.mapNotNull { it.id.toLongOrNull() }.ifEmpty { null },
                    rescheduleAppointmentId = reschedule?.id?.toLongOrNull(),
                )
            // ponytail: PENDING only tells the caller; the $1 checkout WebView comes with BOOK-04.
            bookings.book(request).fold(
                onSuccess = { _booked.send(it.paymentStatus.equals("PENDING", ignoreCase = true)) },
                onFailure = {
                    val error = fail(it)
                    update { copy(isBooking = false, error = error) }
                },
            )
        }
    }

    /**
     * [day] null: 06 §3.1 step 3, the first of the next 30 days with a free slot, else the last of them.
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
                val days = day?.let(::listOf) ?: (0 until SCAN_DAYS).map { today.plus(it, DateTimeUnit.DAY) }
                for (batch in days.chunked(BATCH)) {
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
