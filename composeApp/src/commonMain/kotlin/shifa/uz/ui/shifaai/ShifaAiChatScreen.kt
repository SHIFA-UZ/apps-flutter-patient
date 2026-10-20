package shifa.uz.ui.shifaai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.addToChat
import shifa.composeapp.generated.resources.askAnything
import shifa.composeapp.generated.resources.cancel
import shifa.composeapp.generated.resources.chat
import shifa.composeapp.generated.resources.comingSoon
import shifa.composeapp.generated.resources.delete
import shifa.composeapp.generated.resources.deleteChatMessage
import shifa.composeapp.generated.resources.deleteChatTitle
import shifa.composeapp.generated.resources.errorRecordingVoice
import shifa.composeapp.generated.resources.microphonePermissionDenied
import shifa.composeapp.generated.resources.shifaAiDisclaimer
import shifa.composeapp.generated.resources.transcribing
import shifa.uz.ui.documents.UploadTray
import shifa.uz.ui.register.Banners
import shifa.uz.ui.register.MessagePopUp
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaChatBar
import shifa.uz.ui.widgets.ShifaChatBubble
import shifa.uz.ui.widgets.ShifaChatBubbleType
import shifa.uz.ui.widgets.ShifaChatThinkingBubble
import shifa.uz.ui.widgets.ShifaChatTray
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.rememberMicPermission
import shifa.uz.utils.rememberVoiceRecorder

/**
 * 09 (AI-01, AI-02): the open chat, over the tab shell. The mic records until the bar's stop is tapped (Figma
 * chat-bar recording, animated), then X2's transcript is sent as the question. Attachments aren't built yet.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ShifaAiChatScreen(
    viewModel: ShifaAiViewModel,
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { signedOut() } }
    var adding by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    BackHandler(adding) { adding = false }
    val soon = {
        adding = false
        viewModel.showNotice(Res.string.comingSoon)
    }
    val recorder = rememberVoiceRecorder()
    val askMic =
        rememberMicPermission { granted ->
            when {
                !granted -> viewModel.showError(Res.string.microphonePermissionDenied)
                recorder.start() -> viewModel.onRecording()
                else -> viewModel.showError(Res.string.errorRecordingVoice)
            }
        }
    ShifaAiChatContent(
        state,
        adding,
        onBack = onBack,
        // A chat is only saved once it has a message.
        onMore = { deleting = true }.takeIf { state.chat?.messages?.isNotEmpty() == true },
        onInput = viewModel::edit,
        onSend = viewModel::send,
        onMic = {
            when {
                state.recording -> viewModel.transcribe(recorder.stop())
                !state.isTranscribing && !state.rateLimited -> askMic()
            }
        },
        onAttach = { adding = true },
        onCloseAdd = { adding = false },
        onAdd = soon,
    )
    if (deleting) {
        Dialog({ deleting = false }) {
            MessagePopUp(stringResource(Res.string.deleteChatTitle), stringResource(Res.string.deleteChatMessage), {
                deleting = false
            }) {
                ShifaPrimaryButton(
                    stringResource(Res.string.delete),
                    {
                        deleting = false
                        viewModel.delete()
                    },
                    Modifier.fillMaxWidth(),
                    destructive = true,
                )
                ShifaSecondaryButton(stringResource(Res.string.cancel), { deleting = false }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun ShifaAiChatContent(
    state: ShifaAiUiState,
    adding: Boolean = false,
    onBack: () -> Unit = {},
    onMore: (() -> Unit)? = null,
    onInput: (String) -> Unit = {},
    onSend: () -> Unit = {},
    onMic: () -> Unit = {},
    onAttach: () -> Unit = {},
    onCloseAdd: () -> Unit = {},
    onAdd: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding().imePadding()) {
        ShifaSubHeader(stringResource(Res.string.chat), onBack = onBack, onMore = onMore)
        if (state.error != null || state.notice != null) {
            Column(
                Modifier.padding(horizontal = ShifaSpacing.Inset.Md),
                verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
            ) { Banners(state.error, state.notice) }
        }
        // Reversed, so the newest turn (and a streaming reply) stays in view; Top keeps a short chat at the top.
        LazyColumn(
            Modifier.weight(1f),
            reverseLayout = true,
            contentPadding = PaddingValues(ShifaSpacing.Inset.Md),
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md, Alignment.Top),
        ) {
            state.streaming?.let { reply ->
                item {
                    if (reply.isEmpty()) {
                        ShifaChatThinkingBubble()
                    } else {
                        ShifaChatBubble(reply, ShifaChatBubbleType.ShifaAi)
                    }
                }
            }
            items(state.chat?.messages.orEmpty().asReversed()) {
                ShifaChatBubble(it.content, if (it.isUser) ShifaChatBubbleType.Sent else ShifaChatBubbleType.ShifaAi)
            }
            // Not in Figma: 09 §6 item 5, the disclaimer.
            item {
                Text(
                    stringResource(Res.string.shifaAiDisclaimer),
                    Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = ShifaColors.Text.Tertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (adding) {
            UploadTray(onCloseAdd, onAdd, onAdd, onAdd, title = Res.string.addToChat)
        } else {
            ShifaChatTray(onAttach, onSend, sendEnabled = state.canSend) {
                ShifaChatBar(
                    state.input,
                    onInput,
                    stringResource(if (state.isTranscribing) Res.string.transcribing else Res.string.askAnything),
                    onMic,
                    recording = state.recording,
                )
            }
        }
    }
}
