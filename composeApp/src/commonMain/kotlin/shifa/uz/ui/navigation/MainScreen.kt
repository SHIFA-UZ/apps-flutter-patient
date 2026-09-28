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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.comingSoon
import shifa.composeapp.generated.resources.myTasks
import shifa.composeapp.generated.resources.notifications
import shifa.composeapp.generated.resources.profile
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.PatientDocument
import shifa.uz.ui.bookings.AppointmentDetailsScreen
import shifa.uz.ui.bookings.BookingsList
import shifa.uz.ui.bookings.BookingsListScreen
import shifa.uz.ui.bookings.BookingsScreen
import shifa.uz.ui.bookings.BookingsViewModel
import shifa.uz.ui.bookings.CreateBookingScreen
import shifa.uz.ui.doctors.DoctorProfileScreen
import shifa.uz.ui.doctors.LocalDeviceLocation
import shifa.uz.ui.documents.DocumentViewerScreen
import shifa.uz.ui.documents.DocumentsScreen
import shifa.uz.ui.documents.DocumentsViewModel
import shifa.uz.ui.home.HomeScreen
import shifa.uz.ui.home.HomeViewModel
import shifa.uz.ui.profile.ProfileScreen
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaSubHeader

// Pages over the tabs. ponytail: all but Account are placeholders, each becomes a real screen with its ticket.
private enum class Page(val title: StringResource) {
    Account(Res.string.profile), // PROF-01
    Notifications(Res.string.notifications), // NOTIF-01
    Tasks(Res.string.myTasks), // TASK-01
}

/** The signed-in shell (02): tabs under the floating nav bar, pages over it. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    onLogOut: () -> Unit,
    onAccountDeleted: () -> Unit,
) {
    // ViewModels live as long as this session, so nothing of it outlives a logout.
    ScopedViewModels {
        val home = viewModel { HomeViewModel() }
        val documents = viewModel { DocumentsViewModel() }
        val bookings = viewModel { BookingsViewModel() }
        val homeState by home.state.collectAsState()
        val nearMe = bookings.state.collectAsState().value.location
        var tab by rememberSaveable { mutableStateOf(ShifaTab.Home) }
        var page by rememberSaveable { mutableStateOf<Page?>(null) }
        var list by rememberSaveable { mutableStateOf<BookingsList?>(null) }
        // ponytail: not saved, so the viewer closes on process death; a nav library restores it (deferred).
        var viewing by remember { mutableStateOf<PatientDocument?>(null) }
        var appointment by remember { mutableStateOf<Appointment?>(null) }
        var doctor by remember { mutableStateOf<Doctor?>(null) }
        var booking by remember { mutableStateOf<Doctor?>(null) }
        val tabs = rememberSaveableStateHolder()
        val density = LocalDensity.current
        var navPadding by remember { mutableStateOf(PaddingValues()) }
        BackHandler(
            enabled =
                page == null && list == null && viewing == null && appointment == null && doctor == null && booking == null &&
                    tab != ShifaTab.Home,
        ) { tab = ShifaTab.Home }
        CompositionLocalProvider(LocalDeviceLocation provides nearMe) {
            Box(Modifier.fillMaxSize()) {
                tabs.SaveableStateProvider(tab) {
                    when (tab) {
                        ShifaTab.Home ->
                            HomeScreen(
                                onNotifications = { page = Page.Notifications },
                                onAccount = { page = Page.Account },
                                onBookings = { tab = ShifaTab.Bookings },
                                onAppointment = { appointment = it },
                                onTasks = { page = Page.Tasks },
                                onDoctors = { list = BookingsList.MyDoctors },
                                onDoctor = { doctor = it },
                                onSignedOut = onLogOut,
                                contentPadding = navPadding,
                                viewModel = home,
                            )
                        ShifaTab.Documents ->
                            DocumentsScreen(
                                avatarUrl = homeState.photoUrl,
                                patientName = homeState.fullName,
                                notificationCount = homeState.unreadNotifications,
                                onNotifications = { page = Page.Notifications },
                                onAccount = { page = Page.Account },
                                onOpen = { viewing = it },
                                onSignedOut = onLogOut,
                                contentPadding = navPadding,
                                viewModel = documents,
                            )
                        ShifaTab.Bookings ->
                            BookingsScreen(
                                avatarUrl = homeState.photoUrl,
                                notificationCount = homeState.unreadNotifications,
                                onNotifications = { page = Page.Notifications },
                                onAccount = { page = Page.Account },
                                onAppointment = { appointment = it },
                                onDoctor = { doctor = it },
                                onList = { list = it },
                                onSignedOut = onLogOut,
                                contentPadding = navPadding,
                                viewModel = bookings,
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
                // Overlays, later ones on top: a full list → details → its doctor → Create Booking → a page → a certificate / document.
                list?.let {
                    BackHandler { list = null }
                    BookingsListScreen(
                        it,
                        bookings,
                        onBack = { list = null },
                        onDoctor = { doctor = it },
                        onAppointment = { appointment = it },
                        onSignedOut = onLogOut,
                    )
                }
                appointment?.let {
                    BackHandler { appointment = null }
                    key(it) {
                        ScopedViewModels {
                            AppointmentDetailsScreen(
                                it,
                                onBack = { appointment = null },
                                onDoctor = { doctor = it },
                                onCancelled = {
                                    appointment = null
                                    bookings.onCancelled()
                                    home.refresh()
                                },
                                onSignedOut = onLogOut,
                            )
                        }
                    }
                }
                doctor?.let {
                    BackHandler { doctor = null }
                    key(it) {
                        ScopedViewModels {
                            DoctorProfileScreen(
                                it,
                                onBack = { doctor = null },
                                onCertificate = { viewing = it },
                                onBook = { booking = it },
                                onSignedOut = onLogOut,
                            )
                        }
                    }
                }
                booking?.let {
                    BackHandler { booking = null }
                    key(it) {
                        ScopedViewModels {
                            CreateBookingScreen(
                                it,
                                onBack = { booking = null },
                                onBooked = { pending ->
                                    // 06 §3.1: then /bookings.
                                    booking = null
                                    doctor = null
                                    appointment = null
                                    list = null
                                    tab = ShifaTab.Bookings
                                    bookings.onBooked(pending)
                                    home.refresh()
                                },
                                onSignedOut = onLogOut,
                            )
                        }
                    }
                }
                page?.let {
                    BackHandler { page = null }
                    // Each page gets fresh ViewModels, cleared when it closes.
                    key(it) {
                        ScopedViewModels {
                            if (it == Page.Account) {
                                ProfileScreen(
                                    onBack = { page = null },
                                    onLogOut = onLogOut,
                                    onAccountDeleted = onAccountDeleted,
                                    onProfileChanged = home::refresh,
                                )
                            } else {
                                ComingSoon(stringResource(it.title), onBack = { page = null })
                            }
                        }
                    }
                }
                viewing?.let {
                    BackHandler { viewing = null }
                    key(it) {
                        ScopedViewModels {
                            DocumentViewerScreen(it, homeState.fullName, onBack = { viewing = null }, onDeleted = {
                                viewing = null
                                documents.onDeleted(it.id)
                            })
                        }
                    }
                }
            }
        }
    }
}

/** [content]'s ViewModels are cleared when it leaves the composition. ponytail: a nav library does this per destination (deferred). */
@Composable
internal fun ScopedViewModels(content: @Composable () -> Unit) {
    val owner =
        remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}

@Composable
internal fun ComingSoon(
    title: String,
    onBack: (() -> Unit)? = null,
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
        }
    }
}
