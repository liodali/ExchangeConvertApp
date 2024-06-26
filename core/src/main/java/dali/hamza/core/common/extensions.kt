package dali.hamza.core.common

import android.util.Log
import dali.hamza.domain.models.Currency
import dali.hamza.domain.models.MyResponse
import mohamedali.hamza.database.entities.CurrencyEntity
import mohamedali.hamza.database.entities.HistoricRatesCurrencyEntity
import mohamedali.hamza.database.entities.RatesCurrencyEntity
import okio.IOException
import retrofit2.Response

inline fun <T> Response<T>.onSuccess(
    action: (T) -> Unit
): Response<T> {
    if (isSuccessful) body()?.run(action)
    return this
}

inline fun <T> Response<T>.onFailure(
    action: (Any) -> Unit
) {
    if (!isSuccessful) {
        Log.e("code request ${this.code()}", this.errorBody()?.string() ?: "error unknown")
        errorBody()?.run {
            action(this.string())
        }
    }
}

fun <T, R : Any> Response<T>.data(
    mapTo: (T) -> R
): MyResponse<R> {
    try {
        onSuccess {
            return MyResponse.SuccessResponse(mapTo(it))
        }
        onFailure {
            return MyResponse.ErrorResponse(it)
        }
        return MyResponse.ErrorResponse(
            error = Exception("GENERAL_NETWORK_ERROR")
        )
    } catch (e1: IOException) {
        return MyResponse.ErrorResponse(
            error = IOException("GENERAL_NETWORK_ERROR")
        )
    } catch (e: Exception) {
        return MyResponse.ErrorResponse(
            error = Exception(e.fillInStackTrace().message)
        )
    }

}

fun <T, R : Any> Response<T>.simpleData(
    mapTo: (T) -> R
): R {
    try {
        onSuccess {
            return mapTo(it)
        }
        onFailure {
            throw Exception(it as String)
        }
        throw Exception("GENERAL_NETWORK_ERROR")
    } catch (e1: IOException) {
        throw IOException("GENERAL_NETWORK_ERROR")
    } catch (e: Exception) {
        throw Exception(e.fillInStackTrace().message)
    }

}


fun Currency.toCurrencyEntity(): CurrencyEntity {
    return CurrencyEntity(
        name = this.name,
        fullCountryName = this.fullCountryName
    )
}

fun RatesCurrencyEntity.toHistoricRatesEntity(): HistoricRatesCurrencyEntity {
    return HistoricRatesCurrencyEntity(
        name = this.name,
        rate = this.rate,
        time = this.time,
        selectedCurrency = this.selectedCurrency
    )
}