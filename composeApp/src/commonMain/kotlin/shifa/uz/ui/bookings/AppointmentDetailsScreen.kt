package shifa.uz.ui.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.additionalInformation
import shifa.composeapp.generated.resources.appointmentDetails
import shifa.composeapp.generated.resources.appointmentLessThan48Hours
import shifa.composeapp.generated.resources.areYouSureCancel
import shifa.composeapp.generated.resources.cancel
import shifa.composeapp.generated.resources.cancelAppointment
import shifa.composeapp.generated.resources.checkUp
import shifa.composeapp.generated.resources.comment
import shifa.composeapp.generated.resources.consultationInPerson
import shifa.composeapp.generated.resources.contactDoctorDirectly
import shifa.composeapp.generated.resources.dateAndTime
import shifa.composeapp.generated.resources.howDidYouFindYourExperience
import shifa.composeapp.generated.resources.ic_chevron_down_16
import shifa.composeapp.generated.resources.ic_chevron_up_16
import shifa.composeapp.generated.resources.ic_edit_20
import shifa.composeapp.generated.resources.leaveAComment
import shifa.composeapp.generated.resources.leaveReview
import shifa.composeapp.generated.resources.location
import shifa.composeapp.generated.resources.months
import shifa.composeapp.generated.resources.no
import shifa.composeapp.generated.resources.rateYourVisit
import shifa.composeapp.generated.resources.rateYourVisitNoClinic
import shifa.composeapp.generated.resources.reschedule
import shifa.composeapp.generated.resources.submit
import shifa.composeapp.generated.resources.videoConsultation
import shifa.composeapp.generated.resources.weekdays
import shifa.composeapp.generated.resources.yes
import shifa.composeapp.generated.resources.yourReview
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.ui.doctors.DoctorHeaderCard
import shifa.uz.ui.register.Banners
import shifa.uz.ui.register.MessagePopUp
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.widgets.LocalCardOnSurface
import shifa.uz.ui.widgets.ShifaActionButton
import shifa.uz.ui.widgets.ShifaInputField
import shifa.uz.ui.widgets.ShifaNotification
import shifa.uz.ui.widgets.ShifaNotificationType
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaRating
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSection
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.ui.widgets.ShifaTray
import shifa.uz.ui.widgets.ShifaUpcomingAppointmentCard
import shifa.uz.utils.formatDate
import shifa.uz.utils.formatTime
import shifa.uz.utils.formatVisit
import shifa.uz.utils.parseInstant

/**
 * 06 §4.3 (BOOK-02/06), Figma 1921:25419 / 1662:17723 (upcoming), 1661:17567 (past), 1887:19946 (review).
 * ponytail: no Service, Attach Document, past Documents / Tasks (B2 / B4 have none of them, backend-improvements
 * §13) or View Summary (no design for the summary page yet).
 */
@Composable
fun AppointmentDetailsScreen(
    appointment: Appointment,
    onBack: () -> Unit,
    onDoctor: (Doctor) -> Unit,
    onReschedule: (Appointment) -> Unit,
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
        onToggleDetails = viewModel::toggleDetails,
        onReschedule = { onReschedule(state.appointment) },
        onCancel = viewModel::askCancel,
        onReview = viewModel::openReview,
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
    if (state.reviewOpen) ReviewSheet(viewModel)
}

