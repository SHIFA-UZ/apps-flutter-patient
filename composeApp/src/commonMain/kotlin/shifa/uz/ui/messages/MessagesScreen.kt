package shifa.uz.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.doctor
import shifa.composeapp.generated.resources.messages
import shifa.composeapp.generated.resources.noConversations
import shifa.composeapp.generated.resources.noDoctorsAvailable
import shifa.composeapp.generated.resources.noMessages
import shifa.composeapp.generated.resources.photo
import shifa.composeapp.generated.resources.retry
import shifa.composeapp.generated.resources.search
import shifa.uz.data.model.ChatContact
import shifa.uz.data.model.ChatUser
import shifa.uz.ui.notifications.DayHeader
import shifa.uz.ui.register.Banners
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaMainHeader
import shifa.uz.ui.widgets.ShifaMessageNotification
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaSearchField
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.utils.formatTime
import shifa.uz.utils.getCurrentInstant

/**
 * 08 §5.3 (CHAT-01), Figma 1772:16842: the Messages tab. The search finds doctors (C5) to start a chat with.
 * The avatar and bell are Home's. ponytail: no initials without a photo (08 §5.6), the kit's avatar has none.
 */
@Composable
fun MessagesScreen(
    avatarUrl: String?,
    notificationCount: Int,
    onNotifications: () -> Unit,
    onAccount: () -> Unit,
    onOpen: (ChatTarget) -> Unit,
    onSignedOut: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: MessagesViewModel = viewModel { MessagesViewModel() },
) {
    val state by viewModel.state.collectAsState()
    val open by rememberUpdatedState(onOpen)
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) {
        if (viewModel.state.value.conversations == null) viewModel.load()
        launch { viewModel.open.collect { open(it) } }
        viewModel.signedOut.collect { signedOut() }
    }
    MessagesContent(
        state,
        avatarUrl = avatarUrl,
        notificationCount = notificationCount,
        onNotifications = onNotifications,
        onAccount = onAccount,
        onRefresh = viewModel::load,
        onSearch = viewModel::search,
        onOpen = { onOpen(ChatTarget(it.id, name = it.participantName, photoUrl = it.participantPhotoUrl)) },
        onStart = viewModel::start,
        contentPadding = contentPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessagesContent(
    state: MessagesUiState,
    avatarUrl: String? = null,
    notificationCount: Int = 0,
    onNotifications: () -> Unit = {},
    onAccount: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onOpen: (ChatContact) -> Unit = {},
    onStart: (ChatUser) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    now: Instant = getCurrentInstant(),
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Canvas)) {
        ShifaPageElement(ShifaPageElementType.Header, Modifier.statusBarsPadding()) {
            ShifaMainHeader(
                stringResource(Res.string.messages),
                home = true,
                avatarUrl = avatarUrl,
                onNotifications = onNotifications,
                onAvatar = onAccount,
                notificationCount = notificationCount,
            )
        }
        ShifaPageElement(ShifaPageElementType.ActionTray) {
            ShifaSearchField(state.query, onSearch, stringResource(Res.string.search), Modifier.fillMaxWidth())
        }
        PullToRefreshBox(state.isLoading && state.conversations != null, onRefresh, Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = ShifaSpacing.Inset.Md,
                        top = ShifaSpacing.Inset.Md,
                        end = ShifaSpacing.Inset.Md,
                        bottom = ShifaSpacing.Inset.Md + contentPadding.calculateBottomPadding(),
                    ),
                verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                state.error?.let { item { Banners(it) } }
                if (state.isSearch) results(state, onStart) else chats(state, now, onRefresh, onOpen)
            }
        }
    }
}

private fun LazyListScope.results(
    state: MessagesUiState,
    onStart: (ChatUser) -> Unit,
) {
    val results = state.results
    when {
        results == null -> if (state.isSearching) item { CircularProgressIndicator(color = ShifaColors.Icon.Brand) }
        results.isEmpty() -> item { Hint(Res.string.noDoctorsAvailable) }
        else ->
            items(results) {
                ShifaMessageNotification(
                    it.name,
                    stringResource(Res.string.doctor),
                    time = null,
                    avatarUrl = it.photoUrl,
                    onClick = { onStart(it) }.takeUnless { state.isStarting },
                )
            }
    }
}

private fun LazyListScope.chats(
    state: MessagesUiState,
    now: Instant,
    onRefresh: () -> Unit,
    onOpen: (ChatContact) -> Unit,
) {
    val chats = state.sorted
    when {
        chats == null ->
            item {
                if (state.isLoading) {
                    CircularProgressIndicator(color = ShifaColors.Icon.Brand)
                } else {
                    ShifaSecondaryButton(stringResource(Res.string.retry), onRefresh)
                }
            }
        chats.isEmpty() -> item { Hint(Res.string.noConversations) }
        else -> {
            val zone = TimeZone.currentSystemDefault()
            val today = now.toLocalDateTime(zone).date
            // Ids aren't keys: a repeated one would crash the list.
            chats.groupBy { it.lastMessageAt?.toLocalDateTime(zone)?.date }.forEach { (day, group) ->
                day?.let { item { DayHeader(it, today) } }
                items(group) { chat ->
                    ShifaMessageNotification(
                        chat.participantName,
                        preview(chat.lastMessage),
                        time = chat.lastMessageAt?.let { formatTime(it, zone) },
                        avatarUrl = chat.participantPhotoUrl,
                        unreadCount = chat.unreadCount,
                        onClick = { onOpen(chat) },
                    )
                }
            }
        }
    }
}

/** 08 §5.3: none is "No messages", the backend's "photo" is localized. */
@Composable
private fun preview(lastMessage: String?) =
    when {
        lastMessage.isNullOrBlank() -> stringResource(Res.string.noMessages)
        lastMessage.trim().equals("photo", ignoreCase = true) -> stringResource(Res.string.photo)
        else -> lastMessage
    }

@Composable
private fun Hint(text: StringResource) =
    Text(
        stringResource(text),
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = ShifaColors.Text.Tertiary,
        textAlign = TextAlign.Center,
    )
