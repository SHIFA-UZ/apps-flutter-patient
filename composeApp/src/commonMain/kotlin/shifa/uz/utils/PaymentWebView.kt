package shifa.uz.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A provider's checkout page (Click, Stripe) in the app, as Figma 1825:17388 has it. Links that aren't web pages
 * (a bank or wallet app) open that app. Empty in previews.
 */
@Composable
expect fun PaymentWebView(
    url: String,
    modifier: Modifier = Modifier,
)
