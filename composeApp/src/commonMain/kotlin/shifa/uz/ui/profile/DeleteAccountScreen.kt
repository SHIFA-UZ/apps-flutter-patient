package shifa.uz.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.confirmDeletion
import shifa.composeapp.generated.resources.deleteAccountOtpSubtitle
import shifa.composeapp.generated.resources.deleteAccountVerifyTitle
import shifa.uz.data.model.DeleteAccountChallenge
import shifa.uz.ui.register.Banners
import shifa.uz.ui.register.CodeEntry
import shifa.uz.ui.register.RegisterPage

/** 14 §3.5 (no Figma): the registration code page. */
@Composable
fun DeleteAccountScreen(
    challenge: DeleteAccountChallenge,
    email: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: DeleteAccountViewModel = viewModel { DeleteAccountViewModel(challenge, email) },
) {
    val state by viewModel.state.collectAsState()
    val deleted by rememberUpdatedState(onDeleted)
    LaunchedEffect(viewModel) { viewModel.deleted.collect { deleted() } }
    DeleteAccountContent(state, onBack, viewModel::onOtpChange, viewModel::resend, viewModel::confirm)
}

@Composable
internal fun DeleteAccountContent(
    state: DeleteAccountUiState,
    onBack: () -> Unit,
    onOtpChange: (String) -> Unit,
    onResend: () -> Unit,
    onConfirm: () -> Unit,
) = RegisterPage(
    stringResource(Res.string.deleteAccountVerifyTitle),
    onBack,
    stringResource(Res.string.confirmDeletion),
    onConfirm,
    state.isDeleting,
    null,
) {
    Banners(state.error, state.notice)
    CodeEntry(
        stringResource(Res.string.deleteAccountOtpSubtitle),
        email = true,
        otp = state.otp,
        onOtpChange = onOtpChange,
        resendSeconds = state.resendSeconds,
        canResend = state.canResend,
        onResend = onResend,
        onDone = onConfirm,
    )
}
