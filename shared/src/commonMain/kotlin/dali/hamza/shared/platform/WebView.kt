package dali.hamza.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Minimal full-bleed HTML surface (platform WebView), used by the
 * Chatwoot live-chat screen. [html] is loaded as a data page.
 */
@Composable
expect fun PlatformWebView(html: String, modifier: Modifier = Modifier)

/**
 * Chatwoot client configuration — the self-hosted inbox that manages
 * support conversations and product feedback.
 *
 * Website tokens are public by design (they ship in every web page that
 * embeds the widget), so these are plain constants, not secrets:
 *   - `BASE_URL`: the Chatwoot installation (e.g. https://chatwoot.example.com)
 *   - `WEBSITE_TOKEN`: Inbox → Settings → Configuration → API Settings
 *
 * An empty token disables the chat screen (coming-soon state).
 */
object ChatwootConfig {
    const val BASE_URL = ""
    const val WEBSITE_TOKEN = ""
}

/**
 * Standalone page hosting the Chatwoot website SDK: the chat opens
 * expanded (no floating bubble) and the page paints the app's theme
 * background so the transition into the web surface is seamless.
 */
fun chatwootHtml(backgroundHex: String): String = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"/>
<style>
html,body{margin:0;padding:0;height:100%;background:${backgroundHex};overflow:hidden}
</style>
</head>
<body>
<script>
window.chatwootSettings = {
  hideMessageBubble: true,
  type: 'expanded',
  position: 'right',
  locale: 'en'
};
(function(d,t){
  var BASE_URL = '${ChatwootConfig.BASE_URL}';
  var g = d.createElement(t), s = d.getElementsByTagName(t)[0];
  g.src = BASE_URL + '/packs/js/sdk.js';
  g.defer = true; g.async = true;
  s.parentNode.insertBefore(g, s);
  g.onload = function(){
    window.chatwootSDK.run({
      websiteToken: '${ChatwootConfig.WEBSITE_TOKEN}',
      baseUrl: BASE_URL
    });
  };
})(document, 'script');
</script>
</body>
</html>
""".trimIndent()
