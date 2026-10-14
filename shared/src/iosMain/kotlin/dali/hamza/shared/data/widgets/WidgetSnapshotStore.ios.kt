package dali.hamza.shared.data.widgets

import dali.hamza.shared.domain.models.WidgetConfig
import dali.hamza.shared.domain.models.WidgetSnapshot
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import platform.Foundation.NSUserDefaults

/**
 * iOS actual — App Group-backed JSON blobs: the WidgetKit extension
 * (Phase 3) reads the snapshots in its own process via
 * `group.dali.hamza.ExchangeConvertApp`. Falls back to standard defaults
 * when the group isn't entitled (unit tests / previews).
 */
actual class WidgetSnapshotStore actual constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true // pre-split MARKET_RATE configs → MARKET_CARD
    }

    private val defaults: NSUserDefaults
        get() = NSUserDefaults(suiteName = APP_GROUP) ?: NSUserDefaults.standardUserDefaults

    private inline fun <reified T> read(key: String): List<T> {
        return try {
            val raw = defaults.stringForKey(key) ?: return emptyList()
            json.decodeFromString(ListSerializer(serializer<T>()), raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private inline fun <reified T> write(key: String, values: List<T>) {
        try {
            defaults.setObject(json.encodeToString(ListSerializer(serializer<T>()), values), forKey = key)
        } catch (e: Exception) {
            // storage failures degrade to "no data" until the next refresh
        }
    }

    actual fun readConfigs(): List<WidgetConfig> = read(CONFIGS_KEY)

    actual fun saveConfigs(configs: List<WidgetConfig>) = write(CONFIGS_KEY, configs)

    actual fun readSnapshots(): List<WidgetSnapshot> = read(SNAPSHOTS_KEY)

    actual fun saveSnapshots(snapshots: List<WidgetSnapshot>) = write(SNAPSHOTS_KEY, snapshots)

    @kotlinx.serialization.Serializable
    private data class PendingPair(val base: String, val quote: String)

    actual fun savePendingPair(base: String, quote: String) = write(PENDING_KEY, listOf(PendingPair(base, quote)))

    actual fun takePendingPair(): Pair<String, String>? {
        val pending = read<PendingPair>(PENDING_KEY).firstOrNull() ?: return null
        write(PENDING_KEY, emptyList<PendingPair>())
        return pending.base to pending.quote
    }

    actual fun savePendingPairs(pairs: List<Pair<String, String>>) =
        write(PENDING_KEY, pairs.map { (b, q) -> PendingPair(b, q) })

    actual fun takePendingPairs(): List<Pair<String, String>>? {
        val pending = read<PendingPair>(PENDING_KEY)
        if (pending.isEmpty()) return null
        write(PENDING_KEY, emptyList<PendingPair>())
        return pending.map { it.base to it.quote }
    }

    private companion object {
        const val CONFIGS_KEY = "widgets.configs.json"
        const val SNAPSHOTS_KEY = "widgets.snapshots.json"
        const val PENDING_KEY = "widgets.pending.json"
        const val APP_GROUP = "group.dali.hamza.ExchangeConvertApp"
    }
}

actual fun createWidgetSnapshotStore(): WidgetSnapshotStore = WidgetSnapshotStore()
