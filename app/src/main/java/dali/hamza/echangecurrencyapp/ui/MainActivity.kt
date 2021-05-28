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
import dali.hamza.echangecurrencyapp.ui.compose.page.Home
import dali.hamza.echangecurrencyapp.ui.compose.theme.ExchangeCurrencyAppTheme
import dali.hamza.echangecurrencyapp.ui.dialog.DialogCurrenciesFragment
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel


@ExperimentalMaterialApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var dialogCurrencySelection: DialogCurrenciesFragment

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
            viewModel.retrieveCurrencySelection()
        }
    }


    fun openCurrenciesSelectionBottomSheet() {
        dialogCurrencySelection = DialogCurrenciesFragment.newInstance(
            selectedCurrency = viewModel.getCurrencySelection()
        )
        dialogCurrencySelection.show(supportFragmentManager.also {
            val prevFrag = it.findFragmentByTag(DialogCurrenciesFragment.tag)
            if (prevFrag != null) {
                it.beginTransaction().remove(prevFrag)
            }
            it.beginTransaction().addToBackStack(null)
        }, DialogCurrenciesFragment.tag)
    }
}

