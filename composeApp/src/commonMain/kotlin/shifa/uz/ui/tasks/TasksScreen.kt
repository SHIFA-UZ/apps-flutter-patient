package shifa.uz.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.myTasks
import shifa.composeapp.generated.resources.noTasksFound
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.ui.home.HomeSection
import shifa.uz.ui.home.TaskCard
import shifa.uz.ui.register.Banners
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaActionButton
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.getCurrentInstant

/**
 * 12 §3.1 (TASK-01), Figma 1655:17107 (Open) and 1662:18016 (Completed). Loads on each open.
 * ponytail: no reload on app resume; pull to refresh covers it. The ⋮ menu has no Figma, so it isn't built.
 */
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onBack: () -> Unit,
    onTask: (RemoteCareTask) -> Unit,
    onSignedOut: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val signedOut by rememberUpdatedState(onSignedOut)
    var tab by rememberSaveable { mutableStateOf(TasksTab.Open) }
    LaunchedEffect(viewModel) {
        viewModel.open()
        viewModel.signedOut.collect { signedOut() }
    }
    TasksContent(state, tab, getCurrentInstant(), onBack, { tab = it }, viewModel::load, onTask)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TasksContent(
    state: TasksUiState,
    tab: TasksTab,
    now: Instant,
    onBack: () -> Unit = {},
    onTab: (TasksTab) -> Unit = {},
    onRefresh: () -> Unit = {},
    onTask: (RemoteCareTask) -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(Res.string.myTasks), onBack = onBack)
        }
        ShifaPageElement(ShifaPageElementType.ActionTray) {
            Row(horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
                TasksTab.entries.forEach {
                    ShifaActionButton(
                        { onTab(it) },
                        Modifier.weight(1f),
                        text = stringResource(it.title),
                        secondary = it != tab,
                    )
                }
            }
        }
        // The empty tab pulls to refresh too (12 quirk 2).
        PullToRefreshBox(
            state.isLoading && state.tasks != null,
            onRefresh,
            Modifier.weight(1f).background(ShifaColors.Bg.Canvas),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(ShifaSpacing.Inset.Md),
                verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
            ) {
                Banners(state.error, state.notice)
                // Figma: the Completed cards have no progress bar.
                HomeSection(
                    null,
                    state.tasksIn(tab),
                    state.isLoading,
                    Res.string.noTasksFound,
                    limit = Int.MAX_VALUE,
                ) { TaskCard(it, now, { onTask(it) }, progress = tab == TasksTab.Open) }
            }
        }
    }
}
