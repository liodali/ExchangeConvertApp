package dali.hamza.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Minimal full-bleed HTML surface (platform WebView), used by the
 * Chatwoot live-chat screen. [html] is loaded as a data page with
 * [baseUrl] as its origin — the widget SDK needs storage/session
 * access, which a null origin blocks.
 */
@Composable
expect fun PlatformWebView(
    html: String,
    baseUrl: String? = null,
    modifier: Modifier = Modifier,
)

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
    const val BASE_URL = "https://chatwoot.dev.adetify.com"
    const val WEBSITE_TOKEN = "ZrfM1iLhTVABeE5zHX7Q75rn"
}

/**
 * Standalone page hosting the Chatwoot website SDK: the chat opens
 * expanded (no floating bubble) and the page paints the app's theme
 * background so the transition into the web surface is seamless.
 * [userName] tags the conversation with the guest's display name.
 */
fun chatwootHtml(backgroundHex: String, userName: String? = null): String = """
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
var userName = ${userName?.let { "'$it'" } ?: "null"};
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
window.addEventListener('chatwoot:ready', function(){
  if (!userName || !window.chatwootSDK) return;
  try {
    window.${'$'}chatwoot.setUser('${'$'}chatwoot_user_'.concat(userName.toLowerCase()), {
      name: userName,
      identifier: 'guest'
    });
  } catch (e) { /* identity stays anonymous if setUser is unavailable */ }
});
</script>
</body>
</html>
""".trimIndent()
