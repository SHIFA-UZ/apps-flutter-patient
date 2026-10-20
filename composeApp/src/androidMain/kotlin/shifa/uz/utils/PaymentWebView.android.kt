package shifa.uz.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled") // the checkout pages need it
@Composable
actual fun PaymentWebView(
    url: String,
    modifier: Modifier,
    onNavigate: (String) -> Boolean,
) {
    if (LocalInspectionMode.current) return Box(modifier)
    val navigate by rememberUpdatedState(onNavigate)
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient =
                    object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val uri = request.url
                            if (navigate(uri.toString())) return true
                            if (uri.scheme == "http" || uri.scheme == "https") return false
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                            return true
                        }
                    }
                loadUrl(url)
            }
        },
        modifier = modifier,
        onRelease = { it.destroy() },
    )
}
