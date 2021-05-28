package dali.hamza.echangecurrencyapp.models

data class AmountInput(
    val amount: String
)

fun initAmountInput(): AmountInput {
    return AmountInput(amount = "")
}
