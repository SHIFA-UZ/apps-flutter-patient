package shifa.uz.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.addToChat
import shifa.composeapp.generated.resources.document
import shifa.composeapp.generated.resources.errorRecordingVoice
import shifa.composeapp.generated.resources.microphonePermissionDenied
import shifa.composeapp.generated.resources.photo
import shifa.composeapp.generated.resources.typeMessage
import shifa.composeapp.generated.resources.voiceMessage
import shifa.uz.data.model.ChatMessage
import shifa.uz.data.model.MessageType
import shifa.uz.data.model.PatientDocument
import shifa.uz.ui.documents.DocumentViewerScreen
import shifa.uz.ui.documents.UploadTray
import shifa.uz.ui.navigation.ScopedViewModels
import shifa.uz.ui.register.Banners
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaChatBar
import shifa.uz.ui.widgets.ShifaChatBubble
import shifa.uz.ui.widgets.ShifaChatBubbleType
import shifa.uz.ui.widgets.ShifaChatImageBubble
import shifa.uz.ui.widgets.ShifaChatTray
import shifa.uz.ui.widgets.ShifaChatVoiceBubble
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.Playback
import shifa.uz.utils.rememberAudioPlayer
import shifa.uz.utils.rememberCameraLauncher
import shifa.uz.utils.rememberDocumentPickerLauncher
import shifa.uz.utils.rememberImagePickerLauncher
import shifa.uz.utils.rememberMicPermission
import shifa.uz.utils.rememberVoiceRecorder

