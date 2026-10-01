package shifa.uz.ui.doctors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.consultationInPerson
import shifa.composeapp.generated.resources.consultationOnline
import shifa.composeapp.generated.resources.nearby
import shifa.uz.data.model.Doctor
import shifa.uz.ui.bookings.NEARBY_KM
import shifa.uz.ui.bookings.distanceFrom
import shifa.uz.ui.home.slot
import shifa.uz.ui.widgets.ShifaDoctorCard
import shifa.uz.ui.widgets.ShifaTag
import shifa.uz.ui.widgets.ShifaTagType
import shifa.uz.utils.DeviceLocation
import shifa.uz.utils.parseInstant

/** The Bookings tab's last Near me fix, for the Nearby tag. */
internal val LocalDeviceLocation = compositionLocalOf<DeviceLocation.Found?> { null }

/**
 * The full doctor card with its Clinic / Online tags (07 §4.1), and Nearby within [NEARBY_KM] of the Near me fix.
 * The flex header of Appointment Details and Doctor Profile (no shadow), and the Bookings tab's search results (Figma 1653:12376).
 */
@Composable
internal fun DoctorHeaderCard(
    doctor: Doctor,
    now: Instant,
    onClick: (() -> Unit)? = null,
    shadow: Boolean = false,
) {
    val inPerson = doctor.offersInPerson
    val online = doctor.offersOnline
    val nearby = doctor.distanceFrom(LocalDeviceLocation.current)?.let { it <= NEARBY_KM } == true
    ShifaDoctorCard(
        name = doctor.name,
        specialty = listOfNotNull(doctor.profession, doctor.clinic).filter(String::isNotBlank).joinToString(" - "),
        avatarUrl = doctor.photoUrl,
        verified = doctor.isVerified,
        rating = doctor.ratingText,
        availability = parseInstant(doctor.nextAvailableStartAt)?.let { slot(it, now) },
        tags =
            if (inPerson || online || nearby) {
                {
                    if (inPerson) ShifaTag(stringResource(Res.string.consultationInPerson), type = ShifaTagType.Primary)
                    if (online) ShifaTag(stringResource(Res.string.consultationOnline))
                    if (nearby) ShifaTag(stringResource(Res.string.nearby), type = ShifaTagType.Tertiary)
                }
            } else {
                null
            },
        shadow = shadow,
        onClick = onClick,
    )
}
