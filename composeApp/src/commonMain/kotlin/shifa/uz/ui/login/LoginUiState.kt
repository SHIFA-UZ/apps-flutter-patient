package shifa.uz.ui.login

import androidx.compose.runtime.Stable

private const val MAX_LOGIN_ATTEMPTS = 5 // 05 §6.2

@Stable
data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val identifierError: Boolean = false,
    val passwordError: Boolean = false,
    val isLoading: Boolean = false,
    val loginAttemptsRemaining: Int = 0,
    val loginLockedUntilMinutes: Int = 0,
) {
    val isLocked get() = loginLockedUntilMinutes > 0
    val showAttemptsBanner get() = !isLocked && loginAttemptsRemaining in 1 until MAX_LOGIN_ATTEMPTS
    val isSignInEnabled get() = !isLoading && !isLocked
}
