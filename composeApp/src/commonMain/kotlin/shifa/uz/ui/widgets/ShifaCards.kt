package shifa.uz.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_activity_16
import shifa.composeapp.generated.resources.ic_alert_circle_16
import shifa.composeapp.generated.resources.ic_briefcase_16
import shifa.composeapp.generated.resources.ic_calendar_12
import shifa.composeapp.generated.resources.ic_check_16
import shifa.composeapp.generated.resources.ic_check_circle_16
import shifa.composeapp.generated.resources.ic_checkbox_24
import shifa.composeapp.generated.resources.ic_checkbox_checked_24
import shifa.composeapp.generated.resources.ic_chevron_right_16
import shifa.composeapp.generated.resources.ic_clock_12
import shifa.composeapp.generated.resources.ic_clock_16
import shifa.composeapp.generated.resources.ic_star_12
import shifa.composeapp.generated.resources.ic_target_16
import shifa.composeapp.generated.resources.ic_verified_20
import shifa.composeapp.generated.resources.ic_video_12
import shifa.composeapp.generated.resources.ic_zap_16
import shifa.composeapp.generated.resources.img_certificate
import shifa.composeapp.generated.resources.img_documents
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaShadow
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.lineBox

/** Figma upcoming-appointment-card. [joinText] shows the Join action-button (Now variants). */
@Composable
fun ShifaUpcomingAppointmentCard(
    doctorName: String,
    subtitle: String,
    time: String,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    video: Boolean = false,
    joinText: String? = null,
    onJoin: () -> Unit = {},
    onClick: (() -> Unit)? = null,
) = AppointmentCard(doctorName, subtitle, time, avatarUrl, video, modifier, onClick) {
    joinText?.let {
        Box(Modifier.height(ShifaDimens.AppointmentActionHeight), contentAlignment = Alignment.Center) {
            ShifaActionButton(onClick = onJoin, text = it)
        }
    }
}

enum class ShifaAppointmentStatus(
    internal val bg: Color,
    internal val icon: DrawableResource,
    internal val tint: Color,
) {
    Successful(ShifaColors.Bg.SuccessSubtle, Res.drawable.ic_check_16, ShifaColors.Icon.Brand),
    Warning(ShifaColors.Bg.WarningSubtle, Res.drawable.ic_clock_16, ShifaColors.Icon.Warning),
    Error(ShifaColors.Bg.ErrorSubtle, Res.drawable.ic_alert_circle_16, ShifaColors.Icon.Error),
}

/** Figma past-appointment-card. Figma drops the [shadow] on a full-page list. */
@Composable
fun ShifaPastAppointmentCard(
    doctorName: String,
    subtitle: String,
    time: String,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    status: ShifaAppointmentStatus = ShifaAppointmentStatus.Successful,
    video: Boolean = false,
    shadow: Boolean = true,
    onClick: (() -> Unit)? = null,
) = AppointmentCard(doctorName, subtitle, time, avatarUrl, video, modifier, onClick, shadow) {
    Box(
        Modifier.size(ShifaSize.S28).background(status.bg, ShifaRadius.Full),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(status.icon), status.name, tint = status.tint)
    }
}

enum class ShifaDoctorCardType(internal val avatar: Dp, internal val badge: Dp) {
    Full(ShifaDimens.DoctorAvatar, ShifaSize.S20),
    Compact(ShifaDimens.DoctorAvatarCompact, ShifaSize.S16),
    In(ShifaDimens.DoctorAvatarIn, ShifaSize.S16),
}

/**
 * Figma doctor-card. Rows show when their param is set: Figma has rating + availability on
 * Full / Compact, tags on Full / In. [In] has a fixed width. [rating] is preformatted, e.g. "5.0(760)".
 * Figma drops the [shadow] where the card is a page's flex header or on a full-page list.
 */
