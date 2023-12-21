package dali.hamza.echangecurrencyapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.checkbox.MaterialCheckBox
import dali.hamza.domain.models.Currency
import dali.hamza.echangecurrencyapp.databinding.ItemCurrencySelectionBinding
import dali.hamza.echangecurrencyapp.models.CurrencyDTO
import dali.hamza.echangecurrencyapp.ui.adapter.AdapterCurrenciesPicker.PickerViewHolder

class AdapterCurrenciesPicker(
    list: MutableList<CurrencyDTO> = emptyList<CurrencyDTO>().toMutableList(),
    private val pickerCallback: CurrencyPickerCallback
) :
    BaseAdapter<CurrencyDTO, PickerViewHolder>(list) {
    private var dataSelected: CurrencyDTO? = null

    class PickerViewHolder(binding: ItemCurrencySelectionBinding) :
        BaseViewHolder<CurrencyDTO>(binding) {
        override fun bind(data: CurrencyDTO) {
            (binding as ItemCurrencySelectionBinding)
                .idCurrencyName.text =
                buildString {
                    append("data.currencyInfo.fullCountryName (")
                    append(data.currencyInfo.name)
                    append(") ")
                }
            (binding)
                .idCurrencyCheckbox.isChecked = data.isSelected

        }

        companion object {
            fun create(parent: ViewGroup): PickerViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding = ItemCurrencySelectionBinding.inflate(inflater, parent, false)
                return PickerViewHolder(binding)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickerViewHolder {
        return PickerViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: PickerViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        (holder.binding as ItemCurrencySelectionBinding).idCurrencyCheckbox.setOnClickListener {
            if (dataSelected != null) {
                val index = list.indexOfFirst { c ->
                    c.currencyInfo.name == dataSelected!!.currencyInfo.name
                }
                list[index] = dataSelected!!.copy(
                    isSelected = false
                )
                notifyItemChanged(index)
            }
            (it as MaterialCheckBox).isChecked = true
            dataSelected = list[position].copy(isSelected = true)
            list[position] = dataSelected!!
            notifyItemChanged(position)
            pickerCallback.picked(dataSelected!!.currencyInfo)
        }
    }

    interface CurrencyPickerCallback {
        fun picked(currency: Currency)
    }

    fun clearPicker() {
        val index = list.indexOf(dataSelected)
        dataSelected = null
        notifyItemChanged(index)
    }

    fun initPickedCurrency(currency: CurrencyDTO) {
        if (dataSelected == null) {
            val index = list.indexOf(currency)
            list[index] = currency.copy(isSelected = true)
            dataSelected = list[index]
            notifyItemChanged(index)
        }
    }
}