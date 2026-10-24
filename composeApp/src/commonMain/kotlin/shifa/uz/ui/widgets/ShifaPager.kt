package shifa.uz.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.pageOf
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing

/** Figma pager: [count] dots, the one at [current] (0-based, e.g. PagerState.currentPage) highlighted. */
@Composable
fun ShifaPager(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.pageOf, current + 1, count)
    Row(
        modifier
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(ShifaSpacing.Inset.Xs),
        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
    ) {
        repeat(count) {
            Box(
                Modifier
                    .size(ShifaSize.S12)
                    .background(if (it == current) ShifaColors.Bg.Brand else ShifaColors.Bg.Canvas, ShifaRadius.Full),
            )
        }
    }
}
