package dali.hamza.echangecurrencyapp.common

import android.view.View
import dali.hamza.core.datasource.db.entities.CurrencyEntity
import dali.hamza.domain.models.Currency
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.models.MyResponse
import dali.hamza.echangecurrencyapp.models.CurrencyDTO
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

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
fun Currency.toCurrencyDTO(): CurrencyDTO {
    return CurrencyDTO(
        currencyInfo = this,
        isSelected= false
    )
}
@OptIn(InternalCoroutinesApi::class)
suspend inline fun Flow<IResponse?>.onData(
    crossinline error: (value: Any) -> Unit,
    crossinline success: suspend (value: MyResponse.SuccessResponse<*>) -> Unit,
): Unit =
    collect(object : FlowCollector<IResponse?> {
        override suspend fun emit(value: IResponse?) {
            if (value != null && value is MyResponse.ErrorResponse<*>) {
                error(value.error!!)
            }
            if (value != null && value is MyResponse.SuccessResponse<*>) {
                success(value)
            }
        }
    })