package shifa.uz.ui.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.addedBy
import shifa.composeapp.generated.resources.addedByDoctor
import shifa.composeapp.generated.resources.addedByYou
import shifa.composeapp.generated.resources.all
import shifa.composeapp.generated.resources.camera
import shifa.composeapp.generated.resources.cancel
import shifa.composeapp.generated.resources.change
import shifa.composeapp.generated.resources.delete
import shifa.composeapp.generated.resources.document
import shifa.composeapp.generated.resources.documentCategoryLabel
import shifa.composeapp.generated.resources.documentCategorySelect
import shifa.composeapp.generated.resources.documentCategory_ALLERGY_REPORT
import shifa.composeapp.generated.resources.documentCategory_BIOPSY
import shifa.composeapp.generated.resources.documentCategory_BLOOD_TEST
import shifa.composeapp.generated.resources.documentCategory_CT_SCAN
import shifa.composeapp.generated.resources.documentCategory_DISCHARGE_SUMMARY
import shifa.composeapp.generated.resources.documentCategory_ECG
import shifa.composeapp.generated.resources.documentCategory_EEG
import shifa.composeapp.generated.resources.documentCategory_ENDOSCOPY
import shifa.composeapp.generated.resources.documentCategory_HOSPITAL_REPORT
import shifa.composeapp.generated.resources.documentCategory_IMAGING_OTHER
import shifa.composeapp.generated.resources.documentCategory_LAB_RESULT
import shifa.composeapp.generated.resources.documentCategory_MAMMOGRAPHY
import shifa.composeapp.generated.resources.documentCategory_MRI
import shifa.composeapp.generated.resources.documentCategory_OTHER_MEDICAL
import shifa.composeapp.generated.resources.documentCategory_PATHOLOGY
import shifa.composeapp.generated.resources.documentCategory_PRESCRIPTION
import shifa.composeapp.generated.resources.documentCategory_PRE_APPOINTMENT
import shifa.composeapp.generated.resources.documentCategory_REFERRAL
import shifa.composeapp.generated.resources.documentCategory_STOOL_TEST
import shifa.composeapp.generated.resources.documentCategory_ULTRASOUND
import shifa.composeapp.generated.resources.documentCategory_URINE_TEST
import shifa.composeapp.generated.resources.documentCategory_VACCINATION_RECORD
import shifa.composeapp.generated.resources.documentCategory_XRAY
import shifa.composeapp.generated.resources.documentName
import shifa.composeapp.generated.resources.documentUpload
import shifa.composeapp.generated.resources.documents
import shifa.composeapp.generated.resources.file
import shifa.composeapp.generated.resources.ic_camera_22
import shifa.composeapp.generated.resources.ic_file_22
import shifa.composeapp.generated.resources.ic_image_22
import shifa.composeapp.generated.resources.ic_trash_24
import shifa.composeapp.generated.resources.ic_upload_22
import shifa.composeapp.generated.resources.name
import shifa.composeapp.generated.resources.noDocumentsFound
import shifa.composeapp.generated.resources.photo
import shifa.composeapp.generated.resources.save
import shifa.composeapp.generated.resources.searchDocuments
import shifa.composeapp.generated.resources.upload
import shifa.uz.data.model.PatientDocument
import shifa.uz.ui.register.Banners
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.CardTitle
import shifa.uz.ui.widgets.ShifaActionButton
import shifa.uz.ui.widgets.ShifaChip
import shifa.uz.ui.widgets.ShifaDocumentCard
import shifa.uz.ui.widgets.ShifaDropdown
import shifa.uz.ui.widgets.ShifaInputField
import shifa.uz.ui.widgets.ShifaMainHeader
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPopUp
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSearchField
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaTray
import shifa.uz.utils.formatDay
import shifa.uz.utils.rememberCameraLauncher
import shifa.uz.utils.rememberDocumentPickerLauncher
import shifa.uz.utils.rememberImagePickerLauncher

