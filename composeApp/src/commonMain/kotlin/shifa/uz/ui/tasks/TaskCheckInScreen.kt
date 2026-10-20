package shifa.uz.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.checkInAlreadySubmitted
import shifa.composeapp.generated.resources.checkInEnterGenericValue
import shifa.composeapp.generated.resources.checkInEnterLabeledValue
import shifa.composeapp.generated.resources.checkInValue
import shifa.composeapp.generated.resources.comment
import shifa.composeapp.generated.resources.dueAt
import shifa.composeapp.generated.resources.ic_check_16
import shifa.composeapp.generated.resources.months
import shifa.composeapp.generated.resources.no
import shifa.composeapp.generated.resources.submitCheckIn
import shifa.composeapp.generated.resources.submitEntry
import shifa.composeapp.generated.resources.task
import shifa.composeapp.generated.resources.taskCheckInNotYetAvailable
import shifa.composeapp.generated.resources.yes
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.model.TaskCheckIn
import shifa.uz.ui.home.TaskCard
import shifa.uz.ui.register.Banners
import shifa.uz.ui.tasks.TaskCheckInUiState.Companion.BOOLEAN
import shifa.uz.ui.tasks.TaskCheckInUiState.Companion.NUMERIC
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.widgets.ShifaBanner
import shifa.uz.ui.widgets.ShifaBannerType
import shifa.uz.ui.widgets.ShifaCalendarCard
import shifa.uz.ui.widgets.ShifaCalendarDay
import shifa.uz.ui.widgets.ShifaChip
import shifa.uz.ui.widgets.ShifaDateState
import shifa.uz.ui.widgets.ShifaInputField
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.ui.widgets.ShifaTray
import shifa.uz.utils.getCurrentInstant

/**
 * 12 §3.2 (TASK-02), Figma 1662:18317 (calendar), 1891:21385 (submit), 1891:21727 (submitted), over the tasks list.
 * ponytail: no "From Appointment" (T1 has no link to a visit) and no Delete Entry (no endpoint), see tasks.md.
 */
@Composable
fun TaskCheckInScreen(
    task: RemoteCareTask,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: TaskCheckInViewModel = viewModel { TaskCheckInViewModel(task) },
) {
    val state by viewModel.state.collectAsState()
    val submitted by rememberUpdatedState(onSubmitted)
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) { viewModel.submitted.collect { submitted() } }
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { signedOut() } }
    val now = getCurrentInstant()
    TaskCheckInContent(
        state,
        now,
        onBack = onBack,
        onRefresh = viewModel::load,
        onPrev = viewModel::prevMonth,
        onNext = viewModel::nextMonth,
        onDay = viewModel::openDay,
    )
    if (state.day != null) {
        DaySheet(
            onClose = viewModel::closeDay,
        ) {
            DayTray(
                state,
                onClose = viewModel::closeDay,
                onValueChange = viewModel::onValueChange,
                onAnswer = viewModel::onAnswer,
                onNotesChange = viewModel::onNotesChange,
                onSubmit = viewModel::submit,
            )
        }
    }
}

/**
 * Figma 1891:22050: a task notification's Submit Check in, the due day's tray alone over the inbox. Nothing due (or
 * T3 failed) calls [onNothingDue], so the caller can open the task page instead.
 */
