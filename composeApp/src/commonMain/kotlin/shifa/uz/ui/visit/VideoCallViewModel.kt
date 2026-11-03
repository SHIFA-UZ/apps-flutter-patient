package shifa.uz.ui.visit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.errorVideoCallMediaPermissionRequired
import shifa.composeapp.generated.resources.failedToStartVideoCall
import shifa.uz.data.analytics.Analytics
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.AnalyticsTracker
import shifa.uz.data.analytics.analyticsReason
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.data.video.VideoRepository
import shifa.uz.utils.VideoCall
import shifa.uz.utils.VideoCallState
import shifa.uz.utils.getCurrentInstant

/**
 * Figma 1791:19370 (preview, Join Now) and 1793:15925 (waiting room): V1's token, then the Daily [call], which this
 * ViewModel owns and frees. The page closes when the call ends, from here or by the doctor.
 * ponytail: no join-timeout retry or app-lock pause (06 §3.6); add them with LOCK-01 and if joins time out.
 */
class VideoCallViewModel(
    appointment: Appointment,
    val call: VideoCall,
    private val videos: VideoRepository = VideoRepository(),
    private val analytics: AnalyticsTracker = Analytics,
    private val clock: () -> Instant = ::getCurrentInstant,
) : ViewModel() {
    private val _state = MutableStateFlow(VideoCallUiState(appointment))

    // events.md §6: the call's timeline, for join_ms, waited_s and duration_s.
    private var joinTappedAt: Instant? = null
    private var joinedAt: Instant? = null
    private var leftByPatient = false
    private var wasMinimized = false
    private var ended = false
    private var failedInCall = false
    val state: StateFlow<VideoCallUiState> = _state.asStateFlow()

    private val _closed = Channel<Unit>(Channel.CONFLATED)
    val closed: Flow<Unit> = _closed.receiveAsFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private var released = false

    private fun update(transform: VideoCallUiState.() -> VideoCallUiState) {
        _state.value = _state.value.transform()
    }

    init {
        analytics.track(
            AnalyticsEvent.VideoCallOpened(
                minutesToStart = appointment.start?.let { (it - clock()).inWholeMinutes },
                isVideo = appointment.isVideo,
            ),
        )
        viewModelScope.launch {
            call.state.collect {
                val before = _state.value.call
                update { copy(call = it, error = if (it.failed) Res.string.failedToStartVideoCall else error) }
                if (it.phase == VideoCallState.Phase.Joined && before.phase != VideoCallState.Phase.Joined) {
                    val now = clock()
                    joinedAt = now
                    analytics.track(
                        AnalyticsEvent.VideoJoined(
                            joinMs = joinTappedAt?.let { t -> (now - t).inWholeMilliseconds },
                            at = now.toString(),
                        ),
                    )
                }
                if (it.doctorJoined && !before.doctorJoined) {
                    analytics.track(
                        AnalyticsEvent.VideoDoctorJoined(joinedAt?.let { j -> (clock() - j).inWholeSeconds }),
                    )
                }
                if (it.failed && !before.failed) {
                    if (joinedAt == null) {
                        analytics.track(AnalyticsEvent.VideoJoinFailed("daily_failed"))
                    } else {
                        failedInCall = true
                    }
                }
                if (it.phase == VideoCallState.Phase.Left) {
                    trackEnded(if (it.failed) "error" else "doctor")
                    _closed.trySend(Unit)
                }
            }
        }
    }

    fun onPermissions(granted: Boolean) {
        analytics.track(AnalyticsEvent.VideoPermissionsResult(granted))
        if (granted) call.startPreview() else update { copy(error = Res.string.errorVideoCallMediaPermissionRequired) }
    }

    fun join() {
        val state = _state.value
        if (state.isJoining || state.inCall) return
        analytics.track(AnalyticsEvent.VideoJoinTapped)
        joinTappedAt = clock()
        update { copy(isStarting = true, error = null) }
        viewModelScope.launch {
            videos.token(state.appointment.id).fold(
                onSuccess = {
                    update { copy(isStarting = false) }
                    call.join(it.roomUrl, it.token)
                },
                onFailure = {
                    analytics.track(AnalyticsEvent.VideoJoinFailed(it.analyticsReason()))
                    update { copy(isStarting = false, error = it.toUserMessage()) }
                    if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                },
            )
        }
    }

    fun toggleCamera() {
        val on = !_state.value.call.cameraOn
        analytics.track(AnalyticsEvent.VideoControlToggled("camera", on))
        call.setCamera(on)
    }

    fun toggleMic() {
        val on = !_state.value.call.micOn
        analytics.track(AnalyticsEvent.VideoControlToggled("mic", on))
        call.setMic(on)
    }

    fun flipCamera() {
        analytics.track(AnalyticsEvent.VideoControlToggled("flip", null))
        call.flipCamera()
    }

    /** Back during the call shrank it to the small window. */
    fun onMinimize() {
        wasMinimized = true
        analytics.track(AnalyticsEvent.VideoCallMinimized)
    }

    fun onExpand() = analytics.track(AnalyticsEvent.VideoCallExpanded)

    fun leave() {
        leftByPatient = true
        if (release()) {
            trackEnded("patient")
            _closed.trySend(Unit)
        }
    }

    // Once per call, whoever ended it.
    private fun trackEnded(endedBy: String) {
        if (ended) return
        ended = true
        analytics.track(
            AnalyticsEvent.VideoCallEnded(
                endedBy =
                    if (failedInCall) {
                        "error"
                    } else if (leftByPatient) {
                        "patient"
                    } else {
                        endedBy
                    },
                durationS = joinedAt?.let { (clock() - it).inWholeSeconds },
                doctorJoined = _state.value.call.doctorJoined,
                wasMinimized = wasMinimized,
            ),
        )
    }

    // True the first time only.
    private fun release(): Boolean {
        if (released) return false
        released = true
        call.release()
        return true
    }

    override fun onCleared() {
        release()
    }
}
