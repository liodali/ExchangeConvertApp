package dali.hamza.shared.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dali.hamza.shared.platform.ChatwootConfig
import dali.hamza.shared.platform.PlatformWebView
import dali.hamza.shared.platform.chatwootHtml
import dali.hamza.shared.ui.components.EmptyState
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LedgerStrings

/**
 * Live chat / feedback — the Chatwoot website widget in a WebView
 * (self-hosted inbox; conversations are managed there). Gated on
 * [ChatwootConfig]: without a website token the screen shows a
 * coming-soon state instead of a broken page.
 */
@Composable
fun ChatwootScreen(onBack: () -> Unit) {
    val configured = ChatwootConfig.WEBSITE_TOKEN.isNotBlank()
    // theme-matched page background so the web surface blends in
    val bg = LedgerColors.Canvas
    val html = remember(bg) { chatwootHtml(backgroundHex = bg.toPaddedHex()) }

    Column(modifier = Modifier.fillMaxSize()) {
        LedgerTopAppBar(
            title = LedgerStrings.Support.CHAT_TITLE,
            onBack = onBack,
        )
        if (configured) {
            PlatformWebView(
                html = html,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyState(
                        title = LedgerStrings.Support.CHAT_UNAVAILABLE,
                        message = LedgerStrings.Support.CHAT_UNAVAILABLE_DESC,
                        icon = Icons.Outlined.Forum,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Feedback: ${LedgerStrings.APP_TITLE} · Account → Support",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerColors.TextTertiary,
                    )
                }
            }
        }
    }
}

/** `#rrggbb` for the WebView page background (palette-aware). */
private fun androidx.compose.ui.graphics.Color.toPaddedHex(): String {
    fun c(v: Float): String {
        val h = (v.coerceIn(0f, 1f) * 255f).toInt().toString(16)
        return if (h.length < 2) "0$h" else h
    }
    return "#${c(red)}${c(green)}${c(blue)}"
}
