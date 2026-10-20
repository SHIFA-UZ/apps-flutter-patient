package shifa.uz.ui.shifaai

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.chats
import shifa.composeapp.generated.resources.ic_plus_22
import shifa.composeapp.generated.resources.img_shifa_ai_header
import shifa.composeapp.generated.resources.newChat
import shifa.composeapp.generated.resources.noConversations
import shifa.composeapp.generated.resources.noMessages
import shifa.composeapp.generated.resources.search
import shifa.composeapp.generated.resources.shifaAiTitle
import shifa.uz.ui.notifications.time
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaMainHeader
import shifa.uz.ui.widgets.ShifaNotification
import shifa.uz.ui.widgets.ShifaNotificationType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSearchField
import shifa.uz.ui.widgets.ShifaSection
import shifa.uz.ui.widgets.ShifaSectionType
import shifa.uz.utils.getCurrentInstant

private val HeaderShape = ShifaRadius.ControlLg.copy(topStart = ZeroCornerSize, topEnd = ZeroCornerSize)

/** 09 (AI-01): the Shifa AI tab, the chats saved on this device. The avatar and bell are Home's. */
@Composable
fun ShifaAiScreen(
    viewModel: ShifaAiViewModel,
    avatarUrl: String?,
    notificationCount: Int,
    onNotifications: () -> Unit,
    onAccount: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    ShifaAiContent(
        state,
        avatarUrl = avatarUrl,
        notificationCount = notificationCount,
        onNotifications = onNotifications,
        onAccount = onAccount,
        onSearch = viewModel::search,
        onOpen = viewModel::open,
        onNewChat = { viewModel.open() },
        contentPadding = contentPadding,
    )
}

@Composable
internal fun ShifaAiContent(
    state: ShifaAiUiState,
    avatarUrl: String? = null,
    notificationCount: Int = 0,
    onNotifications: () -> Unit = {},
    onAccount: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onOpen: (String) -> Unit = {},
    onNewChat: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    now: Instant = getCurrentInstant(),
) {
    val density = LocalDensity.current
    var buttonHeight by remember { mutableStateOf(0.dp) }
    Box(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface)) {
        Column(Modifier.fillMaxSize()) {
            // Figma: the header's gradient is an image, anchored to its bottom edge; the header sets the height.
            Box(Modifier.fillMaxWidth().clip(HeaderShape)) {
                Image(
                    painterResource(Res.drawable.img_shifa_ai_header),
                    null,
                    Modifier.matchParentSize(),
                    alignment = Alignment.BottomCenter,
                    contentScale = ContentScale.Crop,
                )
                ShifaMainHeader(
                    stringResource(Res.string.shifaAiTitle),
                    Modifier.statusBarsPadding(),
                    avatarUrl = avatarUrl,
                    onNotifications = onNotifications,
                    onAvatar = onAccount,
                    notificationCount = notificationCount,
                )
            }
            // ponytail: not lazy, the store keeps at most 20 chats.
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(ShifaSpacing.Inset.Md)
                    .padding(bottom = contentPadding.calculateBottomPadding() + buttonHeight),
            ) {
                ShifaSection(stringResource(Res.string.chats), type = ShifaSectionType.CardTransparent) {
                    Column(verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
                        if (state.chats.isNotEmpty()) {
                            ShifaSearchField(
                                state.query,
                                onSearch,
                                stringResource(Res.string.search),
                                Modifier.fillMaxWidth(),
                            )
                        }
                        val shown = state.shown
                        if (shown.isEmpty()) {
                            Text(
                                stringResource(
                                    if (state.chats.isEmpty()) Res.string.noConversations else Res.string.noMessages,
                                ),
                                Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ShifaColors.Text.Tertiary,
                                textAlign = TextAlign.Center,
                            )
                        }
                        shown.forEach { chat ->
                            ShifaNotification(
                                chat.title,
                                chat.preview,
                                ShifaNotificationType.Chat,
                                time = time(Instant.fromEpochMilliseconds(chat.updatedAt), now),
                                onClick = { onOpen(chat.id) },
                            )
                        }
                    }
                }
            }
        }
        // Figma: a transparent page-element floating over the list, above the nav bar.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(contentPadding)
                .onSizeChanged { buttonHeight = with(density) { it.height.toDp() } }
                .padding(horizontal = ShifaSpacing.Inset.Md, vertical = ShifaSpacing.Inset.Sm),
        ) {
            ShifaPrimaryButton(
                stringResource(Res.string.newChat),
                onNewChat,
                Modifier.fillMaxWidth(),
                icon = Res.drawable.ic_plus_22,
            )
        }
    }
}
