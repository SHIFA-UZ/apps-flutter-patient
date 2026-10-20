package shifa.uz.ui.forgotpassword

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.emailOrPhoneRequired
import shifa.composeapp.generated.resources.invalidEmail
import shifa.composeapp.generated.resources.invalidPhone
import shifa.uz.data.auth.effectivePhone
import shifa.uz.data.model.OtpChannel
import shifa.uz.ui.register.confirmPasswordError
import shifa.uz.ui.register.isValidEmail
import shifa.uz.ui.register.passwordError

/** 05 §3.7-3.9: `/forgot-password`, `/forgot-password-otp`, `/forgot-password-new-password`. */
enum class ForgotPasswordStep { Identifier, Code, NewPassword }

@Stable
data class ForgotPasswordUiState(
    val step: ForgotPasswordStep = ForgotPasswordStep.Identifier,
    val identifier: String = "",
    val channel: OtpChannel = OtpChannel.Email,
    val otp: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val showErrors: Boolean = false,
    val expiresInSeconds: Int = 0,
    val resendSeconds: Int = 0,
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val notice: StringResource? = null,
) {
    val isEmail get() = '@' in identifier

    /** 05 §2.6: an email lowercased, else the normalized phone. */
    val normalizedIdentifier get() =
        if (isEmail) identifier.trim().lowercase() else effectivePhone(identifier).orEmpty()

    // Flutter only checked for empty and sent anything else (05 Q8).
    val identifierError get() =
        when {
            identifier.isBlank() -> Res.string.emailOrPhoneRequired
            isEmail -> Res.string.invalidEmail.takeUnless { isValidEmail(normalizedIdentifier) }
            else -> Res.string.invalidPhone.takeIf { normalizedIdentifier.isEmpty() }
        }
    val passwordError: StringResource? get() = passwordError(password)
    val confirmPasswordError: StringResource? get() = confirmPasswordError(password, confirmPassword)

    val canResend get() = resendSeconds == 0 && !isLoading
}
