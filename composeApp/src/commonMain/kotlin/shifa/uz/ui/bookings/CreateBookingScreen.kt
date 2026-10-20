package shifa.uz.ui.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.addDocument
import shifa.composeapp.generated.resources.attachDocumentsHint
import shifa.composeapp.generated.resources.attachDocumentsOptional
import shifa.composeapp.generated.resources.availableTimes
import shifa.composeapp.generated.resources.bookingSelectService
import shifa.composeapp.generated.resources.bookingSelectServiceForVideo
import shifa.composeapp.generated.resources.bookingVideoNoServicesOffered
import shifa.composeapp.generated.resources.completeBooking
import shifa.composeapp.generated.resources.completePayment
import shifa.composeapp.generated.resources.confirmPayment
import shifa.composeapp.generated.resources.consultationServiceFreeBadge
import shifa.composeapp.generated.resources.continuePayment
import shifa.composeapp.generated.resources.createBooking
import shifa.composeapp.generated.resources.documentsAttached
import shifa.composeapp.generated.resources.ic_check_12
import shifa.composeapp.generated.resources.ic_check_16
import shifa.composeapp.generated.resources.ic_info_16
import shifa.composeapp.generated.resources.ic_paperclip_16
import shifa.composeapp.generated.resources.months
import shifa.composeapp.generated.resources.monthsShort
import shifa.composeapp.generated.resources.noAvailableTimeSlots
import shifa.composeapp.generated.resources.payNow
import shifa.composeapp.generated.resources.paymentInClinic
import shifa.composeapp.generated.resources.paymentNotCompletedHeld
import shifa.composeapp.generated.resources.reason
import shifa.composeapp.generated.resources.reasonForVisitOptional
import shifa.composeapp.generated.resources.select
import shifa.composeapp.generated.resources.selectClinic
import shifa.composeapp.generated.resources.selectDate
import shifa.composeapp.generated.resources.selectService
import shifa.composeapp.generated.resources.service
import shifa.composeapp.generated.resources.uploadingDocument
import shifa.composeapp.generated.resources.videoConsultation
import shifa.composeapp.generated.resources.videoConsultationServiceLabel
import shifa.composeapp.generated.resources.weekdays
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.AvailableSlot
import shifa.uz.data.model.Doctor
import shifa.uz.ui.doctors.DoctorHeaderCard
import shifa.uz.ui.navigation.ScopedViewModels
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
import shifa.uz.ui.widgets.ShifaPaymentMethodCard
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSection
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.ui.widgets.ShifaSwitch
import shifa.uz.ui.widgets.ShifaTray
import shifa.uz.utils.formatShortDate
import shifa.uz.utils.formatTime
import shifa.uz.utils.formatVisit
import shifa.uz.utils.getCurrentInstant
import shifa.uz.utils.parseInstant
import shifa.uz.utils.rememberDocumentPickerLauncher