@Composable
fun TaskCheckInTray(
    task: RemoteCareTask,
    onClose: () -> Unit,
    onSubmitted: () -> Unit,
    onNothingDue: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: TaskCheckInViewModel = viewModel { TaskCheckInViewModel(task, openDue = true) },
) {
    val state by viewModel.state.collectAsState()
    val submitted by rememberUpdatedState(onSubmitted)
    val nothingDue by rememberUpdatedState(onNothingDue)
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) { viewModel.submitted.collect { submitted() } }
    LaunchedEffect(viewModel) { viewModel.nothingDue.collect { nothingDue() } }
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { signedOut() } }
    if (state.day != null) {
        val close = {
            viewModel.closeDay()
            if (!state.isSubmitting) onClose()
        }
        DaySheet(onClose = close) {
            DayTray(
                state,
                onClose = close,
                onValueChange = viewModel::onValueChange,
                onAnswer = viewModel::onAnswer,
                onNotesChange = viewModel::onNotesChange,
                onSubmit = viewModel::submit,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskCheckInContent(
    state: TaskCheckInUiState,
    now: Instant,
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    onDay: (LocalDate) -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(Res.string.task), onBack = onBack)
        }
        PullToRefreshBox(state.isLoading, onRefresh, Modifier.weight(1f).background(ShifaColors.Bg.Canvas)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(ShifaSpacing.Inset.Md),
                verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
            ) {
                Banners(state.error.takeIf { state.day == null }, state.notice)
                TaskCard(state.shownTask, now, null, progress = true)
                val dates = monthGrid(state.month)
                val first = state.firstMonth
                val last = state.lastMonth
                ShifaCalendarCard(
                    "${stringArrayResource(Res.array.months)[state.month.monthNumber - 1]} ${state.month.year}",
                    dates.map { ShifaCalendarDay(it.dayOfMonth.toString(), dayState(state, it)) },
                    onDay = { onDay(dates[it]) },
                    onPrev = onPrev.takeIf { first != null && state.month > first },
                    onNext = onNext.takeIf { last != null && state.month < last },
                )
            }
        }
    }
}

/** Monday-first weeks covering [month], padded with the days around it. */
private fun monthGrid(month: LocalDate): List<LocalDate> {
    val lead = month.dayOfWeek.isoDayNumber - 1
    val days = (month.plus(1, DateTimeUnit.MONTH).toEpochDays() - month.toEpochDays()).toInt()
    val weeks = (lead + days + 6) / 7
    return List(weeks * 7) { month.plus(it - lead, DateTimeUnit.DAY) }
}

/** Filled: all of the day's check-ins submitted. Default: one is PENDING. Else (none, or missed) disabled. */
private fun dayState(
    state: TaskCheckInUiState,
    date: LocalDate,
): ShifaDateState {
    if (date.month != state.month.month) return ShifaDateState.NonMonth
    val day = state.on(date)
    return when {
        day.any { it.isPending } -> ShifaDateState.Default
        day.isNotEmpty() && day.all { it.isCompleted } -> ShifaDateState.Filled
        else -> ShifaDateState.Disabled
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) = ModalBottomSheet(
    onDismissRequest = onClose,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape = RectangleShape,
    containerColor = Color.Transparent,
    dragHandle = null,
    contentWindowInsets = { WindowInsets(0) },
) { content() }

/** The day's form for its PENDING check-in, else its submitted ones (read only). */
@Composable
internal fun DayTray(
    state: TaskCheckInUiState,
    onClose: () -> Unit = {},
    onValueChange: (String) -> Unit = {},
    onAnswer: (Boolean) -> Unit = {},
    onNotesChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    val day = state.day ?: return
    val months = stringArrayResource(Res.array.months)
    ShifaTray(stringResource(Res.string.submitCheckIn), onClose, Modifier.navigationBarsPadding().imePadding()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
        ) {
            Text(
                "${day.dayOfMonth} ${months[day.monthNumber - 1]} ${day.year}",
                style = MaterialTheme.typography.titleMedium,
                color = ShifaColors.Text.Primary,
            )
            val entry = state.entry
            if (entry != null) {
                EntryForm(state, entry, onValueChange, onAnswer, onNotesChange, onSubmit)
            } else {
                ShifaBanner(
                    stringResource(Res.string.checkInAlreadySubmitted),
                    Res.drawable.ic_check_16,
                    ShifaBannerType.Success,
                )
                state.submitted.forEach { Submitted(state, it, many = state.submitted.size > 1) }
            }
        }
    }
}

@Composable
private fun DueCard(
    state: TaskCheckInUiState,
    checkIn: TaskCheckIn,
) = TaskCard(
    state.task,
    getCurrentInstant(),
    null,
    due = checkIn.scheduledTime?.take(5)?.let { stringResource(Res.string.dueAt, it) },
)

