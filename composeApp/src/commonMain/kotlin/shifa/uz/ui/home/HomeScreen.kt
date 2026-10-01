package shifa.uz.ui.home

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.availabilityToday
import shifa.composeapp.generated.resources.availabilityTomorrow
import shifa.composeapp.generated.resources.hello
import shifa.composeapp.generated.resources.ic_alert_circle_16
import shifa.composeapp.generated.resources.join
import shifa.composeapp.generated.resources.myDoctors
import shifa.composeapp.generated.resources.myTasks
import shifa.composeapp.generated.resources.noDoctorsFound
import shifa.composeapp.generated.resources.noTasksFound
import shifa.composeapp.generated.resources.noUpcomingAppointments
import shifa.composeapp.generated.resources.now
import shifa.composeapp.generated.resources.patient
import shifa.composeapp.generated.resources.started
import shifa.composeapp.generated.resources.starts
import shifa.composeapp.generated.resources.taskCategoryExercise
import shifa.composeapp.generated.resources.taskCategoryMedication
import shifa.composeapp.generated.resources.taskCategoryOther
import shifa.composeapp.generated.resources.taskCategoryVital
import shifa.composeapp.generated.resources.unknownDoctor
import shifa.composeapp.generated.resources.upcomingAppointments
import shifa.composeapp.generated.resources.videoConsultation
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaBanner
import shifa.uz.ui.widgets.ShifaBannerType
import shifa.uz.ui.widgets.ShifaCarousel
import shifa.uz.ui.widgets.ShifaDoctorCard
import shifa.uz.ui.widgets.ShifaDoctorCardType
import shifa.uz.ui.widgets.ShifaMainHeader
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaSection
import shifa.uz.ui.widgets.ShifaSectionType
import shifa.uz.ui.widgets.ShifaTaskCard
import shifa.uz.ui.widgets.ShifaTaskType
import shifa.uz.ui.widgets.ShifaUpcomingAppointmentCard
import shifa.uz.utils.formatDate
import shifa.uz.utils.formatSlot
import shifa.uz.utils.getCurrentInstant
import shifa.uz.utils.parseInstant

@Composable
fun HomeScreen(
    onNotifications: () -> Unit,
    onAccount: () -> Unit,
    onUpcoming: () -> Unit,
    onAppointment: (Appointment) -> Unit,
    onTasks: () -> Unit,
    onDoctors: () -> Unit,
    onDoctor: (Doctor) -> Unit,
    onSignedOut: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: HomeViewModel = viewModel { HomeViewModel() },
) {
    val state by viewModel.state.collectAsState()
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { signedOut() } }
    val now by rememberNow()
    HomeContent(
        state = state,
        now = now,
        onRefresh = viewModel::refresh,
        onNotifications = onNotifications,
        onAccount = onAccount,
        onUpcoming = onUpcoming,
        onAppointment = onAppointment,
        onTasks = onTasks,
        onDoctors = onDoctors,
        onDoctor = onDoctor,
        contentPadding = contentPadding,
    )
}

/** "Now" and Join follow the clock between loads. */
@Composable
internal fun rememberNow() =
    produceState(getCurrentInstant()) {
        while (true) {
            delay(30_000)
            value = getCurrentInstant()
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeContent(
    state: HomeUiState,
    now: Instant,
    onRefresh: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onAccount: () -> Unit = {},
    onUpcoming: () -> Unit = {},
    onAppointment: (Appointment) -> Unit = {},
    onTasks: () -> Unit = {},
    onDoctors: () -> Unit = {},
    onDoctor: (Doctor) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface)) {
        ShifaPageElement(ShifaPageElementType.Header, Modifier.statusBarsPadding()) {
            ShifaMainHeader(
                "${stringResource(Res.string.hello)}, ${state.firstName ?: stringResource(Res.string.patient)}",
                home = true,
                avatarUrl = state.photoUrl,
                onNotifications = onNotifications,
                onAvatar = onAccount,
                notificationCount = state.unreadNotifications,
            )
        }
        PullToRefreshBox(state.isRefreshing, onRefresh, Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding)) {
                ShifaPageElement(ShifaPageElementType.Body) {
                    Column(verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md)) {
                        state.error?.let {
                            ShifaBanner(stringResource(it), Res.drawable.ic_alert_circle_16, ShifaBannerType.Error)
                        }
                        // Figma hides this arrow; it shows when there are more than fit, like My Tasks and My Doctors.
                        // Gone when there are none.
                        if (state.upcoming?.isEmpty() != true) {
                            HomeSection(
                                Res.string.upcomingAppointments,
                                state.upcoming,
                                state.isLoading,
                                Res.string.noUpcomingAppointments,
                                ShifaSectionType.CardOnWhite,
                                onMore = onUpcoming.takeIf { state.upcoming.orEmpty().size > HOME_ITEMS },
                            ) { UpcomingCard(it, now, onAppointment) }
                        }
                        HomeSection(
                            Res.string.myTasks,
                            state.tasks,
                            state.isLoading,
                            Res.string.noTasksFound,
                            onMore = onTasks,
                        ) { TaskCard(it, now, onTasks) }
                        HomeSection(
                            Res.string.myDoctors,
                            state.doctors,
                            state.isLoading,
                            Res.string.noDoctorsFound,
                            onMore = onDoctors,
                        ) { DoctorCard(it, now, onDoctor) }
                    }
                }
            }
        }
    }
}

