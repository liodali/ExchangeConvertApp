package dali.hamza.shared.platform

import android.content.Intent
import android.net.Uri
import dali.hamza.shared.AndroidAppContext

actual fun openUri(url: String) {
    val context = AndroidAppContext.appContext ?: return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

actual fun appVersionName(): String {
    val context = AndroidAppContext.appContext ?: return "—"
    return runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        info.versionName ?: "—"
    }.getOrDefault("—") ?: "—"
}
