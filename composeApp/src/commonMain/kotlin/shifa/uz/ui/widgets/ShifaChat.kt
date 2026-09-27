package shifa.uz.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_mic_22
import shifa.composeapp.generated.resources.ic_x_22
import shifa.composeapp.generated.resources.img_waveform
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.theme.lineBox

/**
 * Figma chat-bar. Empty: placeholder + mic ([onMic] starts recording). Typed: text + clear.
 * [recording]: mic ([onMic] stops) + waveform.
 */
@Composable
fun ShifaChatBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onMic: () -> Unit,
    modifier: Modifier = Modifier,
    recording: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val active = recording || focused || value.isNotEmpty()
    val style = ShifaTextStyles.chatInput()
    Row(
        modifier
            .fillMaxWidth()
            .background(ShifaColors.Bg.Surface, ShifaRadius.Full)
            .border(
                ShifaSize.S2,
                if (active) ShifaColors.Border.Focus else ShifaColors.Border.Default,
                ShifaRadius.Full,
            )
            .padding(horizontal = ShifaSpacing.Inset.Md, vertical = ShifaSpacing.Inset.Sm),
        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (recording) {
            BarIcon(Res.drawable.ic_mic_22, "Stop recording", onMic)
            Icon(painterResource(Res.drawable.img_waveform), null, tint = ShifaColors.Icon.Primary)
        } else {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = style.copy(color = ShifaColors.Text.Primary),
                cursorBrush = SolidColor(ShifaColors.Border.Focus),
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) Text(placeholder, style = style, color = ShifaColors.Text.Tertiary)
                        innerTextField()
                    }
                },
            )
            if (value.isEmpty()) {
                BarIcon(Res.drawable.ic_mic_22, "Record voice message", onMic)
            } else {
                BarIcon(Res.drawable.ic_x_22, "Clear") { onValueChange("") }
            }
        }
    }
}

@Composable
private fun BarIcon(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
) = Icon(
    painterResource(icon),
    contentDescription,
    Modifier.clip(ShifaRadius.Full).clickable(onClick = onClick),
    tint = ShifaColors.Icon.Primary,
)

enum class ShifaChatBubbleType { Received, Sent, ShifaAi }

/** Figma chat-bubble, type = received / sent / shifa-ai. Sent aligns end, the rest start. */
@Composable
fun ShifaChatBubble(
    text: String,
    type: ShifaChatBubbleType,
    modifier: Modifier = Modifier,
) = Bubble(type, modifier) {
    val style = ShifaTextStyles.chatMessage()
    Text(
        text,
        Modifier.lineBox(style),
        style = style,
        color = if (type == ShifaChatBubbleType.Received) ShifaColors.Text.Inverse else ShifaColors.Text.Primary,
    )
}

/** Figma chat-bubble, type = image: a sent bubble around [image], clipped to ShifaRadius.Sm. */
@Composable
fun ShifaChatImageBubble(
    modifier: Modifier = Modifier,
    image: @Composable () -> Unit,
) = Bubble(ShifaChatBubbleType.Sent, modifier) {
    Box(Modifier.clip(ShifaRadius.Sm)) { image() }
}

@Composable
private fun Bubble(
    type: ShifaChatBubbleType,
    modifier: Modifier,
    content: @Composable () -> Unit,
) = Box(
    modifier.fillMaxWidth(),
    contentAlignment = if (type == ShifaChatBubbleType.Sent) Alignment.CenterEnd else Alignment.CenterStart,
) {
    Box(
        Modifier
            .widthIn(max = ShifaDimens.ChatBubbleMaxWidth)
            .background(
                when (type) {
                    ShifaChatBubbleType.Received -> ShifaColors.Alpha.Teal75
                    ShifaChatBubbleType.Sent -> ShifaColors.Alpha.Black5
                    ShifaChatBubbleType.ShifaAi -> Color.Transparent
                },
                ShifaRadius.ControlLg,
            ).padding(horizontal = ShifaSpacing.Inset.Md, vertical = ShifaSpacing.Stack.Md),
    ) { content() }
}
