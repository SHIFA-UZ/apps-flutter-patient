package shifa.uz.ui.visit

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.uz.data.model.Appointment
import shifa.uz.utils.VideoCallState

/** [call] mirrors the Daily call; [isStarting] covers V1 before Daily joins. */
@Stable
data class VideoCallUiState(
    val appointment: Appointment,
    val call: VideoCallState = VideoCallState(),
    val isStarting: Boolean = false,
    val error: StringResource? = null,
) {
    val inCall get() = call.phase == VideoCallState.Phase.Joined

    val isJoining get() = isStarting || call.phase == VideoCallState.Phase.Joining
}
