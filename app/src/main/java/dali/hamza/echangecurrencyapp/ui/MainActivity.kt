package dali.hamza.echangecurrencyapp.ui

import android.graphics.Rect
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
 * The legacy app-local UI (Home / SelectCurrencyPage / converter pages) is
 * retired from hosting; its files stay in the tree until Phase 6 cleanup
 * verifies full feature parity (plans/redesign-migration-strategy.md).
 */
class MainActivity : ComponentActivity() {

    /**
     * Tap-outside dismisses the keyboard (platform-level, 100% reliable):
     * any touch that starts outside the focused editor clears focus and
     * hides the IME. Compose's own focus system follows.
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            currentFocus?.let { focused ->
                val rect = Rect()
                focused.getGlobalVisibleRect(rect)
                if (!rect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    focused.clearFocus()
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(focused.windowToken, 0)
                }
            }
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
