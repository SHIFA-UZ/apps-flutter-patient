package shifa.uz.ui.documents

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.Appointment

@Stable
data class DocumentViewerUiState(
    /** F4's answer; null until it loads. */
    val bytes: ByteArray? = null,
    /** How to show [bytes]: "pdf", an image extension, or null when unsupported. */
    val extension: String? = null,
    val isLoading: Boolean = true,
    val confirmDelete: Boolean = false,
    val isDeleting: Boolean = false,
    val error: StringResource? = null,
    val notice: StringResource? = null,
    /** Figma's From Appointment (1921:23596): the visit the document came from, when it's known. */
    val appointment: Appointment? = null,
) {
    val canDownload get() = bytes != null && extension != null
}
