package dali.hamza.shared.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import platform.CoreGraphics.CGRectZero
import platform.WebKit.WKWebView
import platform.WebKit.javaScriptEnabled

@Composable
actual fun PlatformWebView(html: String, modifier: Modifier) {
    UIKitView(
        modifier = modifier.fillMaxSize(),
        factory = {
            WKWebView(frame = CGRectZero.readValue()).apply {
                configuration.defaultWebpagePreferences.javaScriptEnabled = true
                allowsBackForwardNavigationGestures = false
                loadHTMLString(html, null)
            }
        },
        update = { /* static page — no updates needed */ },
    )
}
