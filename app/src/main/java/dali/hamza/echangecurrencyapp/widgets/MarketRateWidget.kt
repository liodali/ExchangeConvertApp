package dali.hamza.echangecurrencyapp.widgets

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.data.widgets.WidgetsRepository
import dali.hamza.shared.domain.models.WidgetConfig
import dali.hamza.shared.domain.models.WidgetSnapshot
import dali.hamza.shared.ui.theme.LedgerColorPalette
import dali.hamza.shared.ui.theme.LedgerThemeMode
import dali.hamza.shared.ui.theme.resolvePalette
import org.koin.mp.KoinPlatform
import java.util.Locale
import kotlin.math.abs

/** The two market-widget looks (`ui-design.pen`: `RxvJn` wave blob, `prEnT` card). */
enum class MarketRateVisual { WAVE, CARD }

private fun MarketRateVisual.widgetType(): dali.hamza.shared.domain.models.WidgetType =
    if (this == MarketRateVisual.WAVE) {
        dali.hamza.shared.domain.models.WidgetType.MARKET_WAVE
    } else {
        dali.hamza.shared.domain.models.WidgetType.MARKET_CARD
    }

/**
 * Shared engine for the single-market widget; [MarketRateWidget] (wave) and
 * [MarketRateCardWidget] (card) are the picker entries. Content is the same
 * pair / rate / Δ chip — only the silhouette differs. Tap opens the app.
 */
abstract class BaseMarketRateGlanceWidget(
    private val visual: MarketRateVisual,
) : GlanceAppWidget() {

    // re-render on every cell-size change; backgrounds scale with bounds
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val koin = KoinPlatform.getKoin()
        val repository = koin?.get<WidgetsRepository>()
        val storage = koin?.get<ISessionStorage>()

        // Self-configuration: launchers like Funtouch place the widget
        // WITHOUT running the config activity — adopt the pair stashed by
        // the Account add flow (or a pin extra handoff) and fetch its rate.
        val config = repository?.let { repo ->
            repo.configs().firstOrNull { it.id == appWidgetId.toString() }
                ?: repo.takePendingPair()?.let { pending ->
                    WidgetConfig(
                        id = appWidgetId.toString(),
                        type = visual.widgetType(),
                        base = pending.first,
                        quote = pending.second,
                    ).also {
                        repo.saveConfig(it)
                        WidgetRefreshScheduler.refreshNow(context)
                    }
                }
        }
        val snapshot = config?.let { repository.snapshotFor(it) }

        // accents follow the Account palette (dark surface keeps contrast on
        // any wallpaper)
        val palette = LedgerColorPalette.fromStored(storage?.getColorPalette())
        val mode = LedgerThemeMode.fromStored(storage?.getThemeMode())
        val accent = resolvePalette(mode, palette, systemDark = true).green.toArgb()

        val blob = if (visual == MarketRateVisual.WAVE) WaveBlobRenderer.render(accentArgb = accent) else null

        // unconfigured instances deep-link straight into the picker
        val pickIntent = android.content.Intent(
            context,
            MarketRateWidgetConfigActivity::class.java,
        )
            .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        // configured instances open the HISTORY page for their pair
        val historyIntent = config?.let { pairHistoryIntent(context, it.base, it.quote) }

        provideContent {
            MarketRateContent(config, snapshot, accent, visual, blob, pickIntent, historyIntent)
        }
    }
}

/** Wave-blob widget — the widget's silhouette IS a circular wave. */
class MarketRateWidget : BaseMarketRateGlanceWidget(MarketRateVisual.WAVE)

/** Rounded-card widget — the classic look (`prEnT` sketch). */
class MarketRateCardWidget : BaseMarketRateGlanceWidget(MarketRateVisual.CARD)

@Composable
private fun MarketRateContent(
    config: WidgetConfig?,
    snapshot: WidgetSnapshot?,
    accentArgb: Int,
    visual: MarketRateVisual,
    blob: Bitmap?,
    pickIntent: android.content.Intent,
    historyIntent: android.content.Intent?,
) {
    val accent = Color(accentArgb)
    // unconfigured → tapping opens the market picker; configured → history
    val action = when {
        config?.removed == true -> actionStartActivity(pairHistoryIntent(LocalContext.current, config.base, config.quote))
        config == null -> actionStartActivity(pickIntent)
        historyIntent != null -> actionStartActivity(historyIntent)
        else -> actionStartActivity(pairHistoryIntent(LocalContext.current, config.base, config.quote))
    }
    // launcher-reported size — compact styling below ~1.5 cells
    val options = androidx.glance.appwidget.LocalAppWidgetOptions.current
    val widthDp = options?.getInt(
        android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
    ) ?: 110
    val compact = widthDp < 90
    val pairSize = if (compact) 9.sp else 11.sp
    val rateSize = if (compact) 17.sp else 24.sp
    val root = if (visual == MarketRateVisual.CARD) {
        GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF2A2A2A))
            .cornerRadius(24.dp)
            .clickable(action)
    } else {
        GlanceModifier
            .fillMaxSize()
            .clickable(action)
    }
    Box(modifier = root) {
        if (blob != null) {
            Image(
                provider = ImageProvider(blob),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                // Fit — the square blob stays perfectly circular even when
                // the launcher cell is taller/wider than square
                contentScale = ContentScale.Fit,
            )
        }
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                // forgotten from the app — dead state until the launcher
                // removal finishes it (tap just opens the app)
                config?.removed == true -> {
                    Text(
                        "Removed",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF8F9097)),
                            fontSize = if (compact) 9.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        "Long-press to delete",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF6E6E6E)),
                            fontSize = if (compact) 7.sp else 9.sp,
                        ),
                    )
                }
                config == null -> {
                    Text(
                        "Pick a market",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFC5C6CD)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                else -> {
                    Text(
                        // one unbreakable token — codes never wrap mid-word even in
                        // squeezed cells ("TND" stays whole)
                        "${config.base}/${config.quote}",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFC5C6CD)),
                            fontSize = pairSize,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    if (snapshot == null) {
                        Text(
                            "…",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFE5E2E1)),
                                fontSize = rateSize,
                            ),
                        )
                    } else {
                        Text(
                            formatRate(snapshot.rate),
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFE5E2E1)),
                                fontSize = rateSize,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        snapshot.dayDeltaPercent?.let { delta ->
                            if (!compact) DeltaChip(delta, accent)
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun DeltaChip(delta: Double, accent: Color) {
    val positive = delta >= 0
    val deltaColor = if (positive) accent else Color(0xFFFFB4AB)
    Box(
        modifier = GlanceModifier
            .cornerRadius(10.dp)
            .background(Color((deltaColor.toArgb() and 0x00FFFFFF) or (0x33 shl 24)))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            "${if (positive) "▲ +" else "▼ -"}%.2f%%".format(Locale.US, abs(delta)),
            style = TextStyle(
                color = ColorProvider(deltaColor),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/** Rates keep tabular digits and the sketch's precision ladder. */
internal fun formatRate(rate: Double): String {
    val pattern = when {
        rate >= 100.0 -> "%.2f"
        rate >= 1.0 -> "%.4f"
        rate >= 0.01 -> "%.5f"
        else -> "%.7f"
    }
    return pattern.format(Locale.US, rate)
}
