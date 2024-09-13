package dali.hamza.echangecurrencyapp.ui.compose.page

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.ui.compose.component.Center
import dali.hamza.echangecurrencyapp.ui.compose.component.NavigationBottom
import dali.hamza.echangecurrencyapp.ui.compose.component.NavigationBottomItemEnum
import dali.hamza.echangecurrencyapp.ui.compose.dialog.BottomSheetCurrencies
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.scope.KoinScope
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(ExperimentalMaterial3Api::class, KoinExperimentalAPI::class)
@ExperimentalComposeUiApi
@Composable
fun Home() {
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    val nestedNavController = rememberNavController()
    Scaffold(topBar = {
        TopAppBar(title = {
            Text(stringResource(id = R.string.Home))
        })
    }, bottomBar = {
        NavigationBottom(initSelected = NavigationBottomItemEnum.Rates, bottomNavigate = { item ->
            nestedNavController.navigate(item.name) {
                popUpTo(nestedNavController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        })
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            HomeNavigationBody(
                nestedNavController
            ) {
                scope.launch {
                    //sheetState.show()
                    showBottomSheet = true
                }
            }
        }
        if (showBottomSheet) {
            BottomSheetCurrencySelection(bottomSheetAction = { action ->
                showBottomSheet = action
            })
        }

    }

}

@KoinExperimentalAPI
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeNavigationBody(
    nestedNavController: NavHostController, openBottomSheet: () -> Unit

) {
    NavHost(
        navController = nestedNavController,
        startDestination = NavigationBottomItemEnum.Rates.name,
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(200)
            )
        },
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End, tween(250)
            )
        },
        popExitTransition = {
            fadeOut(
                tween(250)
            )
        }
    ) {
        composable(NavigationBottomItemEnum.Conversion.name) {
            KoinScope<ConverterCurrencyScope>(scopeID = "ConverterCurrencyScope") {
                ConverterCurrency()
            }
        }
        composable(NavigationBottomItemEnum.Rates.name) {

            RatesPageCompose(
                modifier = Modifier,
                openFragment = openBottomSheet,
            )
        }
        composable(NavigationBottomItemEnum.Historic.name) {
            Center {
                Text(text = "coming soon")
            }
        }
        composable(NavigationBottomItemEnum.Setting.name) {
            Center {
                Text(text = "coming soon")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalComposeUiApi
@Composable
fun BottomSheetCurrencySelection(bottomSheetAction: (Boolean) -> Unit) {
    val viewModel = koinViewModel<MainViewModel>()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = {
            scope.launch {
                sheetState.hide()
            }
            bottomSheetAction(false)
        },
        tonalElevation = 6.dp,
        sheetState = sheetState,
        scrimColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
        shape = RoundedCornerShape(6.dp),
    ) {
        BottomSheetCurrencies(
            onClose = {
                scope.launch {
                    sheetState.hide()
                    bottomSheetAction(false)
                }
            },
            onSelect = { currency ->
                viewModel.setCurrencySelection(currency)
                scope.launch {
                    sheetState.hide()
                }
                bottomSheetAction(false)
            },
        )
    }
}



