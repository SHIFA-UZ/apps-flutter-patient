package shifa.uz.ui.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.addDocument
import shifa.composeapp.generated.resources.attachDocumentsHint
import shifa.composeapp.generated.resources.attachDocumentsOptional
import shifa.composeapp.generated.resources.availableTimes
import shifa.composeapp.generated.resources.bookingSelectServiceForVideo
import shifa.composeapp.generated.resources.bookingVideoNoServicesOffered
import shifa.composeapp.generated.resources.completeBooking
import shifa.composeapp.generated.resources.consultationServiceFreeBadge
import shifa.composeapp.generated.resources.createBooking
import shifa.composeapp.generated.resources.documentsAttached
import shifa.composeapp.generated.resources.ic_check_12
import shifa.composeapp.generated.resources.ic_check_16
import shifa.composeapp.generated.resources.ic_info_16
import shifa.composeapp.generated.resources.ic_paperclip_16
import shifa.composeapp.generated.resources.months
import shifa.composeapp.generated.resources.monthsShort
import shifa.composeapp.generated.resources.noAvailableTimeSlots
import shifa.composeapp.generated.resources.reason
import shifa.composeapp.generated.resources.reasonForVisitOptional
import shifa.composeapp.generated.resources.select
import shifa.composeapp.generated.resources.selectDate
import shifa.composeapp.generated.resources.uploadingDocument
import shifa.composeapp.generated.resources.videoConsultation
import shifa.composeapp.generated.resources.videoConsultationServiceLabel
import shifa.composeapp.generated.resources.weekdays
import shifa.uz.data.model.AvailableSlot
import shifa.uz.data.model.Doctor
import shifa.uz.ui.doctors.DoctorHeaderCard
import shifa.uz.ui.register.Banners
import shifa.uz.ui.register.DayPicker
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.widgets.ShifaActionButton
import shifa.uz.ui.widgets.ShifaBanner
import shifa.uz.ui.widgets.ShifaBannerType
import shifa.uz.ui.widgets.ShifaChip
import shifa.uz.ui.widgets.ShifaChipGrid
import shifa.uz.ui.widgets.ShifaDropdown
import shifa.uz.ui.widgets.ShifaInputField
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSection
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.ui.widgets.ShifaSwitch
import shifa.uz.utils.formatShortDate
import shifa.uz.utils.formatTime
import shifa.uz.utils.formatVisit
import shifa.uz.utils.getCurrentInstant
import shifa.uz.utils.parseInstant
import shifa.uz.utils.rememberDocumentPickerLauncher

/**
 * 06 §3.1 (BOOK-01/03), Figma 1662:18783 and 1662:19457, over the doctor profile. Figma titles it "Doctor Profile"
 * (a copy-paste); this says Create Booking.
 * ponytail: no location step, reschedule, attachment removal or ⋮ menu; the $1 checkout comes with BOOK-04.
 */
@Composable
fun CreateBookingScreen(
    doctor: Doctor,
    onBack: () -> Unit,
    onBooked: (pending: Boolean) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: CreateBookingViewModel = viewModel { CreateBookingViewModel(doctor) },
) {
    val state by viewModel.state.collectAsState()
    val booked by rememberUpdatedState(onBooked)
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) { viewModel.booked.collect { booked(it) } }
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { signedOut() } }
    val pickFile = rememberDocumentPickerLauncher { it?.let { viewModel.attach(it.fileName, it.fileBytes) } }
    var pickingDate by remember { mutableStateOf(false) }
    val now = getCurrentInstant()
    CreateBookingContent(
        state,
        now,
        onBack = onBack,
        onVideoChange = viewModel::onVideoChange,
        onServiceChange = viewModel::onServiceChange,
        onPickDate = { pickingDate = true },
        onSlot = viewModel::onSlotSelect,
        onReasonChange = viewModel::onReasonChange,
        onAttach = pickFile,
        onBook = viewModel::book,
    )
    if (pickingDate) {
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        // 06 §3.1 step 4: today to 90 days on.
        DayPicker(
            state.date,
            today..today.plus(90, DateTimeUnit.DAY),
            state.date ?: today,
            viewModel::onDateChange,
        ) { pickingDate = false }
    }
}

