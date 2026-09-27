package shifa.uz.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.bookings
import shifa.composeapp.generated.resources.documents
import shifa.composeapp.generated.resources.home
import shifa.composeapp.generated.resources.ic_nav_bookings
import shifa.composeapp.generated.resources.ic_nav_bookings_selected
import shifa.composeapp.generated.resources.ic_nav_documents
import shifa.composeapp.generated.resources.ic_nav_documents_selected
import shifa.composeapp.generated.resources.ic_nav_home
import shifa.composeapp.generated.resources.ic_nav_home_selected
import shifa.composeapp.generated.resources.ic_nav_messages
import shifa.composeapp.generated.resources.ic_nav_messages_selected
import shifa.composeapp.generated.resources.ic_nav_shifa_ai
import shifa.composeapp.generated.resources.messages
import shifa.composeapp.generated.resources.shifaAiTitle
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaShadow
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles

// Selected icons are drawn with a thicker stroke in Figma, hence two drawables per tab.
enum class ShifaTab(
    val label: StringResource,
    internal val icon: DrawableResource,
    internal val selectedIcon: DrawableResource,
) {
    Home(Res.string.home, Res.drawable.ic_nav_home, Res.drawable.ic_nav_home_selected),
    Bookings(Res.string.bookings, Res.drawable.ic_nav_bookings, Res.drawable.ic_nav_bookings_selected),
    ShifaAi(Res.string.shifaAiTitle, Res.drawable.ic_nav_shifa_ai, Res.drawable.ic_nav_shifa_ai),
    Documents(Res.string.documents, Res.drawable.ic_nav_documents, Res.drawable.ic_nav_documents_selected),
    Messages(Res.string.messages, Res.drawable.ic_nav_messages, Res.drawable.ic_nav_messages_selected),
}

@Composable
fun ShifaNavBar(
    selected: ShifaTab,
    onSelect: (ShifaTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .dropShadow(ShifaRadius.Full, ShifaShadow.NavBar)
            .background(ShifaColors.Alpha.White70, ShifaRadius.Full)
            .padding(horizontal = ShifaSpacing.Inset.Lg, vertical = ShifaSpacing.Inset.Md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        ShifaTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val color = if (isSelected) ShifaColors.Text.Brand else ShifaColors.Text.Primary
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .width(ShifaDimens.NavBarTileWidth)
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(tab) },
                        role = Role.Tab,
                        interactionSource = null,
                        indication = ripple(bounded = false),
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.padding(ShifaSize.S4)) {
                    Icon(
                        painterResource(if (isSelected) tab.selectedIcon else tab.icon),
                        contentDescription = null,
                        tint = if (isSelected) ShifaColors.Icon.Brand else ShifaColors.Icon.Primary,
                    )
                }
                Text(
                    stringResource(tab.label),
                    style =
                        ShifaTextStyles.caption().copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                    color = color,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
