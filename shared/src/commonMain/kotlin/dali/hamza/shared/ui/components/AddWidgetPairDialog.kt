package dali.hamza.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.platform.WidgetKind
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LedgerStrings

/**
 * Add-widget dialog (Account → Widgets): pick a widget TYPE (card market /
 * wave market / markets grid / quick converter), then a pair when the type
 * needs one. The grid follows the Account market preferences — no pair step.
 * Confirm fires the platform pin request; the pair is stashed so the widget
 * self-configures even when the launcher skips the config activity.
 */
@Composable
fun AddWidgetPairDialog(
    currencies: List<Currency>,
    onDismiss: () -> Unit,
    onAdd: (kind: WidgetKind, base: String, quote: String, gridPairs: List<Pair<String, String>>) -> Unit,
    suggestedPairs: List<Pair<String, String>> = emptyList(),
) {
    var kind by remember { mutableStateOf(WidgetKind.MARKET_CARD) }
    var base by remember { mutableStateOf(currencies.firstOrNull { it.name == "USD" }?.name ?: "USD") }
    var quote by remember { mutableStateOf(currencies.firstOrNull { it.name == "EUR" }?.name ?: "EUR") }
    var pickingBase by remember { mutableStateOf(false) }
    var pickingQuote by remember { mutableStateOf(false) }
    // grid: follow the Account markets, or pick 2–3 custom pairs
    var gridFollowDefault by remember { mutableStateOf(true) }
    var gridPairs by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val needsPair = kind != WidgetKind.MARKET_GRID
    val gridCustom = kind == WidgetKind.MARKET_GRID && !gridFollowDefault
    val gridValid = kind != WidgetKind.MARKET_GRID || gridFollowDefault || gridPairs.size >= 2

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = LedgerColors.SurfaceElevated,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    LedgerStrings.Widgets.ADD,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LedgerColors.TextPrimary,
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    LedgerStrings.Widgets.TYPE,
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerColors.TextTertiary,
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetKind.entries.forEach { option ->
                        KindCard(
                            title = LedgerStrings.Widgets.kindTitle(option),
                            description = LedgerStrings.Widgets.kindDescription(option),
                            selected = kind == option,
                            onClick = { kind = option },
                        )
                    }
                }

                if (needsPair) {
                    if (suggestedPairs.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            LedgerStrings.Widgets.YOUR_MARKETS,
                            style = MaterialTheme.typography.labelSmall,
                            color = LedgerColors.TextTertiary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            suggestedPairs.take(3).forEach { (b, q) ->
                                PairChip(pair = "$b/$q", selected = b == base && q == quote) {
                                    base = b
                                    quote = q
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        LedgerStrings.Widgets.NEW_PAIR,
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerColors.TextTertiary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PairField(
                            label = LedgerStrings.RateAlerts.BASE_LABEL,
                            code = base,
                            modifier = Modifier.weight(1f),
                            onClick = { pickingBase = true },
                        )
                        PairField(
                            label = LedgerStrings.RateAlerts.QUOTE_LABEL,
                            code = quote,
                            modifier = Modifier.weight(1f),
                            onClick = { pickingQuote = true },
                        )
                    }
                } else {
                    // grid source: default markets vs custom pairs (2–3)
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        KindCard(
                            title = "Follow your markets",
                            description = "Mirrors the markets selected in your Account.",
                            selected = gridFollowDefault,
                            onClick = { gridFollowDefault = true },
                        )
                        KindCard(
                            title = "Choose pairs",
                            description = "Pick 2–3 pairs just for this widget.",
                            selected = !gridFollowDefault,
                            onClick = { gridFollowDefault = false },
                        )
                    }
                    if (gridCustom) {
                        Spacer(Modifier.height(12.dp))
                        if (gridPairs.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                gridPairs.forEach { (b, q) ->
                                    PairChip(pair = "$b/$q ✕", selected = false) {
                                        gridPairs = gridPairs - (b to q)
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        if (gridPairs.size < 3) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PairField(
                                    label = LedgerStrings.RateAlerts.BASE_LABEL,
                                    code = base,
                                    modifier = Modifier.weight(1f),
                                    onClick = { pickingBase = true },
                                )
                                PairField(
                                    label = LedgerStrings.RateAlerts.QUOTE_LABEL,
                                    code = quote,
                                    modifier = Modifier.weight(1f),
                                    onClick = { pickingQuote = true },
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LedgerButton(
                                text = "Add pair (${gridPairs.size}/3)",
                                onClick = {
                                    if (base != quote) gridPairs = gridPairs + (base to quote)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = base != quote,
                            )
                        } else {
                            Text(
                                "Maximum 3 pairs — tap a chip to remove one.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LedgerColors.TextTertiary,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                LedgerButton(
                    text = LedgerStrings.Widgets.ADD_CONFIRM,
                    onClick = { onAdd(kind, base, quote, if (gridCustom) gridPairs else emptyList()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = gridValid && (!needsPair || base != quote),
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(LedgerStrings.RateAlerts.CANCEL, color = LedgerColors.TextTertiary) }
            }
        }
    }

    CurrencyPickerSheet(
        visible = pickingBase,
        currencies = currencies,
        title = LedgerStrings.RateAlerts.PICK_BASE_TITLE,
        onDismiss = { pickingBase = false },
        onCurrencySelected = { c ->
            if (c.name == quote) quote = base
            base = c.name
            pickingBase = false
        },
    )
    CurrencyPickerSheet(
        visible = pickingQuote,
        currencies = currencies,
        title = LedgerStrings.RateAlerts.PICK_QUOTE_TITLE,
        onDismiss = { pickingQuote = false },
        onCurrencySelected = { c ->
            if (c.name == base) base = quote
            quote = c.name
            pickingQuote = false
        },
    )
}

@Composable
private fun KindCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) LedgerColors.Green.copy(alpha = 0.14f) else LedgerColors.Canvas,
        contentColor = if (selected) LedgerColors.Green else LedgerColors.TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) LedgerColors.Green else LedgerColors.TextPrimary,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = LedgerColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun PairChip(pair: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) LedgerColors.Green.copy(alpha = 0.16f) else LedgerColors.SurfaceElevated,
        contentColor = if (selected) LedgerColors.Green else LedgerColors.TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
    ) {
        Text(
            pair,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun PairField(
    label: String,
    code: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = LedgerColors.TextTertiary,
        )
        Text(
            code,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = LedgerColors.TextPrimary,
        )
    }
}
