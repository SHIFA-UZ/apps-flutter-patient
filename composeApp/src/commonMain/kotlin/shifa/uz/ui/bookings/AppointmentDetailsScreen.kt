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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.appointmentDetails
import shifa.composeapp.generated.resources.appointmentLessThan48Hours
import shifa.composeapp.generated.resources.areYouSureCancel
import shifa.composeapp.generated.resources.cancel
import shifa.composeapp.generated.resources.cancelAppointment
import shifa.composeapp.generated.resources.checkUp
import shifa.composeapp.generated.resources.contact
import shifa.composeapp.generated.resources.contactDoctorDirectly
import shifa.composeapp.generated.resources.dateAndTime
import shifa.composeapp.generated.resources.location
import shifa.composeapp.generated.resources.months
import shifa.composeapp.generated.resources.no
import shifa.composeapp.generated.resources.reasonForVisit
import shifa.composeapp.generated.resources.videoConsultation
import shifa.composeapp.generated.resources.weekdays
import shifa.composeapp.generated.resources.yes
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.ui.doctors.DoctorHeaderCard
import shifa.uz.ui.register.Banners
import shifa.uz.ui.register.MessagePopUp
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.widgets.ShifaNotification
import shifa.uz.ui.widgets.ShifaNotificationType
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSection
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.formatVisit
import shifa.uz.utils.parseInstant

/**
 * 06 §4.3 (BOOK-02), Figma 1662:17723: one appointment from B2, over the tabs.
 * ponytail: no reschedule, payment, visit summary, signing, review or Join yet; they come with their flows.
 */
@Composable
fun AppointmentDetailsScreen(
    appointment: Appointment,
    onBack: () -> Unit,
    onDoctor: (Doctor) -> Unit,
    onCancelled: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: AppointmentDetailsViewModel = viewModel { AppointmentDetailsViewModel(appointment) },
) {
    val state by viewModel.state.collectAsState()
    val cancelled by rememberUpdatedState(onCancelled)
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) { viewModel.cancelled.collect { cancelled() } }
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { signedOut() } }
    val uri = LocalUriHandler.current
    AppointmentDetailsContent(
        state,
        onBack = onBack,
        onDoctor = onDoctor,
        onContact = { state.contactUri?.let { runCatching { uri.openUri(it) } } },
        onCancel = viewModel::askCancel,
    )
    if (state.confirmCancel) {
        Dialog(viewModel::dismissCancel) {
            MessagePopUp(
                stringResource(Res.string.cancelAppointment),
                stringResource(Res.string.areYouSureCancel),
                viewModel::dismissCancel,
            ) {
                ShifaPrimaryButton(
                    stringResource(Res.string.yes),
                    viewModel::cancel,
                    Modifier.fillMaxWidth(),
                    destructive = true,
                    loading = state.isCancelling,
                )
                ShifaSecondaryButton(
                    stringResource(Res.string.no),
                    viewModel::dismissCancel,
                    Modifier.fillMaxWidth(),
                    enabled = !state.isCancelling,
                )
            }
        }
    }
}

@Composable
internal fun AppointmentDetailsContent(
    state: AppointmentDetailsUiState,
    onBack: () -> Unit = {},
    onDoctor: (Doctor) -> Unit = {},
    onContact: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    val appointment = state.appointment
    val doctor = state.shownDoctor
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(Res.string.appointmentDetails), onBack = onBack)
        }
        ShifaPageElement(ShifaPageElementType.FlexHeader) {
            DoctorHeaderCard(doctor, state.now, onClick = { onDoctor(doctor) }.takeIf { doctor.id.isNotBlank() })
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ShifaColors.Bg.Canvas)
                .verticalScroll(rememberScrollState())
                .padding(ShifaSpacing.Inset.Md),
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
        ) {
            Banners(state.error)
            appointment.start?.let {
                val visit =
                    formatVisit(
                        it,
                        parseInstant(appointment.endAt),
                        stringArrayResource(Res.array.weekdays),
                        stringArrayResource(Res.array.months),
                    )
                InfoSection(Res.string.dateAndTime, visit)
            }
            val place =
                if (appointment.isVideo) {
                    stringResource(Res.string.videoConsultation)
                } else {
                    appointment.location?.ifBlank { null } ?: doctor.fullAddress ?: doctor.clinic?.ifBlank { null }
                }
            place?.let { InfoSection(Res.string.location, it) }
            appointment.reason?.trim()?.ifEmpty { null }?.let {
                val checkUp = it.lowercase() == "check up" || it.lowercase() == "checkup"
                InfoSection(Res.string.reasonForVisit, if (checkUp) stringResource(Res.string.checkUp) else it)
            }
            if (state.showWarning) {
                ShifaNotification(
                    stringResource(Res.string.appointmentLessThan48Hours),
                    stringResource(Res.string.contactDoctorDirectly),
                    ShifaNotificationType.Feedback,
                    Modifier.fillMaxWidth(),
                )
            }
        }
        ShifaPageElement(ShifaPageElementType.ActionTray, Modifier.navigationBarsPadding()) {
            ShifaPrimaryButton(
                stringResource(Res.string.contact),
                onContact,
                Modifier.fillMaxWidth(),
                enabled = state.contactUri != null,
            )
            ShifaSecondaryButton(
                stringResource(Res.string.cancel),
                onCancel,
                Modifier.fillMaxWidth(),
                enabled = state.canCancel,
            )
        }
    }
}

/** Figma section with a title and a line of text. Doctor Profile's tabs use it too. */
@Composable
internal fun InfoSection(
    title: StringResource?,
    text: String,
) = ShifaSection(title?.let { stringResource(it) }) {
    Text(text, Modifier.fillMaxWidth(), style = ShifaTextStyles.chatMessage(), color = ShifaColors.Text.Primary)
}
