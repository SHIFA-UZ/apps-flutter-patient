package shifa.uz.ui.login

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.auth.MAX_LOGIN_ATTEMPTS

@Stable
data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val identifierError: Boolean = false,
    val passwordError: Boolean = false,
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val loginAttemptsRemaining: Int = 0,
    val loginLockedUntilMinutes: Int = 0,
) {
    val isLocked get() = loginLockedUntilMinutes > 0
    val showAttemptsBanner get() = !isLocked && loginAttemptsRemaining in 1 until MAX_LOGIN_ATTEMPTS
    val isSignInEnabled get() = !isLoading && !isLocked
}
