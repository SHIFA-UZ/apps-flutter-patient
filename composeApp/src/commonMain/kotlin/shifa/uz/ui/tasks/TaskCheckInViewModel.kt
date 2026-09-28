package shifa.uz.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.checkInNotesRequired
import shifa.composeapp.generated.resources.checkInPleaseSelectYesOrNo
import shifa.composeapp.generated.resources.checkInValueInvalidNumber
import shifa.composeapp.generated.resources.checkInValueRequired
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.CheckInRequest
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.tasks.TasksRepository
import shifa.uz.ui.tasks.TaskCheckInUiState.Companion.BOOLEAN
import shifa.uz.ui.tasks.TaskCheckInUiState.Companion.NUMERIC
import shifa.uz.ui.tasks.TaskCheckInUiState.Companion.TEXT
import shifa.uz.ui.tasks.TaskCheckInUiState.Companion.UPCOMING_SLOTS
import shifa.uz.utils.getCurrentInstant
import kotlin.time.Duration.Companion.minutes

/**
 * 12 §3.2 (TASK-02): the list's task and its T3 slots, submitted with T4.
 * ponytail: no T2 refetch (the list's copy is fresh); T2 comes with the notification deep links (NOTIF).
 */
class TaskCheckInViewModel(
    task: RemoteCareTask,
    private val tasks: TasksRepository = TasksRepository(),
    private val clock: () -> Instant = ::getCurrentInstant,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val _state = MutableStateFlow(TaskCheckInUiState(task))
    val state: StateFlow<TaskCheckInUiState> = _state.asStateFlow()

    private val _submitted = Channel<Unit>(Channel.BUFFERED)
    val submitted: Flow<Unit> = _submitted.receiveAsFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private fun update(transform: TaskCheckInUiState.() -> TaskCheckInUiState) {
        _state.value = _state.value.transform()
    }

    init {
        load()
    }

    /**
     * T3 on open and pull to refresh. What's due is worked out here, with no ticker (12 quirk 10). The
     * selection survives a reload while its slot is still listed (quirk 8).
     */
    fun load() {
        if (_state.value.isLoading) return
        update { copy(isLoading = true) }
        viewModelScope.launch {
            tasks.getCheckIns(_state.value.task.id).fold(
                onSuccess = { all ->
                    val pending = all.filter { it.isPending }
                    val due = clock() + 10.minutes
                    val (slots, later) =
                        pending
                            .mapNotNull { slot -> slot.scheduledAt(zone)?.let { slot to it } }
                            .partition { it.second <= due }
                    val upcoming = later.sortedBy { it.second }.take(UPCOMING_SLOTS).map { it.first }
                    update {
                        val kept = if (slots.any { it.first.id == selected }) this else copy(selected = null).cleared()
                        kept.copy(
                            slots = slots.map { it.first },
                            upcoming = upcoming,
                            waiting = pending.isNotEmpty(),
                            isLoading = false,
                            error = null,
                        )
                    }
                },
                onFailure = {
                    update { copy(isLoading = false, error = it.toUserMessage()) }
                    if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                },
            )
        }
    }

    /** Picking a slot starts an empty form (12 §3.2). */
    fun select(
        id: Long,
        checked: Boolean,
    ) {
        if (!_state.value.isSubmitting) update { copy(selected = id.takeIf { checked }, error = null).cleared() }
    }

    fun onValueChange(value: String) = update { copy(value = value, valueError = null) }

    fun onAnswer(answer: Boolean) = update { copy(answer = answer, valueError = null) }

    fun onNotesChange(notes: String) = update { copy(notes = notes, notesError = null) }

    fun submit() {
        val s = _state.value
        val id = s.selected ?: return
        if (s.isSubmitting) return
        // Trimmed, so whitespace isn't a value (12 quirk 7); a comma decimal is fine (quirk 6).
        val value = s.value.trim()
        val number = value.replace(',', '.').toDoubleOrNull()
        val notes = s.notes.trim().ifEmpty { null }
        val valueError =
            when (s.inputType) {
                NUMERIC ->
                    when {
                        value.isEmpty() -> Res.string.checkInValueRequired
                        number == null -> Res.string.checkInValueInvalidNumber
                        else -> null
                    }
                BOOLEAN -> Res.string.checkInPleaseSelectYesOrNo.takeIf { s.answer == null }
                else -> Res.string.checkInValueRequired.takeIf { value.isEmpty() }
            }
        val notesError = Res.string.checkInNotesRequired.takeIf { s.task.notesRequired && notes == null }
        if (valueError != null || notesError != null) {
            update { copy(valueError = valueError, notesError = notesError) }
            return
        }
        update { copy(isSubmitting = true, error = null) }
        val request =
            CheckInRequest(
                id,
                numericValue = number.takeIf { s.inputType == NUMERIC },
                textValue = value.takeIf { s.inputType == TEXT },
                booleanValue = s.answer.takeIf { s.inputType == BOOLEAN },
                notes = notes,
            )
        viewModelScope.launch {
            tasks.submitCheckIn(s.task.id, request).fold(
                onSuccess = { _submitted.send(Unit) },
                onFailure = {
                    update { copy(isSubmitting = false, error = it.toUserMessage()) }
                    if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                },
            )
        }
    }
}
