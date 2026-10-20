package shifa.uz.ui.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.lastVisit
import shifa.composeapp.generated.resources.myDoctors
import shifa.composeapp.generated.resources.noAppointmentsFound
import shifa.composeapp.generated.resources.noDoctorsFound
import shifa.composeapp.generated.resources.pastAppointments
import shifa.composeapp.generated.resources.recommendedDoctors
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.ui.doctors.DoctorHeaderCard
import shifa.uz.ui.home.DoctorCard
import shifa.uz.ui.home.HomeSection
import shifa.uz.ui.register.Banners
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.formatDate
import shifa.uz.utils.getCurrentInstant

/** The Bookings tab's lists in full, behind their arrows: Figma 1655:15670 (My Doctors), 1655:15892 (Past); Recommended alike. Flat cards (Figma). */
enum class BookingsList(val title: StringResource) {
    Recommended(Res.string.recommendedDoctors),
    MyDoctors(Res.string.myDoctors),
    Past(Res.string.pastAppointments),
}

/** Shares the tab's [viewModel]; Home's My Doctors arrow opens it too, so it loads B2 and D1 if the tab never did. */
@Composable
fun BookingsListScreen(
    list: BookingsList,
    viewModel: BookingsViewModel,
    onBack: () -> Unit,
    onDoctor: (Doctor) -> Unit,
    onAppointment: (Appointment) -> Unit,
    onSignedOut: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) {
        if (viewModel.state.value.past == null) viewModel.load()
        viewModel.signedOut.collect { signedOut() }
    }
    BookingsListContent(list, state, getCurrentInstant(), onBack, onDoctor, onAppointment)
}

@Composable
internal fun BookingsListContent(
    list: BookingsList,
    state: BookingsUiState,
    now: Instant,
    onBack: () -> Unit = {},
    onDoctor: (Doctor) -> Unit = {},
    onAppointment: (Appointment) -> Unit = {},
) {
    ListPage(list.title, state.error, onBack) {
        when (list) {
            BookingsList.Recommended ->
                HomeSection(
                    null,
                    state.recommended,
                    state.isLoading,
                    Res.string.noDoctorsFound,
                    limit = Int.MAX_VALUE,
                ) {
                    DoctorHeaderCard(it, now, { onDoctor(it) })
                }
            BookingsList.MyDoctors ->
                HomeSection(
                    null,
                    state.myDoctors,
                    state.isLoading,
                    Res.string.noDoctorsFound,
                    limit = Int.MAX_VALUE,
                ) {
                    val visit = state.lastVisit(it)?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
                    DoctorCard(
                        it,
                        now,
                        onDoctor,
                        visit?.let { "${stringResource(Res.string.lastVisit)} ${formatDate(it)}" },
                        shadow = false,
                    )
                }
            BookingsList.Past ->
                HomeSection(
                    null,
                    state.past,
                    state.isLoading,
                    Res.string.noAppointmentsFound,
                    limit = Int.MAX_VALUE,
                ) {
                    PastCard(it, onAppointment, shadow = false)
                }
        }
    }
}

// ponytail: a plain Column, fine for the tens B2 and D1 give; LazyColumn if a list grows to hundreds.

/** A full-page list behind a section's arrow: the sub header, then the scrolling [content]. */
@Composable
internal fun ListPage(
    title: StringResource,
    error: StringResource?,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(title), onBack = onBack)
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = ShifaSpacing.Inset.Md),
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
        ) {
            Banners(error)
            content()
        }
    }
}
