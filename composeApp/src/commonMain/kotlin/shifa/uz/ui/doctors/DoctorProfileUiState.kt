package shifa.uz.ui.doctors

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.aboutDoctor
import shifa.composeapp.generated.resources.certificates
import shifa.composeapp.generated.resources.consultationInPerson
import shifa.composeapp.generated.resources.services
import shifa.uz.data.model.Doctor

/** Figma's chips; Clinic is 07's Contacts tab. */
enum class DoctorTab(val label: StringResource) {
    About(Res.string.aboutDoctor),
    Services(Res.string.services),
    Certificates(Res.string.certificates),
    Clinic(Res.string.consultationInPerson),
}

/** [doctor] starts as the tapped card's copy; [isLoaded] once D2 replaced it. */
@Stable
data class DoctorProfileUiState(
    val doctor: Doctor,
    val isLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val tab: DoctorTab = DoctorTab.About,
)
