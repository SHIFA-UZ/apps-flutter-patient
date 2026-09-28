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
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.checkInSubmittedSuccessfully
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.model.ApiException
import shifa.uz.data.tasks.TasksRepository

/** 12 §3.1 (TASK-01). Lives in the tab shell, so the check-in page can report back to it. */
class TasksViewModel(
    private val tasks: TasksRepository = TasksRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(TasksUiState())
    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private fun update(transform: TasksUiState.() -> TasksUiState) {
        _state.value = _state.value.transform()
    }

    /** The page opened: last visit's notice goes, T1 reloads. */
    fun open() {
        update { copy(notice = null) }
        load()
    }

    /** T1. The list stays on screen meanwhile. */
    fun load() {
        if (_state.value.isLoading) return
        update { copy(isLoading = true) }
        viewModelScope.launch {
            tasks.getMyTasks().fold(
                onSuccess = { update { copy(tasks = it, isLoading = false, error = null) } },
                onFailure = {
                    update { copy(isLoading = false, error = it.toUserMessage()) }
                    if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                },
            )
        }
    }

    /** A check-in went through (T4): reload, with a notice. */
    fun onSubmitted() {
        update { copy(notice = Res.string.checkInSubmittedSuccessfully) }
        load()
    }
}
