package dali.hamza.echangecurrencyapp.models

import dali.hamza.domain.models.Currency

data class CurrencyDTO(
    val currencyInfo: Currency,
    val isSelected: Boolean = false
)