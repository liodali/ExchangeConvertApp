package dali.hamza.echangecurrencyapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import dali.hamza.echangecurrencyapp.viewmodel.State
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.compose.KoinContext


class MainActivity : ComponentActivity() {


    private var isLoadingCurrentCurrency: State by mutableStateOf(State.NOT_LOADING)
    private var firstDestination: String by mutableStateOf("home")
    private val sessionManager: SessionManager by inject()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition {
            isLoadingCurrentCurrency != State.FINISH
        }
        lifecycleScope.launch {
            repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                isLoadingCurrentCurrency = State.LOADING
                sessionManager.getCurrencyFromDataStore.collectLatest { currency ->
                    if (currency.isEmpty()) {
                        firstDestination = "selectCurrency"
                        delay(200)
                    }
                    isLoadingCurrentCurrency = State.FINISH

                }
            }
        }
        setContent {
            val navController = rememberNavController()
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
        navController: NavHostController,
        firstPage: String = "home"
    ) {

        ExchangeCurrencyAppTheme {
            NavHost(navController = navController, startDestination = firstPage) {
                composable("home") {
                    KoinContext {
                        Home()
                    }
                }
                composable("selectCurrency") {
                    KoinContext {
                        SelectCurrencyPage(
                            modifier = Modifier,
                            onSelect = { _ ->
                                navController.popBackStack()
                                navController.navigate("home")
                            }
                        )
                    }
                }
            }
        }
    }

}

