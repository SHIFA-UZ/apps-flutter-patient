package shifa.uz.ui.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.bookings
import shifa.composeapp.generated.resources.monthsShort
import shifa.composeapp.generated.resources.myDoctors
import shifa.composeapp.generated.resources.noAppointmentsFound
import shifa.composeapp.generated.resources.noDoctorsFound
import shifa.composeapp.generated.resources.pastAppointments
import shifa.composeapp.generated.resources.recommendedDoctors
import shifa.composeapp.generated.resources.search
import shifa.composeapp.generated.resources.unknownDoctor
import shifa.composeapp.generated.resources.videoConsultation
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.ui.doctors.DoctorHeaderCard
import shifa.uz.ui.home.DoctorCard
import shifa.uz.ui.home.HOME_ITEMS
import shifa.uz.ui.home.HomeSection
import shifa.uz.ui.home.slot
import shifa.uz.ui.register.Banners
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaAppointmentStatus
import shifa.uz.ui.widgets.ShifaCarousel
import shifa.uz.ui.widgets.ShifaChip
import shifa.uz.ui.widgets.ShifaDoctorCard
import shifa.uz.ui.widgets.ShifaDoctorCardType
import shifa.uz.ui.widgets.ShifaMainHeader
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPastAppointmentCard
import shifa.uz.ui.widgets.ShifaSearchField
import shifa.uz.ui.widgets.ShifaSectionType
import shifa.uz.ui.widgets.ShifaTag
import shifa.uz.ui.widgets.ShifaTagType
import shifa.uz.utils.formatTimeDay
import shifa.uz.utils.getCurrentInstant
import shifa.uz.utils.parseInstant

/** 06/07: the Bookings tab, Figma 1648:11080. Find doctors (D1), my doctors and past visits (B2). */
@Composable
fun BookingsScreen(
    avatarUrl: String?,
    notificationCount: Int,
    onNotifications: () -> Unit,
    onAccount: () -> Unit,
    onAppointment: (Appointment) -> Unit,
    onDoctor: (Doctor) -> Unit,
    onList: (BookingsList) -> Unit,
    onSignedOut: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: BookingsViewModel = viewModel { BookingsViewModel() },
) {
    val state by viewModel.state.collectAsState()
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) {
        if (viewModel.state.value.past == null) viewModel.load()
        viewModel.signedOut.collect { signedOut() }
    }
    BookingsContent(
        state,
        now = getCurrentInstant(),
        avatarUrl = avatarUrl,
        notificationCount = notificationCount,
        onRefresh = viewModel::load,
        onSearch = viewModel::search,
        onFilter = viewModel::filterBy,
        onNotifications = onNotifications,
        onAccount = onAccount,
        onAppointment = onAppointment,
        onDoctor = onDoctor,
        onList = onList,
        contentPadding = contentPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookingsContent(
    state: BookingsUiState,
    now: Instant,
    avatarUrl: String? = null,
    notificationCount: Int = 0,
    onRefresh: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onFilter: (DoctorFilter) -> Unit = {},
    onNotifications: () -> Unit = {},
    onAccount: () -> Unit = {},
    onAppointment: (Appointment) -> Unit = {},
    onDoctor: (Doctor) -> Unit = {},
    onList: (BookingsList) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface)) {
        ShifaPageElement(ShifaPageElementType.Header, Modifier.statusBarsPadding()) {
            ShifaMainHeader(
                stringResource(Res.string.bookings),
                avatarUrl = avatarUrl,
                onNotifications = onNotifications,
                onAvatar = onAccount,
                notificationCount = notificationCount,
            )
        }
        ShifaPageElement(ShifaPageElementType.ActionTray) {
            ShifaSearchField(state.query, onSearch, stringResource(Res.string.search), Modifier.fillMaxWidth())
            ShifaCarousel {
                DoctorFilter.entries.forEach {
                    ShifaChip(
                        stringResource(it.label),
                        state.filter == it,
                        { onFilter(it) },
                    )
                }
            }
        }
        PullToRefreshBox(state.isLoading && state.past != null, onRefresh, Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding)) {
                ShifaPageElement(ShifaPageElementType.Body) {
                    Column(verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md)) {
                        Banners(state.error, state.notice)
                        if (state.filter == DoctorFilter.All) {
                            // Gone when D1 has none, unless that's a search's answer.
                            if (state.recommended?.isEmpty() != true || state.query.isNotBlank()) {
                                HomeSection(
                                    Res.string.recommendedDoctors,
                                    state.recommended,
                                    state.isLoading || state.isSearching,
                                    Res.string.noDoctorsFound,
                                    ShifaSectionType.CardOnWhite,
                                    onMore =
                                        { onList(BookingsList.Recommended) }
                                            .takeIf { state.recommended.orEmpty().size > RECOMMENDED_ITEMS },
                                    limit = RECOMMENDED_ITEMS,
                                    carousel = true,
                                ) { RecommendedCard(it, now, onDoctor) }
                            }
                            HomeSection(
                                Res.string.myDoctors,
                                state.myDoctors,
                                state.isLoading,
                                Res.string.noDoctorsFound,
                                onMore =
                                    {
                                        onList(
                                            BookingsList.MyDoctors,
                                        )
                                    }.takeIf { state.myDoctors.orEmpty().size > HOME_ITEMS },
                            ) { DoctorCard(it, now, onDoctor) }
                            HomeSection(
                                Res.string.pastAppointments,
                                state.past,
                                state.isLoading,
                                Res.string.noAppointmentsFound,
                                onMore =
                                    {
                                        onList(
                                            BookingsList.Past,
                                        )
                                    }.takeIf { state.past.orEmpty().size > HOME_ITEMS },
                            ) { PastCard(it, onAppointment) }
                        } else {
                            // Any other chip: just its doctors, as full cards (Figma 1653:12376).
                            HomeSection(
                                null,
                                state.recommended,
                                state.isLoading || state.isSearching,
                                Res.string.noDoctorsFound,
                                limit = Int.MAX_VALUE,
                                gap = ShifaSpacing.Gap.Md,
                            ) { DoctorHeaderCard(it, now, { onDoctor(it) }, shadow = true) }
                        }
                    }
                }
            }
        }
    }
}