// F2's categories, in the spec's order.
internal val DOCUMENT_CATEGORIES: List<Pair<String, StringResource>> =
    listOf(
        "BLOOD_TEST" to Res.string.documentCategory_BLOOD_TEST,
        "URINE_TEST" to Res.string.documentCategory_URINE_TEST,
        "STOOL_TEST" to Res.string.documentCategory_STOOL_TEST,
        "LAB_RESULT" to Res.string.documentCategory_LAB_RESULT,
        "MRI" to Res.string.documentCategory_MRI,
        "CT_SCAN" to Res.string.documentCategory_CT_SCAN,
        "XRAY" to Res.string.documentCategory_XRAY,
        "ULTRASOUND" to Res.string.documentCategory_ULTRASOUND,
        "MAMMOGRAPHY" to Res.string.documentCategory_MAMMOGRAPHY,
        "ECG" to Res.string.documentCategory_ECG,
        "EEG" to Res.string.documentCategory_EEG,
        "ENDOSCOPY" to Res.string.documentCategory_ENDOSCOPY,
        "BIOPSY" to Res.string.documentCategory_BIOPSY,
        "PATHOLOGY" to Res.string.documentCategory_PATHOLOGY,
        "IMAGING_OTHER" to Res.string.documentCategory_IMAGING_OTHER,
        "PRESCRIPTION" to Res.string.documentCategory_PRESCRIPTION,
        "VACCINATION_RECORD" to Res.string.documentCategory_VACCINATION_RECORD,
        "DISCHARGE_SUMMARY" to Res.string.documentCategory_DISCHARGE_SUMMARY,
        "REFERRAL" to Res.string.documentCategory_REFERRAL,
        "HOSPITAL_REPORT" to Res.string.documentCategory_HOSPITAL_REPORT,
        "ALLERGY_REPORT" to Res.string.documentCategory_ALLERGY_REPORT,
        "OTHER_MEDICAL" to Res.string.documentCategory_OTHER_MEDICAL,
    )

// What cards and the filter can show: F2's categories plus the booking flow's. Unknown codes show nothing (11 §5.2).
internal val CATEGORY_LABELS: Map<String, StringResource> =
    (DOCUMENT_CATEGORIES + ("PRE_APPOINTMENT" to Res.string.documentCategory_PRE_APPOINTMENT)).toMap()

