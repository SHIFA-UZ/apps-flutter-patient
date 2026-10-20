package shifa.uz.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIApplication
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationActionPolicy.WKNavigationActionPolicyAllow
import platform.WebKit.WKNavigationActionPolicy.WKNavigationActionPolicyCancel
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PaymentWebView(
    url: String,
    modifier: Modifier,
    onNavigate: (String) -> Boolean,
) {
    val navigate by rememberUpdatedState(onNavigate)
    // The web view only holds its delegate weakly, so this keeps it alive.
    val delegate =
        remember {
            object : NSObject(), WKNavigationDelegateProtocol {
                override fun webView(
                    webView: WKWebView,
                    decidePolicyForNavigationAction: WKNavigationAction,
                    decisionHandler: (WKNavigationActionPolicy) -> Unit,
                ) {
                    val target = decidePolicyForNavigationAction.request.URL
                    val scheme = target?.scheme
                    val allowed =
                        when {
                            target == null -> true
                            navigate(target.absoluteString.orEmpty()) -> false
                            scheme == "http" || scheme == "https" -> true
                            else -> {
                                UIApplication.sharedApplication.openURL(target, emptyMap<Any?, Any>(), null)
                                false
                            }
                        }
                    decisionHandler(if (allowed) WKNavigationActionPolicyAllow else WKNavigationActionPolicyCancel)
                }
            }
        }
    UIKitView(
        factory = {
            WKWebView(CGRectMake(0.0, 0.0, 0.0, 0.0), WKWebViewConfiguration()).apply {
                navigationDelegate = delegate
                NSURL.URLWithString(url)?.let { loadRequest(NSURLRequest.requestWithURL(it)) }
            }
        },
        modifier = modifier,
    )
}
