package dali.hamza.shared.data.repository

import dali.hamza.shared.domain.models.DataTier
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guest-mode data tiering: guests refresh hourly, Sovereign (login,
 * coming soon) every 30 minutes.
 */
class DataTierTest {

    @Test
    fun guestsRefreshHourly() {
        assertEquals(60L * 60L * 1000L, CurrencyRepositoryImpl.refreshIntervalMs(DataTier.GUEST))
    }

    @Test
    fun sovereignRefreshesEvery30Minutes() {
        assertEquals(30L * 60L * 1000L, CurrencyRepositoryImpl.refreshIntervalMs(DataTier.SOVEREIGN))
    }
}
