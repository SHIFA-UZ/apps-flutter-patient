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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import org.jetbrains.compose.resources.painterResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_x_20
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.theme.lineBox

/**
 * Figma input-field. States map to: disabled = !enabled, error = isError,
 * active = focused, filled = non-empty value, default = otherwise.
 */
@Composable
fun ShifaInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    clearable: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val border =
        when {
            !enabled -> ShifaColors.Border.Disabled
            isError -> ShifaColors.Border.Error
            focused -> ShifaColors.Border.Focus
            else -> ShifaColors.Border.Default
        }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ShifaColors.Text.Primary),
            cursorBrush = SolidColor(ShifaColors.Border.Focus),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Row(
                    modifier =
                        Modifier
                            .clip(ShifaRadius.ControlMd)
                            .background(if (enabled) ShifaColors.Bg.Surface else ShifaColors.Bg.Disabled)
                            .border(ShifaSize.S2, border, ShifaRadius.ControlMd)
                            .padding(horizontal = ShifaSpacing.Inset.Sm, vertical = ShifaSpacing.Inset.Xs),
                    horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).height(ShifaSize.S30), verticalArrangement = Arrangement.Center) {
                        val floating = enabled && (focused || isError || value.isNotEmpty())
                        if (floating) Text(label, style = ShifaTextStyles.caption(), color = ShifaColors.Text.Tertiary)
                        Box(Modifier.lineBox(MaterialTheme.typography.bodyLarge)) {
                            if (!floating && value.isEmpty()) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (enabled) ShifaColors.Text.Tertiary else ShifaColors.Text.Disabled,
                                )
                            }
                            innerTextField()
                        }
                    }
                    if (enabled && clearable) {
                        Icon(
                            painterResource(Res.drawable.ic_x_20),
                            contentDescription = "Clear",
                            modifier = Modifier.clip(ShifaRadius.Full).clickable { onValueChange("") },
                            tint = ShifaColors.Icon.Secondary,
                        )
                    }
                }
            },
        )
        supportingText?.let {
            Box(
                Modifier.fillMaxWidth().height(ShifaSize.S16).padding(horizontal = ShifaSpacing.Inset.Sm),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    it,
                    style = ShifaTextStyles.caption(),
                    color = if (isError) ShifaColors.Text.Error else ShifaColors.Text.Primary,
                )
            }
        }
    }
}
