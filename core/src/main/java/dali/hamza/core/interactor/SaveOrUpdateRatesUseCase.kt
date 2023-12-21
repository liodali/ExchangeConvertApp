package dali.hamza.core.interactor

import dali.hamza.domain.repository.IRepository
import dali.hamza.domain.usecase.FlowUseCase0
import dali.hamza.domain.usecase.VoidFlowUseCase0

class SaveOrUpdateRatesUseCase  constructor(
    private val repository: IRepository
) : VoidFlowUseCase0 {
    override suspend fun invoke() {
        repository.saveExchangeRatesOfCurrentCurrency()
    }
}