package shifa.uz.ui.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.appointmentCancelledSuccessfully
import shifa.composeapp.generated.resources.appointmentSlotBooked
import shifa.composeapp.generated.resources.errorGettingCurrentLocation
import shifa.composeapp.generated.resources.locationPermissionDenied
import shifa.composeapp.generated.resources.locationPermissionDeniedForever
import shifa.composeapp.generated.resources.locationServicesDisabled
import shifa.composeapp.generated.resources.paymentPendingMessage
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.doctors.doctorsOf
import shifa.uz.data.model.ApiException
import shifa.uz.utils.DeviceLocation
import shifa.uz.utils.getCurrentInstant

class BookingsViewModel(
    private val bookings: BookingsRepository = BookingsRepository(),
    private val doctors: DoctorsRepository = DoctorsRepository(),
    private val clock: () -> Instant = ::getCurrentInstant,
) : ViewModel() {
    private val _state = MutableStateFlow(BookingsUiState())
    val state: StateFlow<BookingsUiState> = _state.asStateFlow()

    // A 401, as on Home.
    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private var searching: Job? = null

    private fun update(transform: BookingsUiState.() -> BookingsUiState) {
        _state.value = _state.value.transform()
    }

    /** First open and pull to refresh: B2 (past + my doctors) and D1. The lists stay on screen meanwhile. */
    fun load() {
        if (_state.value.isLoading) return
        update { copy(isLoading = true) }
        searching?.cancel()
        viewModelScope.launch {
            val errors = listOf(async { loadAppointments() }, async { search() }).awaitAll()
            update { copy(isLoading = false, error = errors.firstNotNullOfOrNull { it }) }
        }
    }

    /** 07: D1 350 ms after the last keystroke. */
    fun search(query: String) {
        update { copy(query = query) }
        research(DEBOUNCE_MS)
    }

    fun filterBy(filter: DoctorFilter) {
        if (filter == _state.value.filter) return
        update { copy(filter = filter) }
        research(0)
    }

    /** Near me tapped: the chip shows at once, the list waits for [located]. */
    fun locating() {
        searching?.cancel()
        update { copy(filter = DoctorFilter.NearMe, recommended = null, isLocating = true, error = null) }
    }

    /** 07 §2.1: search around the fix, or back to All with why there's none. */
    fun located(location: DeviceLocation) {
        val found = location as? DeviceLocation.Found
        update { copy(location = found ?: this.location) }
        if (_state.value.filter != DoctorFilter.NearMe) { // another chip was picked meanwhile
            update { copy(isLocating = false) }
            return
        }
        val why =
            when (location) {
                is DeviceLocation.Found -> null
                DeviceLocation.Disabled -> Res.string.locationServicesDisabled
                DeviceLocation.Denied -> Res.string.locationPermissionDenied
                DeviceLocation.DeniedForever -> Res.string.locationPermissionDeniedForever
                DeviceLocation.Failed -> Res.string.errorGettingCurrentLocation
            }
        if (why != null) update { copy(filter = DoctorFilter.All) }
        research(0, why)
    }

    /** Details cancelled one (B5): reload, with a notice. */
    fun onCancelled() {
        update { copy(notice = Res.string.appointmentCancelledSuccessfully) }
        load()
    }

    /** Create Booking booked one (B3): reload, with a notice. [pending]: the payment is still due. */
    fun onBooked(pending: Boolean) {
        update { copy(notice = if (pending) Res.string.paymentPendingMessage else Res.string.appointmentSlotBooked) }
        load()
    }

    /** [because]: shown when the search itself goes fine (Near me fell back to All). */
    private fun research(
        delayMs: Long,
        because: StringResource? = null,
    ) {
        searching?.cancel()
        searching =
            viewModelScope.launch {
                delay(delayMs)
                val error = search()
                update { copy(error = error ?: because, isLocating = false) }
            }
    }

    private suspend fun search(): StringResource? {
        val state = _state.value
        val near = state.location.takeIf { state.filter == DoctorFilter.NearMe }
        val query =
            state.filter.query.copy(
                search = state.query,
                latitude = near?.latitude,
                longitude = near?.longitude,
                radiusKm = near?.let { NEARBY_KM },
            )
        val found = doctors.searchDoctors(query).getOrElse { return fail(it) }
        // Near me: the backend adds every doctor without coordinates after the near ones.
        update { copy(recommended = if (near == null) found else found.filter { it.distanceFrom(near) != null }) }
        return null
    }

    // One unfiltered B2. Past: over or cancelled, newest first. Future pending ones show nowhere (no Figma).
    private suspend fun loadAppointments(): StringResource? {
        val all = bookings.getAppointments().getOrElse { return fail(it) }
        val now = clock()
        update {
            copy(
                past = all.filter { it.isOver(now) || it.status == "CANCELLED" }.sortedByDescending { it.start },
            )
        }
        val found = doctors.doctorsOf(all).getOrElse { return fail(it) }
        update { copy(myDoctors = found) }
        return null
    }

    private suspend fun fail(e: Throwable): StringResource {
        if (e is ApiException.Unauthorized) _signedOut.send(Unit)
        return e.toUserMessage()
    }

    private companion object {
        const val DEBOUNCE_MS = 350L
    }
}
