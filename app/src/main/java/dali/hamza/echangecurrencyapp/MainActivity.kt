package dali.hamza.echangecurrencyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import dagger.hilt.android.AndroidEntryPoint
import dali.hamza.echangecurrencyapp.ui.page.Home
import dali.hamza.echangecurrencyapp.ui.theme.ExchangeCurrencyAppTheme
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel

val mainViewModelComposition = compositionLocalOf<MainViewModel> { error("No viewModel found!") }

@ExperimentalMaterialApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(mainViewModelComposition provides viewModel) {
                ExchangeCurrencyAppTheme {
                        Home()
                }
            }

        }
    }
}

