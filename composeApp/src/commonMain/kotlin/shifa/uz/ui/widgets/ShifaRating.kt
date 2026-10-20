package shifa.uz.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.painterResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_star_24
import shifa.composeapp.generated.resources.ic_star_filled_24
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing

/** Figma's five stars: [rating] filled amber, the rest outlined. With [onRate], a tap sets 1 to 5. */
@Composable
fun ShifaRating(
    rating: Int,
    modifier: Modifier = Modifier,
    size: Dp = ShifaSize.S16,
    onRate: ((Int) -> Unit)? = null,
) = Row(modifier, horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
    (1..5).forEach { star ->
        val filled = star <= rating
        Icon(
            painterResource(if (filled) Res.drawable.ic_star_filled_24 else Res.drawable.ic_star_24),
            contentDescription = if (onRate != null) "$star" else null,
            modifier =
                Modifier
                    .size(size)
                    .clip(ShifaRadius.Full)
                    .then(if (onRate != null) Modifier.clickable { onRate(star) } else Modifier),
            tint = if (filled) ShifaColors.Icon.Warning else ShifaColors.Icon.Primary,
        )
    }
}