/**
 * 08 §5.4 (CHAT-02), Figma 1772:17206: one chat, over the tab shell. Photos go from the camera or gallery, files
 * from the document picker, and the mic records a voice message (CHAT-04) like Shifa AI's. Photos and files open
 * in the Documents viewer (DOCS-03, over the chat; Flutter opened the system's); voice messages play in the bubble.
 * ponytail: Figma's ⋮ has no action (there's no API for it), so it isn't built; no times or read ticks either.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConversationScreen(
    target: ChatTarget,
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: ConversationViewModel = viewModel { ConversationViewModel(target) },
) {
    val state by viewModel.state.collectAsState()
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) {
        launch { viewModel.poll() }
        viewModel.signedOut.collect { signedOut() }
    }
    var adding by rememberSaveable { mutableStateOf(false) }
    BackHandler(adding) { adding = false }
    val takePhoto = rememberCameraLauncher { it?.let { viewModel.attach(it.fileName, it.fileBytes) } }
    val pickPhoto = rememberImagePickerLauncher { it?.let { viewModel.attach(it.fileName, it.fileBytes) } }
    val pickFile =
        rememberDocumentPickerLauncher { it?.let { viewModel.attach(it.fileName, it.fileBytes, MessageType.Document) } }
    val recorder = rememberVoiceRecorder()
    val askMic =
        rememberMicPermission { granted ->
            when {
                !granted -> viewModel.showError(Res.string.microphonePermissionDenied)
                recorder.start() -> viewModel.onRecording()
                else -> viewModel.showError(Res.string.errorRecordingVoice)
            }
        }
    val pick = { launch: () -> Unit ->
        adding = false
        launch()
    }
    val player = rememberAudioPlayer()
    var viewing by remember { mutableStateOf<PatientDocument?>(null) }
    BackHandler(viewing != null) { viewing = null }
    val photo = stringResource(Res.string.photo)
    ConversationContent(
        state,
        adding,
        playback = player.playback,
        onToggleVoice = player::toggle,
        onBack = onBack,
        onInput = viewModel::edit,
        onSend = viewModel::send,
        onMic = {
            when {
                state.recording -> viewModel.sendVoice(recorder.stop())
                !state.isSending -> askMic()
            }
        },
        onAttach = { adding = true },
        onCloseAdd = { adding = false },
        onCamera = { pick(takePhoto) },
        onPhoto = { pick(pickPhoto) },
        onFile = { pick(pickFile) },
        onOpenFile = { url, name -> viewing = PatientDocument(title = name ?: photo, fileUrl = url) },
    )
    // No id, so the viewer offers no Delete; Download saves it.
    viewing?.let {
        key(it) { ScopedViewModels { DocumentViewerScreen(it, null, onBack = { viewing = null }, onDeleted = {}) } }
    }
}

@Composable
internal fun ConversationContent(
    state: ConversationUiState,
    adding: Boolean = false,
    playback: Playback? = null,
    onBack: () -> Unit = {},
    onToggleVoice: (String) -> Unit = {},
    onInput: (String) -> Unit = {},
    onSend: () -> Unit = {},
    onMic: () -> Unit = {},
    onAttach: () -> Unit = {},
    onCloseAdd: () -> Unit = {},
    onCamera: () -> Unit = {},
    onPhoto: () -> Unit = {},
    onFile: () -> Unit = {},
    onOpenFile: (url: String, name: String?) -> Unit = { _, _ -> },
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding().imePadding()) {
        ShifaSubHeader(state.name, onBack = onBack, avatar = true, avatarUrl = state.photoUrl)
        if (state.error != null || state.notice != null) {
            Column(
                Modifier.padding(horizontal = ShifaSpacing.Inset.Md),
                verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
            ) { Banners(state.error, state.notice) }
        }
        // Reversed, so it opens at the newest message (Flutter's opened at the oldest); Top keeps a short chat up.
        LazyColumn(
            Modifier.weight(1f),
            reverseLayout = true,
            contentPadding = PaddingValues(ShifaSpacing.Inset.Md),
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val messages = state.messages
            if (messages == null) {
                // A failed first load is the banner's; the poll retries.
                if (state.error == null) item { CircularProgressIndicator(color = ShifaColors.Icon.Brand) }
            } else {
                items(messages.asReversed()) { Message(it, playback, onOpenFile, onToggleVoice) }
            }
        }
        if (adding) {
            UploadTray(onCloseAdd, onCamera, onPhoto, onFile, title = Res.string.addToChat)
        } else {
            ShifaChatTray(onAttach, onSend, sendEnabled = state.canSend) {
                ShifaChatBar(
                    state.input,
                    onInput,
                    stringResource(Res.string.typeMessage),
                    onMic,
                    recording = state.recording,
                )
            }
        }
    }
}

/** 08 §5.5: by type. A message with a file opens it on tap; a voice message plays ([playback] is this one's when its URL matches). */
@Composable
private fun Message(
    message: ChatMessage,
    playback: Playback?,
    onOpenFile: (url: String, name: String?) -> Unit,
    onToggleVoice: (String) -> Unit,
) {
    val type = if (message.isMine) ShifaChatBubbleType.Sent else ShifaChatBubbleType.Received
    val url = message.fileUrl
    val open = url?.let { Modifier.clickable { onOpenFile(it, message.fileName) } } ?: Modifier
    when (message.type) {
        MessageType.Text -> ShifaChatBubble(message.body.orEmpty(), type)
        MessageType.Image ->
            if (url != null) {
                ShifaChatImageBubble(received = !message.isMine, caption = message.body) {
                    KamelImage(
                        resource = asyncPainterResource(url),
                        contentDescription = stringResource(Res.string.photo),
                        modifier = Modifier.size(ShifaDimens.ChatImage).then(open),
                        contentScale = ContentScale.Crop,
                    )
                }
            } else {
                ShifaChatBubble(message.body ?: stringResource(Res.string.photo), type)
            }
        // ponytail: no file icon or size (08 §5.5.4), the kit has no file bubble.
        MessageType.Document ->
            ShifaChatBubble(message.fileName ?: stringResource(Res.string.document), type, open)
        MessageType.Voice ->
            if (url != null) {
                val playing = playback?.takeIf { it.url == url }
                ShifaChatVoiceBubble(
                    playing = playing?.playing == true,
                    position = playing?.position ?: 0,
                    duration = message.durationSeconds ?: playing?.duration ?: 0,
                    onToggle = { onToggleVoice(url) },
                    received = !message.isMine,
                )
            } else {
                ShifaChatBubble(stringResource(Res.string.voiceMessage), type)
            }
        MessageType.System ->
            Text(
                message.body.orEmpty(),
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = ShifaColors.Text.Tertiary,
                textAlign = TextAlign.Center,
            )
    }
}
