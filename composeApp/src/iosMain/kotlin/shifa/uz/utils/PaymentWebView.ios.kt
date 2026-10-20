package shifa.uz.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

// ponytail: links to other apps (a bank's) stay in the page; a WKNavigationDelegate opens them if Click needs it.
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PaymentWebView(
    url: String,
    modifier: Modifier,
) {
    UIKitView(
        factory = {
            WKWebView(CGRectMake(0.0, 0.0, 0.0, 0.0), WKWebViewConfiguration()).apply {
                NSURL.URLWithString(url)?.let { loadRequest(NSURLRequest.requestWithURL(it)) }
            }
        },
        modifier = modifier,
    )
}
