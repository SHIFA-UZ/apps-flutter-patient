package shifa.uz.ui.home

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.RemoteCareTask

/** Cards per section (10 §3.4). */
internal const val HOME_ITEMS = 3

/** Lists are null until they first load; a failed reload keeps the previous ones (12 §3). */
@Stable
data class HomeUiState(
    val firstName: String? = null,
    val fullName: String? = null, // tells Documents which uploads are the patient's
    val photoUrl: String? = null,
    // 09 §7: the profile's features or tier can shut the tab out.
    val shifaAi: Boolean = true,
    val unreadNotifications: Int = 0,
    val upcoming: List<Appointment>? = null,
    val tasks: List<RemoteCareTask>? = null,
    val doctors: List<Doctor>? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: StringResource? = null,
)
