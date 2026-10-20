package shifa.uz.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A provider's checkout page (Click, Stripe) in the app, as Figma 1825:17388 has it. Links that aren't web pages
 * (a bank or wallet app) open that app. [onNavigate] sees every page the checkout moves to and returns true to keep
 * it from loading (the backend's "return to the app" pages). Empty in previews.
 */
@Composable
expect fun PaymentWebView(
    url: String,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Boolean = { false },
)
