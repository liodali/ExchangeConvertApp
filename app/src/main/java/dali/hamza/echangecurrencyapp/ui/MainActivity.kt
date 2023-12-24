package dali.hamza.echangecurrencyapp.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dali.hamza.core.common.SessionManager
import dali.hamza.echangecurrencyapp.ui.compose.page.Home
import dali.hamza.echangecurrencyapp.ui.compose.page.SelectCurrencyPage
import dali.hamza.echangecurrencyapp.ui.compose.theme.ExchangeCurrencyAppTheme
import dali.hamza.echangecurrencyapp.ui.dialog.DialogCurrenciesFragment
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import dali.hamza.echangecurrencyapp.viewmodel.State
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.compose.KoinContext


class MainActivity : AppCompatActivity(), DialogCurrenciesFragment.DialogCurrencySelectionCallback {

    //private val mainViewModel: MainViewModel by  viewModel<MainViewModel>()
    private lateinit var dialogCurrencySelection: DialogCurrenciesFragment

    var isLoadingCurrentCurrency: State by mutableStateOf(State.NOT_LOADING)
    var firstDestination: String by mutableStateOf("home")
    private val sessionManager: SessionManager by inject()


    /*companion object {
        @JvmStatic
        val mainViewModelComposition =
            compositionLocalOf<MainViewModel> { error("No viewModel found!") }

    }*/

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition {
            isLoadingCurrentCurrency != State.FINISH
        }
        lifecycleScope.launch {
            repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                isLoadingCurrentCurrency = State.LOADING
                sessionManager.getCurrencyFromDataStore.collectLatest { currency ->
                    if(currency.isEmpty()){
                        firstDestination = "selectCurrency"

                    }
                    isLoadingCurrentCurrency = State.FINISH

                }
            }
        }
        setContent {
            val navController = rememberNavController()


            /*LaunchedEffect(mainViewModel.getCurrencySelection()) {
                if (mainViewModel.getCurrencySelection().isNullOrEmpty()
                    && mainViewModel.isLoadingCurrentCurrency == State.FINISH
                ) {
                    firstDestination = "selectCurrency"
                }
            }*/
            KoinContext {
                App(
                    //mainViewModel = mainViewModel,
                    navController = navController,
                    firstPage = firstDestination
                )
            }
        }

    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun App(
      //  mainViewModel: MainViewModel,
        navController: NavHostController,
        firstPage: String = "home"
    ) {

        ExchangeCurrencyAppTheme {
            NavHost(navController = navController, startDestination = firstPage) {
                composable("home") {
                    KoinContext {
                        Home(
                            openFragment = {
                                // openCurrenciesSelectionBottomSheet()
                            }
                        )
                    }
                }
                composable("selectCurrency") {
                    KoinContext {
                        SelectCurrencyPage(
                            modifier = Modifier,
                            onSelect = { nCurrency ->
                               // mainViewModel.setCurrencySelection(nCurrency)
                               // mainViewModel.setCurrencySelection(nCurrency)
                                navController.navigate("home") {
                                    popUpTo("selectCurrency")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun openCurrenciesSelectionBottomSheet() {
        /*dialogCurrencySelection = DialogCurrenciesFragment.newInstance(
            selectedCurrency = mainViewModel.getCurrencySelection() ?: "",
            this
        )
        dialogCurrencySelection.show(supportFragmentManager.also {
            val prevFrag = it.findFragmentByTag(DialogCurrenciesFragment.tag)
            if (prevFrag != null) {
                it.beginTransaction().remove(prevFrag)
            }
            it.beginTransaction().addToBackStack(null)
        }, DialogCurrenciesFragment.tag)*/
    }

    override fun resetUIState() {
        //mainViewModel.changeAmount("")
        // mainViewModel.resetExchangeRates()
    }
}