@Composable
fun ShifaDoctorCard(
    name: String,
    specialty: String,
    modifier: Modifier = Modifier,
    type: ShifaDoctorCardType = ShifaDoctorCardType.Full,
    avatarUrl: String? = null,
    verified: Boolean = true,
    rating: String? = null,
    availability: String? = null,
    tags: (@Composable RowScope.() -> Unit)? = null,
    shadow: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    CardChrome(
        if (type == ShifaDoctorCardType.In) modifier.width(ShifaDimens.DoctorCardInWidth) else modifier,
        onClick,
        shadow = shadow,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(type.avatar)) {
                ShifaAvatar(avatarUrl, type.avatar)
                if (verified) {
                    Image(
                        painterResource(Res.drawable.ic_verified_20),
                        "Verified",
                        Modifier.align(Alignment.BottomEnd).size(type.badge),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ShifaSize.S2)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardTitle(name, Modifier.weight(1f, fill = false))
                    rating?.let {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(Res.drawable.ic_star_12), null, tint = ShifaColors.Icon.Warning)
                            val style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                            Text(it, Modifier.lineBox(style), style = style, color = ShifaColors.Text.Primary)
                        }
                    }
                }
                CardBody(specialty)
                availability?.let {
                    MetaRow(Res.drawable.ic_calendar_12, it, style = MaterialTheme.typography.bodySmall)
                }
                tags?.let {
                    Row(
                        Modifier.padding(top = ShifaSpacing.Stack.Xs),
                        Arrangement.spacedBy(ShifaSpacing.Gap.Xs),
                        content = it,
                    )
                }
            }
        }
    }
}

/** Figma display-card, type document. Display cards show the chevron when clickable. */
@Composable
fun ShifaDocumentCard(
    title: String,
    subtitle: String,
    date: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    // Not in Figma: the document's category, next to the date.
    tag: String? = null,
) = DisplayCard(title, modifier, onClick, { Thumb(Res.drawable.img_documents) }) {
    CardBody(subtitle)
    Row(
        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardBody(date, ShifaColors.Text.Tertiary)
        tag?.let { ShifaTag(it, Modifier.weight(1f, fill = false), ShifaTagType.Primary) }
    }
}

/** Figma display-card, type certificate. */
@Composable
fun ShifaCertificateCard(
    title: String,
    date: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) = DisplayCard(title, modifier, onClick, { Thumb(Res.drawable.img_certificate) }) {
    CardBody(date, ShifaColors.Text.Tertiary)
}

/** Figma display-card, type doctor-service. [price] is preformatted, e.g. "300000.00 UZS". */
@Composable
fun ShifaDoctorServiceCard(
    name: String,
    price: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) = DisplayCard(
    name,
    modifier,
    onClick,
    { IconTile(Res.drawable.ic_briefcase_16, ShifaColors.Bg.SuccessSubtle, ShifaColors.Icon.Brand) },
) {
    CardBody(price, ShifaColors.Text.Link, MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
}

/** Figma display-card, types check-in (unchecked) and check-if-fill (checked: input + submit). */
@Composable
fun ShifaCheckInCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    inputLabel: String,
    submitText: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardChrome(modifier, onClick = null, gap = ShifaDimens.CheckInCardGap) {
        Row(
            Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox, onValueChange = onCheckedChange),
            horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(if (checked) Res.drawable.ic_checkbox_checked_24 else Res.drawable.ic_checkbox_24),
                null,
                tint = if (checked) ShifaColors.Icon.Brand else ShifaColors.Icon.Primary,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ShifaSize.S2)) {
                CardTitle(title)
                CardBody(subtitle, ShifaColors.Text.Tertiary)
            }
        }
        if (checked) {
            ShifaInputField(value, onValueChange, inputLabel, Modifier.fillMaxWidth(), clearable = false)
            ShifaActionButton(onSubmit, Modifier.fillMaxWidth(), text = submitText)
        }
    }
}

enum class ShifaTaskType(internal val icon: DrawableResource) {
    Default(Res.drawable.ic_check_circle_16),
    Exercise(Res.drawable.ic_zap_16),
    Vitals(Res.drawable.ic_activity_16),
    Medicine(Res.drawable.ic_target_16),
}

/** Figma display-card, types task (with progress bar, shown when [total] > 0) and task-compact. */
@Composable
fun ShifaTaskCard(
    title: String,
    description: String,
    due: String,
    modifier: Modifier = Modifier,
    type: ShifaTaskType = ShifaTaskType.Default,
    tag: String? = null,
    completed: Int = 0,
    total: Int = 0,
    onClick: (() -> Unit)? = null,
) {
    CardChrome(modifier, onClick) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(type.icon, ShifaColors.Bg.InfoSubtle, ShifaColors.Icon.Info)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ShifaSize.S2)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardTitle(title, Modifier.weight(1f))
                    tag?.let { ShifaTag(it) }
                }
                CardBody(description)
                MetaRow(
                    Res.drawable.ic_calendar_12,
                    due,
                    style = MaterialTheme.typography.bodySmall,
                    iconTint = ShifaColors.Icon.Secondary,
                    color = ShifaColors.Text.Tertiary,
                )
            }
        }
        if (total > 0) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = ShifaSpacing.Inset.Xs),
                horizontalArrangement = Arrangement.spacedBy(ShifaDimens.ProgressBarGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f).height(ShifaSize.S6).background(ShifaColors.Bg.Canvas, ShifaRadius.Full)) {
                    Box(
                        Modifier
                            .fillMaxWidth((completed.toFloat() / total).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(ShifaColors.Border.Success, ShifaRadius.Full),
                    )
                }
                CardBody(
                    "$completed/$total",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

/** Figma display-card, type settings: a plain row, no card chrome. */
@Composable
fun ShifaSettingsItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
    destructive: Boolean = false,
) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = ShifaSpacing.Inset.Xs),
        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (destructive) ShifaColors.Icon.Error else ShifaColors.Icon.Primary
        icon?.let { Icon(painterResource(it), null, tint = tint) }
        CardTitle(title, Modifier.weight(1f), if (destructive) ShifaColors.Text.Error else ShifaColors.Text.Primary)
        Chevron()
    }
}

