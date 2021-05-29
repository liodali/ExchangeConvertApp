package dali.hamza.core.interactor

import dali.hamza.domain.models.IResponse
import dali.hamza.domain.repository.IRepository
import dali.hamza.domain.usecase.FlowIResponseUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CalculateRatesUseCase @Inject constructor(
    private val repository: IRepository
) : FlowIResponseUseCase<Double> {
    override suspend fun invoke(parameter: Double?): Flow<IResponse> {
        return repository.getListRatesCurrencies(parameter!!)
    }
}