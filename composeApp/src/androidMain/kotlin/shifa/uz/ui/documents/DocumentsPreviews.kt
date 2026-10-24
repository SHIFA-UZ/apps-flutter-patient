package shifa.uz.ui.documents

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.documentUploadedSuccessfully
import shifa.composeapp.generated.resources.enterDocumentTitle
import shifa.composeapp.generated.resources.errorNetworkConnection
import shifa.composeapp.generated.resources.failedToDeleteDocument
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.PatientDocument
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.widgets.Gallery
import shifa.uz.ui.widgets.PreviewTheme

private val list =
    listOf(
        PatientDocument(
            "1",
            "Blood test results",
            date = "2026-09-20",
            fileUrl = "https://f/1",
            creatorLabel = "Doctor",
            doctorName = "Dr. Aziza Karimova",
            category = "BLOOD_TEST",
        ),
        PatientDocument(
            "2",
            "MRI scan",
            date = "2026-09-12",
            fileUrl = "https://f/2",
            creatorLabel = "Patient",
            category = "MRI",
        ),
        PatientDocument(
            "3",
            "Prescription",
            date = "2026-08-30",
            fileUrl = "https://f/3",
            creatorLabel = "Doctor",
            category = "PRESCRIPTION",
        ),
        PatientDocument("4", "Vaccination record", date = "2026-07-02", fileUrl = "https://f/4"),
    )

private val draft = UploadDraft("blood-test-2026.pdf", byteArrayOf(), "blood-test-2026", category = "BLOOD_TEST")

@Composable
private fun Documents(state: DocumentsUiState) = PreviewTheme { DocumentsContent(state) }

@Composable
private fun Viewer(state: DocumentViewerUiState) =
    PreviewTheme { DocumentViewerContent(state, "MRI scan", onAppointment = {}, onMore = {}) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DocumentsListPreview() =
    Documents(
        DocumentsUiState(list, notice = Res.string.documentUploadedSuccessfully),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DocumentsFilteredPreview() =
    Documents(
        DocumentsUiState(list, category = "MRI", error = Res.string.failedToDeleteDocument),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DocumentsEmptyPreview() = Documents(DocumentsUiState(list, query = "x-ray"))

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DocumentsErrorPreview() = Documents(DocumentsUiState(error = Res.string.errorNetworkConnection))

@Preview
@Composable
internal fun DocumentUploadPreview() =
    Gallery(background = ShifaColors.Bg.Canvas) {
        UploadPopUp(draft, isUploading = false)
        UploadPopUp(
            draft.copy(title = "", category = null, titleMissing = true, error = Res.string.enterDocumentTitle),
            isUploading = false,
        )
    }

@Preview
@Composable
internal fun DocumentUploadTrayPreview() = Gallery(background = ShifaColors.Bg.Canvas) { UploadTray({}) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DocumentViewerLoadingPreview() = Viewer(DocumentViewerUiState())

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DocumentViewerErrorPreview() =
    Viewer(
        DocumentViewerUiState(isLoading = false, error = Res.string.errorNetworkConnection),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DocumentViewerUnsupportedPreview() =
    Viewer(
        DocumentViewerUiState(bytes = byteArrayOf(1), isLoading = false),
    )

// Figma 1921:23596: the visit the document came from.
@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DocumentViewerFromAppointmentPreview() =
    Viewer(
        DocumentViewerUiState(
            bytes = byteArrayOf(1),
            isLoading = false,
            appointment =
                Appointment(
                    id = "42",
                    doctorName = "Azamat Rakhimov",
                    doctorProfession = "Dentist",
                    doctorClinic = "Sog'lom Hayot Klinikasi",
                    startAt = "2026-08-15T05:00:00Z",
                    status = "COMPLETED",
                ),
        ),
    )