/** 11 (DOCS-01, DOCS-02): the Documents tab. The avatar and bell are Home's. */
@Composable
fun DocumentsScreen(
    avatarUrl: String?,
    patientName: String?,
    notificationCount: Int,
    onNotifications: () -> Unit,
    onAccount: () -> Unit,
    onOpen: (PatientDocument) -> Unit,
    onSignedOut: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: DocumentsViewModel = viewModel { DocumentsViewModel() },
) {
    val state by viewModel.state.collectAsState()
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) {
        if (viewModel.state.value.documents == null) viewModel.load()
        viewModel.signedOut.collect { signedOut() }
    }
    var tray by rememberSaveable { mutableStateOf(false) }
    val takePhoto = rememberCameraLauncher { it?.let { viewModel.pick(it.fileName, it.fileBytes) } }
    val pickPhoto = rememberImagePickerLauncher { it?.let { viewModel.pick(it.fileName, it.fileBytes) } }
    val pickFile = rememberDocumentPickerLauncher { it?.let { viewModel.pick(it.fileName, it.fileBytes) } }
    DocumentsContent(
        state,
        avatarUrl = avatarUrl,
        patientName = patientName,
        notificationCount = notificationCount,
        onRefresh = viewModel::load,
        onSearch = viewModel::search,
        onFilter = viewModel::filterBy,
        onNotifications = onNotifications,
        onAccount = onAccount,
        onOpen = onOpen,
        onDelete = viewModel::delete,
        onUpload = { tray = true },
        contentPadding = contentPadding,
    )
    state.draft?.let {
        Dialog(viewModel::cancelUpload) {
            UploadPopUp(
                it,
                state.isUploading,
                onTitleChange = viewModel::editTitle,
                onCategorySelect = viewModel::selectCategory,
                onChange = { tray = true },
                onSave = viewModel::upload,
                onCancel = viewModel::cancelUpload,
            )
        }
    }
    if (tray) {
        val pick = { launch: () -> Unit ->
            tray = false
            launch()
        }
        UploadTraySheet(
            onDismiss = { tray = false },
            onCamera = { pick(takePhoto) },
            onPhoto = { pick(pickPhoto) },
            onFile = { pick(pickFile) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocumentsContent(
    state: DocumentsUiState,
    avatarUrl: String? = null,
    patientName: String? = null,
    notificationCount: Int = 0,
    onRefresh: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onFilter: (String?) -> Unit = {},
    onNotifications: () -> Unit = {},
    onAccount: () -> Unit = {},
    onOpen: (PatientDocument) -> Unit = {},
    onDelete: (PatientDocument) -> Unit = {},
    onUpload: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val density = LocalDensity.current
    var uploadHeight by remember { mutableStateOf(0.dp) }
    Box(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface)) {
        Column(Modifier.fillMaxSize()) {
            ShifaPageElement(ShifaPageElementType.Header, Modifier.statusBarsPadding()) {
                ShifaMainHeader(
                    stringResource(Res.string.documents),
                    avatarUrl = avatarUrl,
                    onNotifications = onNotifications,
                    onAvatar = onAccount,
                    notificationCount = notificationCount,
                )
            }
            ShifaPageElement(ShifaPageElementType.ActionTray) {
                ShifaSearchField(
                    state.query,
                    onSearch,
                    stringResource(Res.string.searchDocuments),
                    Modifier.fillMaxWidth(),
                )
                // Not in Figma: DOCS-01's category filter.
                if (state.filters.isNotEmpty()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
                    ) {
                        ShifaChip(stringResource(Res.string.all), state.category == null, { onFilter(null) })
                        state.filters.forEach { code ->
                            ShifaChip(
                                stringResource(CATEGORY_LABELS.getValue(code)),
                                state.category == code,
                                { onFilter(code) },
                            )
                        }
                    }
                }
            }
            // Above the list, so a swipe's result shows wherever the list is scrolled.
            if (state.error != null || state.notice != null) {
                Column(
                    Modifier.padding(
                        start = ShifaSpacing.Inset.Md,
                        top = ShifaSpacing.Inset.Md,
                        end = ShifaSpacing.Inset.Md,
                    ),
                    verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
                ) { Banners(state.error, state.notice) }
            }
            PullToRefreshBox(state.isLoading, onRefresh, Modifier.weight(1f)) {
                // Ids may be blank or repeated, so items aren't keyed; key(document) keeps a swipe on its own card.
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = ShifaSpacing.Inset.Md,
                            top = ShifaSpacing.Inset.Md,
                            end = ShifaSpacing.Inset.Md,
                            bottom = ShifaSpacing.Inset.Md + contentPadding.calculateBottomPadding() + uploadHeight,
                        ),
                    verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
                ) {
                    val shown = state.shown
                    if (state.documents != null && shown.isEmpty()) {
                        item {
                            Text(
                                stringResource(Res.string.noDocumentsFound),
                                Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ShifaColors.Text.Tertiary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    items(
                        shown,
                    ) {
                        key(
                            it,
                        ) { DocumentCard(it, it.isUploadedBy(patientName), it.id in state.deleting, onOpen, onDelete) }
                    }
                }
            }
        }
        // Figma: a transparent page-element floating over the list, above the nav bar.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(contentPadding)
                .onSizeChanged { uploadHeight = with(density) { it.height.toDp() } }
                .padding(horizontal = ShifaSpacing.Inset.Md, vertical = ShifaSpacing.Inset.Sm),
        ) {
            ShifaPrimaryButton(
                stringResource(Res.string.upload),
                onUpload,
                Modifier.fillMaxWidth(),
                icon = Res.drawable.ic_upload_22,
            )
        }
    }
}

@Composable
private fun DocumentCard(
    document: PatientDocument,
    own: Boolean,
    isDeleting: Boolean,
    onOpen: (PatientDocument) -> Unit,
    onDelete: (PatientDocument) -> Unit,
) {
    // The backend sends the uploader's name (11 expects "Doctor" / "Patient"). Figma: "Added by Dr. X".
    val uploader =
        when {
            own -> stringResource(Res.string.addedByYou)
            document.creatorLabel == "Doctor" ->
                document.doctorName?.ifBlank { null }?.let { stringResource(Res.string.addedBy, it) }
                    ?: stringResource(Res.string.addedByDoctor)
            else ->
                document.creatorLabel?.takeIf { it.isNotBlank() && it != "Unknown" }
                    ?.let { stringResource(Res.string.addedBy, it) }.orEmpty()
        }
    val card = @Composable {
        ShifaDocumentCard(
            title = document.title.ifBlank { stringResource(Res.string.document) },
            subtitle = uploader,
            date = document.displayDate?.let(::formatDay).orEmpty(),
            onClick = document.file?.let { { onOpen(document) } },
            tag = document.category?.let(CATEGORY_LABELS::get)?.let { stringResource(it) },
        )
    }
    // F3 only takes the patient's own uploads.
    if (!own || document.id.isBlank()) return card()
    val swipe = rememberSwipeToDismissBoxState()
    // The card stays swiped away while F3 runs; a failure brings it back.
    LaunchedEffect(isDeleting) { if (!isDeleting) swipe.reset() }
    SwipeToDismissBox(
        swipe,
        backgroundContent = {
            if (swipe.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(ShifaRadius.ControlMd)
                        .background(ShifaColors.Interactive.Destructive)
                        .padding(horizontal = ShifaSpacing.Inset.Lg),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(Modifier.size(ShifaSize.S24), ShifaColors.Icon.Inverse, ShifaSize.S2)
                    } else {
                        Icon(
                            painterResource(Res.drawable.ic_trash_24),
                            stringResource(Res.string.delete),
                            tint = ShifaColors.Icon.Inverse,
                        )
                    }
                }
            }
        },
        enableDismissFromStartToEnd = false,
        onDismiss = { onDelete(document) },
    ) { card() }
}

