package dali.hamza.echangecurrencyapp.ui

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            val viewModel = remember { GlobalContext.get().get<SharedViewModel>() }
            ExchangeCurrencyApp(viewModel = viewModel)
        }
    }
}
