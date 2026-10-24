package shifa.uz.ui.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
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
import shifa.composeapp.generated.resources.appointmentRescheduledSuccessfully
import shifa.composeapp.generated.resources.appointmentSlotBooked
import shifa.composeapp.generated.resources.paymentPendingMessage
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsQuery
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.doctors.doctorsOf
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Doctor
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

    // One D1 per query until the next load(): a chip seen before shows at once, and after the first load the other
    // chips are fetched in the background (D1 takes 1-2 s a page). ponytail: typed searches are cached too, a
    // session's few dozen at most; add a cap if that ever grows.
    private val searches = mutableMapOf<DoctorsQuery, Deferred<Result<List<Doctor>>>>()

    private fun update(transform: BookingsUiState.() -> BookingsUiState) {
        _state.value = _state.value.transform()
    }

    /** First open and pull to refresh: B2 (past + my doctors) and D1. The lists stay on screen meanwhile. */
    fun load() {
        if (_state.value.isLoading) return
        update { copy(isLoading = true) }
        searching?.cancel()
        searches.clear()
        viewModelScope.launch {
            val errors = listOf(async { loadAppointments() }, async { search(keepList = true) }).awaitAll()
            update { copy(isLoading = false, error = errors.firstNotNullOfOrNull { it }) }
            if (_state.value.query.isBlank()) DoctorFilter.entries.forEach { searchOf(it.query.copy(search = "")) }
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

    /** Reschedule booked the new time (B3): reload, with a notice. */
    fun onRescheduled() {
        update { copy(notice = Res.string.appointmentRescheduledSuccessfully) }
        load()
    }

    private fun research(delayMs: Long) {
        searching?.cancel()
        searching =
            viewModelScope.launch {
                delay(delayMs)
                val error = search()
                update { copy(error = error) }
            }
    }

    private fun searchOf(query: DoctorsQuery) =
        searches.getOrPut(query) { viewModelScope.async { doctors.searchDoctors(query) } }

    /** A cached answer shows at once; otherwise skeletons until D1 is back, except on a reload ([keepList]). */
    private suspend fun search(keepList: Boolean = false): StringResource? {
        val state = _state.value
        val query = state.filter.query.copy(search = state.query)
        val pending = searchOf(query)
        if (!pending.isCompleted) update { copy(isSearching = true, recommended = if (keepList) recommended else null) }
        val result = pending.await()
        update { copy(isSearching = false) }
        val found =
            result.getOrElse {
                searches.remove(query) // asked again next time
                return fail(it)
            }
        update { copy(recommended = found) }
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
