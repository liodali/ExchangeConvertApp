package dali.hamza.shared.platform

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun PlatformWebView(html: String, baseUrl: String?, modifier: Modifier, url: String?) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // matches the app theme so the page never flashes white
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
            }
        },
        update = { web ->
            if (url != null) {
                web.loadUrl(url)
            } else {
                web.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null)
            }
        },
    )
}
