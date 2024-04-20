package mohamedali.hamza.database.commons

import dali.hamza.domain.models.CurrencyRate
import mohamedali.hamza.database.entities.RatesCurrencyEntity

fun CurrencyRate.toRateEntity(currentCurrency: String): RatesCurrencyEntity = RatesCurrencyEntity(
    name = name,
    rate = rate,
    time = time,
    selectedCurrency = currentCurrency
)

fun RatesCurrencyEntity.toCurrencyRate(): CurrencyRate = CurrencyRate(
    name = name,
    rate = rate,
    time = time,
)