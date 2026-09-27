package shifa.uz.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.home
import shifa.composeapp.generated.resources.logOut
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaSecondaryButton

// ponytail: placeholder until HOME-01 (Figma home); Log out stands in for AUTH-08's account screen.
@Composable
fun HomeScreen(onLogOut: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).systemBarsPadding().padding(ShifaSpacing.Inset.Lg),
        verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(Res.string.home),
            style = MaterialTheme.typography.headlineMedium,
            color = ShifaColors.Text.Primary,
        )
        ShifaSecondaryButton(stringResource(Res.string.logOut), onLogOut)
    }
}
