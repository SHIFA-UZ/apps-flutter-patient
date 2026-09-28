package shifa.uz.ui.documents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.cancel
import shifa.composeapp.generated.resources.delete
import shifa.composeapp.generated.resources.deleteDocumentConfirmation
import shifa.composeapp.generated.resources.document
import shifa.composeapp.generated.resources.download
import shifa.composeapp.generated.resources.errorOpeningDocument
import shifa.composeapp.generated.resources.ic_download_22
import shifa.composeapp.generated.resources.loadingDocument
import shifa.composeapp.generated.resources.retry
import shifa.composeapp.generated.resources.unsupportedDocumentType
import shifa.uz.data.model.PatientDocument
import shifa.uz.ui.register.Banners
import shifa.uz.ui.register.MessagePopUp
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.PdfView
import shifa.uz.utils.rememberFileSaver

private const val MAX_ZOOM = 4f

/** 11 (DOCS-03): one document, with Download, and Delete for the patient's own uploads. */
@Composable
fun DocumentViewerScreen(
    document: PatientDocument,
    patientName: String?,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: DocumentViewerViewModel = viewModel { DocumentViewerViewModel(document) },
) {
    val state by viewModel.state.collectAsState()
    val deleted by rememberUpdatedState(onDeleted)
    LaunchedEffect(viewModel) { viewModel.deleted.collect { deleted() } }
    val saver = rememberFileSaver()
    DocumentViewerContent(
        state,
        document.title.ifBlank { stringResource(Res.string.document) },
        onBack = onBack,
        // F3 only takes the patient's own uploads.
        onMore = viewModel::askDelete.takeIf { document.isUploadedBy(patientName) && document.id.isNotBlank() },
        onRetry = viewModel::load,
        onDownload = { viewModel.download(saver::saveFile) },
    )
    if (state.confirmDelete) {
        Dialog(viewModel::dismissDelete) {
            MessagePopUp(
                stringResource(Res.string.delete),
                stringResource(Res.string.deleteDocumentConfirmation),
                viewModel::dismissDelete,
            ) {
                ShifaPrimaryButton(
                    stringResource(Res.string.delete),
                    viewModel::delete,
                    Modifier.fillMaxWidth(),
                    destructive = true,
                    loading = state.isDeleting,
                )
                ShifaSecondaryButton(
                    stringResource(Res.string.cancel),
                    viewModel::dismissDelete,
                    Modifier.fillMaxWidth(),
                    enabled = !state.isDeleting,
                )
            }
        }
    }
}

@Composable
internal fun DocumentViewerContent(
    state: DocumentViewerUiState,
    title: String,
    onBack: () -> Unit = {},
    onMore: (() -> Unit)? = null,
    onRetry: () -> Unit = {},
    onDownload: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(title, onBack = onBack, onMore = onMore)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().background(ShifaColors.Bg.Canvas).padding(ShifaSpacing.Inset.Md),
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
        ) {
            // Without the file, the page itself shows the error.
            if (state.bytes != null) Banners(state.error, state.notice)
            Box(
                Modifier.weight(1f).fillMaxWidth().background(ShifaColors.Bg.Surface).clipToBounds(),
                contentAlignment = Alignment.Center,
            ) {
                val bytes = state.bytes
                when {
                    state.isLoading ->
                        Message(
                            Res.string.loadingDocument,
                        ) { CircularProgressIndicator(color = ShifaColors.Icon.Brand) }
                    bytes == null ->
                        Message(state.error ?: Res.string.errorOpeningDocument) {
                            ShifaSecondaryButton(stringResource(Res.string.retry), onRetry)
                        }
                    state.extension == null -> Message(Res.string.unsupportedDocumentType)
                    state.extension == "pdf" ->
                        PdfView(
                            bytes,
                            Modifier.fillMaxSize(),
                        ) { Message(Res.string.unsupportedDocumentType) }
                    else -> ZoomableImage(bytes)
                }
            }
        }
        ShifaPageElement(ShifaPageElementType.ActionTray, Modifier.navigationBarsPadding()) {
            ShifaPrimaryButton(
                stringResource(Res.string.download),
                onDownload,
                Modifier.fillMaxWidth(),
                icon = Res.drawable.ic_download_22,
                enabled = state.canDownload,
            )
        }
    }
}

@Composable
private fun Message(
    text: StringResource,
    action: @Composable () -> Unit = {},
) = Column(
    Modifier.padding(ShifaSpacing.Inset.Lg),
    verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    action()
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = ShifaColors.Text.Tertiary,
        textAlign = TextAlign.Center,
    )
}

// Pinch to zoom 1–4x, drag while zoomed. A file that doesn't decode is unsupported.
@Composable
private fun ZoomableImage(bytes: ByteArray) {
    val image = remember(bytes) { runCatching { bytes.decodeToImageBitmap() }.getOrNull() }
    if (image == null) return Message(Res.string.unsupportedDocumentType)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Image(
        image,
        contentDescription = null,
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                    }
                }.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        contentScale = ContentScale.Fit,
    )
}
