package shifa.uz.ui.profile

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
import shifa.uz.data.auth.AuthRepository

/** A8. A wrong current password is a 400 "Current password is incorrect"; the session stays. */
class ChangePasswordViewModel(
    private val auth: AuthRepository = AuthRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    private val _changed = Channel<Unit>(Channel.BUFFERED)
    val changed: Flow<Unit> = _changed.receiveAsFlow()

    private fun update(transform: ChangePasswordUiState.() -> ChangePasswordUiState) {
        _state.value = _state.value.transform()
    }

    fun onCurrentPasswordChange(value: String) = update { copy(currentPassword = value, error = null) }

    fun onNewPasswordChange(value: String) = update { copy(newPassword = value, error = null) }

    fun onConfirmPasswordChange(value: String) = update { copy(confirmPassword = value, error = null) }

    fun onTogglePasswordVisibility() = update { copy(isPasswordVisible = !isPasswordVisible) }

    fun save() {
        val s = _state.value
        if (s.isSaving) return
        if (!s.isValid) return update { copy(showErrors = true) }
        update { copy(isSaving = true, error = null) }
        viewModelScope.launch {
            auth.changePassword(s.currentPassword, s.newPassword).fold(
                onSuccess = { _changed.send(Unit) },
                onFailure = { update { copy(isSaving = false, error = it.toUserMessage()) } },
            )
        }
    }
}
