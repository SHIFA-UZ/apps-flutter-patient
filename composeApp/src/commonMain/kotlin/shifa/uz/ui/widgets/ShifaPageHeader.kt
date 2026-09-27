package shifa.uz.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.painterResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_arrow_left_16
import shifa.composeapp.generated.resources.ic_bell_16
import shifa.composeapp.generated.resources.ic_check_square_16
import shifa.composeapp.generated.resources.ic_more_vertical_16
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing

/** Figma page-header, page = main. [home] picks the large title (type = home). */
@Composable
fun ShifaMainHeader(
    title: String,
    modifier: Modifier = Modifier,
    home: Boolean = false,
    avatarUrl: String? = null,
    onNotifications: (() -> Unit)? = null,
    onAvatar: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().height(ShifaSize.S100).padding(horizontal = ShifaSpacing.Inset.Md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderTitle(title, if (home) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            onNotifications?.let {
                ShifaActionButton(
                    it,
                    icon = Res.drawable.ic_bell_16,
                    contentDescription = "Notifications",
                    secondary = true,
                )
            }
            ShifaAvatar(
                avatarUrl,
                ShifaDimens.PageHeaderAvatar,
                Modifier.clip(
                    ShifaRadius.Full,
                ).then(if (onAvatar != null) Modifier.clickable(onClick = onAvatar) else Modifier),
                placeholder = ShifaColors.Icon.Primary,
            )
        }
    }
}

/** Figma page-header, page = sub. Each action shows only when its callback is set. */
@Composable
fun ShifaSubHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onCheck: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().height(ShifaSize.S64).padding(horizontal = ShifaSpacing.Inset.Md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        onBack?.let {
            ShifaActionButton(it, icon = Res.drawable.ic_arrow_left_16, contentDescription = "Back", secondary = true)
        }
        HeaderTitle(title, MaterialTheme.typography.headlineSmall, Modifier.weight(1f, fill = false))
        Row(
            horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            onCheck?.let {
                ShifaActionButton(
                    it,
                    icon = Res.drawable.ic_check_square_16,
                    contentDescription = "Select",
                    secondary = true,
                )
            }
            onMore?.let {
                // Same as a secondary action-button, without the background.
                Box(
                    Modifier
                        .clip(ShifaRadius.Full)
                        .clickable(role = Role.Button, onClick = it)
                        .padding(ShifaSpacing.Inset.Xs),
                ) {
                    Icon(painterResource(Res.drawable.ic_more_vertical_16), "More", tint = ShifaColors.Icon.Primary)
                }
            }
        }
    }
}

@Composable
private fun HeaderTitle(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) = Text(
    text,
    modifier,
    style = style,
    color = ShifaColors.Text.Primary,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)
