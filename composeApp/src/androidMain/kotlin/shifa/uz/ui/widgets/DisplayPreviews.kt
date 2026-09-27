package shifa.uz.ui.widgets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import shifa.uz.ui.theme.ShifaColors

private const val BODY =
    "Lorem ipsum dolor sit amet consectetur. Libero pellentesque nam at nunc urna dignissim. " +
        "Adipiscing pulvinar bibendum nunc tempus in ridiculus dui molestie."

@Preview
@Composable
internal fun NotificationPreview() =
    Gallery {
        ShifaNotificationType.entries.forEach {
            val feedback = it == ShifaNotificationType.Feedback
            ShifaNotification("${it.name} Title", BODY, it, time = if (feedback) null else "17:00")
        }
        ShifaMessageNotification("Sender Name", BODY, "17:00")
    }

@Preview
@Composable
internal fun PagerPreview() =
    Gallery {
        repeat(4) { ShifaPager(count = 4, current = it) }
    }

@Preview
@Composable
internal fun PageElementPreview() =
    Gallery(background = ShifaColors.Bg.Canvas) {
        ShifaPageElement(ShifaPageElementType.Header) { ShifaSubHeader("Header", onBack = {}) }
        ShifaPageElement(ShifaPageElementType.FlexHeader) { Placeholder("Flex header") }
        ShifaPageElement(ShifaPageElementType.Body) { Placeholder("Body") }
        ShifaPageElement(ShifaPageElementType.ActionTray) {
            val wide = Modifier.fillMaxWidth()
            ShifaPrimaryButton("Action", {}, wide)
            ShifaSecondaryButton("Action 2", {}, wide)
        }
        ShifaPageElement(ShifaPageElementType.Footer) { ShifaPrimaryButton("Footer", {}, Modifier.fillMaxWidth()) }
    }

@Preview
@Composable
internal fun ImageContainerPreview() =
    Gallery(background = ShifaColors.Bg.Canvas) {
        ShifaImageContainer(url = null, contentDescription = null)
    }

@Composable
private fun Placeholder(text: String) =
    Text(text, style = MaterialTheme.typography.bodyLarge, color = ShifaColors.Text.Primary)
