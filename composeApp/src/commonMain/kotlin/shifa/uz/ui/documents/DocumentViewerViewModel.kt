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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.documentSaved
import shifa.composeapp.generated.resources.errorOpeningDocument
import shifa.composeapp.generated.resources.errorSomethingWentWrong
import shifa.uz.data.analytics.Analytics
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.AnalyticsTracker
import shifa.uz.data.analytics.analyticsReason
import shifa.uz.data.analytics.fileType
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.documents.DocumentsRepository
import shifa.uz.data.model.PatientDocument
import shifa.uz.data.model.documentExtension
import shifa.uz.utils.parseDay
import shifa.uz.utils.safeFileName

/** 11 (DOCS-03). A 401 here isn't a logout: the file may live on another host (11 quirk 4). */
class DocumentViewerViewModel(
    private val document: PatientDocument,
    private val documents: DocumentsRepository = DocumentsRepository(),
    private val bookings: BookingsRepository = BookingsRepository(),
    private val analytics: AnalyticsTracker = Analytics,
) : ViewModel() {
    private val _state = MutableStateFlow(DocumentViewerUiState())

    // events.md §8: `self` / `doctor` / `unknown`, from the backend's ownership flags.
    private val uploadedBy =
        when {
            document.uploadedByPatient == true || document.creatorType.equals("PATIENT", true) -> "self"
            document.uploadedByPatient == false || document.creatorType.equals("DOCTOR", true) -> "doctor"
            else -> "unknown"
        }
    val state: StateFlow<DocumentViewerUiState> = _state.asStateFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    // events.md §8: `document_viewed` once, after both the file and its visit lookup have answered.
    private var viewedExtension: String? = null
    private var visitChecked = false
    private var viewedTracked = false

    private fun maybeTrackViewed() {
        val extension = viewedExtension ?: return
        if (!visitChecked || viewedTracked) return
        viewedTracked = true
        analytics.track(
            AnalyticsEvent.DocumentViewed(fileType(extension), uploadedBy, _state.value.appointment != null),
        )
    }

    init {
        load()
        findAppointment()
    }

    /**
     * The document's visit, from B2: by [PatientDocument.appointmentId] once the backend sends it, else the doctor's
     * visit on the document's day (the uploader is the doctor by name). No match or a failed B2: no section.
     * ponytail: two visits with one doctor on one day pick the latest; the id fixes that.
     */
    private fun findAppointment() {
        viewModelScope.launch {
            val all = bookings.getAppointments().getOrNull().orEmpty()
            val id = document.appointmentId?.ifBlank { null }
            val found =
                if (id != null) {
                    all.firstOrNull { it.id == id }
                } else {
                    val day = document.displayDate?.let { parseDay(it) }
                    val doctor = document.creatorLabel?.trim()?.lowercase()?.ifEmpty { null }
                    if (day == null || doctor == null) {
                        null
                    } else {
                        all.filter {
                            it.status != "CANCELLED" &&
                                it.doctorName?.trim()?.lowercase() == doctor &&
                                it.start?.toLocalDateTime(TimeZone.currentSystemDefault())?.date == day
                        }.maxByOrNull { it.startAt }
                    }
                }
            update { copy(appointment = found) }
            visitChecked = true
            maybeTrackViewed()
        }
    }

    private fun update(transform: DocumentViewerUiState.() -> DocumentViewerUiState) {
        _state.value = _state.value.transform()
    }

    /** F4; also Retry. */
    fun load() {
        val url =
            document.file ?: run {
                analytics.track(AnalyticsEvent.DocumentViewFailed("no_file"))
                return update { copy(isLoading = false, error = Res.string.errorOpeningDocument) }
            }
        update { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            documents.getFile(url).fold(
                onSuccess = {
                    val extension = documentExtension(it, document.type, url)
                    if (extension == null) {
                        analytics.track(AnalyticsEvent.DocumentViewFailed("unsupported"))
                    } else {
                        viewedExtension = extension
                        maybeTrackViewed()
                    }
                    update { copy(isLoading = false, bytes = it, extension = extension) }
                },
                onFailure = {
                    analytics.track(AnalyticsEvent.DocumentViewFailed(it.analyticsReason()))
                    update { copy(isLoading = false, error = it.toUserMessage()) }
                },
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
        analytics.track(
            if (saved) {
                AnalyticsEvent.DocumentDownloaded(
                    fileType(extension),
                )
            } else {
                AnalyticsEvent.DocumentDownloadFailed
            },
        )
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
                    analytics.track(AnalyticsEvent.DocumentDeleted("viewer"))
                    update { copy(isDeleting = false, confirmDelete = false) }
                    _deleted.send(Unit)
                },
                onFailure = {
                    analytics.track(AnalyticsEvent.DocumentDeleteFailed("viewer", it.analyticsReason()))
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
