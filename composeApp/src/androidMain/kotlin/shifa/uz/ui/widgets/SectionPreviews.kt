package shifa.uz.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import shifa.uz.ui.theme.ShifaColors

@Preview
@Composable
internal fun SectionPreview() =
    Gallery(background = ShifaColors.Bg.Canvas) {
        ShifaSectionType.entries.forEach { type ->
            ShifaSection("Upcoming Appointment", type = type, onMore = {}) {
                ShifaUpcomingAppointmentCard("Azamat Rakhimov", "Dentist - Sog‘lom Hayot Klinikasi", "10:00, Today")
            }
        }
        ShifaSection("My Tasks") {
            ShifaSettingsItem("Without the arrow", {})
        }
    }
