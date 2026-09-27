package shifa.uz.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.lineBox

enum class ShifaTagType(internal val bg: Color, internal val text: Color) {
    Primary(ShifaColors.Bg.SuccessSubtle, ShifaColors.Text.Brand),
    Secondary(ShifaColors.Bg.InfoSubtle, ShifaColors.Text.Info),
    Tertiary(ShifaColors.Bg.WarningSubtle, ShifaColors.Text.Warning),
}

@Composable
fun ShifaTag(
    text: String,
    modifier: Modifier = Modifier,
    type: ShifaTagType = ShifaTagType.Secondary,
) {
    Text(
        text = text,
        modifier =
            modifier
                .background(type.bg, ShifaRadius.Full)
                .padding(horizontal = ShifaSpacing.Inset.Sm, vertical = ShifaSpacing.Stack.Xs)
                .lineBox(MaterialTheme.typography.labelSmall),
        style = MaterialTheme.typography.labelSmall,
        color = type.text,
        maxLines = 1,
    )
}
