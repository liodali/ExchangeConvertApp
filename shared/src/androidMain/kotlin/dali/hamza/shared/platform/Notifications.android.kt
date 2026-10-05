package dali.hamza.shared.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentActivity
import dali.hamza.shared.AndroidAppContext
import dali.hamza.shared.R
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android actual — NotificationManager with a dedicated "rate alerts"
 * channel. Posting is safe from the WorkManager background worker: it
 * no-ops without the POST_NOTIFICATIONS permission (API 33+) or when
 * notifications are disabled app-wide.
 */
internal class LocalNotifierAndroid : LocalNotifier {

    private val channelReady = AtomicBoolean(false)

    override fun areNotificationsEnabled(): Boolean {
        val context = AndroidAppContext.appContext ?: return false
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        val activity = AndroidAppContext.currentActivity
        when {
            Build.VERSION.SDK_INT < 33 ->
                onResult(areNotificationsEnabled())
            areNotificationsEnabled() -> onResult(true)
            activity != null -> NotificationPermissionBridge.request(activity, onResult)
            // no foreground activity to host the system dialog — retry later
            else -> onResult(false)
        }
    }

    override fun notify(id: Long, title: String, body: String) {
        val context = AndroidAppContext.appContext ?: return
        if (!areNotificationsEnabled()) return
        ensureChannel(context)

        val contentIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = contentIntent?.let {
            PendingIntent.getActivity(
                context,
                id.toInt(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        // monochrome glyph — status-bar icons render alpha only, so the
        // adaptive launcher icon would collapse into a white blob
        val icon = R.drawable.ic_stat_rate_alert

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(id.toInt(), notification)
        }
    }

    private fun ensureChannel(context: Context) {
        if (channelReady.get()) return
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rate alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Local exchange-rate alerts"
            }
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
        channelReady.set(true)
    }

    companion object {
        const val CHANNEL_ID = "rate_alerts"
    }
}

actual fun createLocalNotifier(): LocalNotifier = LocalNotifierAndroid()

/**
 * Bridges the asynchronous runtime-permission result back to the shared
 * [LocalNotifier]. The host activity forwards
 * [Activity.onRequestPermissionsResult] here (see MainActivity).
 */
object NotificationPermissionBridge {
    private const val REQUEST_CODE = 0x52A1 // "RATE"1

    @Volatile
    private var pending: ((Boolean) -> Unit)? = null

    fun request(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
        pending = onResult
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE,
        )
    }

    /** Returns true when the result was consumed (matching request code). */
    fun handleResult(requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode != REQUEST_CODE) return false
        val granted = grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        pending?.invoke(granted)
        pending = null
        return true
    }
}
