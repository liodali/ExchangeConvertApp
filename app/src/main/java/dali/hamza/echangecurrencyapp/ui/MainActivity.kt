package dali.hamza.echangecurrencyapp.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dali.hamza.shared.ui.ExchangeCurrencyApp
import dali.hamza.shared.ui.viewmodel.SharedViewModel
import org.koin.core.context.GlobalContext

/**
 * Hosts the shared Compose Multiplatform UI (Sovereign Ledger).
 *
 * Keyboard UX (both platform-level — the Compose focus observer in the
 * shared root covers iOS):
 * - `stateHidden` (manifest): the IME is NEVER restored open on launch —
 *   no autofocus when the app opens.
 * - `dispatchTouchEvent`: every touch first clears focus and hides the
 *   IME. Tapping a text field re-requests focus during the same gesture,
 *   so inputs keep working; tapping anywhere else dismisses the keyboard.
 *   (No bounds check on purpose: in Compose, `currentFocus` is the whole
 *   Compose surface, so a bounds test can never detect "outside".)
 */
class MainActivity : FragmentActivity() {

    private lateinit var updateChecker: AppUpdateChecker

    /** Persisted appearance, resolved in onCreate before Compose draws —
     *  used for the window background and the update dialogs. */
    private var darkTheme = true

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(window.decorView.windowToken, 0)
        }
        return super.dispatchTouchEvent(event)
    }

    /**
     * Covers warm resume (reopen from recents): any focus and IME left over
     * from the previous session are cleared. onResume never fires during
     * keyboard interaction, so it cannot fight the user typing.
     */
    override fun onResume() {
        super.onResume()
        currentFocus?.clearFocus()
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(window.decorView.windowToken, 0)
        // re-check Play for updates (also re-nudges a finished download
        // after warm resume)
        if (::updateChecker.isInitialized) updateChecker.refresh()
        // rate alerts: one throttled engine pass on foreground
        dali.hamza.shared.platform.RateAlertsAndroid.checkOnForeground()
    }

    /**
     * Forwards the Android 13+ notification permission result (requested
     * from the shared Rate Alerts UI) back into the shared module.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        dali.hamza.shared.platform.NotificationPermissionBridge
            .handleResult(requestCode, grantResults)
    }

    /**
     * Compose RESTORES the saved focus of the amount field during the first
     * composition — AFTER onResume — and the restored editor re-opens the
     * IME even with stateHidden. This hook fires last, when the window
     * actually gains focus, and clears it. It never fires while the user is
     * typing (opening the soft IME does not change activity window focus).
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(window.decorView.windowToken, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dali.hamza.shared.AndroidAppContext.currentActivity = this
        installSplashScreen()
        // In-app update prompts (Play In-App Updates) — see AppUpdateChecker
        updateChecker = AppUpdateChecker(this)
        // Flutter-style edge-to-edge: system bars stay visible and transparent,
        // content draws behind them; M3 Scaffold insets position the content
        enableEdgeToEdge()
        // Cold-start background follows the persisted appearance so Light
        // mode doesn't flash dark before Compose draws (shared_session is
        // written by the shared AndroidSessionStorage).
        runCatching {
            val prefs = getSharedPreferences("shared_session", MODE_PRIVATE)
            val systemDark = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            val dark = when (prefs.getString("theme_mode", "dark")) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }
            darkTheme = dark
            window.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(
                    if (dark) android.graphics.Color.parseColor("#0E0E0E")
                    else android.graphics.Color.parseColor("#F5F7F2")
                )
            )
        }
        setContent {
            val viewModel = remember { GlobalContext.get().get<SharedViewModel>() }
            ExchangeCurrencyApp(viewModel = viewModel)
            UpdateDialogs(
                state = updateChecker.uiState,
                dark = darkTheme,
                onUpdate = updateChecker::startFlexibleUpdate,
                onDecline = updateChecker::decline,
                onRestart = updateChecker::completeUpdate,
            )
        }
    }

    override fun onDestroy() {
        updateChecker.destroy()
        super.onDestroy()
    }
}
