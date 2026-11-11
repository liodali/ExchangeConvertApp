package dali.hamza.shared.ui.viewmodel

import dali.hamza.shared.common.DateUtils
import dali.hamza.shared.common.MetalPricing
import dali.hamza.shared.common.nowMillis
import dali.hamza.shared.domain.models.MetalQuote
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.models.PreciousMetal
import dali.hamza.shared.domain.repository.IRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Gold/Silver ratio state (plans/metals-v3.md §5 "Units, deltas and ratio").
 *
 * The current ratio divides two LIVE per-ounce quotes in the SAME currency
 * (the unit and currency cancel — dimensionless). The 30-day range joins
 * daily XAU and XAG series **by date** and takes min/max of the per-date
 * ratios — never the quotient of independent extrema. Missing either leg
 * (live or historical) hides that part rather than fabricating one.
 */
data class GoldSilverRatio(
    val current: Double? = null,
    val rangeLow: Double? = null,
    val rangeHigh: Double? = null,
    val currency: String? = null,
)

/**
 * Metals v3 overview state — one bucket per asset (plans/metals-v3.md §5
 * "Models and state"): a failed platinum fetch must not blank silver.
 *
 * - [quotes] holds the last VALID quote per metal; [staleMetals] marks the
 *   ones whose latest refresh failed but whose old value is still shown;
 * - [loading] only covers the "no data yet" phase of each metal — refreshes
 *   on top of valid quotes never flicker;
 * - [unavailableMetals] lists metals with nothing valid to show (per-card
 *   retry state);
 * - [usdFallbackMetals] marks assets shown in an explicitly labeled USD
 *   quote because the display base has no quote for them.
 */
data class MetalsUiState(
    val quotes: Map<PreciousMetal, MetalQuote> = emptyMap(),
    val loading: Boolean = false,
    val staleMetals: Set<PreciousMetal> = emptySet(),
    val unavailableMetals: Set<PreciousMetal> = emptySet(),
    val usdFallbackMetals: Set<PreciousMetal> = emptySet(),
    val ratio: GoldSilverRatio = GoldSilverRatio(),
    /** Display currency the current load targets (labels the USD fallback). */
    val requestedCurrency: String = "USD",
)

/**
 * State holder for the Metals v3 overview (a pushed route from Home — the
 * Home Gold section keeps its own [GoldViewModel]).
 *
 * Orchestration mirrors Gold's rules: currency-keyed loads with a monotonic
 * generation (outdated responses discarded), in-flight coalescing per metal,
 * last-valid-quote retention across refresh failures, per-metal labeled USD
 * fallback on Empty (never on transport Error), and bounded concurrency —
 * the four pair fetches run together, then the two history windows for the
 * ratio run together.
 *
 * Ratio derivation is fail-open per part: any missing/invalid leg simply
 * leaves that piece null; the UI hides it with an em dash.
 */
