package shifa.uz.ui.tasks

import androidx.compose.runtime.Stable
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.model.TaskCheckIn
import shifa.uz.data.model.TaskProgress

/**
 * The task page: its T3 [checkIns] on a calendar of [month] (its first day). Tapping a day opens its tray at [day]:
 * the form for [entry], the day's first PENDING check-in, else the day's submitted ones. [entryDue]: the form can be
 * sent, from 10 minutes before the check-in's time.
 */
@Stable
data class TaskCheckInUiState(
    val task: RemoteCareTask,
    val month: LocalDate,
    val checkIns: List<TaskCheckIn>? = null,
    val isLoading: Boolean = false,
    val day: LocalDate? = null,
    val entry: TaskCheckIn? = null,
    val entryDue: Boolean = false,
    val value: String = "",
    val answer: Boolean? = null,
    val notes: String = "",
    val valueError: StringResource? = null,
    val notesError: StringResource? = null,
    val isSubmitting: Boolean = false,
    val error: StringResource? = null,
    val notice: StringResource? = null,
) {
    /** NUMERIC, BOOLEAN, else TEXT (12 §5.1). */
    val inputType get() = task.inputType.uppercase().takeIf { it == NUMERIC || it == BOOLEAN } ?: TEXT

    /** The list's progress until T3 loads, then T3's own, so a submit shows at once. */
    val shownTask get() =
        checkIns?.let { all ->
            task.copy(progress = TaskProgress(all.size, all.count { it.isCompleted }, all.count { it.isPending }))
        } ?: task

    /** A day's check-ins, by time. */
    fun on(date: LocalDate) = checkIns.orEmpty().filter { it.date == date }.sortedBy { it.scheduledTime }

    val submitted get() = day?.let { on(it).filter { c -> c.isCompleted } }.orEmpty()

    /** The months with check-ins; before T3, the start date's. */
    val firstMonth get() = (checkIns.orEmpty().mapNotNull { it.date }.minOrNull() ?: startDate)?.firstOfMonth()
    val lastMonth get() = (checkIns.orEmpty().mapNotNull { it.date }.maxOrNull() ?: startDate)?.firstOfMonth()

    private val startDate get() = runCatching { LocalDate.parse(task.startDate.take(10)) }.getOrNull()

    internal fun cleared() = copy(value = "", answer = null, notes = "", valueError = null, notesError = null)

    companion object {
        const val NUMERIC = "NUMERIC"
        const val BOOLEAN = "BOOLEAN"
        const val TEXT = "TEXT"
    }
}

internal fun LocalDate.firstOfMonth() = LocalDate(year, month, 1)

internal fun LocalDate.plusMonths(months: Int) = plus(DatePeriod(months = months))
