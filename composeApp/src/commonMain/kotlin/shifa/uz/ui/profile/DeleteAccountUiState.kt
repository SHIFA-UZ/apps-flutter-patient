package shifa.uz.ui.profile

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource

@Stable
data class DeleteAccountUiState(
    val otp: String = "",
    val resendSeconds: Int = 0,
    val isSending: Boolean = false,
    val isDeleting: Boolean = false,
    val error: StringResource? = null,
    val notice: StringResource? = null,
    /** Set when the code goes by SMS: the (masked) number it went to. Null means the account's e-mail. */
    val smsTo: String? = null,
) {
    val canResend get() = resendSeconds == 0 && !isSending && !isDeleting
}
