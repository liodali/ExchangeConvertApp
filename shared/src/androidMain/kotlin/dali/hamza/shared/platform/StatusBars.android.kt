package dali.hamza.shared.platform

import androidx.core.view.WindowCompat
import dali.hamza.shared.AndroidAppContext

/**
 * Android: flips the status-bar icon appearance (light icons on dark
 * themes, dark icons on light) on the foreground activity's window.
 */
actual fun applySystemBarIcons(darkTheme: Boolean) {
    val activity = AndroidAppContext.currentActivity ?: return
    val window = activity.window ?: return
    WindowCompat.getInsetsController(window, window.decorView)
        .isAppearanceLightStatusBars = !darkTheme
}
