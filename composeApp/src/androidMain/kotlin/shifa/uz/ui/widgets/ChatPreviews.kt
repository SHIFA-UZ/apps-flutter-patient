package shifa.uz.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_camera_22
import shifa.composeapp.generated.resources.ic_file_22
import shifa.composeapp.generated.resources.ic_image_22
import shifa.uz.ui.theme.ShifaColors

@Preview
@Composable
internal fun ChatBarPreview() =
    Gallery {
        ShifaChatBar("", {}, "Ask Anything", {})
        ShifaChatBar("Message Typed", {}, "Ask Anything", {})
        ShifaChatBar("", {}, "Ask Anything", {}, recording = true)
    }

@Preview
@Composable
internal fun ChatBubblePreview() =
    Gallery {
        ShifaChatBubble("chat message", ShifaChatBubbleType.Received)
        ShifaChatBubble("chat message", ShifaChatBubbleType.Sent)
        ShifaChatBubble("chat message", ShifaChatBubbleType.ShifaAi)
        ShifaChatImageBubble { Box(Modifier.size(16.dp).background(ShifaColors.Bg.Surface)) }
        ShifaChatBubble(
            "A longer message that wraps onto several lines to show the max width and line height of the bubble.",
            ShifaChatBubbleType.Received,
        )
    }

@Preview
@Composable
internal fun TrayPreview() =
    Gallery(background = ShifaColors.Bg.Canvas) {
        ShifaChatTray({}, {}, suggestions = List(3) { "Suggested Message" }) {
            ShifaChatBar("", {}, "Ask Anything", {})
        }
        ShifaTray("Add to chat", {}) {
            val wide = Modifier.fillMaxWidth()
            ShifaSecondaryButton("Camera", {}, wide, icon = Res.drawable.ic_camera_22)
            ShifaSecondaryButton("Photo", {}, wide, icon = Res.drawable.ic_image_22)
            ShifaSecondaryButton("File", {}, wide, icon = Res.drawable.ic_file_22)
        }
        ShifaTray("Tray Header", {}) { Box(Modifier.fillMaxWidth().height(140.dp)) }
    }

@Preview
@Composable
internal fun PopUpPreview() =
    Gallery(background = ShifaColors.Bg.Canvas) {
        ShifaPopUp("Pop-up header", {}) { Box(Modifier.fillMaxWidth().height(314.dp)) }
    }