/** A spinner until the first load, the empty text, or up to [limit] cards. Hidden when a first load failed. No [title], no header. */
@Composable
internal fun <T> HomeSection(
    title: StringResource?,
    items: List<T>?,
    isLoading: Boolean,
    empty: StringResource,
    type: ShifaSectionType = ShifaSectionType.CardTransparent,
    onMore: (() -> Unit)? = null,
    limit: Int = HOME_ITEMS,
    carousel: Boolean = false,
    gap: Dp = ShifaSpacing.Gap.Sm,
    card: @Composable (T) -> Unit,
) {
    if (items == null && !isLoading) return
    ShifaSection(title?.let { stringResource(it) }, type = type, onMore = onMore) {
        when {
            items == null -> CircularProgressIndicator(color = ShifaColors.Icon.Brand)
            items.isEmpty() ->
                Text(
                    stringResource(empty),
                    Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShifaColors.Text.Tertiary,
                    textAlign = TextAlign.Center,
                )
            carousel ->
                ShifaCarousel(
                    Modifier.fillMaxWidth(),
                    ShifaSpacing.Gap.Sm,
                ) { items.take(limit).forEach { card(it) } }
            else ->
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    items.take(limit).forEach { card(it) }
                }
        }
    }
}

// ponytail: professions show as the API sends them; the translated catalog comes with the doctors list (DOC-01).
@Composable
internal fun UpcomingCard(
    appointment: Appointment,
    now: Instant,
    onClick: (Appointment) -> Unit,
    shadow: Boolean = true,
) {
    val live = appointment.isNow(now)
    val place = if (appointment.isVideo) stringResource(Res.string.videoConsultation) else appointment.doctorClinic
    ShifaUpcomingAppointmentCard(
        doctorName = appointment.doctorName?.ifBlank { null } ?: stringResource(Res.string.unknownDoctor),
        subtitle = listOfNotNull(appointment.doctorProfession, place).filter(String::isNotBlank).joinToString(" - "),
        time = if (live) stringResource(Res.string.now) else appointment.start?.let { slot(it, now) }.orEmpty(),
        avatarUrl = appointment.doctorPhotoUrl,
        video = appointment.isVideo,
        joinText = stringResource(Res.string.join).takeIf { live && appointment.isVideo },
        onJoin = { onClick(appointment) },
        shadow = shadow,
        onClick = { onClick(appointment) },
    )
}

/** Home's compact card; the tasks pages add the [progress] bar (Figma 1655:17107). */
@Composable
internal fun TaskCard(
    task: RemoteCareTask,
    now: Instant,
    onClick: (() -> Unit)?,
    progress: Boolean = false,
) {
    val (type, category) =
        when (task.category) {
            "VITAL" -> ShifaTaskType.Vitals to Res.string.taskCategoryVital
            "EXERCISE" -> ShifaTaskType.Exercise to Res.string.taskCategoryExercise
            "MEDICATION" -> ShifaTaskType.Medicine to Res.string.taskCategoryMedication
            else -> ShifaTaskType.Default to Res.string.taskCategoryOther
        }
    // 12 §3: "Starts" for a future start date, else "Started".
    val start = runCatching { LocalDate.parse(task.startDate.take(10)) }.getOrNull()
    val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val due =
        start?.let {
            "${stringResource(if (it > today) Res.string.starts else Res.string.started)}: ${formatDate(it)}"
        }
    ShifaTaskCard(
        title = task.taskName,
        description = task.description.orEmpty(),
        due = due.orEmpty(),
        type = type,
        tag = stringResource(category),
        completed = task.progress?.completedCheckIns?.takeIf { progress } ?: 0,
        total = task.progress?.totalCheckIns?.takeIf { progress } ?: 0,
        onClick = onClick,
    )
}

@Composable
internal fun DoctorCard(
    doctor: Doctor,
    now: Instant,
    onClick: (Doctor) -> Unit,
    availability: String? = parseInstant(doctor.nextAvailableStartAt)?.let { slot(it, now) },
    shadow: Boolean = true,
) = ShifaDoctorCard(
    name = doctor.name,
    specialty = listOfNotNull(doctor.profession, doctor.clinic).filter(String::isNotBlank).joinToString(" - "),
    type = ShifaDoctorCardType.Compact,
    avatarUrl = doctor.photoUrl,
    verified = doctor.isVerified,
    rating = doctor.ratingText,
    availability = availability,
    shadow = shadow,
    onClick = { onClick(doctor) },
)

@Composable
internal fun slot(
    at: Instant,
    now: Instant,
) = formatSlot(at, now, stringResource(Res.string.availabilityToday), stringResource(Res.string.availabilityTomorrow))
