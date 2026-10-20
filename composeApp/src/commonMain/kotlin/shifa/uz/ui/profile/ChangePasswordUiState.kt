package shifa.uz.ui.profile

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.passwordRequired
import shifa.uz.ui.register.confirmPasswordError
import shifa.uz.ui.register.passwordError

/** The new password follows the registration policy (05 §6.3). */
@Stable
data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    val error: StringResource? = null,
) {
    val currentPasswordError get() = Res.string.passwordRequired.takeIf { currentPassword.isEmpty() }
    val newPasswordError get() = passwordError(newPassword)
    val confirmPasswordError get() = confirmPasswordError(newPassword, confirmPassword)
    val isValid get() = listOf(currentPasswordError, newPasswordError, confirmPasswordError).all { it == null }
}
