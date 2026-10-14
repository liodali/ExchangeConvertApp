package dali.hamza.echangecurrencyapp.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dali.hamza.echangecurrencyapp.ui.MainActivity
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.data.widgets.WidgetsRepository
import dali.hamza.shared.domain.models.WidgetConfig
import dali.hamza.shared.ui.DeepLinkNav
import dali.hamza.shared.ui.theme.LedgerColorPalette
import dali.hamza.shared.ui.theme.LedgerThemeMode
import dali.hamza.shared.ui.theme.resolvePalette
import org.koin.mp.KoinPlatform

/**
 * Quick Convert widget (`GDiky` sketch, medium 4×2): pick a pair (same
 * self-configuring flow as the market widget), tap an amount preset, and
 * the converted value computes from the latest cached snapshot — offline
 * safe, no conversion call needed. Tapping the result opens the app.
 *
 * Glance 1.2 has NO text input, so amounts are presets (+ steppers) —
 * the full free-form converter stays one tap away.
 */
class QuickConvertWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val koin = KoinPlatform.getKoin()
        val repository = koin?.get<WidgetsRepository>()
        val storage = koin?.get<ISessionStorage>()

        val config = repository?.let { repo ->
            repo.configs().firstOrNull { it.id == appWidgetId.toString() }
                ?: repo.takePendingPair()?.let { pending ->
                    WidgetConfig(
                        id = appWidgetId.toString(),
                        type = dali.hamza.shared.domain.models.WidgetType.QUICK_CONVERT,
                        base = pending.first,
                        quote = pending.second,
                    ).also {
                        repo.saveConfig(it)
                        WidgetRefreshScheduler.refreshNow(context)
                    }
                }
        }
        val snapshot = config?.let { repository.snapshotFor(it) }
        val amount = AmountStore.get(context, appWidgetId)

        provideContent {
            QuickConvertContent(config, snapshot, amount, appWidgetId)
        }
    }
}

private val AMOUNT_PRESETS = listOf(1.0, 10.0, 100.0, 1000.0)

@Composable
private fun QuickConvertContent(
    config: WidgetConfig?,
    snapshot: dali.hamza.shared.domain.models.WidgetSnapshot?,
    amount: Double,
    appWidgetId: Int,
) {
    val openApp = actionStartActivity(
        // deep link: open the app on the HISTORY page for this pair
        if (config != null) {
            pairHistoryIntent(androidx.glance.LocalContext.current, config.base, config.quote)
        } else {
            android.content.Intent(androidx.glance.LocalContext.current, MainActivity::class.java)
        },
    )
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF2A2A2A))
            .cornerRadius(24.dp)
            .clickable(openApp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (config?.removed == true) {
                // forgotten from the app — dead state until the launcher
                // removal finishes it (tap just opens the app)
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
                return@Column
            }
            if (config == null) {
                Text(
                    "Pick a market",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFC5C6CD)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                return@Column
            }
            // GDiky sketch: content block vertically CENTERED — the row wraps
            // its height (no fillHeight) and equal spacers balance it
            Spacer(GlanceModifier.defaultWeight())
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
            ) {
                // LEFT — amount + pair chips with swap
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("AMOUNT", style = labelStyle())
                    Spacer(GlanceModifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            trimAmount(amount),
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFE5E2E1)),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Spacer(GlanceModifier.width(4.dp))
                        CodeChip(config.base)
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CodeChip(config.base)
                        Spacer(GlanceModifier.width(4.dp))
                        // swap ⇄ — action swaps the configured pair
                        Box(
                            modifier = GlanceModifier
                                .background(Color(0xFF353534))
                                .cornerRadius(14.dp)
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                .clickable(
                                    actionRunCallback<SwapPairAction>(
                                        actionParametersOf(SetAmountAction.WIDGET_ID to appWidgetId),
                                    ),
                                ),
                        ) {
                            Text(
                                "⇄",
                                maxLines = 1,
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFC5C6CD)),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        Spacer(GlanceModifier.width(4.dp))
                        CodeChip(config.quote)
                    }
                }
                // RIGHT — converted + cached note
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text("CONVERTED", style = labelStyle())
                    Spacer(GlanceModifier.height(4.dp))
                    val result = snapshot?.rate?.let { amount * it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            result?.let { formatRate(it) } ?: "…",
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFE5E2E1)),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Spacer(GlanceModifier.width(4.dp))
                        CodeChip(config.quote)
                    }
                    snapshot?.rate?.let {
                        Text(
                            "cached rate ${formatRate(it)}",
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF8F9097)),
                                fontSize = 9.sp,
                            ),
                        )
                    }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.height(8.dp))
            // bottom bar: amount presets (left) + gold Open app (right corner)
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row {
                AMOUNT_PRESETS.forEach { preset ->
                    val selected = preset == amount
                    Box(
                        modifier = GlanceModifier
                            .padding(horizontal = 3.dp)
                            .cornerRadius(10.dp)
                            .background(
                                if (selected) Color(0x294EDEA3) else Color(0xFF353534),
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .clickable(
                                actionRunCallback<SetAmountAction>(
                                    actionParametersOf(
                                        SetAmountAction.AMOUNT to preset,
                                        SetAmountAction.WIDGET_ID to appWidgetId,
                                    ),
                                ),
                            ),
                    ) {
                        Text(
                            trimAmount(preset),
                            style = TextStyle(
                                color = ColorProvider(
                                    if (selected) Color(0xFF4EDEA3) else Color(0xFFC5C6CD),
                                ),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
                }
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    "Open app ↗",
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFE9C349)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

private fun labelStyle() = TextStyle(
    color = ColorProvider(Color(0xFF8F9097)),
    fontSize = 9.sp,
    fontWeight = FontWeight.Bold,
)

@Composable
private fun CodeChip(code: String) {
    Box(
        modifier = GlanceModifier
            .width((code.length * 7 + 10).coerceAtLeast(32).dp)
            .background(Color(0xFF353534))
            .cornerRadius(8.dp)
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Text(
            code,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(Color(0xFFC5C6CD)),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/** Preset tap → persist per-widget amount → repaint this instance. */
class SetAmountAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val amount = parameters[AMOUNT]
        val appWidgetId = parameters[WIDGET_ID] ?: return
        if (amount != null) {
            AmountStore.set(context, appWidgetId, amount)
        }
        QuickConvertWidget().update(context, glanceId)
    }

    companion object {
        val AMOUNT: ActionParameters.Key<Double> = ActionParameters.Key("quick.amount")
        val WIDGET_ID: ActionParameters.Key<Int> = ActionParameters.Key("quick.widgetId")
    }
}

/** Swap ⇄ tap — flips the configured pair and repaints. */
class SwapPairAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val appWidgetId = parameters[SetAmountAction.WIDGET_ID] ?: return
        val repository = KoinPlatform.getKoin()?.get<WidgetsRepository>() ?: return
        val config = repository.configs().firstOrNull { it.id == appWidgetId.toString() } ?: return
        repository.saveConfig(config.copy(base = config.quote, quote = config.base))
        QuickConvertWidget().update(context, glanceId)
    }
}

/** Per-widget selected preset (SharedPreferences — amount only, tiny). */
object AmountStore {
    private const val PREFS = "widgets_quick"

    fun get(context: Context, appWidgetId: Int): Double =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat("amount_$appWidgetId", 100f)
            .toDouble()

    fun set(context: Context, appWidgetId: Int, amount: Double) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat("amount_$appWidgetId", amount.toFloat())
            .apply()
    }
}

internal fun trimAmount(value: Double): String =
    if (value >= 1000) "${value.toInt() / 1000}K" else value.toInt().toString()
