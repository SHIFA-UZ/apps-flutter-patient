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
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.emailRequired
import shifa.composeapp.generated.resources.languageChanged
import shifa.composeapp.generated.resources.passwordChangedSuccess
import shifa.composeapp.generated.resources.profilePhotoUpdatedSuccessfully
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.model.ProfileUpdate
import shifa.uz.data.profile.ProfileRepository
import shifa.uz.data.storage.AppLanguage
import shifa.uz.data.storage.LanguageStorage
import shifa.uz.data.storage.languageStorage
import shifa.uz.utils.ImagePickerResult

enum class ProfileEvent { SignedOut, Changed }

class ProfileViewModel(
    private val profile: ProfileRepository = ProfileRepository(),
    private val languages: LanguageStorage = languageStorage,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState(language = languages.language.value))
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    // SignedOut: P1 answered 401, as on Home. Changed: Home's name and avatar are stale.
    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events: Flow<ProfileEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { languages.language.collect { update { copy(language = it) } } }
        load()
    }

    private fun update(transform: ProfileUiState.() -> ProfileUiState) {
        _state.value = _state.value.transform()
    }

    /** P1, also pull to refresh. The last profile stays on screen meanwhile (14 Q13). */
    fun load() {
        if (_state.value.isLoading) return
        update { copy(isLoading = true) }
        viewModelScope.launch {
            profile.getProfile().fold(
                onSuccess = { update { copy(isLoading = false, profile = it, error = null) } },
                onFailure = {
                    update { copy(isLoading = false, error = it.toUserMessage(), notice = null) }
                    if (it is ApiException.Unauthorized) _events.send(ProfileEvent.SignedOut)
                },
            )
        }
    }

    fun showDialog(dialog: ProfileDialog) = update { copy(dialog = dialog) }

    fun dismissDialog() {
        if (!_state.value.isRequestingDeletion) update { copy(dialog = null) }
    }

    /** 14 §3.1.1. The app language is local; the PATCH only tells the backend, errors ignored. */
    fun selectLanguage(language: AppLanguage) {
        languages.set(language)
        update { copy(dialog = null, error = null, notice = Res.string.languageChanged) }
        viewModelScope.launch { profile.updateProfile(ProfileUpdate(language = language.code)) }
    }

    /** 14 §2.3. Null is a cancelled pick. */
    fun uploadPhoto(picked: ImagePickerResult?) {
        if (picked == null || _state.value.isUploading) return
        update { copy(isUploading = true, error = null, notice = null) }
        viewModelScope.launch {
            profile.changePhoto(picked.fileBytes, picked.fileName).fold(
                onSuccess = { onProfileChanged(it, Res.string.profilePhotoUpdatedSuccessfully) },
                onFailure = { update { copy(isUploading = false, error = it.toUserMessage()) } },
            )
        }
    }

    /** From here or from Edit profile; [notice] is what to show on this page. */
    fun onProfileChanged(
        updated: PatientProfile,
        notice: StringResource? = null,
    ) {
        update { copy(profile = updated, isUploading = false, error = null, notice = notice) }
        _events.trySend(ProfileEvent.Changed)
    }

    /** 14 §3.1.3: P5, then the code page. Phone-only accounts can't delete (14 Q16). */
    fun requestDeletion() {
        val s = _state.value
        if (s.isRequestingDeletion) return
        if (s.email.isEmpty()) return update { copy(dialog = null, notice = null, error = Res.string.emailRequired) }
        update { copy(isRequestingDeletion = true) }
        viewModelScope.launch {
            val result = profile.requestDeletion()
            update {
                copy(
                    isRequestingDeletion = false,
                    dialog = null,
                    deletion = result.getOrNull(),
                    error = result.exceptionOrNull()?.toUserMessage(),
                    notice = null,
                )
            }
        }
    }

    fun closeDeletion() = update { copy(deletion = null) }

    fun onPasswordChanged() = update { copy(error = null, notice = Res.string.passwordChangedSuccess) }
}
