package shifa.uz.ui.forgotpassword

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.codeSentAgain
import shifa.composeapp.generated.resources.invalidVerificationCode
import shifa.uz.data.model.OtpChannel
import shifa.uz.ui.widgets.PreviewTheme

@Composable
private fun ForgotPassword(state: ForgotPasswordUiState) =
    PreviewTheme { ForgotPasswordContent(state, {}, {}, {}, {}, {}, {}, {}, {}) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ForgotPasswordIdentifierPreview() = ForgotPassword(ForgotPasswordUiState())

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ForgotPasswordIdentifierErrorPreview() =
    ForgotPassword(ForgotPasswordUiState(identifier = "ali@shifa", showErrors = true))

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ForgotPasswordSmsCodePreview() =
    ForgotPassword(
        ForgotPasswordUiState(
            step = ForgotPasswordStep.Code,
            identifier = "901234567",
            channel = OtpChannel.Sms,
            otp = "123",
            expiresInSeconds = 542,
            resendSeconds = 2,
        ),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ForgotPasswordEmailCodePreview() =
    ForgotPassword(
        ForgotPasswordUiState(
            step = ForgotPasswordStep.Code,
            identifier = "901234567",
            expiresInSeconds = 45,
            error = Res.string.invalidVerificationCode,
            notice = Res.string.codeSentAgain,
        ),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ForgotPasswordNewPasswordPreview() =
    ForgotPassword(
        ForgotPasswordUiState(
            step = ForgotPasswordStep.NewPassword,
            identifier = "ali@shifa.uz",
            password = "secret1",
            confirmPassword = "secret",
            showErrors = true,
        ),
    )
