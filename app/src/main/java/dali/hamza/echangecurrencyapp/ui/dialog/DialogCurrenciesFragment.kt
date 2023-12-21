package dali.hamza.echangecurrencyapp.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputLayout
import dali.hamza.domain.models.Currency
import dali.hamza.echangecurrencyapp.common.onData
import dali.hamza.echangecurrencyapp.common.toCurrencyDTO
import dali.hamza.echangecurrencyapp.databinding.BottomSheetCurrencyPickerBinding
import dali.hamza.echangecurrencyapp.models.CurrencyDTO
import dali.hamza.echangecurrencyapp.ui.adapter.AdapterCurrenciesPicker
import dali.hamza.echangecurrencyapp.viewmodel.DialogCurrencyViewModel
import dali.hamza.echangecurrencyapp.ui.utitlies.AppRecyclerView
import dali.hamza.echangecurrencyapp.ui.utitlies.TitleBottomSheet
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DialogCurrenciesFragment(
    private val action: DialogCurrencySelectionCallback
) : BottomSheetDialogFragment(),
    AdapterCurrenciesPicker.CurrencyPickerCallback {

    companion object {
        const val currentCurrencyKey: String = "currentCurrency"
        const val tag = "currencies_list"

        fun newInstance(
            selectedCurrency: String,
            action: DialogCurrencySelectionCallback
        ): DialogCurrenciesFragment {
            return DialogCurrenciesFragment(action).apply {
                arguments = Bundle().apply {
                    putString(currentCurrencyKey, selectedCurrency)
                }
            }
        }
    }

    private val viewModel: DialogCurrencyViewModel by viewModels()
    private lateinit var binding: BottomSheetCurrencyPickerBinding
    private lateinit var searchTextInput: TextInputLayout
    private lateinit var header: TitleBottomSheet
    private lateinit var recyclerViewApp: AppRecyclerView<AdapterCurrenciesPicker, CurrencyDTO>


    private val listCurrencies: MutableList<CurrencyDTO> =
        emptyList<CurrencyDTO>().toMutableList()
    private var adapter: AdapterCurrenciesPicker = AdapterCurrenciesPicker(listCurrencies, this)
    private var currencySelected: Currency? = null

    private lateinit var emptyValueCurrency: String
    private lateinit var currentCurrency: String
    private lateinit var cacheCurrentCurrency: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emptyValueCurrency = ""//resources.getString(R.string.default_currency_value)
        currentCurrency = arguments?.getString(
            currentCurrencyKey, emptyValueCurrency
        ) ?: emptyValueCurrency
        cacheCurrentCurrency = currentCurrency
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetCurrencyPickerBinding.inflate(inflater, container, false)
        header = binding.idHeaderBottomPickerCurrency


        searchTextInput = binding.idSearchCurrency.idSearchInput
        searchTextInput.isEndIconVisible = false
        recyclerViewApp =
            binding.idListBottomPickerCurrency as AppRecyclerView<AdapterCurrenciesPicker, CurrencyDTO>
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        header.positiveAction = View.OnClickListener {
            viewModel.setPreferenceCurrency(currencySelected!!.name)
            if (cacheCurrentCurrency != currencySelected!!.name) {
                action.resetUIState()
            }
            dismiss()
        }
        header.closeAction = View.OnClickListener {
            dismiss()
        }
        searchTextInput.editText?.addTextChangedListener { textEditable ->
            var endDrawableVisible = false
            if (textEditable.toString().isNotEmpty()) {
                endDrawableVisible = true
            }
            searchTextInput.isEndIconVisible = endDrawableVisible
            viewModel.searchCurrencies(textEditable.toString())
        }
        searchTextInput.setEndIconOnClickListener {
            searchTextInput.editText?.text?.clear()
            viewModel.getCurrenciesFromLocalDb()
            adapter.clearPicker()
        }

        recyclerViewApp.adapter = adapter
        recyclerViewApp.setLayoutManager(
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
        )
        viewModel.getCurrenciesFromLocalDb()
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launchWhenStarted {
            viewModel.getCurrentCurrency()
            viewModel.getCurrencies().onData(
                error = {

                }
            ) { response ->
                val list = response.data as List<Currency>
                var currenciesDTO = emptyList<CurrencyDTO>()
                withContext(IO) {
                    currenciesDTO = list.map {
                        it.toCurrencyDTO(it.name == (currencySelected?.name ?: ""))
                    }
                }
                if (listCurrencies.isEmpty()) {
                    viewModel.setCacheList(list)
                }
                if (currenciesDTO.isNotEmpty()) {
                    listCurrencies.clear()
                    listCurrencies.addAll(currenciesDTO)
                    recyclerViewApp.data = listCurrencies
                    if (currentCurrency.isNotEmpty()) {
                        lifecycleScope.launch(IO) {
                            val currency = listCurrencies.firstOrNull { c ->
                                c.currencyInfo.name.trim() == currentCurrency.trim()
                            }?.currencyInfo
                            withContext(Main) {
                                currencySelected = currency
                            }
                        }
                    }
                    if (searchTextInput.editText!!.text.isEmpty()
                        && currentCurrency != emptyValueCurrency
                        && currentCurrency.isNotEmpty()
                    ) {
                        lifecycleScope.launch(IO) {
                            val index = listCurrencies.indexOfFirst { c ->
                                c.currencyInfo.name.trim() == currentCurrency.trim()
                            }
                            withContext(Main) {
                                header.enableOrDisablePositiveAction(true)
                                adapter.initPickedCurrency(listCurrencies[index])
                                recyclerViewApp.goToPosition(index + 2)
                            }
                        }
                    } else {
                        if (searchTextInput.editText!!.text.isNotEmpty()) {
                            header.enableOrDisablePositiveAction(false)
                            adapter.clearPicker()
                        }
                    }
                }
            }
        }
    }


    override fun picked(currency: Currency) {
        header.enableOrDisablePositiveAction(true)
        currencySelected = currency
        currentCurrency = currency.name
        //viewModel.setPreferenceCurrency(currencySelected!!.name)
    }

    interface DialogCurrencySelectionCallback {
        fun resetUIState()
    }
}