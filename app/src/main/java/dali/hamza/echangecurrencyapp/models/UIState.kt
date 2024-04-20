package dali.hamza.echangecurrencyapp.models

import androidx.compose.runtime.Immutable
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.models.MyResponse

sealed class UIState(val error: Exception?)

@Immutable
class LoadingUIState : UIState(error = null)
class ErrorUIState(error: Exception) : UIState(error)

data class DataUIState<T>(val data: T) : UIState(error = null)
data class InfiniteDataUIState<T>(val data: T, val isLoading: Boolean) : UIState(error = null)

@Immutable
data class NoDataUIState(val data: Nothing? = null) : UIState(error = null)


fun IResponse.toUIState(): UIState {
    return when (this) {
        is MyResponse.SuccessResponse<*> -> {
            if (data is List<*> && (data as List<*>).isEmpty()) {
                return NoDataUIState()
            }
            return DataUIState(data)
        }

        is MyResponse.NoResponse<*> -> NoDataUIState()
        is MyResponse.ErrorResponse<*> -> ErrorUIState(Exception(error.toString()))
        else -> LoadingUIState()
    }

}