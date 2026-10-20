package shifa.uz.ui.register

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
import kotlinx.datetime.LocalDate
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.codeSentAgain
import shifa.composeapp.generated.resources.emailRequired
import shifa.composeapp.generated.resources.invalidVerificationCode
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.auth.AuthRepository
import shifa.uz.data.auth.needsEmailFallback
import shifa.uz.data.model.OtpChannel
import shifa.uz.data.model.ProfileUpdate
import shifa.uz.data.model.RegisterPatientRequest
import shifa.uz.data.profile.ProfileRepository
import shifa.uz.utils.Country
import shifa.uz.utils.ImagePickerResult

internal const val MAX_SMS_SENDS = 3 // 05 §7, per registration
internal const val RESEND_COOLDOWN_S = 300 // 05 §7

class RegisterViewModel(
    private val auth: AuthRepository = AuthRepository(),
    private val profile: ProfileRepository = ProfileRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    private val _registered = Channel<Unit>(Channel.BUFFERED)
    val registered: Flow<Unit> = _registered.receiveAsFlow()

    private var smsSends = 0
    private var cooldown: Job? = null

    private fun update(transform: RegisterUiState.() -> RegisterUiState) {
        _state.value = _state.value.transform()
    }

    fun onFirstNameChange(value: String) = update { copy(firstName = value, error = null) }

    fun onLastNameChange(value: String) = update { copy(lastName = value, error = null) }

    fun onPhoneChange(value: String) = update { copy(phone = value, error = null) }

    fun onEmailChange(value: String) = update { copy(email = value, error = null) }

    fun onPasswordChange(value: String) = update { copy(password = value, error = null) }

    fun onConfirmPasswordChange(value: String) = update { copy(confirmPassword = value, error = null) }

    fun onTogglePasswordVisibility() = update { copy(isPasswordVisible = !isPasswordVisible) }

    fun onBirthDateChange(value: LocalDate) = update { copy(birthDate = value) }

    fun onCountryChange(value: Country) = update { copy(country = value) }

    fun onAddressChange(value: String) = update { copy(address = value) }

    /** Null is a cancelled pick: the previous photo stays. */
    fun onPhotoPicked(value: ImagePickerResult?) {
        if (value != null) update { copy(photo = value) }
    }

    fun onOtpChange(value: String) =
        update {
            copy(
                otp = value.filter(Char::isDigit).take(6),
                error = null,
                notice = null,
            )
        }

    fun onFallbackEmailChange(value: String) =
        update {
            copy(
                fallbackEmailInput = value,
                fallbackEmailInputError = false,
            )
        }

    fun dismissDialog() = update { copy(dialog = null) }

    /** The primary button of each step. */
    fun next() {
        if (_state.value.isLoading) return
        when (_state.value.step) {
            RegisterStep.Account -> checkAccount()
            RegisterStep.Info -> update { copy(step = RegisterStep.Photo) }
            RegisterStep.Photo -> sendOtp()
            RegisterStep.Otp -> verify()
        }
    }

    /** Returns false on the first step, where back leaves the flow. Back from the code clears it (05 §2.4 step 8). */
    fun back(): Boolean {
        val s = _state.value
        if (s.isLoading) return true
        val previous =
            when (s.step) {
                RegisterStep.Account -> return false
                RegisterStep.Info -> RegisterStep.Account
                RegisterStep.Photo -> RegisterStep.Info
                RegisterStep.Otp -> if (s.isDoctorLink) RegisterStep.Account else RegisterStep.Photo
            }
        cooldown?.cancel()
        update { copy(step = previous, isDoctorLink = false, otp = "", resendSeconds = 0, error = null, notice = null) }
        return true
    }

    /** Drops everything, the password included (05 §6.5). The ViewModel outlives the screen. */
    fun reset() {
        cooldown?.cancel()
        smsSends = 0
        _state.value = RegisterUiState()
    }

    // 05 §2.4 steps 1-4.
    private fun checkAccount() {
        update { copy(showErrors = true) }
        val s = _state.value
        if (!s.isAccountValid) return
        launchLoading {
            auth.checkIdentifier(s.phoneOrNull, s.email.trim().ifEmpty { null }).fold(
                onSuccess = {
                    when (it.type) {
                        "patient" -> update { copy(isLoading = false, dialog = RegisterDialog.ExistingPatient) }
                        "doctor" -> {
                            val name = listOfNotNull(it.firstName, it.lastName).joinToString(" ")
                            update { copy(isLoading = false, dialog = RegisterDialog.ExistingDoctor(name)) }
                        }
                        else -> update { copy(isLoading = false, step = RegisterStep.Info) }
                    }
                },
                onFailure = ::fail,
            )
        }
    }

    /** 05 §2.5: the doctor path skips steps 2 and 3 and needs an email; a phone-only match stops here (05 Q7). */
    fun onCreatePatientAccount() {
        val email = _state.value.otpEmail ?: return update { copy(dialog = null, error = Res.string.emailRequired) }
        update { copy(dialog = null, isDoctorLink = true) }
        sendEmailOtp(email)
    }

    fun resend() {
        if (_state.value.canResend) sendOtp()
    }

    // 05 §2.4 steps 5-6, also the resend: the email if there is one, else SMS falling back to email.
    private fun sendOtp() {
        val s = _state.value
        val email = s.otpEmail
        when {
            email != null -> sendEmailOtp(email)
            smsSends >= MAX_SMS_SENDS -> update { copy(dialog = RegisterDialog.EmailFallback) }
            else ->
                launchLoading {
                    // No email means validation passed an Uzbek phone.
                    auth.sendSmsOtp(s.phoneOrNull!!).fold(
                        onSuccess = { channel ->
                            // The server can only email a code it has an address for, and none was sent: ask for one.
                            // The Flutter app waited for that email, which never came.
                            if (channel == OtpChannel.Email) return@fold showEmailFallback()
                            smsSends++
                            otpSent(OtpChannel.Sms)
                        },
                        onFailure = { if (it.needsEmailFallback()) showEmailFallback() else fail(it) },
                    )
                }
        }
    }

    private fun showEmailFallback() = update { copy(isLoading = false, dialog = RegisterDialog.EmailFallback) }

    fun onContinueWithEmail() {
        val email = _state.value.fallbackEmailInput.trim().lowercase()
        if (!isValidEmail(email)) return update { copy(fallbackEmailInputError = true) }
        update { copy(dialog = null, fallbackEmail = email) }
        sendEmailOtp(email)
    }

    private fun sendEmailOtp(email: String) =
        launchLoading {
            auth.sendEmailOtp(email).fold(onSuccess = { otpSent(OtpChannel.Email) }, onFailure = ::fail)
        }

    private fun otpSent(channel: OtpChannel) {
        val resent = _state.value.step == RegisterStep.Otp
        update {
            copy(
                isLoading = false,
                step = RegisterStep.Otp,
                otpChannel = channel,
                notice = Res.string.codeSentAgain.takeIf { resent },
            )
        }
        cooldown?.cancel()
        cooldown =
            viewModelScope.launch {
                for (left in RESEND_COOLDOWN_S downTo 1) {
                    update { copy(resendSeconds = left) }
                    delay(1_000)
                }
                update { copy(resendSeconds = 0) }
            }
    }

    // 05 §2.4 step 8. The backend checks the code; there is no separate verify call.
    private fun verify() {
        val s = _state.value
        val code = s.otp.filter(Char::isDigit)
        if (code.length != 6) return update { copy(error = Res.string.invalidVerificationCode, notice = null) }
        val email = s.otpEmail.takeIf { s.otpChannel == OtpChannel.Email }
        launchLoading {
            val result =
                if (s.isDoctorLink) {
                    auth.createPatientForDoctor(s.phoneOrNull, email!!, code)
                } else {
                    auth.registerPatient(
                        RegisterPatientRequest(
                            firstName = s.firstName.trim(),
                            lastName = s.lastName.trim(),
                            password = s.password,
                            phone = s.phoneOrNull,
                            email = email,
                            emailOtp = code.takeIf { email != null },
                            smsOtp = code.takeIf { email == null },
                        ),
                    )
                }
            result.fold(
                onSuccess = {
                    // ponytail: the PIN setup prompt (05 §2.4 step 9) comes with app lock (LOCK-01).
                    if (!s.isDoctorLink) saveProfile(s)
                    reset()
                    _registered.send(Unit)
                },
                onFailure = ::fail,
            )
        }
    }

    // Steps 2 and 3 go through P2 and P3 once there is a token; register-patient's own birthDate/address keys are
    // unverified (15 A7). Best effort: the account exists, so a failed save doesn't block Home.
    // ponytail: failures are dropped; show them once the profile screen can redo the save.
    private suspend fun saveProfile(s: RegisterUiState) {
        val photoUrl = s.photo?.let { profile.uploadPhoto(it.fileBytes, it.fileName).getOrNull() }
        val update =
            ProfileUpdate(
                birthDate = s.birthDate?.toString(),
                address = s.address.trim().ifEmpty { null },
                locationCountry = s.country?.apiName,
                photoUrl = photoUrl,
            )
        if (update != ProfileUpdate()) profile.updateProfile(update)
    }

    private fun launchLoading(block: suspend () -> Unit) {
        update { copy(isLoading = true, error = null, notice = null) }
        viewModelScope.launch { block() }
    }

    private fun fail(e: Throwable) = update { copy(isLoading = false, error = e.toUserMessage()) }
}
