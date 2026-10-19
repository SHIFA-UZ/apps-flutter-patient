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
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.errorVideoCallMediaPermissionRequired
import shifa.composeapp.generated.resources.failedToStartVideoCall
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.data.video.VideoRepository
import shifa.uz.utils.VideoCall
import shifa.uz.utils.VideoCallState

/**
 * Figma 1791:19370 (preview, Join Now) and 1793:15925 (waiting room): V1's token, then the Daily [call], which this
 * ViewModel owns and frees. The page closes when the call ends, from here or by the doctor.
 * ponytail: no join-timeout retry or app-lock pause (06 §3.6); add them with LOCK-01 and if joins time out.
 */
class VideoCallViewModel(
    appointment: Appointment,
    val call: VideoCall,
    private val videos: VideoRepository = VideoRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(VideoCallUiState(appointment))
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
        viewModelScope.launch {
            call.state.collect {
                update { copy(call = it, error = if (it.failed) Res.string.failedToStartVideoCall else error) }
                if (it.phase == VideoCallState.Phase.Left) _closed.trySend(Unit)
            }
        }
    }

    fun onPermissions(granted: Boolean) =
        if (granted) call.startPreview() else update { copy(error = Res.string.errorVideoCallMediaPermissionRequired) }

    fun join() {
        val state = _state.value
        if (state.isJoining || state.inCall) return
        update { copy(isStarting = true, error = null) }
        viewModelScope.launch {
            videos.token(state.appointment.id).fold(
                onSuccess = {
                    update { copy(isStarting = false) }
                    call.join(it.roomUrl, it.token)
                },
                onFailure = {
                    update { copy(isStarting = false, error = it.toUserMessage()) }
                    if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                },
            )
        }
    }

    fun toggleCamera() = call.setCamera(!_state.value.call.cameraOn)

    fun toggleMic() = call.setMic(!_state.value.call.micOn)

    fun flipCamera() = call.flipCamera()

    fun leave() {
        if (release()) _closed.trySend(Unit)
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
