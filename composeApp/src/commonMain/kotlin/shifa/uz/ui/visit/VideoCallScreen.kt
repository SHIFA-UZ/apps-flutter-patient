package shifa.uz.ui.visit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.camera
import shifa.composeapp.generated.resources.endCall
import shifa.composeapp.generated.resources.ic_camera_32
import shifa.composeapp.generated.resources.ic_clock_12
import shifa.composeapp.generated.resources.ic_loader_24
import shifa.composeapp.generated.resources.ic_mic_32
import shifa.composeapp.generated.resources.ic_phone_32
import shifa.composeapp.generated.resources.ic_refresh_cw_32
import shifa.composeapp.generated.resources.ic_video_22
import shifa.composeapp.generated.resources.joinNow
import shifa.composeapp.generated.resources.microphone
import shifa.composeapp.generated.resources.switchCamera
import shifa.composeapp.generated.resources.unknownDoctor
import shifa.composeapp.generated.resources.videoAppointment
import shifa.composeapp.generated.resources.waitingForDoctorToStart
import shifa.uz.data.model.Appointment
import shifa.uz.ui.register.Banners
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.widgets.ShifaDoctorCard
import shifa.uz.ui.widgets.ShifaDoctorCardType
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.VideoCall
import shifa.uz.utils.VideoTile
import shifa.uz.utils.formatTime
import shifa.uz.utils.parseInstant
import shifa.uz.utils.rememberCallPermissions
import shifa.uz.utils.rememberVideoCallFactory

/** A video visit's call: the preview with Join Now, then the waiting room until the doctor joins, then the call. */
@Composable
fun VideoCallScreen(
    appointment: Appointment,
    onClose: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: VideoCallViewModel =
        rememberVideoCallFactory().let { calls -> viewModel { VideoCallViewModel(appointment, calls()) } },
) {
    val state by viewModel.state.collectAsState()
    val closed by rememberUpdatedState(onClose)
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) { viewModel.closed.collect { closed() } }
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { signedOut() } }
    val askPermissions = rememberCallPermissions(viewModel::onPermissions)
    LaunchedEffect(Unit) { askPermissions() }
    VideoCallContent(
        state,
        viewModel.call,
        onBack = viewModel::leave,
        onJoin = viewModel::join,
        onCamera = viewModel::toggleCamera,
        onMic = viewModel::toggleMic,
        onFlip = viewModel::flipCamera,
    )
}

@Composable
internal fun VideoCallContent(
    state: VideoCallUiState,
    call: VideoCall,
    onBack: () -> Unit = {},
    onJoin: () -> Unit = {},
    onCamera: () -> Unit = {},
    onMic: () -> Unit = {},
    onFlip: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Canvas)) {
        ShifaPageElement(ShifaPageElementType.Header, Modifier.statusBarsPadding()) {
            ShifaSubHeader(stringResource(Res.string.videoAppointment), onBack = onBack)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().navigationBarsPadding().padding(ShifaSpacing.Inset.Md),
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Stack.Md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Banners(state.error)
            if (state.inCall) {
                if (!state.call.doctorJoined) Waiting()
                Box(
                    Modifier.weight(
                        1f,
                    ).fillMaxWidth().clip(ShifaRadius.ControlLg).background(ShifaColors.Alpha.Black50),
                ) {
                    VideoTile(call, remote = state.call.doctorJoined, Modifier.fillMaxSize())
                    if (state.call.doctorJoined) {
                        VideoTile(
                            call,
                            remote = false,
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(ShifaSpacing.Inset.Sm)
                                .size(ShifaDimens.CallSelfViewWidth, ShifaDimens.CallSelfViewHeight)
                                .clip(ShifaRadius.ControlMd),
                        )
                    }
                }
                Controls(state, onCamera, onMic, onFlip, onBack)
            } else {
                CallCard(state.appointment)
                Box(
                    Modifier.weight(
                        1f,
                    ).fillMaxWidth().clip(ShifaRadius.ControlLg).background(ShifaColors.Alpha.Black50),
                ) {
                    VideoTile(call, remote = false, Modifier.fillMaxSize())
                }
                ShifaPrimaryButton(
                    stringResource(Res.string.joinNow),
                    onJoin,
                    Modifier.fillMaxWidth(),
                    icon = Res.drawable.ic_video_22,
                    loading = state.isJoining,
                )
            }
        }
    }
}

