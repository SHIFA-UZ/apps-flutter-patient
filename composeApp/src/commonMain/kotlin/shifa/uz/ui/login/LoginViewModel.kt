package shifa.uz.ui.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onIdentifierChange(value: String) {
        _state.value = _state.value.copy(identifier = value, identifierError = false)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, passwordError = false)
    }

    fun onTogglePasswordVisibility() {
        _state.value = _state.value.copy(isPasswordVisible = !_state.value.isPasswordVisible)
    }

    // ponytail: validation only. AUTH-02 sends the normalized identifier to AuthRepository.login and applies the lockout.
    fun signIn() {
        val s = _state.value
        if (!s.isSignInEnabled) return
        // 05 §2.2: both fields must be non-empty; the identifier is trimmed, the password is not.
        _state.value = s.copy(identifierError = s.identifier.isBlank(), passwordError = s.password.isEmpty())
    }
}
