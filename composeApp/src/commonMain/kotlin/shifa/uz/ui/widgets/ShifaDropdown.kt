package shifa.uz.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.painterResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_check_12
import shifa.composeapp.generated.resources.ic_chevron_down_20
import shifa.composeapp.generated.resources.ic_chevron_up_20
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaShadow
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.lineBox

/**
 * Figma dropdown. Expands inline (the menu pushes content below). As in Figma, the header shows
 * [label] while expanded and the [selected] option once collapsed. Picking an option collapses it.
 */
@Composable
fun ShifaDropdown(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val style = MaterialTheme.typography.bodyLarge
    Column(
        if (expanded) {
            modifier
                .dropShadow(ShifaRadius.ControlMd, ShifaShadow.Dropdown)
                .clip(ShifaRadius.ControlMd)
                .background(ShifaColors.Bg.Surface)
                .border(ShifaSize.S2, ShifaColors.Border.Default, ShifaRadius.ControlMd)
                .padding(bottom = ShifaDimens.DropdownMenuPaddingBottom)
        } else {
            modifier
        },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(ShifaRadius.ControlMd)
                .background(ShifaColors.Bg.Surface)
                .border(
                    ShifaSize.S2,
                    if (isError) ShifaColors.Border.Error else ShifaColors.Border.Default,
                    ShifaRadius.ControlMd,
                ).clickable(role = Role.DropdownList) { onExpandedChange(!expanded) }
                .padding(horizontal = ShifaSpacing.Inset.Sm, vertical = ShifaSpacing.Inset.Xs),
            horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val value = selected.takeUnless { expanded }
            Box(Modifier.weight(1f).height(ShifaSize.S30), contentAlignment = Alignment.CenterStart) {
                Text(
                    value ?: label,
                    Modifier.lineBox(style),
                    style = style,
                    color = if (value == null) ShifaColors.Text.Tertiary else ShifaColors.Text.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painterResource(if (expanded) Res.drawable.ic_chevron_up_20 else Res.drawable.ic_chevron_down_20),
                contentDescription = null,
                tint = ShifaColors.Icon.Secondary,
            )
        }
        if (expanded) {
            options.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(option == selected, role = Role.RadioButton) {
                            onSelect(option)
                            onExpandedChange(false)
                        }
                        // Gap above each option is part of its touch target.
                        .padding(start = ShifaSpacing.Inset.Sm, end = ShifaSpacing.Inset.Sm, top = ShifaSpacing.Gap.Md),
                    horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (option == selected) {
                        Icon(painterResource(Res.drawable.ic_check_12), null, tint = ShifaColors.Icon.Brand)
                    }
                    Text(option, Modifier.lineBox(style), style = style, color = ShifaColors.Text.Primary)
                }
            }
        }
    }
}
