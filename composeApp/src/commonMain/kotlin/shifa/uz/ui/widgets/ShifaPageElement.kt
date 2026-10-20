package shifa.uz.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing

enum class ShifaPageElementType(
    internal val bg: Color,
    internal val padding: PaddingValues,
) {
    Header(ShifaColors.Bg.Surface, PaddingValues()),
    FlexHeader(ShifaColors.Bg.Surface, PaddingValues(horizontal = ShifaSpacing.Inset.Md)),
    Body(ShifaColors.Bg.Surface, PaddingValues(ShifaSpacing.Inset.Md)),
    ActionTray(
        ShifaColors.Bg.Surface,
        PaddingValues(horizontal = ShifaSpacing.Inset.Md, vertical = ShifaSpacing.Inset.Sm),
    ),
    Footer(Color.Transparent, PaddingValues(ShifaSpacing.Inset.Md)),
}

/**
 * Figma page-element: a full-width page region around [content].
 * ActionTray stacks its actions (up to 3 in Figma) with a 12dp gap.
 */
@Composable
fun ShifaPageElement(
    type: ShifaPageElementType,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tray = type == ShifaPageElementType.ActionTray
    // Figma clips here; not clipping keeps the children's shadows intact.
    Column(
        modifier.fillMaxWidth().background(type.bg).padding(type.padding),
        verticalArrangement = if (tray) Arrangement.spacedBy(ShifaSpacing.Gap.Md) else Arrangement.Top,
        content = content,
    )
}
