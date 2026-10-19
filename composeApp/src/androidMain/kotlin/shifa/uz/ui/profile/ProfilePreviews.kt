package shifa.uz.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.LocalDate
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.codeSentAgain
import shifa.composeapp.generated.resources.errorNetworkConnection
import shifa.composeapp.generated.resources.languageChanged
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.storage.AppLanguage
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.widgets.Gallery
import shifa.uz.ui.widgets.PreviewTheme

private val patient =
    PatientProfile(
        fullName = "Sheroziy Saidkhodjaev",
        phone = "+998 90 123 45 67",
        email = "sheroziy@example.com",
    )

private val form =
    EditProfileUiState(
        fullName = "Sheroziy Saidkhodjaev",
        firstName = "Sheroziy",
        lastName = "Saidkhodjaev",
        phone = "+998901234567",
        birthDate = LocalDate(1995, 3, 21),
        gender = Gender.Male,
        address = "Tashkent",
        language = AppLanguage.Uzbek,
        streetAddress = "Amir Temur 108",
    )

@Composable
private fun Profile(state: ProfileUiState) =
    PreviewTheme { ProfileContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {}) }

@Composable
private fun EditProfile(state: EditProfileUiState) =
    PreviewTheme { EditProfileContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ProfileLoadedPreview() = Profile(ProfileUiState(profile = patient))

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ProfileNoticePreview() =
    Profile(ProfileUiState(profile = patient, language = AppLanguage.Russian, notice = Res.string.languageChanged))

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ProfileErrorPreview() = Profile(ProfileUiState(error = Res.string.errorNetworkConnection))

@Preview
@Composable
internal fun ProfileDialogsPreview() =
    Gallery(background = ShifaColors.Bg.Canvas) {
        ProfileDialog.entries.forEach { ProfileDialogContent(it, AppLanguage.English, false, {}, {}, {}, {}) }
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun EditProfilePreview() = EditProfile(form)

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun EditProfileErrorsPreview() =
    EditProfile(form.copy(firstName = "", phone = "+998", gender = null, birthDate = null, showErrors = true))

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ChangePasswordPreview() =
    PreviewTheme {
        ChangePasswordContent(
            ChangePasswordUiState(newPassword = "secret", confirmPassword = "secret1", showErrors = true),
            {},
            {},
            {},
            {},
            {},
            {},
        )
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DeleteAccountPreview() =
    PreviewTheme {
        DeleteAccountContent(
            DeleteAccountUiState(
                otp = "123",
                resendSeconds = 245,
                notice = Res.string.codeSentAgain,
            ),
            {
            },
            {},
            {},
            {},
        )
    }
