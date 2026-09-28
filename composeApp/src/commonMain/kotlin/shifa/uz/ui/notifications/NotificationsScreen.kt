package shifa.uz.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.defaultNotificationTitle
import shifa.composeapp.generated.resources.doctor
import shifa.composeapp.generated.resources.documentAccessRequestMessage
import shifa.composeapp.generated.resources.documentAccessRequestTitle
import shifa.composeapp.generated.resources.ic_arrow_right_16
import shifa.composeapp.generated.resources.justNow
import shifa.composeapp.generated.resources.minuteAgo
import shifa.composeapp.generated.resources.minutesAgo
import shifa.composeapp.generated.resources.monthsShort
import shifa.composeapp.generated.resources.noNotifications
import shifa.composeapp.generated.resources.notificationEmptyBody
import shifa.composeapp.generated.resources.notificationEmptyFilter
import shifa.composeapp.generated.resources.notificationEmptyFilterHint
import shifa.composeapp.generated.resources.notifications
import shifa.composeapp.generated.resources.retry
import shifa.composeapp.generated.resources.signatureRequestedMessage
import shifa.composeapp.generated.resources.signatureRequestedTitle
import shifa.composeapp.generated.resources.timeYesterday
import shifa.composeapp.generated.resources.today
import shifa.composeapp.generated.resources.yesterday
import shifa.uz.data.model.AppNotification
import shifa.uz.ui.documents.DocumentDisplayCard
import shifa.uz.ui.home.TaskCard
import shifa.uz.ui.home.UpcomingCard
import shifa.uz.ui.register.Banners
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.theme.lineBox
import shifa.uz.ui.widgets.LocalCardOnSurface
import shifa.uz.ui.widgets.ShifaCarousel
import shifa.uz.ui.widgets.ShifaChip
import shifa.uz.ui.widgets.ShifaNotification
import shifa.uz.ui.widgets.ShifaNotificationType
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.ui.widgets.ShifaTray
import shifa.uz.utils.formatTime
import shifa.uz.utils.getCurrentInstant

/**
 * 13 §4.1-4.2 (NOTIF-01), Figma 1703:15201 (inbox) and its trays 1703:15698 (appointment), 1703:16143 (document),
 * 1703:16483 (task). [onLink] follows the tray's button; the page closes first, like Flutter's `go`.
 * Unread titles are brand (Figma has no unread look). ponytail: the ⋮ has no Figma, so it isn't built.
 */
