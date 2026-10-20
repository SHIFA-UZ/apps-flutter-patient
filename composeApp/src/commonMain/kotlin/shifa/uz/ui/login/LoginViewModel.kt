package shifa.uz.ui.login

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
import shifa.uz.data.auth.LoginAttempts
import shifa.uz.data.auth.normalizePhone
import shifa.uz.data.model.ApiException

class LoginViewModel(
    private val repository: AuthRepository = AuthRepository(),
    private val attempts: LoginAttempts = LoginAttempts(),
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    // One-off, so a ViewModel reused after logout doesn't navigate again.
    private val _signedIn = Channel<Unit>(Channel.BUFFERED)
    val signedIn: Flow<Unit> = _signedIn.receiveAsFlow()

    init {
        refreshLockout()
    }

    fun onIdentifierChange(value: String) {
        _state.value = _state.value.copy(identifier = value, identifierError = false, error = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, passwordError = false, error = null)
    }

    fun onTogglePasswordVisibility() {
        _state.value = _state.value.copy(isPasswordVisible = !_state.value.isPasswordVisible)
    }

    /** 05 §2.2: re-reads the persisted lockout. The screen calls it every 30 s while locked. */
    fun refreshLockout() {
        val minutes = attempts.lockedMinutes()
        _state.value =
            _state.value.copy(
                loginLockedUntilMinutes = minutes,
                loginAttemptsRemaining = if (minutes > 0) 0 else _state.value.loginAttemptsRemaining,
            )
    }

    fun signIn() {
        val s = _state.value
        if (!s.isSignInEnabled) return
        // 05 §2.2: both fields must be non-empty; the identifier is trimmed, the password is not.
        _state.value = s.copy(identifierError = s.identifier.isBlank(), passwordError = s.password.isEmpty())
        if (s.identifier.isBlank() || s.password.isEmpty()) return
        if (attempts.lockoutRemainingMs() > 0) return refreshLockout()

        _state.value =
            _state.value.copy(
                isLoading = true,
                error = null,
                loginAttemptsRemaining = 0,
                loginLockedUntilMinutes = 0,
            )
        viewModelScope.launch {
            repository.login(loginUsername(s.identifier), s.password).fold(
                onSuccess = {
                    // ponytail: forcePasswordReset isn't routed until AUTH-07 (reset password) exists.
                    attempts.clear()
                    _state.value = LoginUiState()
                    _signedIn.send(Unit)
                },
                onFailure = { e ->
                    // No response means the password was never checked, so it doesn't count (fixes 05 Q11).
                    if (e !is ApiException.NetworkError) attempts.recordFailure()
                    _state.value =
                        _state.value.copy(
                            isLoading = false,
                            error = e.toUserMessage(),
                            loginAttemptsRemaining = attempts.remainingAttempts(),
                            loginLockedUntilMinutes = attempts.lockedMinutes(),
                        )
                },
            )
        }
    }
}

/** 05 §2.2 identifier normalization. Emails and anything that isn't a phone number go as typed. */
internal fun loginUsername(text: String): String {
    val raw = text.trim()
    if ('@' in raw || raw.none(Char::isDigit) || raw.any(Char::isLetter)) return raw
    return normalizePhone(raw)
}
