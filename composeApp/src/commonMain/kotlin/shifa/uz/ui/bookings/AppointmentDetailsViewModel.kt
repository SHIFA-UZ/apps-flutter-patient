package shifa.uz.ui.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.thankYouForYourRating
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Review
import shifa.uz.data.model.ReviewRequest
import shifa.uz.utils.getCurrentInstant

/** 06 §4.3: one B2 appointment, its doctor (D2), cancel (B5), and for a past one its review (B7, D5). */
class AppointmentDetailsViewModel(
    appointment: Appointment,
    private val doctors: DoctorsRepository = DoctorsRepository(),
    private val bookings: BookingsRepository = BookingsRepository(),
    clock: () -> Instant = ::getCurrentInstant,
) : ViewModel() {
    private val _state = MutableStateFlow(AppointmentDetailsUiState(appointment, clock()))
    val state: StateFlow<AppointmentDetailsUiState> = _state.asStateFlow()

    private val _cancelled = Channel<Unit>(Channel.BUFFERED)
    val cancelled: Flow<Unit> = _cancelled.receiveAsFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private fun update(transform: AppointmentDetailsUiState.() -> AppointmentDetailsUiState) {
        _state.value = _state.value.transform()
    }

    init {
        // A failed D2 only loses the rating, contacts and tags; the card keeps B2's copy.
        appointment.doctorId?.ifBlank { null }?.let { id ->
            viewModelScope.launch {
                doctors.getDoctor(id).fold(
                    onSuccess = { update { copy(doctor = it) } },
                    onFailure = { if (it is ApiException.Unauthorized) _signedOut.send(Unit) },
                )
            }
        }
        if (_state.value.isPast && appointment.status != "CANCELLED") {
            viewModelScope.launch {
                val review = bookings.getReview(appointment.id)
                update { copy(review = review, reviewChecked = true) }
            }
        }
    }

    fun toggleDetails() = update { copy(detailsOpen = !detailsOpen) }

    fun openReview() {
        if (_state.value.canReview) update { copy(reviewOpen = true, reviewError = null) }
    }

    fun closeReview() {
        if (!_state.value.isReviewing) update { copy(reviewOpen = false) }
    }

    fun onRate(rating: Int) = update { copy(rating = rating, reviewError = null) }

    fun onCommentChange(comment: String) = update { copy(comment = comment) }

    /** D5 with the visit's id. Its 401 doesn't log out (15 §6.2: a write under /public). */
    fun submitReview() {
        val s = _state.value
        val doctorId = s.appointment.doctorId ?: return
        if (s.rating == 0 || s.isReviewing) return
        val comment = s.comment.trim().ifEmpty { null }
        update { copy(isReviewing = true, reviewError = null) }
        viewModelScope.launch {
            doctors.submitReview(doctorId, ReviewRequest(s.rating, comment, s.appointment.id.toLongOrNull())).fold(
                onSuccess = {
                    update {
                        copy(
                            review = Review(rating = s.rating, comment = comment),
                            reviewOpen = false,
                            isReviewing = false,
                            notice = Res.string.thankYouForYourRating,
                        )
                    }
                },
                onFailure = { update { copy(isReviewing = false, reviewError = it.toUserMessage()) } },
            )
        }
    }

    fun askCancel() {
        if (_state.value.canCancel) update { copy(confirmCancel = true, error = null) }
    }

    fun dismissCancel() {
        if (!_state.value.isCancelling) update { copy(confirmCancel = false) }
    }

    fun cancel() {
        if (_state.value.isCancelling) return
        update { copy(isCancelling = true) }
        viewModelScope.launch {
            bookings.cancel(_state.value.appointment.id).fold(
                onSuccess = { _cancelled.send(Unit) },
                onFailure = {
                    update { copy(isCancelling = false, confirmCancel = false, error = it.toUserMessage()) }
                    if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                },
            )
        }
    }
}