/**
 * 06 §3.1 (BOOK-01/03/04), Figma 1662:18783, 1662:19457, 1823:16296 (pay now), 1823:16350 (pay in clinic), over the
 * doctor profile. Figma titles it "Doctor Profile" (a copy-paste); this says Create Booking. Pay now: Complete
 * Payment → the Confirm Payment tray (1876:18609) → the provider's page (1825:17388) → [onPaid] once PAID.
 * ponytail: no attachment removal or ⋮ menu.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CreateBookingScreen(
    doctor: Doctor,
    onBack: () -> Unit,
    onBooked: (pending: Boolean) -> Unit,
    onPaid: (Appointment) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: CreateBookingViewModel = viewModel { CreateBookingViewModel(doctor) },
) {
    val state by viewModel.state.collectAsState()
    val booked by rememberUpdatedState(onBooked)
    val paid by rememberUpdatedState(onPaid)
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) { viewModel.booked.collect { booked(it) } }
    LaunchedEffect(viewModel) { viewModel.paid.collect { paid(it) } }
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { signedOut() } }
    val pickFile = rememberDocumentPickerLauncher { it?.let { viewModel.attach(it.fileName, it.fileBytes) } }
    var pickingDate by remember { mutableStateOf(false) }
    val now = getCurrentInstant()
    CreateBookingContent(
        state,
        now,
        onBack = onBack,
        onLocationChange = viewModel::onLocationChange,
        onVideoChange = viewModel::onVideoChange,
        onServiceChange = viewModel::onServiceChange,
        onPickDate = { pickingDate = true },
        onSlot = viewModel::onSlotSelect,
        onReasonChange = viewModel::onReasonChange,
        onAttach = pickFile,
        onBook = viewModel::book,
    )
    if (pickingDate) BookingDayPicker(state, now, viewModel::onDateChange) { pickingDate = false }
    if (state.methods != null) {
        PaymentMethodsSheet(state, viewModel::closeMethods) {
            ConfirmPaymentTray(state, viewModel::closeMethods, viewModel::onProviderSelect, viewModel::pay)
        }
    }
    val pending = state.pending
    if (state.paying && pending != null) {
        BackHandler { viewModel.onPaymentClosed() }
        key(pending.id) {
            ScopedViewModels {
                PaymentScreen(pending, viewModel::onPaymentClosed, viewModel::onPaymentDone, onSignedOut)
            }
        }
    }
}

@Composable
internal fun CreateBookingContent(
    state: CreateBookingUiState,
    now: Instant,
    onBack: () -> Unit = {},
    onLocationChange: (Int) -> Unit = {},
    onVideoChange: (Boolean) -> Unit = {},
    onServiceChange: (String) -> Unit = {},
    onPickDate: () -> Unit = {},
    onSlot: (AvailableSlot) -> Unit = {},
    onReasonChange: (String) -> Unit = {},
    onAttach: () -> Unit = {},
    onBook: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding().imePadding()) {
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
            if (state.locations.size > 1) ClinicSection(state, onLocationChange)
            if (state.doctor.serviceItems.isNotEmpty()) {
                ShifaSection(stringResource(Res.string.selectService)) { ServiceDropdown(state, onServiceChange) }
            }
            // Pay later can't be video (MOBILE_BOOKING_PAYMENT §1).
            if (state.canSwitchVideo) VideoSection(state, onVideoChange)
            DateSection(state, Res.string.selectDate, onPickDate)
            TimesSection(state, onSlot)
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
            Banners(state.error.takeIf { state.methods == null })
            val held = state.isHeld(now)
            state.holdEnd?.takeIf { held }?.let {
                ShifaBanner(
                    stringResource(Res.string.paymentNotCompletedHeld, formatTime(it)),
                    Res.drawable.ic_info_16,
                    ShifaBannerType.Warning,
                )
            }
            ShifaPrimaryButton(
                stringResource(
                    when {
                        held -> Res.string.continuePayment
                        state.isPayNow -> Res.string.completePayment
                        else -> Res.string.completeBooking
                    },
                ),
                onBook,
                Modifier.fillMaxWidth(),
                enabled = state.canBook,
                loading = state.isBooking,
            )
        }
    }
}

/** "Select Date" and its button: the picked day, else Select. Reschedule titles it "Select New Date". */
@Composable
internal fun DateSection(
    state: CreateBookingUiState,
    title: StringResource,
    onPickDate: () -> Unit,
) = ShifaSection(
    stringResource(title),
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

/** The day's free slots as chips; a spinner while B1 loads. */
@Composable
internal fun TimesSection(
    state: CreateBookingUiState,
    onSlot: (AvailableSlot) -> Unit,
) = ShifaSection(stringResource(Res.string.availableTimes)) {
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
                ShifaChip(it.start?.let { start -> formatTime(start) }.orEmpty(), it == state.slot, { onSlot(it) })
            }
    }
}

/** Remembers the date picker: today to 90 days on (06 §3.1 step 4). */
@Composable
internal fun BookingDayPicker(
    state: CreateBookingUiState,
    now: Instant,
    onDate: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    DayPicker(state.date, today..today.plus(90, DateTimeUnit.DAY), state.date ?: today, onDate, onDismiss)
}

