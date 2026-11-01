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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.checkInNotesRequired
import shifa.composeapp.generated.resources.checkInPleaseSelectYesOrNo
import shifa.composeapp.generated.resources.checkInSubmittedSuccessfully
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
import shifa.uz.utils.getCurrentInstant
import kotlin.time.Duration.Companion.minutes

/**
 * 12 §3.2 (TASK-02): the task page. Its T3 check-ins on a calendar; a day's PENDING one is submitted with T4.
 * With [openDue] (a task notification's Submit Check in) the first load opens the due check-in's day, or says
 * [nothingDue].
 * ponytail: no T2 refetch (the list's copy is fresh); T2 comes with the notification deep links (NOTIF).
 */
class TaskCheckInViewModel(
    task: RemoteCareTask,
    private val tasks: TasksRepository = TasksRepository(),
    private val clock: () -> Instant = ::getCurrentInstant,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
    private val openDue: Boolean = false,
) : ViewModel() {
    private val _state =
        MutableStateFlow(TaskCheckInUiState(task, clock().toLocalDateTime(zone).date.firstOfMonth()))
    val state: StateFlow<TaskCheckInUiState> = _state.asStateFlow()

    /** After each successful submit, so the list and Home reload. */
    private val _submitted = Channel<Unit>(Channel.BUFFERED)
    val submitted: Flow<Unit> = _submitted.receiveAsFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private val _nothingDue = Channel<Unit>(Channel.CONFLATED)
    val nothingDue: Flow<Unit> = _nothingDue.receiveAsFlow()

    private fun update(transform: TaskCheckInUiState.() -> TaskCheckInUiState) {
        _state.value = _state.value.transform()
    }

    init {
        load()
    }

    /** T3 on open and pull to refresh. The first load moves the calendar into the task's months. */
    fun load() {
        if (_state.value.isLoading) return
        update { copy(isLoading = true) }
        viewModelScope.launch {
            tasks.getCheckIns(_state.value.task.id).fold(
                onSuccess = { all ->
                    val first = _state.value.checkIns == null
                    update {
                        val loaded = copy(checkIns = all, isLoading = false, error = null)
                        val first = loaded.firstMonth
                        val last = loaded.lastMonth
                        val month =
                            when {
                                checkIns != null || first == null || last == null -> month
                                month < first -> first
                                month > last -> last
                                else -> month
                            }
                        loaded.copy(month = month)
                    }
                    if (openDue && first) openDueDay()
                },
                onFailure = {
                    update { copy(isLoading = false, error = it.toUserMessage()) }
                    if (it is ApiException.Unauthorized) {
                        _signedOut.send(Unit)
                    } else if (openDue && _state.value.checkIns == null) {
                        _nothingDue.send(Unit)
                    }
                },
            )
        }
    }

    /**
     * Today's PENDING check-in only. With today finished, another day's form would look like the same task asked
     * again, so nothing is due and the task page opens instead; its calendar still reaches the other days.
     */
    private fun openDueDay() {
        val today = clock().toLocalDateTime(zone).date
        if (_state.value.on(today).any { it.isPending }) openDay(today)
        if (_state.value.day == null) _nothingDue.trySend(Unit)
    }

    fun prevMonth() = update { copy(month = month.plusMonths(-1)) }

    fun nextMonth() = update { copy(month = month.plusMonths(1)) }

    /** A day with a PENDING check-in opens its form; one with only submitted ones shows them. */
    fun openDay(date: LocalDate) {
        val s = _state.value
        val day = s.on(date)
        val entry = day.firstOrNull { it.isPending }
        if (entry == null && day.none { it.isCompleted }) return
        val due = entry?.scheduledAt(zone)?.let { it <= clock() + 10.minutes } ?: false
        update { copy(day = date, entry = entry, entryDue = due, error = null, notice = null).cleared() }
    }

    fun closeDay() {
        if (!_state.value.isSubmitting) update { copy(day = null, entry = null, error = null).cleared() }
    }

    fun onValueChange(value: String) = update { copy(value = value, valueError = null) }

    fun onAnswer(answer: Boolean) = update { copy(answer = answer, valueError = null) }

    fun onNotesChange(notes: String) = update { copy(notes = notes, notesError = null) }

    fun submit() {
        val s = _state.value
        val id = s.entry?.id ?: return
        if (s.isSubmitting || !s.entryDue) return
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
                onSuccess = {
                    update {
                        copy(
                            day = null,
                            entry = null,
                            isSubmitting = false,
                            notice = Res.string.checkInSubmittedSuccessfully,
                        )
                            .cleared()
                    }
                    load()
                    _submitted.send(Unit)
                },
                onFailure = {
                    update { copy(isSubmitting = false, error = it.toUserMessage()) }
                    if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                },
            )
        }
    }
}
