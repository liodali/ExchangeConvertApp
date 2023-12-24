package dali.hamza.echangecurrencyapp.common

import android.view.View
import dali.hamza.domain.models.Currency
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.models.MyResponse
import dali.hamza.echangecurrencyapp.models.CurrencyDTO
import kotlinx.coroutines.flow.Flow

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
