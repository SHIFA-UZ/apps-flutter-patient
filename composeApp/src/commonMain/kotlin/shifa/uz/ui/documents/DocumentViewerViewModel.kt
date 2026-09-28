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
import shifa.composeapp.generated.resources.documentSaved
import shifa.composeapp.generated.resources.errorOpeningDocument
import shifa.composeapp.generated.resources.errorSomethingWentWrong
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.documents.DocumentsRepository
import shifa.uz.data.model.PatientDocument
import shifa.uz.data.model.documentExtension
import shifa.uz.utils.safeFileName

/** 11 (DOCS-03). A 401 here isn't a logout: the file may live on another host (11 quirk 4). */
class DocumentViewerViewModel(
    private val document: PatientDocument,
    private val documents: DocumentsRepository = DocumentsRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(DocumentViewerUiState())
    val state: StateFlow<DocumentViewerUiState> = _state.asStateFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    init {
        load()
    }

    private fun update(transform: DocumentViewerUiState.() -> DocumentViewerUiState) {
        _state.value = _state.value.transform()
    }

    /** F4; also Retry. */
    fun load() {
        val url = document.file ?: return update { copy(isLoading = false, error = Res.string.errorOpeningDocument) }
        update { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            documents.getFile(url).fold(
                onSuccess = {
                    update {
                        copy(
                            isLoading = false,
                            bytes = it,
                            extension = documentExtension(it, document.type, url),
                        )
                    }
                },
                onFailure = { update { copy(isLoading = false, error = it.toUserMessage()) } },
            )
        }
    }

    /** Saves the file as "<title>.<extension>". Flutter's button was a stub (11 quirk 3). */
    fun download(save: (fileName: String, bytes: ByteArray) -> Result<String>) {
        val s = _state.value
        val bytes = s.bytes ?: return
        val extension = s.extension ?: return
        val name = safeFileName(document.title)
        val fileName = if (name.endsWith(".$extension", ignoreCase = true)) name else "$name.$extension"
        // ponytail: writes on the caller's thread; the bytes are already in memory, so it's quick.
        val saved = save(fileName, bytes).isSuccess
        update {
            copy(
                error = Res.string.errorSomethingWentWrong.takeUnless { saved },
                notice = Res.string.documentSaved.takeIf { saved },
            )
        }
    }

    fun askDelete() = update { copy(confirmDelete = true) }

    fun dismissDelete() {
        if (!_state.value.isDeleting) update { copy(confirmDelete = false) }
    }

    /** F3; the list drops the document once [deleted] fires. */
    fun delete() {
        if (_state.value.isDeleting) return
        update { copy(isDeleting = true) }
        viewModelScope.launch {
            documents.delete(document.id).fold(
                onSuccess = {
                    update { copy(isDeleting = false, confirmDelete = false) }
                    _deleted.send(Unit)
                },
                onFailure = {
                    update {
                        copy(
                            isDeleting = false,
                            confirmDelete = false,
                            error = it.toUserMessage(),
                            notice = null,
                        )
                    }
                },
            )
        }
    }
}