@Composable
internal fun AppointmentDetailsContent(
    state: AppointmentDetailsUiState,
    onBack: () -> Unit = {},
    onDoctor: (Doctor) -> Unit = {},
    onContact: () -> Unit = {},
    onToggleDetails: () -> Unit = {},
    onReschedule: () -> Unit = {},
    onCancel: () -> Unit = {},
    onReview: () -> Unit = {},
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
            Banners(state.error, state.notice)
            if (state.isUpcoming) {
                if (state.showWarning) {
                    // Figma has no Contact button: the warning dials or mails the doctor.
                    ShifaNotification(
                        stringResource(Res.string.appointmentLessThan48Hours),
                        stringResource(Res.string.contactDoctorDirectly),
                        ShifaNotificationType.Feedback,
                        Modifier.fillMaxWidth(),
                        onClick = onContact.takeIf { state.contactUri != null },
                    )
                }
                detailRows(appointment, doctor).forEach { InfoSection(it.title, it.text, it.style) }
            } else {
                AppointmentSummaryCard(appointment, doctor, state.detailsOpen, onToggleDetails)
                state.review?.let { review ->
                    ShifaSection(stringResource(Res.string.yourReview)) {
                        ShifaRating(review.rating, Modifier.fillMaxWidth())
                        review.comment?.let {
                            Text(
                                it,
                                Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = ShifaColors.Text.Primary,
                            )
                        }
                    }
                }
            }
        }
        if (state.isUpcoming) {
            ShifaPageElement(ShifaPageElementType.ActionTray, Modifier.navigationBarsPadding()) {
                ShifaPrimaryButton(
                    stringResource(Res.string.reschedule),
                    onReschedule,
                    Modifier.fillMaxWidth(),
                    enabled = state.canCancel,
                )
                ShifaSecondaryButton(
                    stringResource(Res.string.cancel),
                    onCancel,
                    Modifier.fillMaxWidth(),
                    enabled = state.canCancel,
                    destructive = true,
                )
            }
        } else if (state.canReview) {
            ShifaPageElement(ShifaPageElementType.ActionTray, Modifier.navigationBarsPadding()) {
                ShifaSecondaryButton(
                    stringResource(Res.string.leaveReview),
                    onReview,
                    Modifier.fillMaxWidth(),
                    icon = Res.drawable.ic_edit_20,
                )
            }
        }
    }
}

internal class DetailRow(
    val title: StringResource,
    val text: String,
    val style: TextStyle,
)

/** Clinic, Video Consultation, Date and Time, Location (in person) and the reason, each when known. */
@Composable
internal fun detailRows(
    appointment: Appointment,
    doctor: Doctor,
): List<DetailRow> {
    val value = MaterialTheme.typography.bodyLarge
    val small = ShifaTextStyles.chatMessage()
    val visit =
        appointment.start?.let {
            formatVisit(
                it,
                parseInstant(appointment.endAt),
                stringArrayResource(Res.array.weekdays),
                stringArrayResource(Res.array.months),
            )
        }
    val place =
        if (appointment.isVideo) {
            null
        } else {
            listOf(appointment.locationLabel, appointment.location, doctor.fullAddress)
                .firstNotNullOfOrNull { it?.ifBlank { null } }
        }
    val reason =
        appointment.reason?.trim()?.ifEmpty { null }?.let {
            val checkUp = it.lowercase() == "check up" || it.lowercase() == "checkup"
            if (checkUp) stringResource(Res.string.checkUp) else it
        }
    return listOfNotNull(
        (appointment.doctorClinic ?: doctor.clinic)?.ifBlank { null }
            ?.let { DetailRow(Res.string.consultationInPerson, it, value) },
        DetailRow(
            Res.string.videoConsultation,
            stringResource(if (appointment.isVideo) Res.string.yes else Res.string.no),
            value,
        ),
        visit?.let { DetailRow(Res.string.dateAndTime, it, small) },
        place?.let { DetailRow(Res.string.location, it, small) },
        reason?.let { DetailRow(Res.string.additionalInformation, it, value) },
    )
}

/** Figma's "Appointment Details" card on a past visit and on Reschedule: the rows under one title, collapsible. */
@Composable
internal fun AppointmentSummaryCard(
    appointment: Appointment,
    doctor: Doctor,
    open: Boolean,
    onToggle: () -> Unit,
) = ShifaSection(
    stringResource(Res.string.appointmentDetails),
    action = {
        ShifaActionButton(
            onToggle,
            icon = if (open) Res.drawable.ic_chevron_down_16 else Res.drawable.ic_chevron_up_16,
            contentDescription = stringResource(Res.string.appointmentDetails),
            secondary = true,
        )
    },
) {
    if (!open) return@ShifaSection
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
        detailRows(appointment, doctor).forEach {
            Column(verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs)) {
                Text(stringResource(it.title), style = ShifaTextStyles.profileName(), color = ShifaColors.Text.Primary)
                Text(it.text, style = it.style, color = ShifaColors.Text.Primary)
            }
        }
    }
}

