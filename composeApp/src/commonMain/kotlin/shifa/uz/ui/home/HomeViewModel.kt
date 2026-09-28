package shifa.uz.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.doctors.doctorsOf
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.notifications.NotificationsRepository
import shifa.uz.data.profile.ProfileRepository
import shifa.uz.data.tasks.TasksRepository
import shifa.uz.utils.getCurrentInstant

class HomeViewModel(
    private val profile: ProfileRepository = ProfileRepository(),
    private val bookings: BookingsRepository = BookingsRepository(),
    private val doctors: DoctorsRepository = DoctorsRepository(),
    private val tasks: TasksRepository = TasksRepository(),
    private val notifications: NotificationsRepository = NotificationsRepository(),
    private val clock: () -> Instant = ::getCurrentInstant,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    // P1 answered 401: the token is gone. ponytail: only Home reacts; the app-wide 401 handler is CORE-05.
    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private var loading: Job? = null

    init {
        load(refreshing = false)
    }

    /** Pull to refresh: the lists stay on screen meanwhile. */
    fun refresh() = load(refreshing = true)

    private fun update(transform: HomeUiState.() -> HomeUiState) {
        _state.value = _state.value.transform()
    }

    private fun load(refreshing: Boolean) {
        if (loading?.isActive == true) return
        update { copy(isLoading = !refreshing, isRefreshing = refreshing) }
        loading =
            viewModelScope.launch {
                val errors =
                    listOf(
                        async { loadProfile() },
                        async { loadUnread() },
                        async { loadAppointments() },
                        async { loadTasks() },
                    ).awaitAll()
                update { copy(isLoading = false, isRefreshing = false, error = errors.firstNotNullOfOrNull { it }) }
            }
    }

    // A failed P1 only costs the name in the greeting (10 §3.3.1).
    private suspend fun loadProfile(): StringResource? {
        profile.getProfile().fold(
            onSuccess = {
                val firstName = it.fullName.trim().split(Regex("\\s+")).first().ifEmpty { null }
                update {
                    copy(
                        firstName = firstName,
                        fullName = it.fullName.trim().ifEmpty { null },
                        photoUrl = it.photoUrl,
                    )
                }
            },
            onFailure = { if (it is ApiException.Unauthorized) _signedOut.send(Unit) },
        )
        return null
    }

    // N2: no badge when the count fails.
    private suspend fun loadUnread(): StringResource? {
        val count = notifications.unreadCount().getOrDefault(0)
        update { copy(unreadNotifications = count) }
        return null
    }

    // One unfiltered B2 feeds both sections. Unlike Flutter (start > now), a confirmed visit stays until its
    // join window closes, so Home can show "Now" and Join (06).
    private suspend fun loadAppointments(): StringResource? {
        val all = bookings.getAppointments().getOrElse { return it.toUserMessage() }
        val now = clock()
        update { copy(upcoming = all.filter { it.isConfirmed && !it.isOver(now) }.sortedBy { it.start }) }
        return loadDoctors(all)
    }

    private suspend fun loadDoctors(all: List<Appointment>): StringResource? {
        val found = doctors.doctorsOf(all, HOME_ITEMS).getOrElse { return it.toUserMessage() }
        update { copy(doctors = found) }
        return null
    }

    private suspend fun loadTasks(): StringResource? {
        val all = tasks.getMyTasks().getOrElse { return it.toUserMessage() }
        update { copy(tasks = all.filter(RemoteCareTask::isOpen)) }
        return null
    }
}