@Composable
fun NotificationsScreen(
    patientName: String?,
    onBack: () -> Unit,
    onLink: (NotificationLink) -> Unit,
    onRead: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: NotificationsViewModel = viewModel { NotificationsViewModel() },
) {
    val state by viewModel.state.collectAsState()
    val read by rememberUpdatedState(onRead)
    val signedOut by rememberUpdatedState(onSignedOut)
    var filter by rememberSaveable { mutableStateOf(NotificationFilter.All) }
    LaunchedEffect(viewModel) {
        launch { viewModel.read.collect { read() } }
        viewModel.signedOut.collect { signedOut() }
    }
    val now = getCurrentInstant()
    NotificationsContent(
        state,
        filter,
        now,
        onBack,
        { filter = it },
        viewModel::load,
        viewModel::markAllRead,
        viewModel::open,
    )
    state.selected?.let {
        NotificationTraySheet(it, state.link, now, patientName, viewModel::close) { link ->
            viewModel.close()
            onLink(link)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationsContent(
    state: NotificationsUiState,
    filter: NotificationFilter,
    now: Instant,
    onBack: () -> Unit = {},
    onFilter: (NotificationFilter) -> Unit = {},
    onRefresh: () -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    onOpen: (AppNotification) -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(Res.string.notifications), onBack = onBack, onCheck = onMarkAllRead)
        }
        ShifaPageElement(ShifaPageElementType.ActionTray) {
            ShifaCarousel(Modifier.fillMaxWidth()) {
                NotificationFilter.entries.forEach {
                    ShifaChip(stringResource(it.title), it == filter, { onFilter(it) })
                }
            }
        }
        PullToRefreshBox(state.isLoading && state.notifications != null, onRefresh, Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(ShifaSpacing.Inset.Md),
                verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Banners(state.error, state.notice)
                val shown = state.shownIn(filter)
                when {
                    shown == null && state.isLoading -> CircularProgressIndicator(color = ShifaColors.Icon.Brand)
                    // 13 §4.1: a failed first load offers a retry; the error is the banner's.
                    shown == null -> ShifaSecondaryButton(stringResource(Res.string.retry), onRefresh)
                    shown.isEmpty() ->
                        if (filter == NotificationFilter.All) {
                            Empty(Res.string.noNotifications, Res.string.notificationEmptyBody)
                        } else {
                            Empty(Res.string.notificationEmptyFilter, Res.string.notificationEmptyFilterHint)
                        }
                    else -> {
                        val zone = TimeZone.currentSystemDefault()
                        val months = stringArrayResource(Res.array.monthsShort)
                        val today = now.toLocalDateTime(zone).date
                        shown.groupBy { it.createdAt.toLocalDateTime(zone).date }.forEach { (day, items) ->
                            val style = ShifaTextStyles.profileName() // Figma: Bold 16/16
                            Text(
                                when (day.daysUntil(today)) {
                                    0 -> stringResource(Res.string.today)
                                    1 -> stringResource(Res.string.yesterday)
                                    else -> "${months[day.monthNumber - 1]} ${day.dayOfMonth}"
                                },
                                Modifier.fillMaxWidth().lineBox(style),
                                style = style,
                                color = ShifaColors.Text.Primary,
                            )
                            items.forEach {
                                val (title, message) = texts(it)
                                ShifaNotification(
                                    title,
                                    message,
                                    iconOf(it.kind),
                                    time = time(it.createdAt, now),
                                    unread = !it.isRead,
                                ) {
                                    onOpen(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Empty(
    title: StringResource,
    body: StringResource,
) {
    Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = ShifaColors.Text.Primary)
    Text(
        stringResource(body),
        style = MaterialTheme.typography.bodyMedium,
        color = ShifaColors.Text.Tertiary,
        textAlign = TextAlign.Center,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTraySheet(
    notification: AppNotification,
    link: NotificationLink?,
    now: Instant,
    patientName: String?,
    onClose: () -> Unit,
    onLink: (NotificationLink) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RectangleShape,
        containerColor = Color.Transparent,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        NotificationTray(notification, link, now, patientName, onClose, onLink)
    }
}

/** Figma notification-appointment / -document / -task: the message, the linked card and its button. */
@Composable
internal fun NotificationTray(
    notification: AppNotification,
    link: NotificationLink?,
    now: Instant,
    patientName: String?,
    onClose: () -> Unit = {},
    onLink: (NotificationLink) -> Unit = {},
) {
    val (title, message) = texts(notification)
    ShifaTray(title, onClose) {
        val style = MaterialTheme.typography.bodyMedium
        Text(message, Modifier.fillMaxWidth().lineBox(style), style = style, color = ShifaColors.Text.Primary)
        link?.let {
            val open = { onLink(it) }
            CompositionLocalProvider(LocalCardOnSurface provides true) {
                when (it) {
                    is NotificationLink.ToAppointment -> UpcomingCard(it.appointment, now, onClick = { open() })
                    is NotificationLink.ToDocument ->
                        DocumentDisplayCard(it.document, it.document.isUploadedBy(patientName), open)
                    is NotificationLink.ToTask -> TaskCard(it.task, now, open)
                    else -> {}
                }
            }
            ShifaPrimaryButton(
                stringResource(actionLabel(notification.kind)),
                open,
                Modifier.fillMaxWidth(),
                icon = Res.drawable.ic_arrow_right_16,
            )
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

private fun iconOf(kind: String) =
    when {
        "APPOINTMENT" in kind -> ShifaNotificationType.Appointment
        "TASK" in kind -> ShifaNotificationType.Task
        "MESSAGE" in kind -> ShifaNotificationType.Chat
        // Figma's default card. Feedback's is for feedback requests, which N1 doesn't send.
        else -> ShifaNotificationType.Document
    }

/** 13 §6.3: the localized title and message, else the backend's. */
@Composable
private fun texts(n: AppNotification): Pair<String, String> {
    val signature =
        stringResource(Res.string.signatureRequestedTitle) to
            stringResource(Res.string.signatureRequestedMessage, n.doctor ?: stringResource(Res.string.doctor))
    return when (n.kind) {
        "SIGNATURE_REQUESTED" -> signature
        "DOCUMENT_ACCESS_REQUEST" ->
            stringResource(Res.string.documentAccessRequestTitle) to
                (
                    accessRequestParts(n.message)?.let { (who, file, patient) ->
                        stringResource(Res.string.documentAccessRequestMessage, who, file, patient)
                    } ?: n.message
                )
        else ->
            NOTIFICATION_TEXTS[n.kind]?.let { (title, message) -> stringResource(title) to stringResource(message) }
                ?: if ("signature" in n.title.lowercase()) {
                    signature
                } else {
                    n.title.ifBlank { stringResource(Res.string.defaultNotificationTitle) } to n.message
                }
    }
}

/** 13 §4.1 time: "Just now", "N minutes ago", HH:mm today, "Yesterday HH:mm", else "MMM d • HH:mm". */
@Composable
internal fun time(
    at: Instant,
    now: Instant,
): String {
    val zone = TimeZone.currentSystemDefault()
    val minutes = (now - at).inWholeMinutes
    val local = at.toLocalDateTime(zone)
    return when {
        minutes < 1 -> stringResource(Res.string.justNow)
        minutes == 1L -> stringResource(Res.string.minuteAgo)
        minutes < 60 -> stringResource(Res.string.minutesAgo, minutes)
        else ->
            when (local.date.daysUntil(now.toLocalDateTime(zone).date)) {
                0 -> formatTime(at, zone)
                1 -> stringResource(Res.string.timeYesterday, formatTime(at, zone))
                else ->
                    "${stringArrayResource(Res.array.monthsShort)[local.monthNumber - 1]} ${local.dayOfMonth} • " +
                        formatTime(at, zone)
            }
    }
}
