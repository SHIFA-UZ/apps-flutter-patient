package shifa.uz.ui.doctors

import androidx.compose.runtime.Composable
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.consultationInPerson
import shifa.composeapp.generated.resources.consultationOnline
import shifa.uz.data.model.Doctor
import shifa.uz.ui.home.slot
import shifa.uz.ui.widgets.ShifaDoctorCard
import shifa.uz.ui.widgets.ShifaTag
import shifa.uz.ui.widgets.ShifaTagType
import shifa.uz.utils.parseInstant

/**
 * The full doctor card with its Clinic / Online tags (07 §4.1). No Nearby tag: the app has no Near me (the user's call).
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
    ShifaDoctorCard(
        name = doctor.name,
        specialty = listOfNotNull(doctor.profession, doctor.clinic).filter(String::isNotBlank).joinToString(" - "),
        avatarUrl = doctor.photoUrl,
        verified = doctor.isVerified,
        rating = doctor.ratingText,
        availability = parseInstant(doctor.nextAvailableStartAt)?.let { slot(it, now) },
        tags =
            if (inPerson || online) {
                {
                    if (inPerson) ShifaTag(stringResource(Res.string.consultationInPerson), type = ShifaTagType.Primary)
                    if (online) ShifaTag(stringResource(Res.string.consultationOnline))
                }
            } else {
                null
            },
        shadow = shadow,
        onClick = onClick,
    )
}
