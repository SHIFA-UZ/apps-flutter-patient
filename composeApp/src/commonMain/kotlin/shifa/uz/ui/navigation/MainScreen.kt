package shifa.uz.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.appointmentDetails
import shifa.composeapp.generated.resources.comingSoon
import shifa.composeapp.generated.resources.logOut
import shifa.composeapp.generated.resources.myDoctors
import shifa.composeapp.generated.resources.myTasks
import shifa.composeapp.generated.resources.notifications
import shifa.composeapp.generated.resources.profile
import shifa.uz.ui.home.HomeScreen
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSubHeader

// ponytail: pages Home links to that aren't built yet. Each becomes a real screen with its ticket.
private enum class Page(val title: StringResource) {
    Account(Res.string.profile), // AUTH-08 / PROF-01; holds Log out meanwhile
    Notifications(Res.string.notifications), // NOTIF-01
    Tasks(Res.string.myTasks), // TASK-01
    Doctors(Res.string.myDoctors), // DOC-01, also a doctor's page
    Appointment(Res.string.appointmentDetails), // BOOK-02, also Join until the video call
}

/** The signed-in shell (02): tabs under the floating nav bar, pages over it. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(onLogOut: () -> Unit) {
    // ViewModels live as long as this session, so nothing of it outlives a logout.
    // ponytail: hand-rolled; a nav library scopes them per destination (deferred).
    val owner = remember { SessionViewModels() }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        var tab by rememberSaveable { mutableStateOf(ShifaTab.Home) }
        var page by rememberSaveable { mutableStateOf<Page?>(null) }
        val tabs = rememberSaveableStateHolder()
        val density = LocalDensity.current
        var navPadding by remember { mutableStateOf(PaddingValues()) }
        BackHandler(enabled = page == null && tab != ShifaTab.Home) { tab = ShifaTab.Home }
        Box(Modifier.fillMaxSize()) {
            tabs.SaveableStateProvider(tab) {
                when (tab) {
                    ShifaTab.Home ->
                        HomeScreen(
                            onNotifications = { page = Page.Notifications },
                            onAccount = { page = Page.Account },
                            onBookings = { tab = ShifaTab.Bookings },
                            onAppointment = { page = Page.Appointment },
                            onTasks = { page = Page.Tasks },
                            onDoctors = { page = Page.Doctors },
                            onDoctor = { page = Page.Doctors },
                            onSignedOut = onLogOut,
                            contentPadding = navPadding,
                        )
                    else -> ComingSoon(stringResource(tab.label))
                }
            }
            ShifaNavBar(
                tab,
                { tab = it },
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { navPadding = PaddingValues(bottom = with(density) { it.height.toDp() }) }
                    .navigationBarsPadding()
                    .padding(ShifaSpacing.Inset.Md),
            )
            page?.let {
                BackHandler { page = null }
                ComingSoon(stringResource(it.title), onBack = { page = null }) {
                    if (it == Page.Account) ShifaSecondaryButton(stringResource(Res.string.logOut), onLogOut)
                }
            }
        }
    }
}

private class SessionViewModels : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

@Composable
private fun ComingSoon(
    title: String,
    onBack: (() -> Unit)? = null,
    action: @Composable () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaSubHeader(title, onBack = onBack)
        Column(
            Modifier.fillMaxSize().padding(ShifaSpacing.Inset.Lg),
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Lg, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(Res.string.comingSoon),
                style = MaterialTheme.typography.bodyMedium,
                color = ShifaColors.Text.Tertiary,
            )
            action()
        }
    }
}
