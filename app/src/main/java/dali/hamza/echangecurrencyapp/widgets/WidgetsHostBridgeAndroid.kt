package dali.hamza.echangecurrencyapp.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import dali.hamza.shared.platform.WidgetExtras
import dali.hamza.shared.platform.WidgetKind
import dali.hamza.shared.platform.WidgetsHostBridge

/**
 * Android [WidgetsHostBridge]: pin via `requestPinAppWidget` (pair travels
 * as launch extras → the config activity prefills), reconfigure relaunches
 * the config activity directly, refresh rides the WorkManager pipeline.
 *
 * Providers cannot unplace widgets — "forget" (ViewModel) only clears the
 * stored config; the instance falls back to its "Pick a market" state.
 */
class WidgetsHostBridgeAndroid(
    private val context: Context,
) : WidgetsHostBridge {

    private val appWidgetManager: AppWidgetManager get() = AppWidgetManager.getInstance(context)

    override val canPin: Boolean
        get() = appWidgetManager.isRequestPinAppWidgetSupported

    override val canManage: Boolean = true

    override fun requestPin(kind: WidgetKind, base: String, quote: String) {
        if (!canPin) return
        val provider = ComponentName(
            context,
            when (kind) {
                WidgetKind.MARKET_CARD -> MarketRateCardWidgetReceiver::class.java
                WidgetKind.MARKET_WAVE -> MarketRateWidgetReceiver::class.java
                WidgetKind.MARKET_GRID -> MarketGridWidgetReceiver::class.java
                WidgetKind.QUICK_CONVERT -> QuickConvertWidgetReceiver::class.java
            },
        )
        val extras = Bundle().apply {
            putString(WidgetExtras.PREFILL_BASE, base)
            putString(WidgetExtras.PREFILL_QUOTE, quote)
        }
        appWidgetManager.requestPinAppWidget(provider, extras, null)
    }

    override fun reconfigure(id: String) {
        val appWidgetId = id.toIntOrNull() ?: return
        val intent = Intent(context, MarketRateWidgetConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun refreshAll() {
        WidgetRefreshScheduler.refreshNow(context)
        WidgetRefreshScheduler.update(context, hasWidgets = true)
    }

    override fun applyRefreshPolicy(realtime: Boolean) {
        WidgetRefreshScheduler.update(context, hasWidgets = true, realtime = realtime)
        if (realtime) WidgetRefreshScheduler.refreshNow(context)
    }
}
