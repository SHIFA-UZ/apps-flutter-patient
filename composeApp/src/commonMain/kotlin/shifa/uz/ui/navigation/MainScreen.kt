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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.comingSoon
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.PatientDocument
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.ui.bookings.AppointmentDetailsScreen
import shifa.uz.ui.bookings.BookingsList
import shifa.uz.ui.bookings.BookingsListScreen
import shifa.uz.ui.bookings.BookingsScreen
import shifa.uz.ui.bookings.BookingsViewModel
import shifa.uz.ui.bookings.CreateBookingScreen
import shifa.uz.ui.bookings.RescheduleScreen
import shifa.uz.ui.doctors.DoctorProfileScreen
import shifa.uz.ui.documents.DocumentViewerScreen
import shifa.uz.ui.documents.DocumentsScreen
import shifa.uz.ui.documents.DocumentsViewModel
import shifa.uz.ui.home.HomeScreen
import shifa.uz.ui.home.HomeViewModel
import shifa.uz.ui.home.UpcomingScreen
import shifa.uz.ui.messages.ChatTarget
import shifa.uz.ui.messages.ConversationScreen
import shifa.uz.ui.messages.MessagesScreen
import shifa.uz.ui.messages.MessagesViewModel
import shifa.uz.ui.notifications.NotificationLink
import shifa.uz.ui.notifications.NotificationsScreen
import shifa.uz.ui.notifications.PushViewModel
import shifa.uz.ui.profile.ProfileScreen
import shifa.uz.ui.shifaai.ShifaAiChatScreen
import shifa.uz.ui.shifaai.ShifaAiScreen
import shifa.uz.ui.shifaai.ShifaAiViewModel
import shifa.uz.ui.tasks.TaskCheckInScreen
import shifa.uz.ui.tasks.TasksScreen
import shifa.uz.ui.tasks.TasksViewModel
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.visit.LiveAppointmentScreen
import shifa.uz.ui.visit.VideoCallScreen
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.PushSetup

