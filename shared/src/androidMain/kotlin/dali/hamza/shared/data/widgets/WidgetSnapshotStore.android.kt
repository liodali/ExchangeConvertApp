package dali.hamza.shared.data.widgets

import android.content.Context
import dali.hamza.shared.AndroidAppContext
import dali.hamza.shared.domain.models.WidgetConfig
import dali.hamza.shared.domain.models.WidgetSnapshot
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File

/**
 * Android actual — JSON files under `filesDir/widgets/`. Glance receivers
 * and workers run in the app process, so plain file storage is safe; the
 * same files also back the runtime-rendered wave-blob bitmaps.
 */
actual class WidgetSnapshotStore actual constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true // pre-split MARKET_RATE configs → MARKET_CARD
    }

    private fun dir(context: Context): File =
        File(context.filesDir, "widgets").apply { mkdirs() }

    private inline fun <reified T> read(context: Context, name: String): List<T> {
        return try {
            val file = File(dir(context), name)
            if (!file.exists()) emptyList() else json.decodeFromString(ListSerializer(serializer<T>()), file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    private inline fun <reified T> write(context: Context, name: String, values: List<T>) {
        try {
            File(dir(context), name).writeText(json.encodeToString(ListSerializer(serializer<T>()), values))
        } catch (e: Exception) {
            // storage failures degrade to "no data" — the widget shows its
            // loading state until the next successful refresh
        }
    }

    actual fun readConfigs(): List<WidgetConfig> {
        val context = AndroidAppContext.appContext ?: return emptyList()
        return read(context, CONFIGS_FILE)
    }

    actual fun saveConfigs(configs: List<WidgetConfig>) {
        val context = AndroidAppContext.appContext ?: return
        write(context, CONFIGS_FILE, configs)
    }

    actual fun readSnapshots(): List<WidgetSnapshot> {
        val context = AndroidAppContext.appContext ?: return emptyList()
        return read(context, SNAPSHOTS_FILE)
    }

    actual fun saveSnapshots(snapshots: List<WidgetSnapshot>) {
        val context = AndroidAppContext.appContext ?: return
        write(context, SNAPSHOTS_FILE, snapshots)
    }

    @kotlinx.serialization.Serializable
    private data class PendingPair(val base: String, val quote: String)

    actual fun savePendingPair(base: String, quote: String) {
        val context = AndroidAppContext.appContext ?: return
        write(context, PENDING_FILE, listOf(PendingPair(base, quote)))
    }

    actual fun takePendingPair(): Pair<String, String>? {
        val context = AndroidAppContext.appContext ?: return null
        val pending = read<PendingPair>(context, PENDING_FILE).firstOrNull() ?: return null
        write(context, PENDING_FILE, emptyList<PendingPair>())
        return pending.base to pending.quote
    }

    actual fun savePendingPairs(pairs: List<Pair<String, String>>) {
        val context = AndroidAppContext.appContext ?: return
        write(context, PENDING_FILE, pairs.map { (b, q) -> PendingPair(b, q) })
    }

    actual fun takePendingPairs(): List<Pair<String, String>>? {
        val context = AndroidAppContext.appContext ?: return null
        val pending = read<PendingPair>(context, PENDING_FILE)
        if (pending.isEmpty()) return null
        write(context, PENDING_FILE, emptyList<PendingPair>())
        return pending.map { it.base to it.quote }
    }

    private companion object {
        const val CONFIGS_FILE = "configs.json"
        const val SNAPSHOTS_FILE = "snapshots.json"
        const val PENDING_FILE = "pending.json"
    }
}

actual fun createWidgetSnapshotStore(): WidgetSnapshotStore = WidgetSnapshotStore()
