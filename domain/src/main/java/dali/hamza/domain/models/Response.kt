package dali.hamza.domain.models

open class IResponse

sealed  class  MyResponse<T>(val data: T?, val error: Any?) : IResponse() {
    class SuccessResponse<T>(data: T) : MyResponse<T>(data, null)
    class ErrorResponse<T>(error: Any) : MyResponse<T>(null, error = error)
}

inline fun <T : Any> MyResponse<T>.onSuccess(action: (T) -> Unit): MyResponse<T> {
    if (this is MyResponse.SuccessResponse) action(data!!)
    return this
}

inline fun <T : Any> MyResponse<T>.onFailure(action: (Any) -> Unit) {
    if (this is MyResponse.ErrorResponse) action(error!!)
}
data object NoResponse {
    val error = "No response"
}
data object EmptyResponse {
    val list = emptyList<Any>()
}