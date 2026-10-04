package dali.hamza.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Minimal full-bleed HTML surface (platform WebView), used by the
 * Chatwoot live-chat screen and the in-app legal pages. Loads [html]
 * as a data page with [baseUrl] as its origin — the chat iframe needs
 * storage/session access, which a null origin blocks. When [url] is
 * set, the URL is loaded directly instead (legal pages on the backend).
 */
@Composable
expect fun PlatformWebView(
    html: String,
    baseUrl: String? = null,
    modifier: Modifier = Modifier,
    url: String? = null,
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
 * Full-page Chatwoot chat — hosts the inbox `/widget` endpoint as a
 * full-bleed iframe (Chatwoot's documented full-page-chat approach) and
 * mirrors the website SDK's postMessage handshake.
 *
 * Why not the widget SDK snippet: the SDK only auto-opens its panel via
 * the launcher bubble, and `chatwootSettings.type` accepts just
 * `"standard"`/`"expanded_bubble"` — `"expanded"` is silently ignored.
 * Combined with `hideMessageBubble: true` there is no bubble to tap and
 * the hidden panel never opens → blank page. The `/widget` iframe IS the
 * chat UI the SDK overlays, so we host it directly and drive it with the
 * same `config-set`/`set-user` messages the SDK sends.
 *
 * [userName] tags the conversation with the guest's display name and
 * [dark] switches the widget to Chatwoot's dark color scheme. The page
 * paints [backgroundHex] so the web surface blends with the app theme.
 */
fun chatwootHtml(backgroundHex: String, userName: String? = null, dark: Boolean = false): String {
    // the name is embedded in a JS string literal — strip what could break out
    val safeName = userName?.replace("\\", "")?.replace("'", "")?.trim().orEmpty()
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"/>
<style>
html,body{margin:0;padding:0;height:100%;background:$backgroundHex;overflow:hidden}
iframe{border:0;position:fixed;top:0;left:0;width:100%;height:100%}
</style>
</head>
<body>
<iframe id="chat" title="chat"></iframe>
<script>
(function () {
  var user = '$safeName';
  var frame = document.getElementById('chat');
  // resume the previous conversation if the SDK-style cookie survived
  var src = '${ChatwootConfig.BASE_URL}/widget?website_token=${ChatwootConfig.WEBSITE_TOKEN}';
  var prior = (document.cookie.match(/(?:^|;\s*)cw_conversation=([^;]+)/) || [])[1];
  if (prior) { src += '&cw_conversation=' + prior; }
  frame.src = src;
  function send(event, extra) {
    frame.contentWindow.postMessage(
      'chatwoot-widget:' + JSON.stringify(Object.assign({ event: event }, extra || {})),
      '*'
    );
  }
  window.addEventListener('message', function (e) {
    var d = e.data;
    if (typeof d !== 'string' || d.indexOf('chatwoot-widget:') !== 0) { return; }
    var msg;
    try { msg = JSON.parse(d.slice('chatwoot-widget:'.length)); } catch (err) { return; }
    if (msg.event !== 'loaded') { return; }
    // same handshake the widget SDK performs once the iframe is ready
    send('config-set', {
      locale: 'en',
      position: 'right',
      widgetStyle: 'standard',
      hideMessageBubble: true,
      showUnreadMessagesDialog: false,
      darkMode: '${if (dark) "dark" else "light"}',
      welcomeTitle: 'Hi there 👋',
      welcomeDescription: 'Ask us anything about Sovereign Ledger — currencies, rates, or feedback.',
      availableMessage: 'We are online',
      unavailableMessage: 'We are away — leave a message and we will get back to you.'
    });
    if (user) {
      send('set-user', {
        identifier: 'guest_' + user.toLowerCase(),
        user: { name: user, identifier: 'guest' }
      });
    }
    // persist the session token the way the SDK does (cw_conversation
    // cookie on the Chatwoot origin — our page origin via baseUrl)
    var token = msg.config && msg.config.authToken;
    if (token) {
      document.cookie = 'cw_conversation=' + encodeURIComponent(token) +
        ';path=/;max-age=31536000;secure;samesite=lax';
    }
  });
})();
</script>
</body>
</html>
""".trimIndent()
}