@Composable
private fun AppointmentCard(
    doctorName: String,
    subtitle: String,
    time: String,
    avatarUrl: String?,
    video: Boolean,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    shadow: Boolean = true,
    trailing: @Composable () -> Unit,
) {
    CardChrome(modifier, onClick, shadow = shadow) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ShifaAvatar(avatarUrl, ShifaDimens.AppointmentAvatar)
            Spacer(Modifier.width(ShifaSpacing.Gap.Md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ShifaSize.S2)) {
                CardTitle(doctorName)
                if (video) MetaRow(Res.drawable.ic_video_12, subtitle) else CardBody(subtitle)
                MetaRow(Res.drawable.ic_clock_12, time)
            }
            trailing()
        }
    }
}

@Composable
private fun DisplayCard(
    title: String,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    leading: @Composable () -> Unit,
    lines: @Composable ColumnScope.() -> Unit,
) {
    CardChrome(modifier, onClick) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ShifaSize.S2)) {
                CardTitle(title)
                lines()
            }
            if (onClick != null) Chevron()
        }
    }
}

// Figma display-image: the 54x47 illustration scaled to the 48dp frame height.
@Composable
private fun Thumb(image: DrawableResource) =
    Image(
        painterResource(image),
        null,
        Modifier.width(ShifaDimens.DocumentThumbWidth).height(ShifaDimens.DocumentThumbHeight),
    )

// Figma task-icons / service: tinted tile with a filled circle holding a 16dp icon.
@Composable
private fun IconTile(
    icon: DrawableResource,
    bg: Color,
    fill: Color,
) {
    Box(
        Modifier
            .size(ShifaDimens.TaskIcon)
            .background(bg, ShifaRadius.ControlMd)
            .padding(ShifaSpacing.Inset.Xs)
            .background(fill, ShifaRadius.Full),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), null, tint = ShifaColors.Icon.Inverse)
    }
}

@Composable
internal fun ShifaAvatar(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    placeholder: Color = ShifaColors.Bg.Canvas,
) {
    Box(modifier.size(size).clip(ShifaRadius.Full).background(placeholder)) {
        if (url != null) {
            KamelImage(
                resource = asyncPainterResource(url),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
internal fun CardChrome(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    gap: Dp = ShifaSpacing.Gap.Sm,
    shadow: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .then(if (shadow) Modifier.dropShadow(ShifaRadius.ControlMd, ShifaShadow.Card) else Modifier)
            .clip(ShifaRadius.ControlMd)
            .background(ShifaColors.Bg.Surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(ShifaSpacing.Inset.Sm),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

@Composable
internal fun CardTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ShifaColors.Text.Primary,
) = Text(
    text,
    modifier.lineBox(MaterialTheme.typography.titleSmall),
    style = MaterialTheme.typography.titleSmall,
    color = color,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)

@Composable
internal fun CardBody(
    text: String,
    color: Color = ShifaColors.Text.Primary,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) = Text(
    text,
    Modifier.lineBox(style),
    style = style,
    color = color,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)

@Composable
internal fun MetaRow(
    icon: DrawableResource,
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    iconTint: Color = ShifaColors.Icon.Primary,
    color: Color = ShifaColors.Text.Primary,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, tint = iconTint)
        Text(
            text,
            Modifier.lineBox(style),
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun Chevron() {
    Box(Modifier.background(ShifaColors.Bg.Canvas, ShifaRadius.Full).padding(ShifaSpacing.Inset.Xs)) {
        Icon(painterResource(Res.drawable.ic_chevron_right_16), null, tint = ShifaColors.Icon.Primary)
    }
}
