package shifa.uz.ui.profile

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.female
import shifa.composeapp.generated.resources.invalidPhone
import shifa.composeapp.generated.resources.male
import shifa.composeapp.generated.resources.other
import shifa.composeapp.generated.resources.required
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.storage.AppLanguage

/** 14 §3.2. An unknown stored value is no selection, and isn't sent back (14 Q15). */
enum class Gender(val value: String, val label: StringResource) {
    Male("Male", Res.string.male),
    Female("Female", Res.string.female),
    Other("Other", Res.string.other),
    ;

    companion object {
        fun fromValue(value: String?) = entries.firstOrNull { it.value.equals(value?.trim(), ignoreCase = true) }
    }
}

@Stable
data class EditProfileUiState(
    val fullName: String = "",
    val photoUrl: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val birthDate: LocalDate? = null,
    val gender: Gender? = null,
    val address: String = "",
    val language: AppLanguage = AppLanguage.English,
    val streetAddress: String = "",
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    val isUploading: Boolean = false,
    val error: StringResource? = null,
    val notice: StringResource? = null,
) {
    // The backend only renames when both names are sent (14 Q11).
    val firstNameError get() = Res.string.required.takeIf { firstName.isBlank() }
    val lastNameError get() = Res.string.required.takeIf { lastName.isBlank() }

    // 14 §7: optional, else at least 10 digits with the dial code.
    val phoneError get() = Res.string.invalidPhone.takeIf { phone.isNotBlank() && phone.count(Char::isDigit) < 10 }
    val isValid get() = firstNameError == null && lastNameError == null && phoneError == null
}

/** 14 §3.2 field initialisation; the name splits on the first space. */
internal fun PatientProfile.toEditState(language: AppLanguage): EditProfileUiState {
    val name = fullName.trim()
    return EditProfileUiState(
        fullName = name,
        photoUrl = photoUrl,
        firstName = name.substringBefore(' '),
        lastName = name.substringAfter(' ', "").trim(),
        phone = phone.orEmpty(),
        birthDate = birthDate?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() },
        gender = Gender.fromValue(gender),
        address = address.orEmpty(),
        language = language,
        streetAddress = locationStreetAddress.orEmpty(),
    )
}
