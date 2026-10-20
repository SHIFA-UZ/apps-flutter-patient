package shifa.uz.ui.visit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.additionalNotes
import shifa.composeapp.generated.resources.appointmentDetails
import shifa.composeapp.generated.resources.completeSignOff
import shifa.composeapp.generated.resources.confirmAppointmentSummaryReflectsDiscussion
import shifa.composeapp.generated.resources.consultationInPerson
import shifa.composeapp.generated.resources.dateAndTime
import shifa.composeapp.generated.resources.doctorsNotes
import shifa.composeapp.generated.resources.documents
import shifa.composeapp.generated.resources.endAppointment
import shifa.composeapp.generated.resources.ic_chevron_down_16
import shifa.composeapp.generated.resources.ic_video_22
import shifa.composeapp.generated.resources.joinVideoCall
import shifa.composeapp.generated.resources.liveAppointment
import shifa.composeapp.generated.resources.location
import shifa.composeapp.generated.resources.months
import shifa.composeapp.generated.resources.no
import shifa.composeapp.generated.resources.requestFromDoctor
import shifa.composeapp.generated.resources.requestToSign
import shifa.composeapp.generated.resources.signHere
import shifa.composeapp.generated.resources.signNow
import shifa.composeapp.generated.resources.signature
import shifa.composeapp.generated.resources.videoConsultation
import shifa.composeapp.generated.resources.weekdays
import shifa.composeapp.generated.resources.yes
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.ui.doctors.DoctorHeaderCard
import shifa.uz.ui.register.Banners
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.widgets.ShifaActionButton
import shifa.uz.ui.widgets.ShifaDocumentActionCard
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSection
import shifa.uz.ui.widgets.ShifaSectionType
import shifa.uz.ui.widgets.ShifaSignaturePad
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.ui.widgets.ShifaTray
import shifa.uz.ui.widgets.SignatureStroke
import shifa.uz.ui.widgets.signaturePng
import shifa.uz.utils.formatVisit
import shifa.uz.utils.parseInstant

/**
 * Figma 1918:22602: Home's live visit. Request to sign (B6) shows while the doctor waits for a signature; video
 * visits can join at any time (the user's call). End Appointment leaves the page: the backend has no end-visit call.
 */
@Composable
fun LiveAppointmentScreen(
    appointment: Appointment,
    onBack: () -> Unit,
    onDoctor: (Doctor) -> Unit,
    onJoin: (Appointment) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: LiveAppointmentViewModel = viewModel { LiveAppointmentViewModel(appointment) },
) {
    val state by viewModel.state.collectAsState()
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { signedOut() } }
    LiveAppointmentContent(
        state,
        onBack = onBack,
        onDoctor = onDoctor,
        onToggleDetails = viewModel::toggleDetails,
        onSign = viewModel::openSign,
        onJoin = { onJoin(state.appointment) },
    )
    if (state.signing) {
        SignSheet(state, onClose = viewModel::closeSign, onSubmit = viewModel::sign)
    }
}

@Composable
internal fun LiveAppointmentContent(
    state: LiveAppointmentUiState,
    onBack: () -> Unit = {},
    onDoctor: (Doctor) -> Unit = {},
    onToggleDetails: () -> Unit = {},
    onSign: () -> Unit = {},
    onJoin: () -> Unit = {},
) {
    val appointment = state.appointment
    val doctor = state.shownDoctor
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(Res.string.liveAppointment), onBack = onBack)
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
            Banners(state.error, state.notice)
            ShifaSection(
                stringResource(Res.string.appointmentDetails),
                action = {
                    ShifaActionButton(
                        onToggleDetails,
                        // Figma shows it open, pointing down; closed it points right.
                        Modifier.rotate(if (state.detailsOpen) 0f else -90f),
                        icon = Res.drawable.ic_chevron_down_16,
                        contentDescription = stringResource(Res.string.appointmentDetails),
                        secondary = true,
                    )
                },
            ) { if (state.detailsOpen) Details(appointment, doctor) }
            if (appointment.needsSignature) {
                ShifaSection(stringResource(Res.string.documents), type = ShifaSectionType.CardTransparent) {
                    ShifaDocumentActionCard(
                        stringResource(Res.string.requestToSign),
                        stringResource(Res.string.requestFromDoctor, doctor.name),
                        stringResource(Res.string.signNow),
                        onSign,
                        Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        ShifaPageElement(ShifaPageElementType.ActionTray, Modifier.navigationBarsPadding()) {
            if (appointment.isVideo) {
                ShifaPrimaryButton(
                    stringResource(Res.string.joinVideoCall),
                    onJoin,
                    Modifier.fillMaxWidth(),
                    icon = Res.drawable.ic_video_22,
                )
            }
            ShifaSecondaryButton(
                stringResource(Res.string.endAppointment),
                onBack,
                Modifier.fillMaxWidth(),
                destructive = true,
            )
        }
    }
}

// ponytail: no Service or Documents rows: B2 and B4 send neither (2026-10-19). Add them when the backend does.
@Composable
private fun Details(
    appointment: Appointment,
    doctor: Doctor,
) = Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
    (appointment.doctorClinic ?: appointment.locationLabel ?: doctor.clinic)?.ifBlank { null }?.let {
        DetailRow(Res.string.consultationInPerson, it)
    }
    DetailRow(Res.string.videoConsultation, stringResource(if (appointment.isVideo) Res.string.yes else Res.string.no))
    visitText(appointment)?.let { DetailRow(Res.string.dateAndTime, it, ShifaTextStyles.chatMessage()) }
    placeText(appointment, doctor)?.let { DetailRow(Res.string.location, it, ShifaTextStyles.chatMessage()) }
    appointment.reason?.trim()?.ifEmpty { null }?.let { DetailRow(Res.string.additionalNotes, it) }
}

