package shifa.uz.ui.tasks

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.completed
import shifa.composeapp.generated.resources.openTasks
import shifa.uz.data.model.RemoteCareTask

enum class TasksTab(
    val title: StringResource,
) {
    Open(Res.string.openTasks),
    Completed(Res.string.completed),
}

/** [tasks] is null until the first load; a failed reload keeps the previous list (12 §6). */
@Stable
data class TasksUiState(
    val tasks: List<RemoteCareTask>? = null,
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val notice: StringResource? = null,
) {
    /** API order, no client sort (12 §3.1). */
    fun tasksIn(tab: TasksTab) = tasks?.filter { if (tab == TasksTab.Open) it.isOpen else it.isCompleted }
}
