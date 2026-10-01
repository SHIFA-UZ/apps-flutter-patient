package shifa.uz.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.additionalNotes
import shifa.composeapp.generated.resources.checkInEnterGenericValue
import shifa.composeapp.generated.resources.checkInEnterLabeledValue
import shifa.composeapp.generated.resources.no
import shifa.composeapp.generated.resources.noPendingCheckIns
import shifa.composeapp.generated.resources.selectCheckIn
import shifa.composeapp.generated.resources.submitCheckIn
import shifa.composeapp.generated.resources.taskCheckIn
import shifa.composeapp.generated.resources.taskCheckInNotYetAvailable
import shifa.composeapp.generated.resources.yes
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.ui.home.HomeSection
import shifa.uz.ui.home.TaskCard
import shifa.uz.ui.register.Banners
import shifa.uz.ui.tasks.TaskCheckInUiState.Companion.BOOLEAN
import shifa.uz.ui.tasks.TaskCheckInUiState.Companion.NUMERIC
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.widgets.ShifaActionButton
import shifa.uz.ui.widgets.ShifaCheckInCard
import shifa.uz.ui.widgets.ShifaChip
import shifa.uz.ui.widgets.ShifaInputField
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.formatDay
import shifa.uz.utils.getCurrentInstant

/** 12 §3.2 (TASK-02), Figma 1662:18317, over the tasks list. On success the list shows the notice. */
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
    TaskCheckInContent(
        state,
        getCurrentInstant(),
        onBack = onBack,
        onRefresh = viewModel::load,
        onSelect = viewModel::select,
        onValueChange = viewModel::onValueChange,
        onAnswer = viewModel::onAnswer,
        onNotesChange = viewModel::onNotesChange,
        onSubmit = viewModel::submit,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskCheckInContent(
    state: TaskCheckInUiState,
    now: Instant,
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSelect: (Long, Boolean) -> Unit = { _, _ -> },
    onValueChange: (String) -> Unit = {},
    onAnswer: (Boolean) -> Unit = {},
    onNotesChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding().imePadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(Res.string.taskCheckIn), onBack = onBack)
        }
        PullToRefreshBox(state.isLoading && state.slots != null, onRefresh, Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(ShifaSpacing.Inset.Md),
                verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
            ) {
                TaskCard(state.task, now, null, progress = true)
                // With a slot open, the error shows above its Submit instead.
                Banners(state.error.takeIf { state.selected == null })
                // The due slots, then the next ones greyed; with none due, the 10-minute rule is spelled out under them.
                val nothingDue = state.slots?.isEmpty() == true && state.upcoming.isNotEmpty()
                HomeSection(
                    Res.string.selectCheckIn,
                    state.slots?.let { it + state.upcoming },
                    state.isLoading,
                    if (state.waiting) Res.string.taskCheckInNotYetAvailable else Res.string.noPendingCheckIns,
                    limit = Int.MAX_VALUE,
                ) { slot ->
                    ShifaCheckInCard(
                        formatDay(slot.scheduledDate),
                        slot.scheduledTime?.take(5),
                        slot.id == state.selected,
                        { onSelect(slot.id, it) },
                        enabled = slot !in state.upcoming,
                    ) { CheckInForm(state, onValueChange, onAnswer, onNotesChange, onSubmit) }
                }
                if (nothingDue) {
                    Text(
                        stringResource(Res.string.taskCheckInNotYetAvailable),
                        Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShifaColors.Text.Tertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** The value by the task's input type, the notes when asked for, then Submit (12 §3.2). */
@Composable
private fun ColumnScope.CheckInForm(
    state: TaskCheckInUiState,
    onValueChange: (String) -> Unit,
    onAnswer: (Boolean) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
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
    if (state.showsNotes) {
        val notesError = state.notesError?.let { stringResource(it) }
        ShifaInputField(
            state.notes,
            onNotesChange,
            state.task.notesLabel ?: stringResource(Res.string.additionalNotes),
            Modifier.fillMaxWidth(),
            supportingText = notesError,
            isError = notesError != null,
            clearable = false,
        )
    }
    Banners(state.error)
    ShifaActionButton(
        onSubmit,
        Modifier.fillMaxWidth(),
        text = stringResource(Res.string.submitCheckIn),
        enabled = !state.isSubmitting,
    )
}
