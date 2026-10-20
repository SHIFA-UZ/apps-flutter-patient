package shifa.uz.ui.register

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.emailRequiredForForeignPhone
import shifa.composeapp.generated.resources.invalidEmail
import shifa.composeapp.generated.resources.invalidPhone
import shifa.composeapp.generated.resources.passwordRequired
import shifa.composeapp.generated.resources.passwordRequirementDigit
import shifa.composeapp.generated.resources.passwordRequirementLowercase
import shifa.composeapp.generated.resources.passwordRequirementMaxLength
import shifa.composeapp.generated.resources.passwordRequirementMinLength
import shifa.composeapp.generated.resources.passwordRequirementSpecialChar
import shifa.composeapp.generated.resources.passwordRequirementUppercase
import shifa.composeapp.generated.resources.passwordTooLong
import shifa.composeapp.generated.resources.passwordTooShort
import shifa.composeapp.generated.resources.passwordsDoNotMatch
import shifa.composeapp.generated.resources.phoneOrEmailRequired
import shifa.composeapp.generated.resources.pleaseConfirmPassword
import shifa.composeapp.generated.resources.required
import shifa.uz.data.auth.effectivePhone
import shifa.uz.data.auth.isUzbekPhone
import shifa.uz.data.model.OtpChannel
import shifa.uz.utils.Country
import shifa.uz.utils.ImagePickerResult

/** The three Figma steps, then the code screen (05 §3.4). */
enum class RegisterStep { Account, Info, Photo, Otp }

sealed interface RegisterDialog {
    data object ExistingPatient : RegisterDialog

    data class ExistingDoctor(
        val name: String,
    ) : RegisterDialog

    /** 05 §2.4 step 6: SMS failed, ask for an email. Not dismissible. */
    data object EmailFallback : RegisterDialog
}

/** 05 §6.3, in the order the first failure is reported. */
enum class PasswordRule(
    val label: StringResource,
    val passes: (String) -> Boolean,
    val error: StringResource = label,
) {
    MinLength(Res.string.passwordRequirementMinLength, { it.length >= 8 }, Res.string.passwordTooShort),
    MaxLength(Res.string.passwordRequirementMaxLength, { it.length <= 128 }, Res.string.passwordTooLong),
    Uppercase(Res.string.passwordRequirementUppercase, { p -> p.any { it in 'A'..'Z' } }),
    Lowercase(Res.string.passwordRequirementLowercase, { p -> p.any { it in 'a'..'z' } }),
    Digit(Res.string.passwordRequirementDigit, { p -> p.any { it in '0'..'9' } }),
    SpecialChar(Res.string.passwordRequirementSpecialChar, { SPECIAL.containsMatchIn(it) }),
}

private val SPECIAL = Regex("""[^A-Za-z0-9\s]""")

// 05 §3.3, but any TLD length: the Flutter regex caps it at 4 and rejects `.solutions` (05 Q19).
private val EMAIL = Regex("""^[\w.-]+@([\w-]+\.)+[\w-]{2,}$""")

internal fun isValidEmail(email: String) = EMAIL.matches(email)

/** 05 §6.3, also used by forgot password. */
internal fun passwordError(password: String) =
    if (password.isEmpty()) {
        Res.string.passwordRequired
    } else {
        PasswordRule.entries.firstOrNull { !it.passes(password) }?.error
    }

internal fun confirmPasswordError(
    password: String,
    confirm: String,
) = when {
    confirm.isEmpty() -> Res.string.pleaseConfirmPassword
    confirm != password -> Res.string.passwordsDoNotMatch
    else -> null
}

@Stable
data class RegisterUiState(
    val step: RegisterStep = RegisterStep.Account,
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val showErrors: Boolean = false,
    val birthDate: LocalDate? = null,
    val country: Country? = null,
    val address: String = "",
    val photo: ImagePickerResult? = null,
    val isDoctorLink: Boolean = false,
    val fallbackEmail: String? = null,
    val otpChannel: OtpChannel = OtpChannel.Email,
    val otp: String = "",
    val resendSeconds: Int = 0,
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val notice: StringResource? = null,
    val dialog: RegisterDialog? = null,
    val fallbackEmailInput: String = "",
    val fallbackEmailInputError: Boolean = false,
) {
    val phoneOrNull get() = effectivePhone(phone)

    /** Where an email code goes: the typed email, else the one from the fallback dialog. */
    val otpEmail get() = email.trim().ifEmpty { null } ?: fallbackEmail

    // 05 §3.3 validation. Names are checked trimmed, so spaces alone don't pass (05 Q19).
    val firstNameError get() = Res.string.required.takeIf { firstName.isBlank() }
    val lastNameError get() = Res.string.required.takeIf { lastName.isBlank() }
    val emailError get() = Res.string.invalidEmail.takeIf { email.isNotBlank() && !isValidEmail(email.trim()) }
    val phoneError: StringResource?
        get() {
            val noEmail = email.isBlank()
            val phone = phoneOrNull
            return when {
                this.phone.none(Char::isDigit) -> Res.string.phoneOrEmailRequired.takeIf { noEmail }
                phone == null -> Res.string.invalidPhone
                noEmail && !isUzbekPhone(phone) -> Res.string.emailRequiredForForeignPhone
                else -> null
            }
        }
    val passwordError: StringResource? get() = passwordError(password)
    val confirmPasswordError: StringResource? get() = confirmPasswordError(password, confirmPassword)
    val isAccountValid
        get() =
            listOf(firstNameError, lastNameError, emailError, phoneError, passwordError, confirmPasswordError)
                .all { it == null }

    /** 05 §2.4 step 8: an email as is, a phone masked as `+998 ** *** 1234` (SMS only goes to Uzbek numbers). */
    val otpDestination get() =
        if (otpChannel == OtpChannel.Email) otpEmail.orEmpty() else "+998 ** *** ${phoneOrNull.orEmpty().takeLast(4)}"

    val canResend get() = resendSeconds == 0 && !isLoading
}