@Composable
internal fun CreateBookingContent(
    state: CreateBookingUiState,
    now: Instant,
    onBack: () -> Unit = {},
    onVideoChange: (Boolean) -> Unit = {},
    onServiceChange: (String) -> Unit = {},
    onPickDate: () -> Unit = {},
    onSlot: (AvailableSlot) -> Unit = {},
    onReasonChange: (String) -> Unit = {},
    onAttach: () -> Unit = {},
    onBook: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(Res.string.createBooking), onBack = onBack)
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
            VideoSection(state, onVideoChange, onServiceChange)
            ShifaSection(
                stringResource(Res.string.selectDate),
                action = {
                    val date = state.date
                    ShifaActionButton(
                        onPickDate,
                        text =
                            date?.let { formatShortDate(it, stringArrayResource(Res.array.monthsShort)) }
                                ?: stringResource(Res.string.select),
                        icon = Res.drawable.ic_check_16.takeIf { date != null },
                        secondary = date == null,
                        iconAfter = true,
                    )
                },
            ) {}
            ShifaSection(stringResource(Res.string.availableTimes)) {
                val slots = state.slots
                when {
                    slots == null -> CircularProgressIndicator(color = ShifaColors.Icon.Brand)
                    slots.isEmpty() ->
                        Text(
                            stringResource(Res.string.noAvailableTimeSlots),
                            Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ShifaColors.Text.Tertiary,
                            textAlign = TextAlign.Center,
                        )
                    else ->
                        ShifaChipGrid(slots, Modifier.fillMaxWidth()) {
                            ShifaChip(
                                it.start?.let {
                                        start ->
                                    formatTime(start)
                                }.orEmpty(),
                                it == state.slot,
                                { onSlot(it) },
                            )
                        }
                }
            }
            ShifaSection(stringResource(Res.string.reasonForVisitOptional)) {
                ShifaInputField(
                    state.reason,
                    onReasonChange,
                    stringResource(Res.string.reason),
                    Modifier.fillMaxWidth(),
                    clearable = false,
                )
            }
            ShifaSection(stringResource(Res.string.attachDocumentsOptional)) {
                Column(verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
                    Text(
                        stringResource(Res.string.attachDocumentsHint),
                        Modifier.fillMaxWidth(),
                        style = ShifaTextStyles.caption(),
                        color = ShifaColors.Text.Primary,
                    )
                    if (state.documents.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(Res.drawable.ic_check_12), null, tint = ShifaColors.Icon.Brand)
                            val count = state.documents.size
                            Text(
                                pluralStringResource(Res.plurals.documentsAttached, count, count),
                                style = MaterialTheme.typography.labelSmall,
                                color = ShifaColors.Text.Primary,
                            )
                        }
                    }
                    ShifaActionButton(
                        onAttach,
                        Modifier.fillMaxWidth(),
                        text =
                            stringResource(
                                if (state.isUploading) Res.string.uploadingDocument else Res.string.addDocument,
                            ),
                        icon = Res.drawable.ic_paperclip_16,
                        enabled = state.canAttach,
                        iconAfter = true,
                    )
                }
            }
        }
        ShifaPageElement(ShifaPageElementType.ActionTray, Modifier.navigationBarsPadding()) {
            // Above the button, so a booking error stays in sight.
            Banners(state.error)
            ShifaPrimaryButton(
                stringResource(Res.string.completeBooking),
                onBook,
                Modifier.fillMaxWidth(),
                enabled = state.canBook,
                loading = state.isBooking,
            )
        }
    }
}

/** The switch, the chosen visit, and for video the service (06 §3.1 step 7). */
@Composable
private fun VideoSection(
    state: CreateBookingUiState,
    onVideoChange: (Boolean) -> Unit,
    onServiceChange: (String) -> Unit,
) = ShifaSection(
    stringResource(Res.string.videoConsultation),
    action = { ShifaSwitch(state.isVideo, onVideoChange) },
) {
    state.slot?.let { slot ->
        slot.start?.let {
            val visit =
                formatVisit(
                    it,
                    parseInstant(slot.endAt),
                    stringArrayResource(Res.array.weekdays),
                    stringArrayResource(Res.array.months),
                )
            Text(visit, Modifier.fillMaxWidth(), style = ShifaTextStyles.caption(), color = ShifaColors.Text.Primary)
        }
    }
    if (!state.isVideo) return@ShifaSection
    val services = state.doctor.serviceItems
    if (services.isEmpty()) {
        ShifaBanner(
            stringResource(Res.string.bookingVideoNoServicesOffered),
            Res.drawable.ic_info_16,
            ShifaBannerType.Warning,
        )
        return@ShifaSection
    }
    val free = stringResource(Res.string.consultationServiceFreeBadge)
    val labels = services.map { if (it.isFreeConsultation) "${it.title} — $free" else it.title }
    var expanded by remember { mutableStateOf(false) }
    ShifaDropdown(
        stringResource(Res.string.videoConsultationServiceLabel),
        labels,
        labels.getOrNull(services.indexOfFirst { it.id == state.serviceId }),
        onSelect = { onServiceChange(services[labels.indexOf(it)].id) },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
        isError = state.error == Res.string.bookingSelectServiceForVideo,
    )
}
