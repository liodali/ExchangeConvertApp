package dali.hamza.echangecurrencyapp.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.toArgb
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.data.widgets.WidgetsRepository
import dali.hamza.shared.domain.models.WidgetConfig
import dali.hamza.shared.domain.models.WidgetSnapshot
import dali.hamza.echangecurrencyapp.ui.MainActivity
import java.util.Locale
import kotlin.math.abs

/**
 * Markets grid widget (`p7ayhz` sketch, medium 4×2) — **zero config**: it
 * always mirrors the Account market preferences (base + up to 3 tracked
 * codes); editing markets in the app is the configuration. Each mini card
 * shows pair / rate / day-Δ; tapping opens the app.
 */
class MarketGridWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = org.koin.mp.KoinPlatform.getKoin()
        val storage = koin?.get<ISessionStorage>()
        val repository = koin?.get<WidgetsRepository>()
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        // custom pairs stored at add time; default = market preferences.
        // Self-configures from the pending handoff like the other widgets.
        val config = repository?.configs()?.firstOrNull { it.id == appWidgetId.toString() }
            ?: repository?.takePendingPairs()?.let { pending ->
                WidgetConfig(
                    id = appWidgetId.toString(),
                    type = dali.hamza.shared.domain.models.WidgetType.MARKET_GRID,
                    base = pending.first().first,
                    quote = pending.first().second,
                    pairs = pending.map { (b, q) -> "$b/$q" },
                ).also {
                    repository.saveConfig(it)
                    WidgetRefreshScheduler.refreshNow(context)
                }
            }

        val base = (storage?.getCurrency() ?: "").ifBlank { "USD" }
        val removed = config?.removed == true
        val pairs = if (removed) {
            emptyList()
        } else {
            config?.gridPairs?.takeIf { it.size >= 2 }
                ?: storage?.getMarketPreferences()
                    .orEmpty()
                    .filter { it != base }
                    .take(3)
                    .map { base to it }
        }
        val snapshots: Map<String, WidgetSnapshot> =
            repository?.readSnapshotsForPairs().orEmpty()

        provideContent {
            GridContent(pairs, snapshots, context, removed)
        }
    }
}

@Composable
private fun GridContent(
    pairs: List<Pair<String, String>>,
    snapshots: Map<String, WidgetSnapshot>,
    context: android.content.Context,
    removed: Boolean = false,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF2A2A2A))
            .cornerRadius(24.dp)
            .padding(14.dp),
    ) {
        if (removed) {
            // forgotten from the app — dead state (long-press on launcher)
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Removed",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8F9097)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    "Long-press to delete",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF6E6E6E)),
                        fontSize = 9.sp,
                    ),
                )
            }
        } else if (pairs.size < 2) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(
                        actionStartActivity(
                            android.content.Intent(context, MainActivity::class.java),
                        ),
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Pick 2–3 markets",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFC5C6CD)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    "Account → Market Preferences",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8F9097)),
                        fontSize = 9.sp,
                    ),
                )
            }
        } else {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
            ) {
                // p7ayhz sketch header: MARKETS + subtitle + gold Manage
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "MARKETS",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFC5C6CD)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        "your markets",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF8F9097)),
                            fontSize = 8.sp,
                        ),
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        "Manage",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFE9C349)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = GlanceModifier.clickable(
                            actionStartActivity(
                                android.content.Intent(context, MainActivity::class.java),
                            ),
                        ),
                    )
                }
                Spacer(GlanceModifier.height(10.dp))
                // 2 or 3 cards — defaultWeight shares the width equally, so
                // two markets each get the extra space (max 3 per grid).
                // REAL Spacers separate the cards (Glance paints background
                // over outer padding — padding alone shows no gap).
                // Each card opens the HISTORY page for its own pair.
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    pairs.take(3).forEachIndexed { index, (base, quote) ->
                        val snapshot = snapshots["$base/$quote"]
                        if (index > 0) Spacer(GlanceModifier.width(10.dp))
                        MiniMarketCard(
                            pair = "$base/$quote",
                            rate = snapshot?.rate?.let { formatRate(it) } ?: "…",
                            deltaPercent = snapshot?.dayDeltaPercent,
                            intent = pairHistoryIntent(context, base, quote),
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMarketCard(
    pair: String,
    rate: String,
    deltaPercent: Double?,
    intent: android.content.Intent,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        modifier = modifier
            .background(Color(0xFF3A3A39))
            .cornerRadius(12.dp)
            .clickable(actionStartActivity(intent))
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                pair,
                style = TextStyle(
                    color = ColorProvider(Color(0xFFC5C6CD)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(8.dp))
            Text(
                rate,
                style = TextStyle(
                    color = ColorProvider(Color(0xFFE5E2E1)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            deltaPercent?.let { delta ->
                Spacer(GlanceModifier.height(8.dp))
                val positive = delta >= 0
                val deltaColor = if (positive) Color(0xFF4EDEA3) else Color(0xFFFFB4AB)
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(8.dp)
                        .background(Color((deltaColor.toArgb() and 0x00FFFFFF) or (0x26 shl 24)))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        "${if (positive) "▲ +" else "▼ -"}%.2f%%".format(Locale.US, abs(delta)),
                        style = TextStyle(
                            color = ColorProvider(deltaColor),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}
