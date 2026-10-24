package dali.hamza.shared.platform

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dali.hamza.shared.AndroidAppContext

actual fun currentLanguageTag(): String =
    java.util.Locale.getDefault().toLanguageTag()

actual fun openLanguageSettings() {
    val context = AndroidAppContext.appContext ?: return
    val packageUri = Uri.fromParts("package", context.packageName, null)
    // 33+: the dedicated per-app language picker (locales_config.xml lists
    // our seven locales). Older: app details — locale follows the system.
    val intent = if (Build.VERSION.SDK_INT >= 33) {
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, packageUri)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
    }.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    runCatching { context.startActivity(intent) }
}
