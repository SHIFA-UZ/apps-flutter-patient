package shifa.uz.ui.documents

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.PatientDocument

@Stable
data class DocumentsUiState(
    /** Null until F1 first answers. */
    val documents: List<PatientDocument>? = null,
    val query: String = "",
    /** The category filter; null is All. */
    val category: String? = null,
    val isLoading: Boolean = false,
    /** The upload popup; open while a picked file waits for its title and type. */
    val draft: UploadDraft? = null,
    val isUploading: Boolean = false,
    /** Ids swiped away, waiting for F3. */
    val deleting: Set<String> = emptySet(),
    val error: StringResource? = null,
    val notice: StringResource? = null,
) {
    /** 11: a case-insensitive title search and the category, in memory. */
    val shown get() =
        query.trim().let { q ->
            documents.orEmpty().filter {
                (category == null || it.category == category) &&
                    it.title.contains(
                        q,
                        ignoreCase = true,
                    )
            }
        }

    /** The filter's chips: the known categories the list uses, in the spec's order, and the chosen one. */
    val filters get() =
        CATEGORY_LABELS.keys.filter {
                code ->
            code == category || documents.orEmpty().any { it.category == code }
        }
}

@Stable
data class UploadDraft(
    val fileName: String,
    val bytes: ByteArray,
    val title: String,
    val category: String? = null,
    val titleMissing: Boolean = false,
    val error: StringResource? = null,
)
