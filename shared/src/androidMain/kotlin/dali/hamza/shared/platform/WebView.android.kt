package dali.hamza.shared.platform

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun PlatformWebView(html: String, baseUrl: String?, modifier: Modifier, url: String?) {
    // AndroidView's update block runs on every recomposition — reloading
    // unconditionally would restart the page (a live chat would lose its
    // session mid-conversation), so only load when the payload changed.
    val loadedKey: MutableState<String?> = remember { mutableStateOf(null) }
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
            val key = url ?: (baseUrl.orEmpty() + "\n\n" + html)
            if (loadedKey.value != key) {
                loadedKey.value = key
                if (url != null) {
                    web.loadUrl(url)
                } else {
                    web.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null)
                }
            }
        },
    )
}