/** The switch and the chosen visit; video without any services says why it can't book. */
@Composable
private fun VideoSection(
    state: CreateBookingUiState,
    onVideoChange: (Boolean) -> Unit,
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
    if (state.isVideo && state.doctor.serviceItems.isEmpty()) NoServicesBanner()
}

@Composable
private fun NoServicesBanner() =
    ShifaBanner(
        stringResource(Res.string.bookingVideoNoServicesOffered),
        Res.drawable.ic_info_16,
        ShifaBannerType.Warning,
    )

/**
 * The visit's services, "Title (300000.00 UZS)" or "Title — Free" (Figma "Check up (price)"), or the warning when
 * video has none. A pay-later one says it's paid at the clinic (Figma 1823:16350).
 */
@Composable
internal fun ServiceDropdown(
    state: CreateBookingUiState,
    onServiceChange: (String) -> Unit,
) {
    val services = state.services
    if (services.isEmpty()) return NoServicesBanner()
    val free = stringResource(Res.string.consultationServiceFreeBadge)
    val location = state.locationId.takeUnless { state.isVideo }
    val labels =
        services.map {
            val price = it.price(location)?.text
            when {
                it.isFreeConsultation -> "${it.title} — $free"
                price != null -> "${it.title} ($price)"
                else -> it.title
            }
        }
    var expanded by remember { mutableStateOf(false) }
    ShifaDropdown(
        stringResource(if (state.isVideo) Res.string.videoConsultationServiceLabel else Res.string.service),
        labels,
        labels.getOrNull(services.indexOfFirst { it.id == state.serviceId }),
        onSelect = { onServiceChange(services[it].id) },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
        isError = state.error == Res.string.bookingSelectServiceForVideo || state.error == Res.string.bookingSelectService,
    )
    if (state.isPayInClinic) {
        ShifaBanner(
            listOfNotNull(stringResource(Res.string.paymentInClinic), state.price).joinToString(" · "),
            Res.drawable.ic_info_16,
            ShifaBannerType.Success,
        )
    }
}

/** Figma 1823:16296's Select Clinic, when the doctor works at several. */
@Composable
private fun ClinicSection(
    state: CreateBookingUiState,
    onLocationChange: (Int) -> Unit,
) = ShifaSection(stringResource(Res.string.selectClinic)) {
    val names = state.locations.map { it.name ?: "#${it.id}" }
    var expanded by remember { mutableStateOf(false) }
    ShifaDropdown(
        stringResource(Res.string.selectClinic),
        names,
        names.getOrNull(state.locations.indexOfFirst { it.id == state.locationId }),
        onSelect = { onLocationChange(state.locations[it].id) },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodsSheet(
    state: CreateBookingUiState,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) = ModalBottomSheet(
    onDismissRequest = onClose,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape = RectangleShape,
    containerColor = Color.Transparent,
    dragHandle = null,
    contentWindowInsets = { WindowInsets(0) },
) { content() }

/** Figma 1876:18609: the providers from payments/methods (one is chosen; several can be picked), then Pay Now. */
@Composable
internal fun ConfirmPaymentTray(
    state: CreateBookingUiState,
    onClose: () -> Unit = {},
    onProvider: (String) -> Unit = {},
    onPay: () -> Unit = {},
) = ShifaTray(stringResource(Res.string.confirmPayment), onClose, Modifier.navigationBarsPadding()) {
    val methods = state.methods.orEmpty()
    methods.forEach {
        ShifaPaymentMethodCard(
            it.name,
            it.price,
            Modifier.fillMaxWidth(),
            selected = methods.size > 1 && it.provider == state.provider,
            onClick = { onProvider(it.provider) }.takeIf { methods.size > 1 },
        )
    }
    Banners(state.error)
    ShifaPrimaryButton(
        stringResource(Res.string.payNow),
        onPay,
        Modifier.fillMaxWidth(),
        enabled = state.provider != null,
        loading = state.isBooking,
    )
}