private const val RECOMMENDED_ITEMS = 10

// Figma's in-card: the next slot as a tag.
@Composable
private fun RecommendedCard(
    doctor: Doctor,
    now: Instant,
    onClick: (Doctor) -> Unit,
) {
    val next = parseInstant(doctor.nextAvailableStartAt)?.let { slot(it, now) }
    ShifaDoctorCard(
        name = doctor.name,
        specialty = doctor.profession.orEmpty(),
        type = ShifaDoctorCardType.In,
        avatarUrl = doctor.photoUrl,
        verified = doctor.isVerified,
        tags = next?.let { { ShifaTag(it, type = ShifaTagType.Primary) } },
        shadow = false, // Figma 1653:12016: flat on the section's teal
        onClick = { onClick(doctor) },
    )
}

// 06: cancelled cards don't open.
@Composable
internal fun PastCard(
    appointment: Appointment,
    onClick: (Appointment) -> Unit,
    shadow: Boolean = true,
    link: Boolean = false,
) {
    val cancelled = appointment.status == "CANCELLED"
    val place = if (appointment.isVideo) stringResource(Res.string.videoConsultation) else appointment.doctorClinic
    ShifaPastAppointmentCard(
        doctorName = appointment.doctorName?.ifBlank { null } ?: stringResource(Res.string.unknownDoctor),
        subtitle = listOfNotNull(appointment.doctorProfession, place).filter(String::isNotBlank).joinToString(" - "),
        time = appointment.start?.let { formatTimeDay(it, stringArrayResource(Res.array.monthsShort)) }.orEmpty(),
        avatarUrl = appointment.doctorPhotoUrl,
        status =
            when {
                cancelled -> ShifaAppointmentStatus.Error
                appointment.isConfirmed || appointment.status == "COMPLETED" -> ShifaAppointmentStatus.Successful
                else -> ShifaAppointmentStatus.Warning // never confirmed
            },
        video = appointment.isVideo,
        shadow = shadow,
        link = link,
        onClick = { onClick(appointment) }.takeUnless { cancelled },
    )
}
