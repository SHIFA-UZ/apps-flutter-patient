package shifa.uz.ui.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_loader_22
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaShadow
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.lineBox

// Figma: primary-button / secondary-button / tertiary-button use 22dp icons,
// action-button 16dp, icon-button 24dp. Pass a drawable drawn at that size.

@Composable
fun ShifaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    loading: Boolean = false,
) {
    val bg =
        when {
            !enabled -> ShifaColors.Interactive.DisabledBg
            destructive -> ShifaColors.Interactive.Destructive
            else -> ShifaColors.Interactive.Primary
        }
    val content = if (enabled) ShifaColors.Interactive.OnPrimary else ShifaColors.Text.Disabled
    ButtonContent(
        text = text,
        icon = icon,
        color = content,
        loading = loading,
        modifier =
            modifier
                .dropShadow(ShifaRadius.ControlMd, ShifaShadow.Default)
                .clip(ShifaRadius.ControlMd)
                .background(bg)
                .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
                .padding(ShifaSpacing.Inset.Sm),
    )
}

@Composable
fun ShifaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    loading: Boolean = false,
) {
    val (border, content) =
        when {
            !enabled -> ShifaColors.Border.Disabled to ShifaColors.Text.Disabled
            destructive -> ShifaColors.Border.Error to ShifaColors.Text.Error
            else -> ShifaColors.Border.Brand to ShifaColors.Text.Brand
        }
    ButtonContent(
        text = text,
        icon = icon,
        color = content,
        loading = loading,
        modifier =
            modifier
                .dropShadow(ShifaRadius.ControlMd, ShifaShadow.Default)
                .clip(ShifaRadius.ControlMd)
                .background(ShifaColors.Bg.Surface)
                .border(ShifaSize.S2, border, ShifaRadius.ControlMd)
                .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
                .padding(ShifaSpacing.Inset.Sm),
    )
}

@Composable
fun ShifaTertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val content =
        when {
            !enabled -> ShifaColors.Text.Disabled
            destructive -> ShifaColors.Text.Error
            else -> ShifaColors.Text.Brand
        }
    ButtonContent(
        text = text,
        icon = icon,
        color = content,
        loading = false,
        modifier =
            modifier
                .clip(ShifaRadius.ControlMd)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(
                    start = ShifaDimens.TertiaryButtonPaddingStart,
                    end = ShifaDimens.TertiaryButtonPaddingEnd,
                    top = ShifaDimens.TertiaryButtonPaddingVertical,
                    bottom = ShifaDimens.TertiaryButtonPaddingVertical,
                ),
    )
}

/** Figma action-button (pill). Text only, icon only (pass [contentDescription]) or both. */
@Composable
fun ShifaActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: DrawableResource? = null,
    contentDescription: String? = null,
    secondary: Boolean = false,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val (bg, content) =
        when {
            !enabled -> ShifaColors.Interactive.DisabledBg to ShifaColors.Text.Disabled
            destructive -> ShifaColors.Interactive.Destructive to ShifaColors.Interactive.OnPrimary
            secondary -> ShifaColors.Bg.Canvas to ShifaColors.Icon.Primary
            else -> ShifaColors.Interactive.Primary to ShifaColors.Interactive.OnPrimary
        }
    val padding =
        when {
            text == null -> PaddingValues(ShifaSpacing.Inset.Xs)
            icon == null -> PaddingValues(horizontal = ShifaSpacing.Inset.Md, vertical = ShifaSpacing.Inset.Xs)
            else -> PaddingValues(horizontal = ShifaSpacing.Inset.Sm, vertical = ShifaSpacing.Inset.Xs)
        }
    Row(
        modifier =
            modifier
                .clip(ShifaRadius.Full)
                .background(bg)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(padding),
        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(painterResource(it), contentDescription, tint = content) }
        text?.let {
            val style = MaterialTheme.typography.labelMedium
            Text(
                it,
                Modifier.lineBox(style),
                style = style,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Figma icon-button: circular, 24dp icon. */
@Composable
fun ShifaIconButton(
    icon: DrawableResource,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: Boolean = false,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val accent =
        when {
            !enabled -> if (secondary) ShifaColors.Border.Disabled else ShifaColors.Interactive.DisabledBg
            destructive -> ShifaColors.Interactive.Destructive
            else -> ShifaColors.Interactive.Primary
        }
    Box(
        modifier =
            modifier
                .dropShadow(ShifaRadius.Full, ShifaShadow.Default)
                .clip(ShifaRadius.Full)
                .then(
                    if (secondary) {
                        Modifier.background(ShifaColors.Bg.Surface).border(ShifaSize.S2, accent, ShifaRadius.Full)
                    } else {
                        Modifier.background(accent)
                    },
                ).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(ShifaDimens.IconButtonPadding),
    ) {
        Icon(painterResource(icon), contentDescription, tint = if (secondary) accent else ShifaColors.Icon.Inverse)
    }
}

@Composable
private fun ButtonContent(
    text: String,
    icon: DrawableResource?,
    color: Color,
    loading: Boolean,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            // Static in previews / screenshot tests, where an infinite animation never lets the UI idle.
            val angle =
                if (LocalInspectionMode.current) {
                    0f
                } else {
                    rememberInfiniteTransition().animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
                    ).value
                }
            Icon(
                painterResource(Res.drawable.ic_loader_22),
                contentDescription = text,
                modifier = Modifier.graphicsLayer { rotationZ = angle },
                tint = color,
            )
        } else {
            icon?.let { Icon(painterResource(it), contentDescription = null, tint = color) }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
