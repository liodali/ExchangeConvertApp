package dali.hamza.echangecurrencyapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dali.hamza.core.common.SessionManager
import dali.hamza.echangecurrencyapp.ui.compose.page.Home
import dali.hamza.echangecurrencyapp.ui.compose.theme.ExchangeCurrencyAppTheme
import dali.hamza.echangecurrencyapp.ui.dialog.DialogCurrenciesFragment
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import javax.inject.Inject


@ExperimentalMaterialApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), DialogCurrenciesFragment.DialogCurrencySelectionCallback {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var dialogCurrencySelection: DialogCurrenciesFragment

    @Inject
    lateinit var sessionManager: SessionManager


    companion object {
        val mainViewModelComposition =
            compositionLocalOf<MainViewModel> { error("No viewModel found!") }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(mainViewModelComposition provides viewModel) {
                ExchangeCurrencyAppTheme {
                    Home(
                        openFragment = {
                            openCurrenciesSelectionBottomSheet()
                        }
                    )
                }
            }

        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launchWhenStarted {
            sessionManager.getCurrencyFromDataStore.collect { newCurrency ->
                val cache = viewModel.getCurrencySelection()
                viewModel.setCurrencySelection(newCurrency)
                if (cache != null &&
                    cache.isNotEmpty()
                    && cache != viewModel.getCurrencySelection()
                ) {
                    withContext(IO) {
                        sessionManager.removeTimeLastUpdateRate()
                        viewModel.retrieveOrUpdateRates(cache)
                    }
                }
            }

        }

    }


    private fun openCurrenciesSelectionBottomSheet() {
        dialogCurrencySelection = DialogCurrenciesFragment.newInstance(
            selectedCurrency = viewModel.getCurrencySelection() ?: "",
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
        viewModel.changeAmount("")
        viewModel.resetExchangeRates()
    }
}