/** The value by the task's input type, the comment, then Submit Entry (12 §3.2). */
@Composable
private fun ColumnScope.EntryForm(
    state: TaskCheckInUiState,
    entry: TaskCheckIn,
    onValueChange: (String) -> Unit,
    onAnswer: (Boolean) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    DueCard(state, entry)
    val valueError = state.valueError?.let { stringResource(it) }
    if (state.inputType == BOOLEAN) {
        Row(horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
            ShifaChip(stringResource(Res.string.yes), state.answer == true, { onAnswer(true) })
            ShifaChip(stringResource(Res.string.no), state.answer == false, { onAnswer(false) })
        }
        valueError?.let {
            Text(
                it,
                Modifier.padding(horizontal = ShifaSpacing.Inset.Sm),
                style = ShifaTextStyles.caption(),
                color = ShifaColors.Text.Error,
            )
        }
    } else {
        val label = state.task.inputLabel?.trim()?.ifEmpty { null }
        ShifaInputField(
            state.value,
            onValueChange,
            label?.let { stringResource(Res.string.checkInEnterLabeledValue, it) }
                ?: stringResource(Res.string.checkInEnterGenericValue),
            Modifier.fillMaxWidth(),
            supportingText = valueError,
            isError = valueError != null,
            clearable = false,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = if (state.inputType == NUMERIC) KeyboardType.Decimal else KeyboardType.Text,
                ),
        )
    }
    // Figma always has the comment; it's optional unless the task requires notes.
    val notesError = state.notesError?.let { stringResource(it) }
    ShifaInputField(
        state.notes,
        onNotesChange,
        state.task.notesLabel ?: stringResource(Res.string.comment),
        Modifier.fillMaxWidth(),
        supportingText = notesError,
        isError = notesError != null,
        clearable = false,
        multiline = true,
    )
    if (!state.entryDue) {
        Text(
            stringResource(Res.string.taskCheckInNotYetAvailable),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = ShifaColors.Text.Tertiary,
            textAlign = TextAlign.Center,
        )
    }
    Banners(state.error)
    ShifaPrimaryButton(
        stringResource(Res.string.submitEntry),
        onSubmit,
        Modifier.fillMaxWidth(),
        enabled = state.entryDue,
        loading = state.isSubmitting,
    )
}

/** A submitted check-in: its card, value and comment. With [many] on the day, the value says its time. */
@Composable
private fun Submitted(
    state: TaskCheckInUiState,
    checkIn: TaskCheckIn,
    many: Boolean,
) {
    DueCard(state, checkIn)
    val value =
        checkIn.numericValue?.toString()?.removeSuffix(".0")
            ?: checkIn.booleanValue?.let { stringResource(if (it) Res.string.yes else Res.string.no) }
            ?: checkIn.textValue
    val label = state.task.inputLabel?.trim()?.ifEmpty { null } ?: stringResource(Res.string.checkInValue)
    value?.let {
        val time = checkIn.scheduledTime?.take(5)
        EntryField(if (many && time != null) "$time · $label" else label, it)
    }
    checkIn.notes?.ifBlank { null }?.let {
        EntryField(state.task.notesLabel ?: stringResource(Res.string.comment), it, multiline = true)
    }
}

/** Figma input-field, filled and read only. */
@Composable
private fun EntryField(
    label: String,
    text: String,
    multiline: Boolean = false,
) = Column(
    Modifier
        .fillMaxWidth()
        .then(if (multiline) Modifier.heightIn(min = ShifaDimens.TextAreaHeight) else Modifier)
        .background(ShifaColors.Bg.Surface, ShifaRadius.ControlMd)
        .border(ShifaSize.S2, ShifaColors.Border.Default, ShifaRadius.ControlMd)
        .padding(horizontal = ShifaSpacing.Inset.Sm, vertical = ShifaSpacing.Inset.Xs),
) {
    Text(label, style = ShifaTextStyles.caption(), color = ShifaColors.Text.Tertiary)
    Text(text, style = MaterialTheme.typography.bodyLarge, color = ShifaColors.Text.Primary)
}
