package shifa.uz.ui.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.cancel
import shifa.composeapp.generated.resources.confirm
import shifa.composeapp.generated.resources.rescheduleAppointment
import shifa.composeapp.generated.resources.selectNewDate
import shifa.composeapp.generated.resources.videoConsultation
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.AvailableSlot
import shifa.uz.data.model.shownDoctor
import shifa.uz.ui.doctors.DoctorHeaderCard
import shifa.uz.ui.register.Banners
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSection
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.getCurrentInstant

/**
 * 06 §3.5, Figma 1921:25675, over the appointment details: a new B1 slot for the same doctor and kind of visit,
 * booked with B3 naming the old one. Cancel just goes back. A video visit asks for its service again: B2 doesn't
 * say which one was booked, and B3 needs it.
 */
@Composable
fun RescheduleScreen(
    appointment: Appointment,
    onBack: () -> Unit,
    onRescheduled: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: CreateBookingViewModel =
        viewModel { CreateBookingViewModel(appointment.shownDoctor(null), reschedule = appointment) },
) {
    val state by viewModel.state.collectAsState()
    val rescheduled by rememberUpdatedState(onRescheduled)
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) { viewModel.booked.collect { rescheduled() } }
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { signedOut() } }
    var pickingDate by remember { mutableStateOf(false) }
    var detailsOpen by rememberSaveable { mutableStateOf(true) }
    val now = getCurrentInstant()
    RescheduleContent(
        appointment,
        state,
        now,
        detailsOpen,
        onBack = onBack,
        onToggleDetails = { detailsOpen = !detailsOpen },
        onServiceChange = viewModel::onServiceChange,
        onPickDate = { pickingDate = true },
        onSlot = viewModel::onSlotSelect,
        onConfirm = viewModel::book,
    )
    if (pickingDate) BookingDayPicker(state, now, viewModel::onDateChange) { pickingDate = false }
}

@Composable
internal fun RescheduleContent(
    appointment: Appointment,
    state: CreateBookingUiState,
    now: Instant,
    detailsOpen: Boolean,
    onBack: () -> Unit = {},
    onToggleDetails: () -> Unit = {},
    onServiceChange: (String) -> Unit = {},
    onPickDate: () -> Unit = {},
    onSlot: (AvailableSlot) -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(Res.string.rescheduleAppointment), onBack = onBack)
        }
        ShifaPageElement(ShifaPageElementType.FlexHeader) { DoctorHeaderCard(state.doctor, now) }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ShifaColors.Bg.Canvas)
                .verticalScroll(rememberScrollState())
                .padding(ShifaSpacing.Inset.Md),
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
        ) {
            AppointmentSummaryCard(appointment, state.doctor, detailsOpen, onToggleDetails)
            if (state.isVideo) {
                ShifaSection(stringResource(Res.string.videoConsultation)) { ServiceDropdown(state, onServiceChange) }
            }
            DateSection(state, Res.string.selectNewDate, onPickDate)
            TimesSection(state, onSlot)
        }
        ShifaPageElement(ShifaPageElementType.ActionTray, Modifier.navigationBarsPadding()) {
            Banners(state.error)
            ShifaPrimaryButton(
                stringResource(Res.string.confirm),
                onConfirm,
                Modifier.fillMaxWidth(),
                enabled = state.canBook,
                loading = state.isBooking,
            )
            ShifaSecondaryButton(
                stringResource(Res.string.cancel),
                onBack,
                Modifier.fillMaxWidth(),
                enabled = !state.isBooking,
                destructive = true,
            )
        }
    }
}
