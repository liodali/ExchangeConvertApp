package dali.hamza.echangecurrencyapp.common

import android.view.View
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import dali.hamza.domain.models.Currency
import dali.hamza.domain.models.ExchangeRate
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.models.MyResponse
import dali.hamza.echangecurrencyapp.models.CurrencyDTO
import kotlinx.coroutines.flow.Flow
import java.util.Date

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.enabled() {
    isEnabled = true
}

fun View.disabled() {
    isEnabled = false
}

fun View.gone() {
    visibility = View.GONE
}

fun Currency.toCurrencyDTO(selected: Boolean = false): CurrencyDTO {
    return CurrencyDTO(
        currencyInfo = this,
        isSelected = selected
    )
}

suspend inline fun Flow<IResponse?>.onData(
    crossinline error: (value: Any) -> Unit,
    crossinline success: suspend (value: MyResponse.SuccessResponse<*>) -> Unit,
): Unit =
    collect { value ->
        if (value != null && value is MyResponse.ErrorResponse<*>) {
            error(value.error!!)
        }
        if (value != null && value is MyResponse.SuccessResponse<*>) {
            success(value)
        }
    }

val ratesSaver: Saver<List<ExchangeRate>, Any> = run {
    val nameKey = "Name"
    val amountKey = "amount"
    val rateKey = "rate"
    val timeKey = "time"
    listSaver(
        save = {
            it.map { rate ->
                mapOf(
                    nameKey to rate.name,
                    amountKey to rate.calculatedAmount,
                    rateKey to rate.rate,
                    timeKey to rate.time.time
                )
            }.toList()
        },
        restore = {
            it.map { rateMap ->
                ExchangeRate(
                    rateMap[nameKey] as String,
                    rateMap[amountKey] as Double,
                    rateMap[rateKey] as Double,
                    Date(rateMap[timeKey] as Long)
                )
            }.toList()
        }
    )
}