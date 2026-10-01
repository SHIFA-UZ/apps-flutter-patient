package shifa.uz.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
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
    BubbleText(text, if (type == ShifaChatBubbleType.Sent) TextAlign.End else TextAlign.Start)
}

/**
 * Figma chat-bubble, type = image: [image] clipped to ShifaRadius.Sm, in a sent bubble unless [received].
 * Not in Figma: a [caption] under it, and the received look.
 */
@Composable
fun ShifaChatImageBubble(
    modifier: Modifier = Modifier,
    received: Boolean = false,
    caption: String? = null,
    image: @Composable () -> Unit,
) = Bubble(if (received) ShifaChatBubbleType.Received else ShifaChatBubbleType.Sent, modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
        Box(Modifier.clip(ShifaRadius.Sm)) { image() }
        caption?.let { BubbleText(it, if (received) TextAlign.Start else TextAlign.End) }
    }
}

@Composable
private fun BubbleText(
    text: String,
    align: TextAlign,
) {
    val style = ShifaTextStyles.chatBubble()
    Text(text, Modifier.lineBox(style), style = style, color = ShifaColors.Text.Primary, textAlign = align)
}

// Figma: the corner at the sender's side is square.
private val ReceivedShape = ShifaRadius.ControlLg.copy(topStart = ZeroCornerSize)
private val SentShape = ShifaRadius.ControlLg.copy(topEnd = ZeroCornerSize)

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
            .then(
                when (type) {
                    ShifaChatBubbleType.Received -> Modifier.background(ShifaColors.Alpha.Teal25, ReceivedShape)
                    ShifaChatBubbleType.Sent -> Modifier.background(ShifaColors.Alpha.Black5, SentShape)
                    ShifaChatBubbleType.ShifaAi -> Modifier
                },
            ).padding(horizontal = ShifaSpacing.Inset.Md, vertical = ShifaSpacing.Stack.Md),
    ) { content() }
}
