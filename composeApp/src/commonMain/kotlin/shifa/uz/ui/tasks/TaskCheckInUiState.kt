package shifa.uz.ui.tasks

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.model.TaskCheckIn

/** [slots]: the PENDING check-ins due within 10 minutes, null until T3 first loads. The form is for [selected]. */
@Stable
data class TaskCheckInUiState(
    val task: RemoteCareTask,
    val slots: List<TaskCheckIn>? = null,
    val waiting: Boolean = false, // some are PENDING, none due yet
    val isLoading: Boolean = false,
    val selected: Long? = null,
    val value: String = "",
    val answer: Boolean? = null,
    val notes: String = "",
    val valueError: StringResource? = null,
    val notesError: StringResource? = null,
    val isSubmitting: Boolean = false,
    val error: StringResource? = null,
) {
    /** NUMERIC, BOOLEAN, else TEXT (12 §5.1). */
    val inputType get() = task.inputType.uppercase().takeIf { it == NUMERIC || it == BOOLEAN } ?: TEXT

    val showsNotes get() = task.notesRequired || task.notesLabel != null

    internal fun cleared() = copy(value = "", answer = null, notes = "", valueError = null, notesError = null)

    companion object {
        const val NUMERIC = "NUMERIC"
        const val BOOLEAN = "BOOLEAN"
        const val TEXT = "TEXT"
    }
}
