package shifa.uz.ui.register

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.LocalDate
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.codeSentAgain
import shifa.composeapp.generated.resources.invalidVerificationCode
import shifa.uz.data.model.OtpChannel
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.widgets.Gallery
import shifa.uz.ui.widgets.PreviewTheme
import shifa.uz.utils.Country
import shifa.uz.utils.countries

private val filled =
    RegisterUiState(
        firstName = "Ali",
        lastName = "Valiyev",
        phone = "+998901234567",
        password = "Secret1!",
        confirmPassword = "Secret1!",
    )

@Composable
private fun Register(state: RegisterUiState) =
    PreviewTheme {
        when (state.step) {
            RegisterStep.Account -> CreateAccountContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {})
            RegisterStep.Info -> AccountInfoContent(state, {}, {}, {}, {}, {})
            RegisterStep.Photo -> ProfileImageContent(state, {}, {}, {})
            RegisterStep.Otp -> VerifyCodeContent(state, {}, {}, {}, {})
        }
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun RegisterAccountPreview() = Register(RegisterUiState())

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun RegisterAccountErrorsPreview() =
    Register(RegisterUiState(firstName = "Ali", phone = "+49 151", password = "secret1", showErrors = true))

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun RegisterInfoPreview() =
    Register(
        filled.copy(
            step = RegisterStep.Info,
            birthDate = LocalDate(1990, 5, 1),
            country = Country("UZ", "Uzbekistan", "Uzbekistan"),
            address = "Tashkent, Amir Temur 1",
        ),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun RegisterPhotoPreview() = Register(filled.copy(step = RegisterStep.Photo))

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun RegisterSmsCodePreview() =
    Register(
        filled.copy(
            step = RegisterStep.Otp,
            otpChannel = OtpChannel.Sms,
            otp = "123",
            resendSeconds = 245,
            notice = Res.string.codeSentAgain,
        ),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun RegisterEmailCodeErrorPreview() =
    Register(
        filled.copy(
            step = RegisterStep.Otp,
            email = "ali@shifa.uz",
            otp = "12",
            error = Res.string.invalidVerificationCode,
        ),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun RegisterCountryTrayPreview() =
    PreviewTheme {
        val countries = countries()
        CountryTray(countries, countries[2], "", {}, {}, {})
    }

@Preview
@Composable
internal fun RegisterDialogsPreview() =
    Gallery(background = ShifaColors.Bg.Canvas) {
        RegisterDialogContent(RegisterDialog.ExistingPatient, "", false, {}, {}, {}, {})
        RegisterDialogContent(RegisterDialog.ExistingDoctor("Aziz Karimov"), "", false, {}, {}, {}, {})
        RegisterDialogContent(RegisterDialog.EmailFallback, "ali@", true, {}, {}, {}, {})
    }
