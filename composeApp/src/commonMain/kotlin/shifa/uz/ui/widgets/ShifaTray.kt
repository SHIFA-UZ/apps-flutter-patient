package shifa.uz.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.attach
import shifa.composeapp.generated.resources.close
import shifa.composeapp.generated.resources.ic_arrow_up_18
import shifa.composeapp.generated.resources.ic_plus_18
import shifa.composeapp.generated.resources.ic_x_16
import shifa.composeapp.generated.resources.send
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaShadow
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles

// Trays are visual only. In a ModalBottomSheet pass shape = RectangleShape,
// containerColor = Color.Transparent and dragHandle = null.
private val TrayShape = ShifaRadius.Xl.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize)

/** Figma tray, type = template (and upload: [content] = secondary buttons). */
@Composable
fun ShifaTray(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = TraySheet(TrayShape, modifier) {
    TrayHeader(title, onClose)
    content()
}

/** Figma tray, type = shifa-chat. [chatBar] is usually a [ShifaChatBar]; it fills the space between the buttons. */
@Composable
fun ShifaChatTray(
    onAttach: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    suggestions: List<String> = emptyList(),
    onSuggestion: (String) -> Unit = {},
    sendEnabled: Boolean = true,
    chatBar: @Composable () -> Unit,
) = TraySheet(TrayShape, modifier) {
    if (suggestions.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs),
        ) {
            suggestions.forEach { ShifaActionButton({ onSuggestion(it) }, text = it, secondary = true) }
        }
    }
    // The tray sits on the screen's bottom edge, so it pads for the system bar itself. No shadows in the row (1772:17215).
    Row(
        Modifier.navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShifaIconButton(
            Res.drawable.ic_plus_18,
            stringResource(Res.string.attach),
            onAttach,
            secondary = true,
            shadow = false,
        )
        Box(Modifier.weight(1f)) { chatBar() }
        ShifaIconButton(
            Res.drawable.ic_arrow_up_18,
            stringResource(Res.string.send),
            onSend,
            enabled = sendEnabled,
            shadow = false,
        )
    }
}

// Shared by tray and pop-up.
@Composable
internal fun TraySheet(
    shape: Shape,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Column(
    modifier
        .fillMaxWidth()
        .dropShadow(shape, ShifaShadow.Tray)
        .background(ShifaColors.Bg.Surface, shape)
        .padding(horizontal = ShifaSpacing.Inset.Md, vertical = ShifaSpacing.Stack.Md),
    verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
    content = content,
)

@Composable
internal fun TrayHeader(
    title: String,
    onClose: () -> Unit,
) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(
        title,
        Modifier.weight(1f),
        style = ShifaTextStyles.trayTitle(),
        color = ShifaColors.Text.Primary,
        textAlign = TextAlign.Center,
    )
    ShifaActionButton(
        onClose,
        icon = Res.drawable.ic_x_16,
        contentDescription = stringResource(Res.string.close),
        secondary = true,
    )
}
