package shifa.uz.ui.visit

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
import shifa.composeapp.generated.resources.pleaseSignAbove
import shifa.composeapp.generated.resources.signatureSubmittedSuccess
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.utils.getCurrentInstant

/** Figma 1918:22602: a visit in its join window, its doctor (D2), B4 for the signing flags, and signing (B6). */
class LiveAppointmentViewModel(
    appointment: Appointment,
    private val doctors: DoctorsRepository = DoctorsRepository(),
    private val bookings: BookingsRepository = BookingsRepository(),
    clock: () -> Instant = ::getCurrentInstant,
) : ViewModel() {
    private val _state = MutableStateFlow(LiveAppointmentUiState(appointment, clock()))
    val state: StateFlow<LiveAppointmentUiState> = _state.asStateFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private fun update(transform: LiveAppointmentUiState.() -> LiveAppointmentUiState) {
        _state.value = _state.value.transform()
    }

    init {
        // A failed D2 only loses the rating and tags; the card keeps the list's copy.
        appointment.doctorId?.ifBlank { null }?.let { id ->
            viewModelScope.launch {
                doctors.getDoctor(id).fold(onSuccess = { update { copy(doctor = it) } }, onFailure = ::failed)
            }
        }
        reload()
    }

    // A failed B4 keeps the list's copy, which has no signing flags: no Request to sign card then.
    private fun reload() {
        viewModelScope.launch {
            bookings.getAppointment(_state.value.appointment.id).fold(
                onSuccess = { update { copy(appointment = it) } },
                onFailure = ::failed,
            )
        }
    }

    private fun failed(e: Throwable) {
        if (e is ApiException.Unauthorized) _signedOut.trySend(Unit)
    }

    fun toggleDetails() = update { copy(detailsOpen = !detailsOpen) }

    fun openSign() = update { copy(signing = true, signError = null) }

    fun closeSign() {
        if (!_state.value.isSubmitting) update { copy(signing = false) }
    }

    /** [png] is null while the pad is empty. */
    fun sign(png: ByteArray?) {
        if (_state.value.isSubmitting) return
        if (png == null) return update { copy(signError = Res.string.pleaseSignAbove) }
        update { copy(isSubmitting = true, signError = null) }
        viewModelScope.launch {
            bookings.submitSignature(_state.value.appointment.id, png).fold(
                onSuccess = {
                    update {
                        copy(
                            appointment = appointment.copy(alreadySigned = true),
                            signing = false,
                            isSubmitting = false,
                            notice = Res.string.signatureSubmittedSuccess,
                        )
                    }
                    reload()
                },
                onFailure = {
                    update { copy(isSubmitting = false, signError = it.toUserMessage()) }
                    failed(it)
                },
            )
        }
    }
}
