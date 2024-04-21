package dali.hamza.echangecurrencyapp.models

import androidx.compose.runtime.saveable.mapSaver

data class AmountInput(
    val amount: String
)

fun initAmountInput(): AmountInput {
    return AmountInput(amount = "")
}

val amountInputSaver = run {
    val amountKey = "amount"
    mapSaver(
        save = { mapOf(amountKey to it.amount) },
        restore = { AmountInput(it[amountKey] as String) }
    )
}