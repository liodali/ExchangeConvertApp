package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.shared.common.DateUtils
import dali.hamza.shared.domain.models.Transaction
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.LedgerChip
import dali.hamza.shared.ui.components.LedgerLineChart
import dali.hamza.shared.ui.components.LedgerLogoMark
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.viewmodel.AssetBalance
import dali.hamza.shared.ui.viewmodel.HistoryRange
import dali.hamza.shared.ui.viewmodel.HistoryViewModel
import dali.hamza.shared.ui.viewmodel.SharedViewModel

/**
 * Asset Performance & Records (design frame `72Xe2`) — History tab.
 *
 * Balance wallets (computed from the recorded ledger), premium benefit card,
 * the historical graph with range chips, the all-time-best comparison, and
 * the transaction list for the active pair.
 *
 * Collects the pair from [sharedViewModel] itself (same pattern as Home) —
 * destination-content params fed from the NavHost don't reliably propagate
 * state changes in the JetBrains navigation fork.
 */
@Composable
fun HistoryScreen(
    sharedViewModel: SharedViewModel,
    historyViewModel: HistoryViewModel,
) {
    val state by historyViewModel.state.collectAsState()
    val sharedState by sharedViewModel.state.collectAsState()
    val base = sharedState.fromCurrency?.name
    val quote = sharedState.toCurrency?.name
    val username = sharedState.username

    LaunchedEffect(base, quote) {
        historyViewModel.load(base, quote)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        LedgerTopAppBar(
            title = "Sovereign Ledger",
            leading = { LedgerLogoMark() },
            avatarInitials = username,
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))

            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.Error,
                )
                Spacer(Modifier.height(12.dp))
            }

            // ============ Premium benefit =================================
            BentoCard(fill = LedgerColors.Canvas, cornerRadius = 24.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = LedgerColors.Gold,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "PREMIUM BENEFIT",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LedgerColors.Gold,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "As a Sovereign tier member, you enjoy 0% exchange fees on all major currency pairs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerColors.TextSecondary,
                )
            }

            Spacer(Modifier.height(24.dp))

            // ============ Historical graph ================================
            BentoCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${state.base ?: "—"} TO ${state.quote ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LedgerColors.TextSecondary,
                    )
                    state.changePercent?.let { percent ->
                        LedgerChip(
                            text = signed(percent, 2, "%"),
                            tint = if (percent >= 0) LedgerColors.Green else LedgerColors.Error,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = state.currentRate?.let { formatRate(it) } ?: "—",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = LedgerColors.TextPrimary,
                    )
                    state.delta?.let { delta ->
                        Text(
                            text = signed(delta, 6, ""),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (delta >= 0) LedgerColors.Green else LedgerColors.Error,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Live market rate" + (state.lastUpdatedLabel?.let { " • $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerColors.TextSecondary,
                )
                Spacer(Modifier.height(16.dp))

                // ---- range chips -----------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LedgerColors.Canvas)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    HistoryRange.entries.forEach { range ->
                        val selected = state.range == range
                        Text(
                            text = range.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            // accent pairing on both modes (white text was
                            // unreadable on the light chip)
                            color = if (selected) LedgerColors.Green else LedgerColors.TextSecondary,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) LedgerColors.GreenSoft else Color.Transparent)
                                .clickable { historyViewModel.selectRange(range) }
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                LedgerLineChart(
                    points = state.series.map { it.rate },
                    labels = state.series.map { it.date },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    state.series.firstOrNull()?.let {
                        MonoDate(it.date)
                    }
                    state.series.getOrNull(state.series.size / 2)?.let {
                        MonoDate(it.date)
                    }
                    state.series.lastOrNull()?.let {
                        MonoDate(it.date)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ All-time best rate ==============================
            BentoCard(fill = LedgerColors.SurfaceRaised, cornerRadius = 32.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = LedgerColors.Gold,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "ALL-TIME BEST RATE",
                        style = MaterialTheme.typography.labelMedium,
                        color = LedgerColors.TextSecondary,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = state.bestRate?.let { formatRate(it) } ?: "—",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = LedgerColors.TextPrimary,
                    )
                    Text(
                        text = "${state.base ?: ""}/${state.quote ?: ""}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = LedgerColors.Gold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.bestTimestamp?.let {
                        "Achieved on ${DateUtils.formatDateTime(it)}"
                    } ?: "From the recorded market history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerColors.TextSecondary,
                )

                Spacer(Modifier.height(24.dp))

                // ---- comparison ------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LedgerColors.SurfaceElevated)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "COMPARISON",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerColors.TextSecondary,
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Current Rate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LedgerColors.TextSecondary,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = state.currentRate?.let { formatRate(it) } ?: "—",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = LedgerColors.TextPrimary,
                        )
                    }
                    val current = state.currentRate
                    val best = state.bestRate
                    val fraction = if (current != null && best != null && best > 0.0) {
                        (current / best).coerceIn(0.0, 1.0).toFloat()
                    } else {
                        0f
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(LedgerColors.CardHigh),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(8.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(LedgerColors.Green),
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val fromPeak = if (current != null && best != null && best > 0.0) {
                            signed((current - best) / best * 100.0, 2, "%") + " from Peak"
                        } else {
                            "—"
                        }
                        Text(
                            text = fromPeak,
                            style = MaterialTheme.typography.labelSmall,
                            color = LedgerColors.TextSecondary,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "Historical High",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = LedgerColors.Green,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ---- insight ---------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LedgerColors.Green.copy(alpha = 0.06f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QueryStats,
                        contentDescription = null,
                        tint = LedgerColors.Green,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = state.topPercentile?.let { top ->
                            "The current rate is performing within the top $top% of the selected range."
                        } ?: "Execute an exchange to start building your ledger history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerColors.TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ History for this pair ===========================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "History for this Pair",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = LedgerColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "VIEW ALL",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LedgerColors.Blue,
                )
            }
            Spacer(Modifier.height(16.dp))
            BentoCard(fill = LedgerColors.SurfaceElevated, cornerRadius = 24.dp, padding = PaddingValues(0.dp)) {
                if (state.transactions.isEmpty()) {
                    EmptyLedger()
                } else {
                    state.transactions.take(10).forEachIndexed { index, tx ->
                        TransactionRow(tx)
                        if (index != minOf(9, state.transactions.size - 1)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(1.dp)
                                    .background(LedgerColors.Steel.copy(alpha = 0.30f)),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// sections
// ---------------------------------------------------------------------------

@Composable
private fun TransactionRow(tx: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(LedgerColors.Green.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CurrencyExchange,
                contentDescription = null,
                tint = LedgerColors.Green,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Exchanged ${tx.base} to ${tx.quote}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = LedgerColors.TextPrimary,
            )
            Text(
                text = DateUtils.formatDateTime(tx.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = LedgerColors.TextSecondary,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "-${formatAmount(tx.amountBase)} ${tx.base}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = LedgerColors.TextPrimary,
            )
            Text(
                text = "+${formatAmount(tx.amountQuote)} ${tx.quote}",
                style = MaterialTheme.typography.bodySmall,
                color = LedgerColors.Green,
            )
        }
    }
}

@Composable
private fun EmptyLedger() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "No records for this pair yet",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = LedgerColors.TextPrimary,
        )
        Text(
            text = "Exchanges you execute appear here instantly.",
            style = MaterialTheme.typography.bodySmall,
            color = LedgerColors.TextSecondary,
        )
    }
}

@Composable
private fun MonoDate(date: String) {
    Text(
        text = date,
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
        color = LedgerColors.TextSecondary.copy(alpha = 0.50f),
    )
}

// ---------------------------------------------------------------------------
// formatting helpers
// ---------------------------------------------------------------------------

private fun fixed(value: Double, precision: Int): String {
    val factor = pow10(precision)
    val rounded = kotlin.math.round(value * factor) / factor
    var text = rounded.toString()
    if (!text.contains('.')) text += ".0"
    while (text.substringAfter('.').length < precision) text += "0"
    return text
}

private fun signed(value: Double, precision: Int, suffix: String): String {
    val sign = if (value >= 0) "+" else "-"
    return "$sign${fixed(kotlin.math.abs(value), precision)}$suffix"
}

private fun pow10(exponent: Int): Double {
    var result = 1.0
    repeat(exponent) { result *= 10.0 }
    return result
}

/** Rate display: 4 decimals ≥ 1, 6 below (small cross rates / crypto pairs). */
internal fun formatRate(value: Double): String =
    fixed(kotlin.math.abs(value), if (kotlin.math.abs(value) >= 1.0) 4 else 6)

internal fun formatAmount(value: Double): String = formatLedgerNumber(value)

/** Fixed-decimal rate display (design pair cards: "1.0842" — 4 digits). */
internal fun formatRateDigits(value: Double, precision: Int): String {
    var factor = 1.0
    repeat(precision) { factor *= 10.0 }
    val rounded = kotlin.math.round(value * factor) / factor
    var text = rounded.toString()
    if (!text.contains('.')) text += ".0"
    while (text.substringAfter('.').length < precision) text += "0"
    return text
}