// Figma doctor-card, compact: no badge or rating; the visit's time with a clock.
@Composable
private fun CallCard(appointment: Appointment) {
    val time =
        appointment.start?.let { start ->
            listOfNotNull(start, parseInstant(appointment.endAt)).joinToString("-") { formatTime(it) }
        }
    ShifaDoctorCard(
        appointment.doctorName?.ifBlank { null } ?: stringResource(Res.string.unknownDoctor),
        listOfNotNull(
            appointment.doctorProfession,
            appointment.doctorClinic,
        ).filter(String::isNotBlank).joinToString(" - "),
        Modifier.fillMaxWidth(),
        type = ShifaDoctorCardType.Compact,
        avatarUrl = appointment.doctorPhotoUrl,
        verified = false,
        availability = time,
        availabilityIcon = Res.drawable.ic_clock_12,
    )
}

@Composable
private fun Waiting() {
    // Still in previews and screenshots: an endless animation never lets them settle.
    val turn =
        if (LocalInspectionMode.current) {
            0f
        } else {
            rememberInfiniteTransition()
                .animateFloat(
                    0f,
                    360f,
                    infiniteRepeatable(tween(LOADER_TURN_MS, easing = LinearEasing), RepeatMode.Restart),
                )
                .value
        }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(Res.drawable.ic_loader_24), null, Modifier.rotate(turn), tint = ShifaColors.Icon.Secondary)
        Text(
            stringResource(Res.string.waitingForDoctorToStart),
            style = ShifaTextStyles.callStatus(),
            color = ShifaColors.Text.Secondary,
        )
    }
}

private const val LOADER_TURN_MS = 1_200

// Figma call-controls: a pill of round buttons. Not in Figma: an off camera or microphone shows its button dimmed.
@Composable
private fun Controls(
    state: VideoCallUiState,
    onCamera: () -> Unit,
    onMic: () -> Unit,
    onFlip: () -> Unit,
    onEnd: () -> Unit,
) = Row(
    Modifier
        .height(ShifaDimens.CallControlsHeight)
        .background(ShifaColors.Alpha.Black50, ShifaRadius.Full)
        .padding(ShifaSpacing.Inset.Sm),
    horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
    verticalAlignment = Alignment.CenterVertically,
) {
    CallButton(Res.drawable.ic_camera_32, stringResource(Res.string.camera), onCamera, on = state.call.cameraOn)
    CallButton(Res.drawable.ic_mic_32, stringResource(Res.string.microphone), onMic, on = state.call.micOn)
    CallButton(Res.drawable.ic_refresh_cw_32, stringResource(Res.string.switchCamera), onFlip)
    CallButton(
        Res.drawable.ic_phone_32,
        stringResource(Res.string.endCall),
        onEnd,
        bg = ShifaColors.Interactive.Destructive,
        tint = ShifaColors.Icon.Inverse,
    )
}

@Composable
private fun CallButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    on: Boolean = true,
    bg: Color = ShifaColors.Bg.Canvas,
    tint: Color = ShifaColors.Icon.Primary,
) = Box(
    Modifier
        .fillMaxHeight()
        .aspectRatio(1f)
        .alpha(if (on) 1f else OFF_ALPHA)
        .clip(ShifaRadius.Full)
        .background(bg)
        .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
) { Icon(painterResource(icon), contentDescription, tint = tint) }

private const val OFF_ALPHA = 0.5f
