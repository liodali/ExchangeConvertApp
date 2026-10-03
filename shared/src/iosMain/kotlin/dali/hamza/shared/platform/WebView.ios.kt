package dali.hamza.shared.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKPreferences
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.javaScriptEnabled

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformWebView(html: String, baseUrl: String?, modifier: Modifier, url: String?) {
    UIKitView(
        modifier = modifier.fillMaxSize(),
        factory = {
            val preferences = WKPreferences().apply { javaScriptEnabled = true }
            val config = WKWebViewConfiguration().apply {
                this.preferences = preferences
            }
            WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config).apply {
                allowsBackForwardNavigationGestures = false
                if (url != null) {
                    val nsUrl = NSURL.URLWithString(url)
                    if (nsUrl != null) {
                        loadRequest(NSURLRequest.requestWithURL(nsUrl))
                    }
                } else {
                    val nsBase = baseUrl?.let { NSURL.URLWithString(it) }
                    loadHTMLString(html, nsBase)
                }
            }
        },
        update = { /* static page — no updates needed */ },
    )
}
