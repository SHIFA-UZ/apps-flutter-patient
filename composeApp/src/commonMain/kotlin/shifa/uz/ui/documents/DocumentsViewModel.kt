package shifa.uz.ui.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.documentDeleted
import shifa.composeapp.generated.resources.documentUploadedSuccessfully
import shifa.composeapp.generated.resources.enterDocumentTitle
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.documents.DocumentsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.PatientDocument

private val EXTENSION = Regex("\\.(pdf|jpe?g|png|gif|webp|heic)$", RegexOption.IGNORE_CASE)

private fun defaultTitle(fileName: String) = fileName.replace(EXTENSION, "")

class DocumentsViewModel(
    private val documents: DocumentsRepository = DocumentsRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(DocumentsUiState())
    val state: StateFlow<DocumentsUiState> = _state.asStateFlow()

    // F1 answered 401, as on Home.
    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private fun update(transform: DocumentsUiState.() -> DocumentsUiState) {
        _state.value = _state.value.transform()
    }

    /** F1, on first open and pull to refresh. The list stays on screen meanwhile. */
    fun load() {
        if (_state.value.isLoading) return
        update { copy(isLoading = true) }
        viewModelScope.launch {
            documents.getDocuments().fold(
                onSuccess = { update { copy(isLoading = false, documents = it, error = null) } },
                onFailure = {
                    update { copy(isLoading = false, error = it.toUserMessage(), notice = null) }
                    if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                },
            )
        }
    }

    fun search(query: String) = update { copy(query = query) }

    /** Null is All. */
    fun filterBy(category: String?) = update { copy(category = category) }

    /** A picked file opens the popup, or replaces the popup's file ("Change"). A title the user typed is kept. */
    fun pick(
        fileName: String,
        bytes: ByteArray,
    ) {
        val draft = _state.value.draft
        if (_state.value.isUploading) return
        val title = draft?.title?.takeIf { it != defaultTitle(draft.fileName) } ?: defaultTitle(fileName)
        update {
            copy(
                draft =
                    draft?.copy(
                        fileName = fileName,
                        bytes = bytes,
                        title = title,
                        error = null,
                    ) ?: UploadDraft(fileName, bytes, title),
                error = null,
                notice = null,
            )
        }
    }

    fun editTitle(title: String) =
        update {
            copy(
                draft = draft?.copy(title = title, titleMissing = false, error = null),
            )
        }

    /** Picking the chosen type again clears it (11 quirk 8). */
    fun selectCategory(code: String) =
        update { copy(draft = draft?.copy(category = code.takeIf { it != draft.category })) }

    fun cancelUpload() {
        if (!_state.value.isUploading) update { copy(draft = null) }
    }

    /** F2. A failure keeps the popup open and the list as it was (11 quirk 6). */
    fun upload() {
        val s = _state.value
        val draft = s.draft ?: return
        if (s.isUploading) return
        val title = draft.title.trim()
        if (title.isEmpty()) {
            return update {
                copy(draft = draft.copy(titleMissing = true, error = Res.string.enterDocumentTitle))
            }
        }
        update { copy(isUploading = true, draft = draft.copy(error = null)) }
        viewModelScope.launch {
            documents.upload(draft.bytes, draft.fileName, title, draft.category).fold(
                onSuccess = {
                    update {
                        copy(
                            isUploading = false,
                            draft = null,
                            documents = listOf(it) + documents.orEmpty(),
                            error = null,
                            notice = Res.string.documentUploadedSuccessfully,
                        )
                    }
                },
                onFailure = {
                    update {
                        copy(
                            isUploading = false,
                            draft = this.draft?.copy(error = it.toUserMessage()),
                        )
                    }
                },
            )
        }
    }

    /** F3 from a swipe; the card only swipes for the patient's own uploads. A failure puts the card back with the error. */
    fun delete(document: PatientDocument) {
        val id = document.id
        if (id.isBlank() || id in _state.value.deleting) return
        update { copy(deleting = deleting + id, error = null, notice = null) }
        viewModelScope.launch {
            documents.delete(id).fold(
                onSuccess = { onDeleted(id) },
                onFailure = {
                    update { copy(deleting = deleting - id, error = it.toUserMessage()) }
                    if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                },
            )
        }
    }

    /** [id] is gone (F3), from a swipe or the viewer. */
    fun onDeleted(id: String) =
        update {
            copy(
                documents = documents?.filterNot { it.id == id },
                deleting = deleting - id,
                error = null,
                notice = Res.string.documentDeleted,
            )
        }
}
