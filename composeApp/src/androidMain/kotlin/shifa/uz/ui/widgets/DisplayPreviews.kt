package shifa.uz.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_alert_circle_16
import shifa.composeapp.generated.resources.ic_check_circle_16
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaSpacing

private const val BODY =
    "Lorem ipsum dolor sit amet consectetur. Libero pellentesque nam at nunc urna dignissim. " +
        "Adipiscing pulvinar bibendum nunc tempus in ridiculus dui molestie."

@Preview
@Composable
internal fun NotificationPreview() =
    Gallery(ShifaColors.Bg.Canvas) {
        ShifaNotificationType.entries.forEach {
            val feedback = it == ShifaNotificationType.Feedback
            ShifaNotification("${it.name} Title", BODY, it, time = if (feedback) null else "17:00")
        }
        ShifaNotification("Unread Title", BODY, ShifaNotificationType.Task, time = "17:00", unreadCount = 20)
        ShifaNotification("Unread Chat", BODY, ShifaNotificationType.Chat, time = "17:00", unreadCount = 1)
        ShifaMessageNotification("Sender Name", BODY, "17:00")
        ShifaMessageNotification("Sender Name", BODY, "17:00", unreadCount = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
            listOf(1, 20, 120).forEach { ShifaNotificationBubble(it) }
        }
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

@Preview
@Composable
internal fun BannerPreview() =
    Gallery {
        ShifaBannerType.entries.forEach {
            val success = it == ShifaBannerType.Success
            ShifaBanner(
                "${it.name}: $BODY",
                if (success) Res.drawable.ic_check_circle_16 else Res.drawable.ic_alert_circle_16,
                it,
            )
        }
    }

@Composable
private fun Placeholder(text: String) =
    Text(text, style = MaterialTheme.typography.bodyLarge, color = ShifaColors.Text.Primary)

@Preview
@Composable
internal fun SkeletonPreview() =
    Gallery(ShifaColors.Bg.Canvas) {
        ShifaListSkeleton(count = 2)
        ShifaCardSkeleton(avatar = ShifaDimens.DoctorAvatarIn, width = ShifaDimens.DoctorCardInMaxWidth, shadow = false)
        ShifaChipsSkeleton()
        ShifaChatSkeleton()
    }
