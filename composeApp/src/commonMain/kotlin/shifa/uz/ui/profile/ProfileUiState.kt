package shifa.uz.ui.profile

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.DeleteAccountChallenge
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.storage.AppLanguage

enum class ProfileDialog { Language, LogOut, DeleteAccount }

@Stable
data class ProfileUiState(
    val profile: PatientProfile? = null,
    val language: AppLanguage = AppLanguage.English,
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val isRequestingDeletion: Boolean = false,
    val dialog: ProfileDialog? = null,
    /** Set once P5 answers: opens the code page (14 §3.5). */
    val deletion: DeleteAccountChallenge? = null,
    val error: StringResource? = null,
    val notice: StringResource? = null,
) {
    val email get() = profile?.email?.trim().orEmpty()
}
