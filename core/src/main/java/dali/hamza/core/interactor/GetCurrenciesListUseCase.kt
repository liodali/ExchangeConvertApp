package dali.hamza.core.interactor

import dali.hamza.core.repository.CurrencyRepository
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.usecase.FlowIResponseUseCase0
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrenciesListUseCase @Inject constructor(
    private val repository: CurrencyRepository
): FlowIResponseUseCase0 {
    override suspend fun invoke(): Flow<IResponse> {
        return repository.getListCurrencies()
    }
}