/** Figma section with a title and a line of text. Doctor Profile's tabs use it too. */
@Composable
internal fun InfoSection(
    title: StringResource?,
    text: String,
    style: TextStyle = ShifaTextStyles.chatMessage(),
) = ShifaSection(title?.let { stringResource(it) }) {
    Text(text, Modifier.fillMaxWidth(), style = style, color = ShifaColors.Text.Primary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSheet(viewModel: AppointmentDetailsViewModel) {
    val state by viewModel.state.collectAsState()
    ModalBottomSheet(
        onDismissRequest = viewModel::closeReview,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RectangleShape,
        containerColor = Color.Transparent,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        ReviewTray(
            state,
            onClose = viewModel::closeReview,
            onRate = viewModel::onRate,
            onCommentChange = viewModel::onCommentChange,
            onSubmit = viewModel::submitReview,
        )
    }
}

/** Figma 1887:19946 / 1891:20212: the visit, 1 to 5 stars, a comment, Submit once rated (D5). */
@Composable
internal fun ReviewTray(
    state: AppointmentDetailsUiState,
    onClose: () -> Unit = {},
    onRate: (Int) -> Unit = {},
    onCommentChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    val appointment = state.appointment
    val doctor = state.shownDoctor
    val start = appointment.start
    val day = start?.toLocalDateTime(TimeZone.currentSystemDefault())?.date?.let(::formatDate).orEmpty()
    val lastName = doctor.name.trim().substringAfterLast(' ')
    val clinic = (appointment.doctorClinic ?: doctor.clinic)?.ifBlank { null }
    ShifaTray(stringResource(Res.string.leaveReview), onClose, Modifier.navigationBarsPadding().imePadding()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
        ) {
            Text(
                clinic?.let { stringResource(Res.string.rateYourVisit, lastName, it, day) }
                    ?: stringResource(Res.string.rateYourVisitNoClinic, lastName, day),
                Modifier.fillMaxWidth(),
                style = ShifaTextStyles.notificationBody(),
                color = ShifaColors.Text.Primary,
                textAlign = TextAlign.Center,
            )
            // Figma: the appointment card on the tray's canvas fill, no shadow.
            CompositionLocalProvider(LocalCardOnSurface provides true) {
                ShifaUpcomingAppointmentCard(
                    doctor.name,
                    listOfNotNull(doctor.profession, clinic).joinToString(" - "),
                    start?.let { "${formatTime(it)}, $day" }.orEmpty(),
                    avatarUrl = doctor.photoUrl,
                )
            }
            TrayTitle(Res.string.howDidYouFindYourExperience) {
                ShifaRating(state.rating, Modifier.fillMaxWidth(), ShifaDimens.RatingStar, onRate = onRate)
            }
            TrayTitle(Res.string.leaveAComment) {
                ShifaInputField(
                    state.comment,
                    onCommentChange,
                    stringResource(Res.string.comment),
                    Modifier.fillMaxWidth(),
                    clearable = false,
                    multiline = true,
                )
            }
            Banners(state.reviewError)
            ShifaPrimaryButton(
                stringResource(Res.string.submit),
                onSubmit,
                Modifier.fillMaxWidth(),
                enabled = state.rating > 0,
                loading = state.isReviewing,
            )
        }
    }
}

/** A section header and its content, flush: a transparent [ShifaSection] would add its own padding in the tray. */
@Composable
private fun TrayTitle(
    title: StringResource,
    content: @Composable () -> Unit,
) = Column(verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md)) {
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = ShifaColors.Text.Primary)
    content()
}
