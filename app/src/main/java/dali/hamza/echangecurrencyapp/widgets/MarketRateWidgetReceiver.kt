package dali.hamza.echangecurrencyapp.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dali.hamza.shared.data.widgets.WidgetSnapshotStore
import org.koin.mp.KoinPlatform

/**
 * Shared receiver behaviour for both market-widget looks (wave + card):
 * keeps the refresh worker aligned with the widget set — first instance
 * enables the 30-min periodic fetch, last removal cancels it, deleted
 * instances drop their stored config.
 */
abstract class BaseMarketRateReceiver(
    override val glanceAppWidget: GlanceAppWidget,
) : GlanceAppWidgetReceiver() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshScheduler.update(context, hasWidgets = true)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val store = KoinPlatform.getKoin()?.let {
            runCatching { it.get<WidgetSnapshotStore>() }.getOrNull()
        } ?: return
        val removed = appWidgetIds.map { it.toString() }.toSet()
        store.saveConfigs(store.readConfigs().filterNot { it.id in removed })
        WidgetRefreshScheduler.update(context, hasWidgets = store.readConfigs().isNotEmpty())
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        val hasWidgets = KoinPlatform.getKoin()
            ?.get<WidgetSnapshotStore>()?.readConfigs()?.isNotEmpty() == true
        WidgetRefreshScheduler.update(context, hasWidgets)
    }
}

/** Wave-blob look (`RxvJn` sketch) — the widget shape is a circular wave. */
class MarketRateWidgetReceiver : BaseMarketRateReceiver(MarketRateWidget())

/** Rounded-card look (`prEnT` sketch). */
class MarketRateCardWidgetReceiver : BaseMarketRateReceiver(MarketRateCardWidget())

/** Markets grid (`p7ayhz` sketch) — follows Account market preferences. */
class MarketGridWidgetReceiver : BaseMarketRateReceiver(MarketGridWidget())

/** Quick convert (`GDiky` sketch) — amount presets over the cached rate. */
class QuickConvertWidgetReceiver : BaseMarketRateReceiver(QuickConvertWidget())
