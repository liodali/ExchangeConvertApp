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
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.alerts_pick_base
import echangecurrencyapp.shared.generated.resources.alerts_pick_quote
import echangecurrencyapp.shared.generated.resources.common_cancel
import echangecurrencyapp.shared.generated.resources.common_from
import echangecurrencyapp.shared.generated.resources.common_to
import echangecurrencyapp.shared.generated.resources.wg_add
import echangecurrencyapp.shared.generated.resources.wg_add_confirm
import echangecurrencyapp.shared.generated.resources.wg_dialog_add_pair
import echangecurrencyapp.shared.generated.resources.wg_dialog_grid_desc
import echangecurrencyapp.shared.generated.resources.wg_dialog_grid_title
import echangecurrencyapp.shared.generated.resources.wg_dialog_max_pairs
import echangecurrencyapp.shared.generated.resources.wg_dialog_pick_desc
import echangecurrencyapp.shared.generated.resources.wg_dialog_pick_title
import echangecurrencyapp.shared.generated.resources.wg_kind_card
import echangecurrencyapp.shared.generated.resources.wg_kind_card_desc
import echangecurrencyapp.shared.generated.resources.wg_kind_convert
import echangecurrencyapp.shared.generated.resources.wg_kind_convert_desc
import echangecurrencyapp.shared.generated.resources.wg_kind_grid
import echangecurrencyapp.shared.generated.resources.wg_kind_grid_desc
import echangecurrencyapp.shared.generated.resources.wg_kind_wave
import echangecurrencyapp.shared.generated.resources.wg_kind_wave_desc
import echangecurrencyapp.shared.generated.resources.wg_new_pair
import echangecurrencyapp.shared.generated.resources.wg_type
import echangecurrencyapp.shared.generated.resources.wg_your_markets
import org.jetbrains.compose.resources.stringResource

/** Localized widget-kind title (was LedgerStrings.Widgets.kindTitle). */
@Composable
private fun kindTitle(kind: WidgetKind): String = when (kind) {
    WidgetKind.MARKET_CARD -> stringResource(Res.string.wg_kind_card)
    WidgetKind.MARKET_WAVE -> stringResource(Res.string.wg_kind_wave)
    WidgetKind.MARKET_GRID -> stringResource(Res.string.wg_kind_grid)
    WidgetKind.QUICK_CONVERT -> stringResource(Res.string.wg_kind_convert)
}

/** Localized widget-kind description (was LedgerStrings.Widgets.kindDescription). */
@Composable
private fun kindDescription(kind: WidgetKind): String = when (kind) {
    WidgetKind.MARKET_CARD -> stringResource(Res.string.wg_kind_card_desc)
    WidgetKind.MARKET_WAVE -> stringResource(Res.string.wg_kind_wave_desc)
    WidgetKind.MARKET_GRID -> stringResource(Res.string.wg_kind_grid_desc)
    WidgetKind.QUICK_CONVERT -> stringResource(Res.string.wg_kind_convert_desc)
}

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
                    stringResource(Res.string.wg_add),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LedgerColors.TextPrimary,
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    stringResource(Res.string.wg_type),
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerColors.TextTertiary,
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetKind.entries.forEach { option ->
                        KindCard(
                            title = kindTitle(option),
                            description = kindDescription(option),
                            selected = kind == option,
                            onClick = { kind = option },
                        )
                    }
                }

                if (needsPair) {
                    if (suggestedPairs.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(Res.string.wg_your_markets),
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
                        stringResource(Res.string.wg_new_pair),
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerColors.TextTertiary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PairField(
                            label = stringResource(Res.string.common_from),
                            code = base,
                            modifier = Modifier.weight(1f),
                            onClick = { pickingBase = true },
                        )
                        PairField(
                            label = stringResource(Res.string.common_to),
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
                            title = stringResource(Res.string.wg_dialog_grid_title),
                            description = stringResource(Res.string.wg_dialog_grid_desc),
                            selected = gridFollowDefault,
                            onClick = { gridFollowDefault = true },
                        )
                        KindCard(
                            title = stringResource(Res.string.wg_dialog_pick_title),
                            description = stringResource(Res.string.wg_dialog_pick_desc),
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
                                    label = stringResource(Res.string.common_from),
                                    code = base,
                                    modifier = Modifier.weight(1f),
                                    onClick = { pickingBase = true },
                                )
                                PairField(
                                    label = stringResource(Res.string.common_to),
                                    code = quote,
                                    modifier = Modifier.weight(1f),
                                    onClick = { pickingQuote = true },
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LedgerButton(
                                text = stringResource(Res.string.wg_dialog_add_pair, gridPairs.size),
                                onClick = {
                                    if (base != quote) gridPairs = gridPairs + (base to quote)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = base != quote,
                            )
                        } else {
                            Text(
                                stringResource(Res.string.wg_dialog_max_pairs),
                                style = MaterialTheme.typography.bodySmall,
                                color = LedgerColors.TextTertiary,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                LedgerButton(
                    text = stringResource(Res.string.wg_add_confirm),
                    onClick = { onAdd(kind, base, quote, if (gridCustom) gridPairs else emptyList()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = gridValid && (!needsPair || base != quote),
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.common_cancel), color = LedgerColors.TextTertiary) }
            }
        }
    }

    CurrencyPickerSheet(
        visible = pickingBase,
        currencies = currencies,
        title = stringResource(Res.string.alerts_pick_base),
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
        title = stringResource(Res.string.alerts_pick_quote),
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
