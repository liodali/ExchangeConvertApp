package dali.hamza.echangecurrencyapp.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
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
class MainActivity : ComponentActivity() {

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            currentFocus?.clearFocus()
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(window.decorView.windowToken, 0)
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        // Flutter-style edge-to-edge: system bars stay visible and transparent,
        // content draws behind them; M3 Scaffold insets position the content
        enableEdgeToEdge()
        setContent {
            val viewModel = remember { GlobalContext.get().get<SharedViewModel>() }
            ExchangeCurrencyApp(viewModel = viewModel)
        }
    }
}
