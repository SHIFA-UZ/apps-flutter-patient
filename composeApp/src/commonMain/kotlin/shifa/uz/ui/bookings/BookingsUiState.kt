package shifa.uz.ui.bookings

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.all
import shifa.composeapp.generated.resources.filterEmergency
import shifa.composeapp.generated.resources.filterNearMe
import shifa.composeapp.generated.resources.filterRemote
import shifa.composeapp.generated.resources.today
import shifa.uz.data.doctors.DoctorsQuery
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.utils.DeviceLocation

/** The Figma chips, as D1 params. */
enum class DoctorFilter(val label: StringResource, val query: DoctorsQuery) {
    All(Res.string.all, DoctorsQuery()),

    // Plus the fix, within NEARBY_KM (07 §2.2).
    NearMe(Res.string.filterNearMe, DoctorsQuery(sortBy = "distance")),
    Remote(Res.string.filterRemote, DoctorsQuery(consultationType = "online")),
    Today(Res.string.today, DoctorsQuery(availableWithin = "today")),

    // No D1 param for emergencies: the soonest free slot first.
    Emergency(Res.string.filterEmergency, DoctorsQuery(sortBy = "availability")),
}

/** Lists are null until they first load; a failed reload keeps the previous ones. */
@Stable
data class BookingsUiState(
    val query: String = "",
    val filter: DoctorFilter = DoctorFilter.All,
    val recommended: List<Doctor>? = null,
    val myDoctors: List<Doctor>? = null,
    val past: List<Appointment>? = null,
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val notice: StringResource? = null,
    val location: DeviceLocation.Found? = null, // the last Near me fix, kept for the session
    val isLocating: Boolean = false, // Near me: waiting for the fix, then for its search
) {
    /** Figma My Doctors: the latest confirmed or completed visit ([past] is newest first). */
    fun lastVisit(doctor: Doctor) =
        past?.firstOrNull { it.doctorId == doctor.id && (it.isConfirmed || it.status == "COMPLETED") }?.start
}

/** 07 §2.2 Near me radius (01 `nearbyDoctorsMaxRadiusKm`); also where Figma's Nearby tag ends. */
internal const val NEARBY_KM = 50.0

/** D1's distance, else from [from] (07 §2.1). Null when either side has no coordinates. */
internal fun Doctor.distanceFrom(from: DeviceLocation.Found?): Double? {
    distanceKm?.let { return it }
    val latitude = latitude ?: return null
    val longitude = longitude ?: return null
    return from?.distanceKm(latitude, longitude)
}
