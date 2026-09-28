package shifa.uz.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.datetime.Instant
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.noUpcomingAppointments
import shifa.composeapp.generated.resources.upcomingAppointments
import shifa.uz.data.model.Appointment
import shifa.uz.ui.bookings.ListPage

/** Home's upcoming visits in full, behind the section's arrow. No Figma frame: laid out like Past (1655:15892). */
@Composable
fun UpcomingScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onAppointment: (Appointment) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val now by rememberNow()
    UpcomingContent(state, now, onBack, onAppointment)
}

@Composable
internal fun UpcomingContent(
    state: HomeUiState,
    now: Instant,
    onBack: () -> Unit = {},
    onAppointment: (Appointment) -> Unit = {},
) = ListPage(Res.string.upcomingAppointments, state.error, onBack) {
    HomeSection(
        null,
        state.upcoming,
        state.isLoading,
        Res.string.noUpcomingAppointments,
        limit = Int.MAX_VALUE,
    ) { UpcomingCard(it, now, onAppointment, shadow = false) }
}
