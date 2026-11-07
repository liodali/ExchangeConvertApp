package dali.hamza.echangecurrencyapp.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dali.hamza.shared.common.MetalPricing
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.data.widgets.WidgetsRepository
import dali.hamza.shared.domain.models.WidgetConfig
import dali.hamza.shared.domain.models.WidgetSnapshot
import dali.hamza.shared.domain.models.WidgetType
import dali.hamza.shared.ui.DeepLinkNav
import dali.hamza.shared.ui.theme.LedgerColorPalette
import dali.hamza.shared.ui.theme.LedgerThemeMode
import dali.hamza.shared.ui.theme.resolvePalette
import org.koin.mp.KoinPlatform
import java.util.Locale
import kotlin.math.round

/**
 * Gold widget (plans/widgets.md + plans/gold-section.md): spot value per
 * gram at 24K in the picked display fiat, with the per-ounce spot below.
 * Base is always XAU — the add dialog fixes it and only the fiat is
 * chosen (metals convert one way only). Tap opens the converter
 * pre-filled with 1 g of gold.
 */
class GoldWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val koin = KoinPlatform.getKoin()
        val repository = koin?.get<WidgetsRepository>()
        val storage = koin?.get<ISessionStorage>()

        // Self-configuration (Funtouch places widgets without the config
        // activity): adopt the pending pair stashed by the Account add flow
        val config = repository?.let { repo ->
            repo.configs().firstOrNull { it.id == appWidgetId.toString() }
                ?: repo.takePendingPair()?.let { pending ->
                    WidgetConfig(
                        id = appWidgetId.toString(),
                        type = WidgetType.GOLD,
                        // gold widgets are XAU-based even if a pending fiat
                        // pair leaks in from another widget kind's add flow
                        base = "XAU",
                        quote = pending.second,
                    ).also {
                        repo.saveConfig(it)
                        WidgetRefreshScheduler.refreshNow(context)
                    }
                }
        }
        val snapshot = config?.let { repository.snapshotFor(it) }

        val palette = LedgerColorPalette.fromStored(storage?.getColorPalette())
        val mode = LedgerThemeMode.fromStored(storage?.getThemeMode())
        val gold = resolvePalette(mode, palette, systemDark = true).gold.toArgb()

        val pickIntent = Intent(
            context,
            MarketRateWidgetConfigActivity::class.java,
        )
            .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        provideContent {
            GoldContent(config, snapshot, gold, pickIntent)
        }
    }
}

/** Widget tap: converter pre-filled with 1 gram of gold (Home card parity). */
private fun goldConverterIntent(context: Context, quote: String): Intent =
    Intent(context, dali.hamza.echangecurrencyapp.ui.MainActivity::class.java)
        .putExtra(DeepLinkNav.EXTRA_ROUTE, "converter")
        .putExtra(DeepLinkNav.EXTRA_PAIR_BASE, "XAU")
        .putExtra(DeepLinkNav.EXTRA_PAIR_QUOTE, quote)
        .putExtra(DeepLinkNav.EXTRA_AMOUNT, 1.0)

@Composable
private fun GoldContent(
    config: WidgetConfig?,
    snapshot: WidgetSnapshot?,
    goldArgb: Int,
    pickIntent: Intent,
) {
    val gold = Color(goldArgb)
    val action = when {
        config?.removed == true ->
            actionStartActivity(goldConverterIntent(LocalContext.current, config.quote))
        config == null -> actionStartActivity(pickIntent)
        else -> actionStartActivity(goldConverterIntent(LocalContext.current, config.quote))
    }
    val options = androidx.glance.appwidget.LocalAppWidgetOptions.current
    val widthDp = options?.getInt(
        android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
    ) ?: 110
    val compact = widthDp < 90

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF2A2A2A))
            .cornerRadius(24.dp)
            .clickable(action)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            config?.removed == true -> {
                Text(
                    "Removed",
                    style = TextStyle(ColorProvider(Color(0xFF8F9097)), 11.sp, FontWeight.Bold),
                )
                Text(
                    "Long-press to delete",
                    style = TextStyle(ColorProvider(Color(0xFF6E6E6E)), 9.sp),
                )
            }
            config == null -> {
                Text(
                    "Pick gold currency",
                    style = TextStyle(ColorProvider(Color(0xFFC5C6CD)), 12.sp, FontWeight.Bold),
                )
            }
            else -> {
                val spot = snapshot?.rate?.takeIf { it > 0.0 && it.isFinite() }
                if (spot == null) {
                    Text(
                        "GOLD · XAU/${config.quote}",
                        style = TextStyle(ColorProvider(Color(0xFFC5C6CD)), 11.sp, FontWeight.Bold),
                    )
                    Text(
                        "…",
                        style = TextStyle(ColorProvider(Color(0xFF6E6E6E)), 12.sp),
                    )
                } else {
                    val perGram = MetalPricing.fineGoldPerGram(spot)
                    Text(
                        "GOLD 24K",
                        style = TextStyle(
                            ColorProvider(gold),
                            if (compact) 9.sp else 11.sp,
                            FontWeight.Bold,
                        ),
                    )
                    Text(
                        // per-gram price, e.g. "372.93 TND/g"
                        "${formatWidgetAmount(perGram)} ${config.quote}/g",
                        style = TextStyle(
                            ColorProvider(Color(0xFFF4F4F6)),
                            if (compact) 15.sp else 20.sp,
                            FontWeight.Bold,
                        ),
                    )
                    Text(
                        "1 oz = ${formatWidgetAmount(spot)}",
                        style = TextStyle(
                            ColorProvider(Color(0xFF8F9097)),
                            if (compact) 8.sp else 10.sp,
                        ),
                    )
                }
            }
        }
    }
}

/** Grouped 2-decimal amount, locale-stable (widget-safe, no compose resources). */
private fun formatWidgetAmount(value: Double): String {
    val rounded = round(value * 100.0) / 100.0
    val text = String.format(Locale.US, "%.2f", rounded)
    val intPart = text.substringBefore('.')
    val frac = text.substringAfter('.')
    val grouped = buildString {
        intPart.reversed().chunked(3).forEachIndexed { i, chunk ->
            if (i > 0) append(',')
            append(chunk)
        }
    }.reversed()
    return "$grouped.$frac"
}
