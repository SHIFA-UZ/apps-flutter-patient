package shifa.uz.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import shifa.uz.ui.widgets.PreviewTheme

@Composable
private fun Login(state: LoginUiState) =
    PreviewTheme {
        LoginScreenContent(state, {}, {}, {}, {}, {}, {})
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun LoginDefaultPreview() = Login(LoginUiState())

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun LoginFilledPreview() =
    Login(
        LoginUiState(identifier = "+998901234567", password = "secret", isPasswordVisible = true),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun LoginErrorsPreview() = Login(LoginUiState(identifierError = true, passwordError = true))

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun LoginAttemptsPreview() =
    Login(LoginUiState(identifier = "patient@shifa.uz", password = "wrong", loginAttemptsRemaining = 2))

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun LoginLockedPreview() =
    Login(LoginUiState(identifier = "patient@shifa.uz", password = "wrong", loginLockedUntilMinutes = 5))
