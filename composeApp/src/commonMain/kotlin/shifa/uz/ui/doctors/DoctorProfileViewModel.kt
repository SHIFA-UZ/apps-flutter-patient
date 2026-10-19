package shifa.uz.ui.doctors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Doctor

/** 07 §4.3: a doctor's page, from D2, and its reviews (D4). */
class DoctorProfileViewModel(
    doctor: Doctor,
    private val doctors: DoctorsRepository = DoctorsRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(DoctorProfileUiState(doctor))
    val state: StateFlow<DoctorProfileUiState> = _state.asStateFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private fun update(transform: DoctorProfileUiState.() -> DoctorProfileUiState) {
        _state.value = _state.value.transform()
    }

    init {
        load()
        // D4 alongside; a failure shows in the Reviews tab only.
        viewModelScope.launch {
            doctors.getReviews(doctor.id).fold(
                onSuccess = { update { copy(reviews = it, reviewsError = null) } },
                onFailure = { update { copy(reviews = emptyList(), reviewsError = it.toUserMessage()) } },
            )
        }
    }

    fun load() {
        if (_state.value.isLoading) return
        update { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            doctors.getDoctor(_state.value.doctor.id).fold(
                onSuccess = { update { copy(doctor = it, isLoaded = true, isLoading = false) } },
                onFailure = {
                    update { copy(isLoading = false, error = it.toUserMessage()) }
                    if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                },
            )
        }
    }

    fun selectTab(tab: DoctorTab) = update { copy(tab = tab) }
}
