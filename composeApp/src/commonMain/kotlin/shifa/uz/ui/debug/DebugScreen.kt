package shifa.uz.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_check_circle_16
import shifa.composeapp.generated.resources.ic_info_16
import shifa.uz.data.api.ApiConfig
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaBanner
import shifa.uz.ui.widgets.ShifaBannerType
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSection
import shifa.uz.ui.widgets.ShifaSectionType
import shifa.uz.ui.widgets.ShifaSettingsItem
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.ui.widgets.ShifaSwitch
import shifa.uz.utils.rememberLocalPush

// ponytail: English only, as only debug builds have this screen.
@Composable
fun DebugScreen(
    onBack: () -> Unit,
    viewModel: DebugViewModel = viewModel { DebugViewModel() },
) {
    val state by viewModel.state.collectAsState()
    val push = rememberLocalPush()
    DebugContent(
        state,
        onBack = onBack,
        onDemoChange = viewModel::onDemoChange,
        onDevelopmentChange = viewModel::onDevelopmentChange,
        onReset = viewModel::resetDemoData,
        onPush = { viewModel.onPushed(push(it.label, it.message, it.data)) },
    )
}

@Composable
internal fun DebugContent(
    state: DebugUiState,
    onBack: () -> Unit = {},
    onDemoChange: (Boolean) -> Unit = {},
    onDevelopmentChange: (Boolean) -> Unit = {},
    onReset: () -> Unit = {},
    onPush: (FakePush) -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) { ShifaSubHeader("Debug menu", onBack = onBack) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            ShifaPageElement(ShifaPageElementType.Body) {
                Column(verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md)) {
                    state.notice?.let {
                        ShifaBanner(
                            it,
                            if (state.noticeIsError) Res.drawable.ic_info_16 else Res.drawable.ic_check_circle_16,
                            if (state.noticeIsError) ShifaBannerType.Warning else ShifaBannerType.Success,
                        )
                    }
                    ShifaSection(
                        "Development backend",
                        type = ShifaSectionType.CardOnColor,
                        action = { ShifaSwitch(state.development, onDevelopmentChange) },
                    ) {
                        Note(
                            "Every call goes to ${ApiConfig.DEV_BASE_URL.removeSuffix("/api/")} instead of " +
                                "production. Its data is test data: sign in with seed.patient1@shifa.dev / " +
                                "Dev12345!. Switching signs you out and restarts the app's screens.",
                        )
                    }
                    ShifaSection(
                        "Demo data",
                        type = ShifaSectionType.CardOnColor,
                        action = { ShifaSwitch(state.demo, onDemoChange) },
                    ) {
                        Note(
                            "A fake backend: a video visit you can join now, a pending and an unpaid visit, " +
                                "check-ins due now, unread notifications and chats, and two demo doctors. Booking, " +
                                "check-ins, cancel, chat and uploads change only demo data. Reads it doesn't fake " +
                                "come from the real backend, and other writes are refused. Switching restarts the " +
                                "app's screens.",
                        )
                        ShifaSecondaryButton("Reset demo data", onReset, Modifier.fillMaxWidth(), enabled = state.demo)
                    }
                    ShifaSection("Fake pushes", type = ShifaSectionType.CardOnColor) {
                        Note(
                            "Each shows as a real notification; tap it to test where it leads. It links to the first " +
                                "visit, task, document or chat of the current backend.",
                        )
                        Column {
                            state.pushes.forEach { push ->
                                val id =
                                    push.link?.let {
                                            (key, value) ->
                                        " · ${key.removeSuffix("Id")} ${value ?: "none"}"
                                    }.orEmpty()
                                ShifaSettingsItem(push.label + id, { onPush(push) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) =
    Text(text, style = MaterialTheme.typography.bodyMedium, color = ShifaColors.Text.Tertiary)
