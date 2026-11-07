package dali.hamza.echangecurrencyapp.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import dali.hamza.shared.data.CurrenciesCatalog
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.data.widgets.WidgetsRepository
import dali.hamza.shared.domain.models.WidgetConfig
import dali.hamza.shared.domain.models.WidgetType
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * APPWIDGET_CONFIGURE activity for the market-rate widget: pick base +
 * quote from the shared catalog (defaults: session base + USD), then the
 * widget is placed and an immediate refresh worker fetches its snapshot.
 *
 * Follows the platform contract: RESULT_CANCELED up front (widget removed
 * if the user backs out), RESULT_OK with EXTRA_APPWIDGET_ID on confirm.
 */
class MarketRateWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // backing out must cancel the widget placement
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))

        val koin = KoinPlatform.getKoin()
        val repository = koin?.get<WidgetsRepository>()
        if (repository == null) {
            finish()
            return
        }
        val storage = koin.get<ISessionStorage>()
        // gold widgets get their own config surface: XAU base is fixed, the
        // user picks the display FIAT and the mass unit (g/oz/kg, oz default)
        val isGold = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(appWidgetId)?.provider?.className
            ?.endsWith("GoldWidgetReceiver") == true
        val codes = CurrenciesCatalog.currencies.map { it.name }
        // quick-select: existing widget pairs + Account market preferences
        val suggestions = buildList {
            repository.configs().forEach { add(it.base to it.quote) }
            val prefsBase = storage.getCurrency().ifBlank { "USD" }
            storage.getMarketPreferences().filter { it != prefsBase }.forEach { add(prefsBase to it) }
        }.distinct()
        // prefill chain: an existing config (reconfigure) → pin extras →
        // pending pair from the Account add flow (launchers don't reliably
        // forward pin extras) → session defaults
        val existing = repository.configs().firstOrNull { it.id == appWidgetId.toString() }
        // keep the widget's kind on reconfigure (set at self-configuration)
        val existingType = existing?.type ?: if (isGold) WidgetType.GOLD else WidgetType.MARKET_CARD
        val prefillBase = intent?.extras?.getString(dali.hamza.shared.platform.WidgetExtras.PREFILL_BASE)
        val prefillQuote = intent?.extras?.getString(dali.hamza.shared.platform.WidgetExtras.PREFILL_QUOTE)
        val pending = runCatching { repository.takePendingPair() }.getOrNull()
        val fallbackBase = storage.getCurrency().ifBlank { "USD" }
        val defaultBase = existing?.base ?: prefillBase ?: pending?.first ?: fallbackBase
        val defaultQuote = existing?.quote
            ?: prefillQuote
            ?: pending?.second
            ?: if (defaultBase == "USD") "EUR" else "USD"

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF131313))) {
                if (isGold) {
                    GoldConfigScreen(
                        fiatCodes = codes.filterNot { it in CurrenciesCatalog.assetCodes },
                        initialQuote = (existing?.quote ?: pending?.second ?: fallbackBase)
                            .takeIf { it.isNotBlank() && it !in CurrenciesCatalog.assetCodes }
                            ?: "USD",
                        initialFollow = existing?.quote.isNullOrBlank() || existing == null,
                        initialUnit = existing?.unit ?: dali.hamza.shared.common.MetalPricing.GoldUnit.TROY_OUNCE.name,
                        onConfirm = { quote, unit ->
                            lifecycleScope.launch {
                                repository.saveConfig(
                                    WidgetConfig(
                                        id = appWidgetId.toString(),
                                        type = WidgetType.GOLD,
                                        base = "XAU",
                                        quote = quote,
                                        unit = unit,
                                    ),
                                )
                                runCatching {
                                    val manager = GlanceAppWidgetManager(this@MarketRateWidgetConfigActivity)
                                    GoldWidget().update(
                                        this@MarketRateWidgetConfigActivity,
                                        manager.getGlanceIdBy(appWidgetId),
                                    )
                                }
                                WidgetRefreshScheduler.refreshNow(this@MarketRateWidgetConfigActivity)
                                WidgetRefreshScheduler.update(this@MarketRateWidgetConfigActivity, hasWidgets = true)
                                setResult(
                                    RESULT_OK,
                                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                                )
                                finish()
                            }
                        },
                        onCancel = { finish() },
                    )
                } else {
                ConfigScreen(
                    codes = codes,
                    initialBase = defaultBase,
                    initialQuote = defaultQuote,
                    suggested = suggestions,
                    onConfirm = { base, quote ->
                        lifecycleScope.launch {
                            repository.saveConfig(
                                WidgetConfig(
                                    id = appWidgetId.toString(),
                                    type = existingType,
                                    base = base,
                                    quote = quote,
                                ),
                            )
                            // paint the pair immediately (the widget shows "…"
                            // until the refresh worker lands its snapshot)
                            runCatching {
                                val manager = GlanceAppWidgetManager(this@MarketRateWidgetConfigActivity)
                                MarketRateWidget().update(
                                    this@MarketRateWidgetConfigActivity,
                                    manager.getGlanceIdBy(appWidgetId),
                                )
                            }
                            WidgetRefreshScheduler.refreshNow(this@MarketRateWidgetConfigActivity)
                            WidgetRefreshScheduler.update(this@MarketRateWidgetConfigActivity, hasWidgets = true)
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                            )
                            finish()
                        }
                    },
                    onCancel = { finish() },
                )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ConfigScreen(
    codes: List<String>,
    initialBase: String,
    initialQuote: String,
    suggested: List<Pair<String, String>>,
    onConfirm: (String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var base by remember { mutableStateOf(initialBase) }
    var quote by remember { mutableStateOf(initialQuote) }
    // null = pair fields, 0 = picking base, 1 = picking quote
    var pickerOpen by remember { mutableStateOf<Int?>(null) }
    var query by remember { mutableStateOf("") }
    val sheetInteraction = remember { MutableInteractionSource() }

    // bottom-sheet presentation: scrim tap cancels, sheet swallows taps
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
            ))
            .clickable(interactionSource = sheetInteraction, indication = null) { onCancel() },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(interactionSource = sheetInteraction, indication = null) {}
                .background(
                    Color(0xFF1C1B1B),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .background(Color(0xFF4A4A4A), RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.height(14.dp))

            if (pickerOpen != null) {
                // picking mode: the sheet becomes a dedicated currency step —
                // search + list own the space, no crowding under NEW PAIR
                val pickingBase = pickerOpen == 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFC5C6CD),
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { pickerOpen = null; query = "" },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Choose ${if (pickingBase) "from" else "to"} currency",
                        fontSize = 16.sp,
                        color = Color(0xFFE5E2E1),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (pickingBase) base else quote,
                        fontSize = 13.sp,
                        color = Color(0xFF8F9097),
                        modifier = Modifier
                            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Picker(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp),
                    codes = codes,
                    query = query,
                    onQueryChange = { query = it },
                    onPick = { code ->
                        if (pickingBase) {
                            if (code == quote) quote = base
                            base = code
                        } else {
                            if (code == base) base = quote
                            quote = code
                        }
                        pickerOpen = null
                        query = ""
                    },
                )
                Spacer(Modifier.height(12.dp))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Select market", fontSize = 20.sp, color = Color(0xFFE5E2E1))
                    if (suggested.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("YOUR MARKETS", fontSize = 10.sp, color = Color(0xFF8F9097))
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                suggested.take(8).forEach { (b, q) ->
                                    val selected = b == base && q == quote
                                    androidx.compose.material3.Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) Color(0x294EDEA3) else Color(0xFF2A2A2A),
                                        contentColor = if (selected) Color(0xFF4EDEA3) else Color(0xFFE5E2E1),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { base = b; quote = q },
                                    ) {
                                        Text(
                                            "$b/$q",
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text("NEW PAIR", fontSize = 10.sp, color = Color(0xFF8F9097))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PairField("FROM", base, Modifier.weight(1f)) { pickerOpen = 0; query = "" }
                        PairField("TO", quote, Modifier.weight(1f)) { pickerOpen = 1; query = "" }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text("Cancel", color = Color(0xFF8F9097)) }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { onConfirm(base, quote) }, enabled = base != quote) {
                        Text("Add widget", color = Color(0xFF4EDEA3))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * Gold widget configuration (same bottom-sheet recipe): pick the display
 * FIAT (base is fixed XAU) and the mass unit — g / oz / kg, oz default.
 */
@Composable
private fun GoldConfigScreen(
    fiatCodes: List<String>,
    initialQuote: String,
    initialFollow: Boolean,
    initialUnit: String,
    onConfirm: (quote: String, unit: String) -> Unit,
    onCancel: () -> Unit,
) {
    var quote by remember { mutableStateOf(initialQuote) }
    var unitName by remember { mutableStateOf(initialUnit) }
    // by default the widget follows the app's display base at RUNTIME;
    // "Pick a currency" pins a fixed fiat (blank quote = follow)
    var follow by remember { mutableStateOf(initialFollow) }
    val units = dali.hamza.shared.common.MetalPricing.GoldUnit.entries
    val unit = units.firstOrNull { it.name == unitName }
        ?: dali.hamza.shared.common.MetalPricing.GoldUnit.TROY_OUNCE
    var pickerOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val sheetInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
            ))
            .clickable(interactionSource = sheetInteraction, indication = null) { onCancel() },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(interactionSource = sheetInteraction, indication = null) {}
                .background(
                    Color(0xFF1C1B1B),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .background(Color(0xFF4A4A4A), RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.height(14.dp))

            if (pickerOpen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFC5C6CD),
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { pickerOpen = false; query = "" },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Gold price currency",
                        fontSize = 16.sp,
                        color = Color(0xFFE5E2E1),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Picker(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp),
                    codes = fiatCodes,
                    query = query,
                    onQueryChange = { query = it },
                    onPick = { code ->
                        quote = code
                        pickerOpen = false
                        query = ""
                    },
                )
                Spacer(Modifier.height(12.dp))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Gold price", fontSize = 20.sp, color = Color(0xFFE5E2E1))
                    Text("XAU (troy ounce) is fixed — choose the price currency.", fontSize = 12.sp, color = Color(0xFF8F9097))
                    Text("CURRENCY", fontSize = 10.sp, color = Color(0xFF8F9097))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (follow) Color(0x294EDEA3) else Color(0xFF2A2A2A),
                            contentColor = if (follow) Color(0xFF4EDEA3) else Color(0xFFE5E2E1),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { follow = true },
                        ) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Text("Follow the app", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("tracks your currency live", fontSize = 10.sp, color = Color(0xFF8F9097))
                            }
                        }
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!follow) Color(0x294EDEA3) else Color(0xFF2A2A2A),
                            contentColor = if (!follow) Color(0xFF4EDEA3) else Color(0xFFE5E2E1),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { follow = false },
                        ) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Text("Pick a currency", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(quote, fontSize = 10.sp, color = Color(0xFF8F9097))
                            }
                        }
                    }
                    if (follow) {
                        PairField("APP (tracks your currency)", "auto", Modifier.fillMaxWidth()) { }
                    } else {
                        PairField("CURRENCY", quote, Modifier.fillMaxWidth()) { pickerOpen = true; query = "" }
                    }
                    Text("UNIT", fontSize = 10.sp, color = Color(0xFF8F9097))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        units.forEach { candidate ->
                            val selected = candidate == unit
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) Color(0x29E9C349) else Color(0xFF2A2A2A),
                                contentColor = if (selected) Color(0xFFE9C349) else Color(0xFFE5E2E1),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { unitName = candidate.name },
                            ) {
                                Text(
                                    candidate.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text("Cancel", color = Color(0xFF8F9097)) }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { onConfirm(if (follow) "" else quote, unit.name) }) {
                        Text("Add widget", color = Color(0xFF4EDEA3))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PairField(label: String, code: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(label, fontSize = 10.sp, color = Color(0xFF8F9097))
            Text(code, fontSize = 20.sp, color = Color(0xFFE5E2E1))
        }
    }
}

@Composable
private fun Picker(
    modifier: Modifier,
    codes: List<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    onPick: (String) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search currency", color = Color(0xFF8F9097)) },
            singleLine = true,
        )
        LazyColumn {
            items(codes.filter { it.contains(query, ignoreCase = true) }) { code ->
                Text(
                    code,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(code) }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    color = Color(0xFFE5E2E1),
                    fontSize = 15.sp,
                )
            }
        }
    }
}
