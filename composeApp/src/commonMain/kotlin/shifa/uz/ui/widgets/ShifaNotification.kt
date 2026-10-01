package shifa.uz.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_calendar_16
import shifa.composeapp.generated.resources.ic_check_circle_bold_16
import shifa.composeapp.generated.resources.ic_folder_16
import shifa.composeapp.generated.resources.ic_info_16
import shifa.composeapp.generated.resources.ic_sparkles_16
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.theme.lineBox

enum class ShifaNotificationType(
    internal val icon: DrawableResource,
    internal val tint: Color,
) {
    Task(Res.drawable.ic_check_circle_bold_16, ShifaColors.Icon.Info),
    Document(Res.drawable.ic_folder_16, ShifaColors.Icon.Warning),
    Appointment(Res.drawable.ic_calendar_16, ShifaColors.Icon.Brand),
    Chat(Res.drawable.ic_sparkles_16, ShifaColors.Icon.Brand),
    Feedback(Res.drawable.ic_info_16, ShifaColors.Icon.Warning),
}

/** Figma notification, every type but message. Feedback has no [time] in Figma. [unread] titles are brand (not in Figma). */
@Composable
fun ShifaNotification(
    title: String,
    body: String,
    type: ShifaNotificationType,
    modifier: Modifier = Modifier,
    time: String? = null,
    unread: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val feedback = type == ShifaNotificationType.Feedback
    Column(
        modifier
            .notificationChrome(if (feedback) ShifaColors.Bg.WarningSubtle else ShifaColors.Bg.Canvas, onClick)
            .then(
                if (feedback) {
                    Modifier.border(
                        ShifaSize.S2,
                        ShifaColors.Border.Warning,
                        ShifaRadius.ControlMd,
                    )
                } else {
                    Modifier
                },
            )
            .padding(ShifaSpacing.Inset.Sm),
        verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
    ) {
        TitleBar(title, time, type, unread)
        NotificationBody(body)
    }
}

/** Figma notification, type = message. [unreadCount] > 0 is unread: a brand time and the count's bubble. */
@Composable
fun ShifaMessageNotification(
    sender: String,
    body: String,
    time: String?,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    unreadCount: Int = 0,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier.notificationChrome(ShifaColors.Bg.Surface, onClick).padding(ShifaSpacing.Inset.Sm),
        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShifaAvatar(avatarUrl, ShifaDimens.NotificationAvatar, placeholder = ShifaColors.Bg.Disabled)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
            Row(
                Modifier.fillMaxWidth().height(ShifaSize.S22),
                horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val title = ShifaTextStyles.profileName()
                Text(
                    sender,
                    Modifier.weight(1f).lineBox(title),
                    style = title,
                    color = ShifaColors.Text.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val unread = unreadCount > 0
                time?.let {
                    val style = MaterialTheme.typography.labelSmall
                    Text(
                        it,
                        Modifier.lineBox(style),
                        style = if (unread) style else style.copy(fontWeight = FontWeight.Medium),
                        color = if (unread) ShifaColors.Text.Brand else ShifaColors.Text.Tertiary,
                        maxLines = 1,
                    )
                }
                if (unread) {
                    // Figma notification-bubble: a 14 text box in a 4 padding.
                    val style = MaterialTheme.typography.labelSmall
                    Box(
                        Modifier
                            .background(ShifaColors.Icon.Brand, ShifaRadius.Full)
                            .padding(ShifaSpacing.Stack.Xs)
                            .defaultMinSize(ShifaSize.S14, ShifaSize.S14),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (unreadCount > 99) "99+" else "$unreadCount",
                            Modifier.lineBox(style),
                            style = style,
                            color = ShifaColors.Text.Inverse,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            val style = ShifaTextStyles.notificationBody()
            Text(
                body,
                Modifier.fillMaxWidth().height(ShifaSize.S26),
                style = style,
                color = ShifaColors.Text.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun Modifier.notificationChrome(
    bg: Color,
    onClick: (() -> Unit)?,
) = fillMaxWidth()
    .clip(ShifaRadius.ControlMd)
    .background(bg)
    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

@Composable
private fun TitleBar(
    title: String,
    time: String?,
    type: ShifaNotificationType?,
    unread: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            type?.let { Icon(painterResource(it.icon), null, tint = it.tint) }
            val style = MaterialTheme.typography.labelMedium
            Text(
                title,
                Modifier.lineBox(style),
                style = style,
                color = if (unread) ShifaColors.Text.Brand else ShifaColors.Text.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        time?.let {
            val style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
            Text(it, Modifier.lineBox(style), style = style, color = ShifaColors.Text.Tertiary, maxLines = 1)
        }
    }
}

@Composable
private fun NotificationBody(text: String) {
    val style = MaterialTheme.typography.bodyMedium
    Text(
        text,
        Modifier.lineBox(style),
        style = style,
        color = ShifaColors.Text.Primary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
