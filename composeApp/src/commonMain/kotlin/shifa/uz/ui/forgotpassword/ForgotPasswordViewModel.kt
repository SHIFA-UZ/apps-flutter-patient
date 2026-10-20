package shifa.uz.ui.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.codeSentAgain
import shifa.composeapp.generated.resources.errorInvalidOrExpiredEmailVerificationCode
import shifa.composeapp.generated.resources.invalidVerificationCode
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.auth.AuthRepository

internal const val FORGOT_RESEND_COOLDOWN_S = 60 // 05 §7
internal const val FORGOT_CODE_EXPIRY_S = 600 // 05 §7, display only

private val CODE_ERRORS =
    setOf(Res.string.invalidVerificationCode, Res.string.errorInvalidOrExpiredEmailVerificationCode)

class ForgotPasswordViewModel(
    private val auth: AuthRepository = AuthRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    private val _signedIn = Channel<Unit>(Channel.BUFFERED)
    val signedIn: Flow<Unit> = _signedIn.receiveAsFlow()

    private var timers: Job? = null

    private fun update(transform: ForgotPasswordUiState.() -> ForgotPasswordUiState) {
        _state.value = _state.value.transform()
    }

    fun onIdentifierChange(value: String) = update { copy(identifier = value, error = null) }

    fun onOtpChange(value: String) =
        update {
            copy(
                otp = value.filter(Char::isDigit).take(6),
                error = null,
                notice = null,
            )
        }

    fun onPasswordChange(value: String) = update { copy(password = value, error = null) }

    fun onConfirmPasswordChange(value: String) = update { copy(confirmPassword = value, error = null) }

    fun onTogglePasswordVisibility() = update { copy(isPasswordVisible = !isPasswordVisible) }

    /** The primary button of each step. */
    fun next() {
        if (_state.value.isLoading) return
        when (_state.value.step) {
            ForgotPasswordStep.Identifier -> sendCode()
            ForgotPasswordStep.Code -> checkCode()
            ForgotPasswordStep.NewPassword -> resetPassword()
        }
    }

    fun resend() {
        if (_state.value.canResend) sendCode()
    }

    /** Returns false on the first step, where back leaves the flow. */
    fun back(): Boolean {
        val s = _state.value
        if (s.isLoading) return true
        when (s.step) {
            ForgotPasswordStep.Identifier -> return false
            ForgotPasswordStep.Code -> {
                timers?.cancel()
                update { ForgotPasswordUiState(identifier = identifier) }
            }
            ForgotPasswordStep.NewPassword ->
                update { copy(step = ForgotPasswordStep.Code, showErrors = false, error = null) }
        }
        return true
    }

    /** Drops everything, the password included. The ViewModel outlives the screen. */
    fun reset() {
        timers?.cancel()
        _state.value = ForgotPasswordUiState()
    }

    // 05 §2.6 step 1, also the resend (step 2).
    private fun sendCode() {
        update { copy(showErrors = true) }
        val s = _state.value
        if (s.identifierError != null) return
        update { copy(isLoading = true, error = null, notice = null) }
        viewModelScope.launch {
            auth.sendForgotPasswordOtp(s.normalizedIdentifier).fold(
                onSuccess = { channel ->
                    val resent = s.step == ForgotPasswordStep.Code
                    update {
                        copy(
                            isLoading = false,
                            step = ForgotPasswordStep.Code,
                            channel = channel,
                            showErrors = false,
                            notice = Res.string.codeSentAgain.takeIf { resent },
                        )
                    }
                    startTimers()
                },
                onFailure = ::fail,
            )
        }
    }

    private fun startTimers() {
        timers?.cancel()
        timers =
            viewModelScope.launch {
                for (elapsed in 0 until FORGOT_CODE_EXPIRY_S) {
                    update {
                        copy(
                            expiresInSeconds = FORGOT_CODE_EXPIRY_S - elapsed,
                            resendSeconds = (FORGOT_RESEND_COOLDOWN_S - elapsed).coerceAtLeast(0),
                        )
                    }
                    delay(1_000)
                }
                update { copy(expiresInSeconds = 0) }
            }
    }

    // 05 §2.6 step 2: there is no verify call, A10 checks the code.
    private fun checkCode() {
        if (_state.value.otp.length != 6) {
            return update {
                copy(
                    error = Res.string.invalidVerificationCode,
                    notice = null,
                )
            }
        }
        update { copy(step = ForgotPasswordStep.NewPassword, error = null, notice = null) }
    }

    // 05 §2.6 step 3. The password is sent as typed, like register and login (Flutter trimmed it here, 05 Q10).
    private fun resetPassword() {
        update { copy(showErrors = true) }
        val s = _state.value
        if (s.passwordError != null || s.confirmPasswordError != null) return
        update { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            auth.resetForgottenPassword(s.normalizedIdentifier, s.channel, s.otp, s.password).fold(
                onSuccess = {
                    // ponytail: the passwordResetSuccess SnackBar (05 §2.6) needs a snackbar host on Home.
                    reset()
                    _signedIn.send(Unit)
                },
                onFailure = { e ->
                    val message = e.toUserMessage()
                    // A wrong code only shows up here (05 Q12): go back to the code screen, where resend is.
                    if (message in CODE_ERRORS) {
                        update { copy(isLoading = false, error = message, step = ForgotPasswordStep.Code, otp = "") }
                    } else {
                        update { copy(isLoading = false, error = message) }
                    }
                },
            )
        }
    }

    private fun fail(e: Throwable) = update { copy(isLoading = false, error = e.toUserMessage()) }
}
