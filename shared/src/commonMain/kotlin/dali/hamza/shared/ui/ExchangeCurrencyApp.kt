package dali.hamza.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dali.hamza.shared.ui.components.SharedBottomNavigation
import dali.hamza.shared.ui.components.SharedDestination
import dali.hamza.shared.ui.screens.ConverterCurrencyScreen
import dali.hamza.shared.ui.screens.RatesScreen
import dali.hamza.shared.ui.theme.ExchangeCurrencyAppTheme
import dali.hamza.shared.ui.viewmodel.SharedViewModel

/**
 * Root of the shared Compose Multiplatform app.
 * Hosted by:
 * - Android: MainActivity (planned, see plans/kmp-ui-unification-plan.md Phase 3/4)
 * - iOS:     MainViewController() → SwiftUI wrapper
 */
@Composable
fun ExchangeCurrencyApp(
    viewModel: SharedViewModel,
    modifier: Modifier = Modifier,
) {
    ExchangeCurrencyAppTheme {
        var destination by remember { mutableStateOf(SharedDestination.CONVERTER) }

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            bottomBar = {
                SharedBottomNavigation(
                    currentDestination = destination,
                    onSelectDestination = { destination = it }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (destination) {
                    SharedDestination.CONVERTER -> ConverterCurrencyScreen(viewModel)
                    SharedDestination.RATES -> RatesScreen(viewModel)
                }
            }
        }
    }
}
