package dali.hamza.shared.data.widgets

import dali.hamza.shared.data.CurrenciesCatalog
import dali.hamza.shared.data.network.CurrencyApi
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.domain.models.WidgetConfig
import dali.hamza.shared.domain.models.WidgetSnapshot
import dali.hamza.shared.domain.models.WidgetType
import dali.hamza.shared.platform.currentEpochMillis
import io.github.aakira.napier.Napier
import kotlin.math.floor

/**
 * Effective display fiat for a gold widget ([WidgetType.GOLD]): the
 * explicitly picked currency, else "FOLLOW" the app's session base
 * at runtime (config quote blank), else USD for a non-fiat session base.
 * Never another asset — metals quote one way only.
 */
fun resolveGoldQuote(config: WidgetConfig, sessionBase: String?): String =
    config.quote
        .takeIf { it.isNotBlank() && it !in CurrenciesCatalog.assetCodes }
        ?: sessionBase
            ?.takeIf { it.isNotBlank() && it !in CurrenciesCatalog.assetCodes }
            ?: "USD"

/**
 * Shared data source for home-screen widgets (`plans/widgets.md`).
 *
 * Fetch strategy (mirrors the rate-alerts engine): ONE live-rates call per
 * DISTINCT base currency across all configured widgets, plus one historic
 * call per base for the previous close (day Δ). Failures degrade — a pair
 * without a live rate is skipped, without a close just hides its Δ chip.
 */
class WidgetsRepository(
    private val currencyApi: CurrencyApi,
    private val store: WidgetSnapshotStore,
    private val storage: ISessionStorage? = null,
) {

    fun configs(): List<WidgetConfig> = store.readConfigs()

    /** Insert or replace one widget's config (keyed by [WidgetConfig.id]). */
    fun saveConfig(config: WidgetConfig) {
        val next = store.readConfigs().filterNot { it.id == config.id } + config
        store.saveConfigs(next)
    }

    fun removeConfig(id: String) {
        store.saveConfigs(store.readConfigs().filterNot { it.id == id })
    }

    /**
     * Forget from the management list — marks the config removed so the
     * placed widget renders its dead state (Android providers cannot
     * unplace widgets; the user long-presses on the launcher to finish).
     */
    fun forgetConfig(id: String) {
        store.saveConfigs(
            store.readConfigs().map { if (it.id == id) it.copy(removed = true) else it },
        )
    }

    fun hasConfigs(): Boolean = store.readConfigs().isNotEmpty()

    /** Stash the pair for the config activity (pin flow handoff). */
    fun savePendingPair(base: String, quote: String) = store.savePendingPair(base, quote)

    /** Return and clear the pending pin pair; `null` when none stashed. */
    fun takePendingPair(): Pair<String, String>? = store.takePendingPair()

    /** Multi-pair pending handoff for the grid widget. */
    fun savePendingPairs(pairs: List<Pair<String, String>>) = store.savePendingPairs(pairs)

    fun takePendingPairs(): List<Pair<String, String>>? = store.takePendingPairs()

    /** Latest stored snapshot for one config; `null` before the first fetch. */
    fun snapshotFor(config: WidgetConfig): WidgetSnapshot? =
        store.readSnapshots().firstOrNull { it.base == config.base && it.quote == config.quote }

    /** Snapshots keyed by "BASE/QUOTE" — the management list lookup. */
    fun readSnapshotsForPairs(): Map<String, WidgetSnapshot> =
        store.readSnapshots().associateBy { "${it.base}/${it.quote}" }

    /**
     * Fetch fresh snapshots for every configured pair and persist them.
     * Returns the new snapshot list (empty when no widgets are configured
     * or every fetch failed — never throws).
     */
    suspend fun refreshSnapshots(): List<WidgetSnapshot> {
        val configs = store.readConfigs()
        // the DEFAULT grid has no config — it mirrors the Account market
        // preferences, so those pairs are fetched too or the grid stays "…"
        val defaultGridPairs = storage?.let { s ->
            val base = s.getCurrency().ifBlank { "USD" }
            s.getMarketPreferences().filter { it != base }.map { base to it }
        }.orEmpty()
        if (configs.isEmpty() && defaultGridPairs.isEmpty()) return emptyList()

        val now = currentEpochMillis()
        val yesterday = isoDateOf(now - DAY_MS)
        val sessionBase = storage?.getCurrency()
        val wanted = (
            configs.filterNot { it.removed }.map { config ->
                // gold "follow" configs (blank quote) track the app's base
                if (config.type == WidgetType.GOLD) {
                    config.base to resolveGoldQuote(config, sessionBase)
                } else {
                    config.base to config.quote
                }
            } + defaultGridPairs
            ).distinct()

        val out = fetchSnapshots(currencyApi, wanted, now, yesterday)
        if (out.isNotEmpty()) {
            store.saveSnapshots(out)
        }
        Napier.i(tag = TAG) { "refreshed ${out.size}/${wanted.size} widget snapshot(s)" }
        return out
    }

    private companion object {
        const val TAG = "WidgetsRepo"
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}

/**
 * One snapshot per pair, best-effort (internal for tests — no store).
 *
 * Asset bases (XAU/XAG/BTC) never ride in the no-symbol `latest` payload:
 * they resolve through the symbol-filtered pair call so the backend's
 * all-fiat USD pivot applies (plans/gold-section.md). Their day-Δ is
 * omitted — the historic waterfall mixes spot and futures quotes, so no
 * comparable previous close exists (same decision as the Home gold card).
 */
internal suspend fun fetchSnapshots(
    api: CurrencyApi,
    wanted: List<Pair<String, String>>,
    now: Long,
    yesterday: String,
): List<WidgetSnapshot> {
    val out = mutableListOf<WidgetSnapshot>()
    for ((base, quote) in wanted) {
        val isAssetBase = base in CurrenciesCatalog.assetCodes
        val rate: Double? = if (isAssetBase) {
            runCatching { api.convert(from = base, to = quote, amount = 1.0) }
                .getOrNull()?.getOrNull()
        } else {
            val live = runCatching { api.getLiveRates(base) }
                .getOrNull()?.getOrNull()
            if (live?.rates == null) {
                Napier.w(tag = "WidgetsRepo") { "live rates fetch failed for base=$base — skipping" }
                null
            } else {
                live.rates[quote]
            }
        }
        val validRate = rate?.takeIf { it > 0.0 && it.isFinite() } ?: continue
        val prevClose = if (isAssetBase) {
            null // metals: no comparable previous close (mixed provenance)
        } else {
            runCatching { api.getHistoricRates(yesterday, base) }
                .getOrNull()?.getOrNull()?.quotes
                ?.let { historicQuotes ->
                    // key format varies by backend shape: "BASEQUOTE" or plain quote
                    historicQuotes["$base$quote"] ?: historicQuotes[quote]
                }
        }
        out += WidgetSnapshot(
            base = base,
            quote = quote,
            rate = validRate,
            prevClose = prevClose,
            fetchedAt = now,
        )
    }
    return out
}

/**
 * Epoch millis → ISO `yyyy-MM-dd` (local civil date, no date library —
 * Howard Hinnant's civil-from-days algorithm).
 */
private const val ISO_DAY_MS = 24L * 60 * 60 * 1000

internal fun isoDateOf(epochMillis: Long): String {
    val days = floor(epochMillis.toDouble() / ISO_DAY_MS.toDouble()).toLong()
    val z = days + 719_468
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val year = if (m <= 2) y + 1 else y
    return year.toString().padStart(4, '0') + "-" +
        m.toString().padStart(2, '0') + "-" +
        d.toString().padStart(2, '0')
}
