package dali.hamza.core.interactor

import dali.hamza.domain.models.IResponse
import dali.hamza.domain.repository.IRepository
import dali.hamza.domain.usecase.FlowIResponseUseCase
import kotlinx.coroutines.flow.Flow

class CalculateRatesUseCase  constructor(
    private val repository: IRepository
) : FlowIResponseUseCase<Double> {
    override suspend fun invoke(parameter: Double?): Flow<IResponse> {
        return repository.getListRatesCurrencies(parameter!!)
    }
}