package dali.hamza.shared.domain.models

sealed class MyResponse<out T> {
    data class Success<out T>(val data: T) : MyResponse<T>()
    data object Empty : MyResponse<Nothing>()
    data class Error(val error: Any) : MyResponse<Nothing>()
}

inline fun <T> MyResponse<T>.onSuccess(action: (T) -> Unit): MyResponse<T> {
    if (this is MyResponse.Success) action(data)
    return this
}

inline fun <T> MyResponse<T>.onFailure(action: (Any) -> Unit): MyResponse<T> {
    if (this is MyResponse.Error) action(error)
    return this
}
