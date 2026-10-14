package dali.hamza.shared.data.widgets

import dali.hamza.shared.domain.models.WidgetConfig
import dali.hamza.shared.domain.models.WidgetSnapshot

/**
 * Persistence for widget configs (per instance) and the latest snapshots
 * (per pair), as small JSON blobs — deliberately NOT the SQLDelight DB:
 * the future iOS WidgetKit extension process must read snapshots without
 * opening the app database (App Group storage there).
 *
 * Android actual: files under `filesDir/widgets/` (same process as Glance).
 * iOS actual: NSUserDefaults (App Group suite in the extension phase).
 */
expect class WidgetSnapshotStore() {
    fun readConfigs(): List<WidgetConfig>

    fun saveConfigs(configs: List<WidgetConfig>)

    fun readSnapshots(): List<WidgetSnapshot>

    fun saveSnapshots(snapshots: List<WidgetSnapshot>)

    /**
     * Stash the pair chosen in the Account → Widgets add flow. The config
     * activity picks it up on the pin request (many launchers don't forward
     * the pin extras) — [takePendingPair] returns AND clears it.
     */
    fun savePendingPair(base: String, quote: String)

    fun takePendingPair(): Pair<String, String>?

    /** Multi-pair variant for the grid widget (2–3 custom pairs). */
    fun savePendingPairs(pairs: List<Pair<String, String>>)

    fun takePendingPairs(): List<Pair<String, String>>?
}

expect fun createWidgetSnapshotStore(): WidgetSnapshotStore
