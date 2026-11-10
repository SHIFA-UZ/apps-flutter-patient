package shifa.uz.ui.profile

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
import shifa.composeapp.generated.resources.invalidVerificationCode
import shifa.uz.data.analytics.Analytics
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.AnalyticsTracker
import shifa.uz.data.analytics.analyticsReason
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.auth.AuthRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.DeleteAccountChallenge
import shifa.uz.data.profile.ProfileRepository

private const val ACCOUNT_DELETION = "ACCOUNT_DELETION"

/** 14 §3.5: A4 on open and on resend, then P6 and an awaited logout. */
class DeleteAccountViewModel(
    private val challenge: DeleteAccountChallenge,
    private val email: String,
    private val profile: ProfileRepository = ProfileRepository(),
    private val auth: AuthRepository = AuthRepository(),
    private val analytics: AnalyticsTracker = Analytics,
) : ViewModel() {
    private val _state = MutableStateFlow(DeleteAccountUiState())
    val state: StateFlow<DeleteAccountUiState> = _state.asStateFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    private var cooldown: Job? = null

    init {
        sendCode(resent = false)
    }

    private fun update(transform: DeleteAccountUiState.() -> DeleteAccountUiState) {
        _state.value = _state.value.transform()
    }

    fun onOtpChange(value: String) = update { copy(otp = value.filter(Char::isDigit).take(6), error = null) }

    fun resend() {
        if (!_state.value.canResend) return
        analytics.track(AnalyticsEvent.AccountDeletionCodeResent)
        sendCode(resent = true)
    }

    // The cooldown is the challenge's lifetime, not Flutter's fixed 5 min (14 Q17).
    private fun sendCode(resent: Boolean) {
        update { copy(isSending = true, error = null, notice = null) }
        cooldown?.cancel()
        cooldown =
            viewModelScope.launch {
                for (left in challenge.expiresInSeconds downTo 1) {
                    update { copy(resendSeconds = left) }
                    delay(1_000)
                }
                update { copy(resendSeconds = 0) }
            }
        viewModelScope.launch {
            auth.sendEmailOtp(email, ACCOUNT_DELETION).fold(
                onSuccess = { update { copy(isSending = false, notice = Res.string.codeSentAgain.takeIf { resent }) } },
                onFailure = {
                    // No code went out: Resend works at once instead of after the whole countdown.
                    cooldown?.cancel()
                    update { copy(isSending = false, resendSeconds = 0, error = it.toUserMessage()) }
                },
            )
        }
    }

    fun confirm() {
        val s = _state.value
        if (s.isDeleting) return
        if (s.otp.length != 6) {
            analytics.track(AnalyticsEvent.AccountDeletionFailed("confirm", "invalid_code"))
            return update { copy(error = Res.string.invalidVerificationCode, notice = null) }
        }
        update { copy(isDeleting = true, error = null, notice = null) }
        viewModelScope.launch {
            profile.confirmDeletion(challenge.challengeId, email, s.otp).fold(
                onSuccess = {
                    analytics.track(AnalyticsEvent.AccountDeleted)
                    auth.logout("account_deleted")
                    _deleted.send(Unit)
                },
                onFailure = {
                    analytics.track(
                        AnalyticsEvent.AccountDeletionFailed(
                            "confirm",
                            if (it is ApiException.ValidationError || it is ApiException.Unauthorized) {
                                "invalid_code"
                            } else {
                                it.analyticsReason()
                            },
                        ),
                    )
                    update { copy(isDeleting = false, error = it.toUserMessage()) }
                },
            )
        }
    }
}
