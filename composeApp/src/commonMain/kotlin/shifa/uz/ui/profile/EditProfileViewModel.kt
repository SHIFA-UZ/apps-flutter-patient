package shifa.uz.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.profilePhotoUpdatedSuccessfully
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.model.ProfileUpdate
import shifa.uz.data.profile.ProfileRepository
import shifa.uz.data.storage.AppLanguage
import shifa.uz.data.storage.LanguageStorage
import shifa.uz.data.storage.languageStorage
import shifa.uz.utils.ImagePickerResult

sealed interface EditProfileEvent {
    val profile: PatientProfile

    /** The photo is saved on its own, before Save. */
    data class PhotoChanged(override val profile: PatientProfile) : EditProfileEvent

    data class Saved(override val profile: PatientProfile) : EditProfileEvent
}

/** 14 §3.2, from the profile already loaded (no fetch). */
class EditProfileViewModel(
    initial: PatientProfile?,
    private val profile: ProfileRepository = ProfileRepository(),
    private val languages: LanguageStorage = languageStorage,
) : ViewModel() {
    private val _state = MutableStateFlow((initial ?: PatientProfile()).toEditState(languages.language.value))
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    private val _events = Channel<EditProfileEvent>(Channel.BUFFERED)
    val events: Flow<EditProfileEvent> = _events.receiveAsFlow()

    private fun update(transform: EditProfileUiState.() -> EditProfileUiState) {
        _state.value = _state.value.transform()
    }

    fun onFirstNameChange(value: String) = update { copy(firstName = value) }

    fun onLastNameChange(value: String) = update { copy(lastName = value) }

    fun onPhoneChange(value: String) = update { copy(phone = value) }

    fun onBirthDateChange(value: LocalDate) = update { copy(birthDate = value) }

    fun onGenderChange(value: Gender) = update { copy(gender = value) }

    fun onAddressChange(value: String) = update { copy(address = value) }

    fun onLanguageChange(value: AppLanguage) = update { copy(language = value) }

    fun onStreetAddressChange(value: String) = update { copy(streetAddress = value) }

    /** The real upload, never a local path (14 Q1). Null is a cancelled pick. */
    fun uploadPhoto(picked: ImagePickerResult?) {
        if (picked == null || _state.value.isUploading) return
        update { copy(isUploading = true, error = null, notice = null) }
        viewModelScope.launch {
            profile.changePhoto(picked.fileBytes, picked.fileName).fold(
                onSuccess = {
                    update {
                        copy(
                            isUploading = false,
                            photoUrl = it.photoUrl,
                            notice = Res.string.profilePhotoUpdatedSuccessfully,
                        )
                    }
                    _events.send(EditProfileEvent.PhotoChanged(it))
                },
                onFailure = { update { copy(isUploading = false, error = it.toUserMessage()) } },
            )
        }
    }

    /** P2 with the non-empty fields; no photoUrl (14 Q1, Q3). The language applies once saved. */
    fun save() {
        val s = _state.value
        if (s.isSaving) return
        if (!s.isValid) return update { copy(showErrors = true) }
        update { copy(isSaving = true, error = null, notice = null) }
        val request =
            ProfileUpdate(
                firstName = s.firstName.trim(),
                lastName = s.lastName.trim(),
                birthDate = s.birthDate?.toString(),
                gender = s.gender?.value,
                phone = s.phone.trim().ifEmpty { null },
                address = s.address.trim().ifEmpty { null },
                language = s.language.code,
                locationStreetAddress = s.streetAddress.trim().ifEmpty { null },
            )
        viewModelScope.launch {
            profile.updateProfile(request).fold(
                onSuccess = {
                    languages.set(s.language)
                    _events.send(EditProfileEvent.Saved(it))
                },
                onFailure = { update { copy(isSaving = false, error = it.toUserMessage()) } },
            )
        }
    }
}
