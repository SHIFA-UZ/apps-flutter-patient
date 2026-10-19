package shifa.uz.ui.visit

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import shifa.uz.data.model.Appointment
import shifa.uz.ui.bookings.previewDoctor
import shifa.uz.ui.widgets.PreviewTheme
import shifa.uz.utils.VideoCall
import shifa.uz.utils.VideoCallState

private val now = Instant.parse("2026-07-31T07:05:00Z")

// Figma 1918:22602's visit: 12:00-12:30 in Tashkent.
private val visit =
    Appointment(
        id = "1",
        doctorId = "d1",
        doctorName = "Azamat Rakhimov",
        doctorProfession = "Dentist",
        doctorClinic = "Sog'lom Hayot Klinikasi",
        startAt = "2026-07-31T07:00:00Z",
        endAt = "2026-07-31T07:30:00Z",
        location = "SEBAMED",
        reason = "Notes",
        signatureRequested = true,
    )

private val video = visit.copy(location = "Video Consultation", videoFlag = true, signatureRequested = false)

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun LiveAppointmentPreview() =
    PreviewTheme { LiveAppointmentContent(LiveAppointmentUiState(visit, now, previewDoctor)) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun LiveAppointmentVideoPreview() =
    PreviewTheme { LiveAppointmentContent(LiveAppointmentUiState(video, now, previewDoctor, detailsOpen = false)) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun SignTrayPreview() = PreviewTheme { SignTray(LiveAppointmentUiState(visit, now, previewDoctor)) }

/** No camera in previews: the tiles stay empty. */
private class PreviewCall(
    state: VideoCallState,
) : VideoCall {
    override val state = MutableStateFlow(state)

    override fun startPreview() = Unit

    override fun join(
        roomUrl: String,
        token: String,
    ) = Unit

    override fun setCamera(on: Boolean) = Unit

    override fun setMic(on: Boolean) = Unit

    override fun flipCamera() = Unit

    override fun release() = Unit
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun VideoCallJoinPreview() =
    PreviewTheme {
        val call = VideoCallState(VideoCallState.Phase.Preview)
        VideoCallContent(VideoCallUiState(video, call), PreviewCall(call))
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun VideoCallWaitingPreview() =
    PreviewTheme {
        val call = VideoCallState(VideoCallState.Phase.Joined, micOn = false)
        VideoCallContent(VideoCallUiState(video, call), PreviewCall(call))
    }
