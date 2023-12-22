package dali.hamza.echangecurrencyapp.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dali.hamza.core.common.SessionManager
import dali.hamza.echangecurrencyapp.ui.compose.page.Home
import dali.hamza.echangecurrencyapp.ui.compose.page.SplashScreen
import dali.hamza.echangecurrencyapp.ui.compose.theme.ExchangeCurrencyAppTheme
import dali.hamza.echangecurrencyapp.ui.dialog.DialogCurrenciesFragment
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.dsl.koinApplication


class MainActivity : AppCompatActivity(), DialogCurrenciesFragment.DialogCurrencySelectionCallback {

    private val mainViewModel: MainViewModel by viewModel<MainViewModel>()
    private lateinit var dialogCurrencySelection: DialogCurrenciesFragment


    private val sessionManager: SessionManager by inject()


    companion object {
        @JvmStatic
        val mainViewModelComposition
             = compositionLocalOf<MainViewModel>{ error("No viewModel found!") }

    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
               /* var pageInit by remember {
                    mutableStateOf("splash")
                }

                // This will always refer to the latest onTimeout function that
                // LandingScreen was recomposed with
                val moveToHome by rememberUpdatedState {
                    pageInit = "page1"
                }

                // Create an effect that matches the lifecycle of LandingScreen.
                // If LandingScreen recomposes, the delay shouldn't start again.
                LaunchedEffect(true) {
                    delay(200)
                    moveToHome()
                }*/
                CompositionLocalProvider(mainViewModelComposition provides mainViewModel) {
                    ExchangeCurrencyAppTheme {
                        Crossfade(targetState = "home", label = "") { page ->
                            when (page) {
                                "splash" -> SplashScreen()
                                else ->
                                    Home(
                                        openFragment = {
                                            openCurrenciesSelectionBottomSheet()
                                        }
                                    )
                            }
                        }
                    }
                }

            }

        lifecycleScope.launch {
            repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                sessionManager.getCurrencyFromDataStore.collect { newCurrency ->
                    val cache = mainViewModel.getCurrencySelection()
                    mainViewModel.setCurrencySelection(newCurrency)
                    if (cache != null &&
                        cache.isNotEmpty()
                        && cache != mainViewModel.getCurrencySelection()
                    ) {
                        withContext(IO) {
                            sessionManager.removeTimeLastUpdateRate()
                            mainViewModel.retrieveOrUpdateRates(cache)
                        }
                    }
                }
            }
        }
    }




    private fun openCurrenciesSelectionBottomSheet() {
        dialogCurrencySelection = DialogCurrenciesFragment.newInstance(
            selectedCurrency = mainViewModel.getCurrencySelection() ?: "",
            this
        )
        dialogCurrencySelection.show(supportFragmentManager.also {
            val prevFrag = it.findFragmentByTag(DialogCurrenciesFragment.tag)
            if (prevFrag != null) {
               it.beginTransaction().remove(prevFrag)
            }
             it.beginTransaction().addToBackStack(null)
        }, DialogCurrenciesFragment.tag)
    }

    override fun resetUIState() {
        mainViewModel.changeAmount("")
        mainViewModel.resetExchangeRates()
    }
}