class MetalsViewModel(
    private val repository: IRepository,
    private val clock: () -> Long = { nowMillis() },
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(MetalsUiState())
    val state: StateFlow<MetalsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    /** Display currency the current/last load targeted (upper-cased). */
    private var loadedCurrency: String = "USD"

    /** Monotonic generation: only the newest load may write state. */
    private var generation = 0

    /**
     * Load all four metals in [displayCurrency]. Safe to call repeatedly:
     * a same-currency load with fresh data or one in flight is a no-op
     * unless [forceRefresh].
     */
    fun load(displayCurrency: String?, forceRefresh: Boolean = false) {
        val currency = displayCurrency
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.uppercase()
            ?: "USD"
        val current = _state.value
        if (!forceRefresh && currency == loadedCurrency) {
            val inFlight = current.loading
            val fresh = current.quotes.values.any { it.currency == currency } &&
                current.staleMetals.isEmpty() && current.unavailableMetals.isEmpty()
            if (inFlight || fresh) return
        }
        loadedCurrency = currency
        generation += 1
        val gen = generation
        loadJob?.cancel()
        _state.value = current.copy(
            loading = current.quotes.isEmpty(),
            staleMetals = emptySet(),
            unavailableMetals = emptySet(),
            requestedCurrency = currency,
        )
        loadJob = viewModelScope.launch {
            // four pair fetches, bounded to exactly this set — no unbounded
            // fan-out, each metal fails/succeeds on its own
            val quotes = PreciousMetal.entries.map { metal ->
                async { fetchQuote(metal, currency) }
            }.map { it.await() }

            if (gen != generation) return@launch

            val quoteMap = quotes.mapNotNull { q -> q?.let { it.metal to it } }.toMap()
            val stale = _state.value.quotes.keys - quoteMap.keys
            val unavailable = PreciousMetal.entries.toSet() - quoteMap.keys - stale
            val usdFallback = quoteMap.values
                .filter { it.currency != currency }
                .map { it.metal }
                .toSet()
            _state.update {
                it.copy(
                    quotes = quoteMap,
                    loading = false,
                    staleMetals = stale,
                    unavailableMetals = unavailable,
                    usdFallbackMetals = usdFallback,
                )
            }

            // ratio legs: LIVE current + joined 30D history, same currency
            val liveRatio = liveRatio(quoteMap, currency)
            val historyRatio = historyRatio(currency, gen)
            if (gen != generation) return@launch
            _state.update {
                it.copy(
                    ratio = GoldSilverRatio(
                        current = liveRatio,
                        rangeLow = historyRatio?.first,
                        rangeHigh = historyRatio?.second,
                        currency = if (liveRatio != null || historyRatio != null) currency else null,
                    ),
                )
            }

            // day-Δ per metal, painted after the prices (a slow historic leg
            // must not delay the spot cards — chips arrive a beat later)
            val withDeltas = attachDeltas(quoteMap, gen)
            if (gen != generation) return@launch
            _state.update { it.copy(quotes = withDeltas) }
        }
    }

    /** Section-local retry for the unavailable state. */
    fun retry() {
        load(loadedCurrency, forceRefresh = true)
    }

    /**
     * One metal's quote in [currency], or the labeled USD fallback when the
     * pair is quotable nowhere. Null on transport failure — the previous
     * snapshot is kept and flagged stale, never masked with USD.
     */
    private suspend fun fetchQuote(metal: PreciousMetal, currency: String): MetalQuote? {
        fetchPair(metal, currency)?.let { return it }
        if (currency == USD) return null
        return fetchPair(metal, USD)
    }

    private suspend fun fetchPair(metal: PreciousMetal, currency: String): MetalQuote? =
        when (val response = repository.getLiveRateForPair(metal.code, currency)) {
            is MyResponse.Error -> null // transport failure — stale/retry, no USD masking
            is MyResponse.Success ->
                response.data
                    .takeIf { MetalPricing.isValidSpot(it) }
                    ?.let {
                        MetalQuote(
                            metal = metal,
                            currency = currency,
                            spotPerTroyOunce = it,
                            fetchedAtMs = clock(),
                        )
                    }
            is MyResponse.Empty -> null // caller falls back to USD
        }

    /**
     * Dimensionless current ratio from two LIVE quotes in the same currency.
     * Mixed-currency legs (one USD fallback, one local) are incomparable —
     * null. Non-finite or extreme quotients (guard: silver ≥ $0.01/oz and
     * ratio within 1..1000) are rejected as junk, not displayed.
     */
    private fun liveRatio(quotes: Map<PreciousMetal, MetalQuote>, currency: String): Double? {
        val gold = quotes[PreciousMetal.GOLD] ?: return null
        val silver = quotes[PreciousMetal.SILVER] ?: return null
        if (gold.currency != silver.currency) return null
        if (silver.spotPerTroyOunce < MIN_SILVER_FOR_RATIO) return null
        val ratio = gold.spotPerTroyOunce / silver.spotPerTroyOunce
        return ratio.takeIf { it.isFinite() && it in 1.0..1000.0 }
    }

    /**
     * 30-day min/max of per-DATE XAU/XAG ratios: the two series are joined
     * by date first — dividing independent extrema would overstate the
     * range. At least [MIN_RATIO_MATCHED_DAYS] matched dates are required;
     * otherwise the range stays hidden (em dash) instead of lying.
     */
    private suspend fun historyRatio(
        currency: String,
        gen: Int,
    ): Pair<Double, Double>? {
        val (from, to) = ratioWindow()
        val series = coroutineScope {
            listOf(PreciousMetal.GOLD, PreciousMetal.SILVER).map { metal ->
                async {
                    when (
                        val response = repository.getHistoricalRates(
                            metal.code,
                            currency,
                            from,
                            to,
                        )
                    ) {
                        is MyResponse.Success -> response.data
                            .filter { MetalPricing.isValidSpot(it.rate) }
                            .associate { it.date to it.rate }
                        else -> emptyMap()
                    }
                }
            }.map { it.await() }
        }
        if (gen != generation) return null
        val gold = series[0]
        val silver = series[1]
        val ratios = gold.keys.intersect(silver.keys)
            .mapNotNull { date ->
                val g = gold.getValue(date)
                val s = silver.getValue(date)
                if (s < MIN_SILVER_FOR_RATIO) return@mapNotNull null
                (g / s).takeIf { it.isFinite() && it in 1.0..1000.0 }
            }
        if (ratios.size < MIN_RATIO_MATCHED_DAYS) return null
        return ratios.min() to ratios.max()
    }

    private fun ratioWindow(): Pair<String, String> {
        val toDay = DateUtils.epochDayOf(clock())
        val fromDay = toDay - RATIO_WINDOW_DAYS
        return DateUtils.toIsoDate(fromDay) to DateUtils.toIsoDate(toDay)
    }

    /**
     * Day-Δ for every quoted metal, each fetched in the QUOTE's own currency
     * (a USD-fallback card must not mix legs). One failed historic leg costs
     * only that metal's chip (em dash) — never the card or its neighbors.
     */
    private suspend fun attachDeltas(
        quotes: Map<PreciousMetal, MetalQuote>,
        gen: Int,
    ): Map<PreciousMetal, MetalQuote> = coroutineScope {
        quotes.map { (metal, quote) ->
            async { metal to quote.copy(deltaPercent = prevCloseDelta(quote, gen)) }
        }.map { it.await() }.toMap()
    }

    /**
     * Spot now vs the previous completed session close: the latest historic
     * rate strictly BEFORE today (ISO dates sort lexicographically), same
     * currency as the quote. Junk guard rejects non-finite or absurd moves
     * (|Δ| > [MAX_ABS_DELTA_PERCENT] — a bad close must not print "+900%").
     */
    private suspend fun prevCloseDelta(quote: MetalQuote, gen: Int): Double? {
        val (from, to) = deltaWindow()
        val rates = when (
            val response = repository.getHistoricalRates(
                quote.metal.code,
                quote.currency,
                from,
                to,
            )
        ) {
            is MyResponse.Success -> response.data
            else -> return null
        }
        if (gen != generation) return null
        val today = DateUtils.toIsoDate(DateUtils.epochDayOf(clock()))
        val prevClose = rates
            .filter { MetalPricing.isValidSpot(it.rate) && it.date < today }
            .maxByOrNull { it.date }
            ?.rate
            ?: return null
        val delta = (quote.spotPerTroyOunce - prevClose) / prevClose * 100.0
        return delta.takeIf { it.isFinite() && kotlin.math.abs(it) <= MAX_ABS_DELTA_PERCENT }
    }

    private fun deltaWindow(): Pair<String, String> {
        val toDay = DateUtils.epochDayOf(clock())
        val fromDay = toDay - DELTA_WINDOW_DAYS
        return DateUtils.toIsoDate(fromDay) to DateUtils.toIsoDate(toDay)
    }

    fun onCleared() {
        viewModelScope.cancel()
    }

    private companion object {
        const val USD = "USD"
        const val RATIO_WINDOW_DAYS = 30L
        const val MIN_RATIO_MATCHED_DAYS = 10
        const val MIN_SILVER_FOR_RATIO = 0.01

        /** Day-Δ lookback + junk guard (see [prevCloseDelta]). */
        const val DELTA_WINDOW_DAYS = 7L
        const val MAX_ABS_DELTA_PERCENT = 20.0
    }
}
