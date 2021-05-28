package dali.hamza.domain.usecase

import dali.hamza.domain.models.IResponse
import kotlinx.coroutines.flow.Flow

interface VoidFlowUseCase<in T> {
    suspend operator fun invoke(parameter: T?)
}

interface FlowUseCase<in T, out R : Any> {
    suspend operator fun invoke(parameter: T?): Flow<R>
}

interface FlowUseCases<in T, out R : Any> {
    suspend operator fun invoke(vararg parameters: T?): Flow<R>
}

interface FlowUseCase0<out R> {
    suspend operator fun invoke(): Flow<R>
}

interface FlowIResponseUseCase<in T> : FlowUseCase<T, IResponse>

interface FlowIResponseUseCase0 : FlowUseCase0<IResponse>

interface FlowIResponseUseCases<in T> : FlowUseCases<T, IResponse>
