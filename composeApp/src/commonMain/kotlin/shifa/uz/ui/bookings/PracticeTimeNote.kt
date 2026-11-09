package shifa.uz.ui.bookings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.timesInDoctorZone
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.utils.utcLabel

/**
 * Booking times are the doctor's practice time (practiceZone), as the doctor app and the SMS show them. When the phone's
 * zone has another offset [at] the time shown (a patient abroad; offsets compared then, not today, so a summer visit
 * booked in winter names its summer offset), this says so: "Times are in the doctor's time zone (UTC+5)". Nothing
 * otherwise.
 */
@Composable
internal fun PracticeTimeNote(
    zone: TimeZone,
    at: Instant,
    modifier: Modifier = Modifier,
    phone: TimeZone = TimeZone.currentSystemDefault(),
) {
    val offset = zone.offsetAt(at)
    if (offset == phone.offsetAt(at)) return
    Text(
        stringResource(Res.string.timesInDoctorZone, utcLabel(offset)),
        modifier.fillMaxWidth(),
        style = ShifaTextStyles.caption(),
        color = ShifaColors.Text.Tertiary,
    )
}
