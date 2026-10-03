package dali.hamza.shared.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dali.hamza.shared.platform.PlatformWebView
import dali.hamza.shared.ui.components.LedgerTopAppBar

/**
 * In-app legal page (Terms / Privacy) — the backend's styled HTML inside
 * a themed WebView, keeping users inside the app instead of a browser.
 */
@Composable
fun LegalScreen(
    title: String,
    url: String,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LedgerTopAppBar(
            title = title,
            onBack = onBack,
        )
        PlatformWebView(
            html = "",
            url = url,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding(),
        )
    }
}