// Pages over the tabs.
private enum class Page { Account, Notifications }

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
        val tasks = viewModel { TasksViewModel() }
        val push = viewModel { PushViewModel() }
        val shifaAi = viewModel { ShifaAiViewModel() }
        val messages = viewModel { MessagesViewModel() }
        val homeState by home.state.collectAsState()
        val aiState by shifaAi.state.collectAsState()
        var tab by rememberSaveable { mutableStateOf(ShifaTab.Home) }
        var page by rememberSaveable { mutableStateOf<Page?>(null) }
        var list by rememberSaveable { mutableStateOf<BookingsList?>(null) }
        var tasksOpen by rememberSaveable { mutableStateOf(false) }
        var upcomingOpen by rememberSaveable { mutableStateOf(false) }
        // ponytail: not saved, so the viewer closes on process death; a nav library restores it (deferred).
        var viewing by remember { mutableStateOf<PatientDocument?>(null) }
        var checkIn by remember { mutableStateOf<RemoteCareTask?>(null) }
        var appointment by remember { mutableStateOf<Appointment?>(null) }
        var live by remember { mutableStateOf<Appointment?>(null) }
        var call by remember { mutableStateOf<Appointment?>(null) }
        var callMinimized by remember { mutableStateOf(false) }
        var doctor by remember { mutableStateOf<Doctor?>(null) }
        var booking by remember { mutableStateOf<Doctor?>(null) }
        var rescheduling by remember { mutableStateOf<Appointment?>(null) }
        var chat by remember { mutableStateOf<ChatTarget?>(null) }
        val tabs = rememberSaveableStateHolder()
        val density = LocalDensity.current
        var navPadding by remember { mutableStateOf(PaddingValues()) }
        val follow = { link: NotificationLink ->
            page = null
            when (link) {
                is NotificationLink.ToAppointment -> appointment = link.appointment
                is NotificationLink.ToDocument -> viewing = link.document
                is NotificationLink.ToTask -> checkIn = link.task
                is NotificationLink.ToSigning -> live = link.appointment
                NotificationLink.ToBookings -> tab = ShifaTab.Bookings
                NotificationLink.ToDocuments -> tab = ShifaTab.Documents
                NotificationLink.ToTasks -> tasksOpen = true
                is NotificationLink.ToChat -> chat = ChatTarget(link.chatId)
                NotificationLink.ToMessages -> tab = ShifaTab.Messages
            }
        }
        PushSetup()
        LaunchedEffect(push) {
            launch {
                push.stale.collect {
                    home.refresh()
                    messages.load()
                }
            }
            push.open.collect { link ->
                // 13 §3.3: like Flutter's go, the target replaces whatever is open.
                list = null
                upcomingOpen = false
                tasksOpen = false
                checkIn = null
                appointment = null
                live = null
                // A call goes on in its small window.
                if (call != null) callMinimized = true
                doctor = null
                booking = null
                rescheduling = null
                viewing = null
                chat = null
                shifaAi.close()
                link?.let(follow) ?: run { page = Page.Notifications }
            }
        }
        BackHandler(
            enabled =
                page == null && list == null && !upcomingOpen && !tasksOpen && checkIn == null && viewing == null &&
                    appointment == null && live == null && (call == null || callMinimized) && doctor == null &&
                    booking == null && rescheduling == null && aiState.chat == null && chat == null &&
                    tab != ShifaTab.Home,
        ) { tab = ShifaTab.Home }
        // 09 §7: the tab goes when the profile shuts Shifa AI out.
        LaunchedEffect(homeState.shifaAi) {
            if (!homeState.shifaAi) {
                if (tab == ShifaTab.ShifaAi) tab = ShifaTab.Home
                shifaAi.close()
            }
        }
        Box(Modifier.fillMaxSize()) {
            tabs.SaveableStateProvider(tab) {
                when (tab) {
                    ShifaTab.Home ->
                        HomeScreen(
                            onNotifications = { page = Page.Notifications },
                            onAccount = { page = Page.Account },
                            onUpcoming = { upcomingOpen = true },
                            onAppointment = { appointment = it },
                            onLive = { live = it },
                            onTasks = { tasksOpen = true },
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
                    ShifaTab.ShifaAi ->
                        ShifaAiScreen(
                            shifaAi,
                            avatarUrl = homeState.photoUrl,
                            notificationCount = homeState.unreadNotifications,
                            onNotifications = { page = Page.Notifications },
                            onAccount = { page = Page.Account },
                            contentPadding = navPadding,
                        )
                    ShifaTab.Messages ->
                        MessagesScreen(
                            avatarUrl = homeState.photoUrl,
                            notificationCount = homeState.unreadNotifications,
                            onNotifications = { page = Page.Notifications },
                            onAccount = { page = Page.Account },
                            onOpen = { chat = it },
                            onSignedOut = onLogOut,
                            contentPadding = navPadding,
                            viewModel = messages,
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
                ShifaTab.entries.filter { it != ShifaTab.ShifaAi || homeState.shifaAi },
            )
            // Overlays, later ones on top: a Shifa AI chat → a doctor's chat → a full list → Upcoming → My Tasks → a check-in → details → a live visit → its doctor → Create Booking → a page → a certificate / document → a call.
            if (aiState.chat != null) {
                BackHandler { shifaAi.close() }
                ShifaAiChatScreen(shifaAi, onBack = shifaAi::close, onSignedOut = onLogOut)
            }
            chat?.let {
                // Back on the list, its unread counts and last messages are refetched.
                val close = {
                    chat = null
                    messages.load()
                }
                BackHandler { close() }
                key(it) { ScopedViewModels { ConversationScreen(it, onBack = close, onSignedOut = onLogOut) } }
            }
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
            if (upcomingOpen) {
                BackHandler { upcomingOpen = false }
                UpcomingScreen(home, onBack = { upcomingOpen = false }, onAppointment = { appointment = it })
            }
            if (tasksOpen) {
                BackHandler { tasksOpen = false }
                TasksScreen(
                    tasks,
                    onBack = { tasksOpen = false },
                    onTask = { checkIn = it },
                    onSignedOut = onLogOut,
                )
            }
            checkIn?.let {
                BackHandler { checkIn = null }
                key(it) {
                    ScopedViewModels {
                        TaskCheckInScreen(
                            it,
                            onBack = { checkIn = null },
                            onSubmitted = {
                                tasks.onSubmitted()
                                home.refresh()
                            },
                            onSignedOut = onLogOut,
                        )
                    }
                }
            }
            appointment?.let {
                BackHandler { appointment = null }
                key(it) {
                    ScopedViewModels {
                        AppointmentDetailsScreen(
                            it,
                            onBack = { appointment = null },
                            onDoctor = { doctor = it },
                            onReschedule = { rescheduling = it },
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
            rescheduling?.let {
                BackHandler { rescheduling = null }
                key(it) {
                    ScopedViewModels {
                        RescheduleScreen(
                            it,
                            onBack = { rescheduling = null },
                            onRescheduled = {
                                rescheduling = null
                                appointment = null
                                list = null
                                tab = ShifaTab.Bookings
                                bookings.onRescheduled()
                                home.refresh()
                            },
                            onSignedOut = onLogOut,
                        )
                    }
                }
            }
            live?.let {
                BackHandler { live = null }
                key(it) {
                    ScopedViewModels {
                        LiveAppointmentScreen(
                            it,
                            onBack = { live = null },
                            onDoctor = { doctor = it },
                            onJoin = {
                                call = it
                                callMinimized = false
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
                            // Figma 1825:17774: paid, the visit's details with Payment Completed.
                            onPaid = { paid ->
                                booking = null
                                doctor = null
                                list = null
                                appointment = paid
                                bookings.load()
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
                        when (it) {
                            Page.Account ->
                                ProfileScreen(
                                    onBack = { page = null },
                                    onLogOut = onLogOut,
                                    onAccountDeleted = onAccountDeleted,
                                    onProfileChanged = home::onProfileChanged,
                                )
                            Page.Notifications ->
                                NotificationsScreen(
                                    homeState.fullName,
                                    onBack = { page = null },
                                    onLink = follow,
                                    onRead = home::refresh,
                                    onCheckInSubmitted = {
                                        tasks.onSubmitted()
                                        home.refresh()
                                    },
                                    onSignedOut = onLogOut,
                                )
                        }
                    }
                }
            }
            viewing?.let {
                BackHandler { viewing = null }
                key(it) {
                    ScopedViewModels {
                        DocumentViewerScreen(
                            it,
                            homeState.fullName,
                            onBack = { viewing = null },
                            onDeleted = {
                                viewing = null
                                documents.onDeleted(it.id)
                            },
                            // The details sit under the viewer, so the viewer closes to show them.
                            onAppointment = { visit ->
                                viewing = null
                                appointment = visit
                            },
                        )
                    }
                }
            }
            // Over everything, full screen or in its small window; it handles its own Back.
            call?.let {
                key(it) {
                    ScopedViewModels {
                        VideoCallScreen(
                            it,
                            minimized = callMinimized,
                            onMinimize = { callMinimized = true },
                            onExpand = { callMinimized = false },
                            onClose = {
                                call = null
                                callMinimized = false
                            },
                            onSignedOut = onLogOut,
                        )
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
            Modifier.fillMaxSize().background(ShifaColors.Bg.Canvas).padding(ShifaSpacing.Inset.Lg),
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
