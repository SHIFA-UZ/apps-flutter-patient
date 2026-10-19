package shifa.uz.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_chevron_left_24
import shifa.composeapp.generated.resources.ic_chevron_right_24
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.theme.lineBox

/** Figma date-bubble states. Only [Default] and [Filled] can be tapped. */
enum class ShifaDateState(internal val bg: Color, internal val text: Color) {
    NonMonth(ShifaColors.Bg.Surface, ShifaColors.Text.Disabled),
    Default(ShifaColors.Bg.Canvas, ShifaColors.Text.Primary),
    Disabled(ShifaColors.Bg.Disabled, ShifaColors.Text.Disabled),
    Filled(ShifaColors.Icon.Brand, ShifaColors.Text.Inverse),
}

data class ShifaCalendarDay(val label: String, val state: ShifaDateState)

/** Figma calendar-card: the month toggle, then [days] in weeks of 7. A null [onPrev] / [onNext] hides that arrow. */
@Composable
fun ShifaCalendarCard(
    title: String,
    days: List<ShifaCalendarDay>,
    onDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onPrev: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
) = CardChrome(modifier, null, gap = ShifaDimens.CalendarRowGap) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MonthArrow(Res.drawable.ic_chevron_left_24, "Previous month", onPrev)
        val style = ShifaTextStyles.profileName()
        Text(
            title,
            Modifier.weight(1f).lineBox(style),
            style = style,
            color = ShifaColors.Text.Primary,
            textAlign = TextAlign.Center,
        )
        MonthArrow(Res.drawable.ic_chevron_right_24, "Next month", onNext)
    }
    days.chunked(7).forEachIndexed { week, row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            row.forEachIndexed { i, day -> DateBubble(day) { onDay(week * 7 + i) } }
        }
    }
}

@Composable
private fun MonthArrow(
    icon: DrawableResource,
    description: String,
    onClick: (() -> Unit)?,
) = Box(
    Modifier
        .size(ShifaSize.S24)
        .clip(ShifaRadius.Full)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
) {
    if (onClick != null) Icon(painterResource(icon), description, tint = ShifaColors.Icon.Primary)
}

@Composable
private fun DateBubble(
    day: ShifaCalendarDay,
    onClick: () -> Unit,
) {
    val tappable = day.state == ShifaDateState.Default || day.state == ShifaDateState.Filled
    Box(
        Modifier
            .size(ShifaSize.S36)
            .clip(ShifaRadius.Full)
            .background(day.state.bg)
            .then(if (tappable) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val style = MaterialTheme.typography.titleSmall
        Text(day.label, Modifier.lineBox(style), style = style, color = day.state.text)
    }
}
