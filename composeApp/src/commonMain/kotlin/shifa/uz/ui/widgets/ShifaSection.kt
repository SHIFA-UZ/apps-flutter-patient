package shifa.uz.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_arrow_right_16
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSpacing

// Figma names the background the section sits on: card-on-color = white on a coloured page, etc.
enum class ShifaSectionType(internal val bg: Color) {
    CardOnColor(ShifaColors.Alpha.White90),
    CardOnWhite(ShifaColors.Alpha.Teal15),
    CardTransparent(Color.Transparent),
}

/** Figma section: header ("Upcoming Appointment", "My Tasks") with an optional arrow, then [content]. */
@Composable
fun ShifaSection(
    title: String,
    modifier: Modifier = Modifier,
    type: ShifaSectionType = ShifaSectionType.CardOnColor,
    onMore: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            // Figma clips here; a rounded background alone keeps the cards' shadows intact.
            .background(type.bg, ShifaRadius.ControlLg)
            .padding(vertical = ShifaSpacing.Inset.Sm)
            .then(
                if (type == ShifaSectionType.CardTransparent) {
                    Modifier
                } else {
                    Modifier.padding(
                        horizontal = ShifaSpacing.Inset.Sm,
                    )
                },
            ),
        verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = ShifaColors.Text.Primary,
            )
            onMore?.let {
                ShifaActionButton(
                    it,
                    icon = Res.drawable.ic_arrow_right_16,
                    contentDescription = "See all",
                    secondary = true,
                )
            }
        }
        content()
    }
}