/** Figma popup "Document Upload". */
@Composable
internal fun UploadPopUp(
    draft: UploadDraft,
    isUploading: Boolean,
    onTitleChange: (String) -> Unit = {},
    onCategorySelect: (String) -> Unit = {},
    onChange: () -> Unit = {},
    onSave: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val labels = DOCUMENT_CATEGORIES.map { stringResource(it.second) }
    ShifaPopUp(stringResource(Res.string.documentUpload), onCancel, Modifier.verticalScroll(rememberScrollState())) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(ShifaRadius.ControlMd)
                .background(ShifaColors.Bg.Canvas)
                .padding(ShifaSpacing.Inset.Sm),
            horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardTitle(draft.fileName, Modifier.weight(1f))
            ShifaActionButton(onChange, text = stringResource(Res.string.change), enabled = !isUploading)
        }
        Banners(draft.error)
        Label(Res.string.documentName)
        ShifaInputField(
            draft.title,
            onTitleChange,
            stringResource(Res.string.name),
            Modifier.fillMaxWidth(),
            isError = draft.titleMissing,
            enabled = !isUploading,
        )
        Label(Res.string.documentCategoryLabel)
        ShifaDropdown(
            stringResource(Res.string.documentCategorySelect),
            labels,
            DOCUMENT_CATEGORIES.indexOfFirst { it.first == draft.category }.let(labels::getOrNull),
            onSelect = { onCategorySelect(DOCUMENT_CATEGORIES[labels.indexOf(it)].first) },
            expanded = expanded,
            onExpandedChange = { expanded = it && !isUploading },
            modifier = Modifier.fillMaxWidth(),
        )
        ShifaPrimaryButton(stringResource(Res.string.save), onSave, Modifier.fillMaxWidth(), loading = isUploading)
        ShifaSecondaryButton(
            stringResource(Res.string.cancel),
            onCancel,
            Modifier.fillMaxWidth(),
            enabled = !isUploading,
        )
    }
}

@Composable
private fun Label(text: StringResource) =
    Text(stringResource(text), style = MaterialTheme.typography.titleSmall, color = ShifaColors.Text.Primary)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadTraySheet(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onPhoto: () -> Unit,
    onFile: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RectangleShape,
        containerColor = Color.Transparent,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        UploadTray(onDismiss, onCamera, onPhoto, onFile)
    }
}

/** Figma tray (its title, "Add to chat", is a copy of the chat tray's). */
@Composable
internal fun UploadTray(
    onClose: () -> Unit,
    onCamera: () -> Unit = {},
    onPhoto: () -> Unit = {},
    onFile: () -> Unit = {},
) = ShifaTray(stringResource(Res.string.upload), onClose) {
    ShifaSecondaryButton(
        stringResource(Res.string.camera),
        onCamera,
        Modifier.fillMaxWidth(),
        icon = Res.drawable.ic_camera_22,
    )
    ShifaSecondaryButton(
        stringResource(Res.string.photo),
        onPhoto,
        Modifier.fillMaxWidth(),
        icon = Res.drawable.ic_image_22,
    )
    ShifaSecondaryButton(
        stringResource(Res.string.file),
        onFile,
        Modifier.fillMaxWidth(),
        icon = Res.drawable.ic_file_22,
    )
    Spacer(Modifier.navigationBarsPadding())
}