@Composable
private fun DetailRow(
    label: StringResource,
    value: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) = Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs)) {
    Text(stringResource(label), style = ShifaTextStyles.profileName(), color = ShifaColors.Text.Primary)
    Text(value, style = style, color = ShifaColors.Text.Primary)
}

@Composable
private fun visitText(appointment: Appointment) =
    appointment.start?.let {
        formatVisit(
            it,
            parseInstant(appointment.endAt),
            stringArrayResource(Res.array.weekdays),
            stringArrayResource(Res.array.months),
        )
    }

@Composable
private fun placeText(
    appointment: Appointment,
    doctor: Doctor,
) = if (appointment.isVideo) {
    stringResource(Res.string.videoConsultation)
} else {
    listOf(appointment.locationAddress, appointment.locationLabel, appointment.location, doctor.fullAddress)
        .firstNotNullOfOrNull { it?.ifBlank { null } }
}

/** Figma 1919:23186. ponytail: B6 signs the visit summary, which has no notes: the summary stands in for them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignSheet(
    state: LiveAppointmentUiState,
    onClose: () -> Unit,
    onSubmit: (ByteArray?) -> Unit,
) = ModalBottomSheet(
    onDismissRequest = onClose,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    sheetGesturesEnabled = false, // a stroke on the pad would drag the tray down
    shape = RectangleShape,
    containerColor = Color.Transparent,
    dragHandle = null,
    contentWindowInsets = { WindowInsets(0) },
) { SignTray(state, onClose, onSubmit) }

@Composable
internal fun SignTray(
    state: LiveAppointmentUiState,
    onClose: () -> Unit = {},
    onSubmit: (ByteArray?) -> Unit = {},
) {
    var strokes by remember { mutableStateOf(emptyList<SignatureStroke>()) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val appointment = state.appointment
    val notes =
        listOfNotNull(
            state.shownDoctor.name.ifBlank {
                null
            },
            visitText(appointment),
            placeText(appointment, state.shownDoctor),
            appointment.reason,
        )
            .joinToString("\n") + "\n\n" + stringResource(Res.string.confirmAppointmentSummaryReflectsDiscussion)
    ShifaTray(stringResource(Res.string.requestToSign), onClose, Modifier.navigationBarsPadding()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(ShifaColors.Bg.Surface, ShifaRadius.ControlMd)
                    .border(ShifaSize.S2, ShifaColors.Border.Default, ShifaRadius.ControlMd)
                    .padding(horizontal = ShifaSpacing.Inset.Sm, vertical = ShifaSpacing.Inset.Xs),
            ) {
                Text(
                    stringResource(Res.string.doctorsNotes),
                    style = ShifaTextStyles.caption(),
                    color = ShifaColors.Text.Tertiary,
                )
                Text(notes, style = MaterialTheme.typography.bodyLarge, color = ShifaColors.Text.Primary)
            }
            ShifaSection(stringResource(Res.string.signature), type = ShifaSectionType.CardTransparent) {
                ShifaSignaturePad(
                    strokes,
                    { strokes = it },
                    stringResource(Res.string.signHere),
                    Modifier.onSizeChanged { size = it },
                )
            }
            Banners(state.signError)
            ShifaPrimaryButton(
                stringResource(Res.string.completeSignOff),
                { onSubmit(strokes.takeIf { it.isNotEmpty() }?.let { signaturePng(it, size, density) }) },
                Modifier.fillMaxWidth(),
                loading = state.isSubmitting,
            )
        }
    }
}
