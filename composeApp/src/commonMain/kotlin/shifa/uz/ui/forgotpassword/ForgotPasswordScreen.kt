package shifa.uz.ui.forgotpassword

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.confirmPassword
import shifa.composeapp.generated.resources.emailOrPhone
import shifa.composeapp.generated.resources.enterNewPassword
import shifa.composeapp.generated.resources.forgotPassword
import shifa.composeapp.generated.resources.forgotPasswordEnterEmailOrPhone
import shifa.composeapp.generated.resources.newPassword
import shifa.composeapp.generated.resources.otpSentToAccountEmail
import shifa.composeapp.generated.resources.otpSentToEmail
import shifa.composeapp.generated.resources.otpSentToPhone
import shifa.composeapp.generated.resources.resetPassword
import shifa.composeapp.generated.resources.sendCode
import shifa.composeapp.generated.resources.verify
import shifa.uz.data.model.OtpChannel
import shifa.uz.ui.register.Banners
import shifa.uz.ui.register.CodeEntry
import shifa.uz.ui.register.Field
import shifa.uz.ui.register.PasswordChecklist
import shifa.uz.ui.register.PasswordEye
import shifa.uz.ui.register.RegisterPage
import shifa.uz.ui.register.SubLogo
import shifa.uz.ui.theme.ShifaColors

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ForgotPasswordScreen(
    onSignedIn: () -> Unit,
    onClose: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel { ForgotPasswordViewModel() },
) {
    val state by viewModel.state.collectAsState()
    val signedIn by rememberUpdatedState(onSignedIn)
    LaunchedEffect(viewModel) { viewModel.signedIn.collect { signedIn() } }
    val onBack = {
        if (!viewModel.back()) {
            viewModel.reset()
            onClose()
        }
    }
    BackHandler(onBack = onBack)
    ForgotPasswordContent(
        state = state,
        onIdentifierChange = viewModel::onIdentifierChange,
        onOtpChange = viewModel::onOtpChange,
        onResend = viewModel::resend,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onBack = onBack,
        onNext = viewModel::next,
    )
}

/** 05 §3.7-3.9, no Figma frames: the register page layout, one step at a time. */
@Composable
internal fun ForgotPasswordContent(
    state: ForgotPasswordUiState,
    onIdentifierChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onResend: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    fun shown(error: StringResource?) = error.takeIf { state.showErrors }
    val (title, action) =
        when (state.step) {
            ForgotPasswordStep.Identifier -> Res.string.forgotPassword to Res.string.sendCode
            ForgotPasswordStep.Code -> Res.string.forgotPassword to Res.string.verify
            ForgotPasswordStep.NewPassword -> Res.string.resetPassword to Res.string.resetPassword
        }
    RegisterPage(stringResource(title), onBack, stringResource(action), onNext, state.isLoading, page = null) {
        SubLogo()
        Banners(state.error, state.notice)
        when (state.step) {
            ForgotPasswordStep.Identifier -> {
                Intro(stringResource(Res.string.forgotPasswordEnterEmailOrPhone))
                Field(
                    state.identifier,
                    onIdentifierChange,
                    Res.string.emailOrPhone,
                    shown(state.identifierError),
                    KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    onDone = onNext,
                )
            }
            ForgotPasswordStep.Code ->
                CodeEntry(
                    message =
                        when {
                            state.channel == OtpChannel.Sms ->
                                stringResource(Res.string.otpSentToPhone, state.normalizedIdentifier)
                            state.isEmail -> stringResource(Res.string.otpSentToEmail, state.normalizedIdentifier)
                            // A phone whose code the server emailed: the address isn't known here.
                            else -> stringResource(Res.string.otpSentToAccountEmail)
                        },
                    email = state.channel == OtpChannel.Email,
                    otp = state.otp,
                    onOtpChange = onOtpChange,
                    resendSeconds = state.resendSeconds,
                    canResend = state.canResend,
                    onResend = onResend,
                    onDone = onNext,
                    expiresInSeconds = state.expiresInSeconds,
                )
            ForgotPasswordStep.NewPassword -> {
                Intro(stringResource(Res.string.enterNewPassword))
                val transformation =
                    if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
                val eye: @Composable () -> Unit = { PasswordEye(state.isPasswordVisible, onTogglePasswordVisibility) }
                Field(
                    state.password,
                    onPasswordChange,
                    Res.string.newPassword,
                    shown(state.passwordError),
                    KeyboardType.Password,
                    transformation,
                    eye,
                )
                if (state.password.isNotEmpty()) PasswordChecklist(state.password)
                Field(
                    state.confirmPassword,
                    onConfirmPasswordChange,
                    Res.string.confirmPassword,
                    shown(state.confirmPasswordError),
                    KeyboardType.Password,
                    transformation,
                    eye,
                    ImeAction.Done,
                    onNext,
                )
            }
        }
    }
}

@Composable
private fun Intro(text: String) =
    Text(
        text,
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = ShifaColors.Text.Primary,
        textAlign = TextAlign.Center,
    )
