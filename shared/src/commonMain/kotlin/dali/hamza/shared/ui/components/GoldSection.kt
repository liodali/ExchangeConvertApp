package dali.hamza.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dali.hamza.shared.common.DateUtils
import dali.hamza.shared.common.MetalPricing
import dali.hamza.shared.common.nowMillis
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.viewmodel.GoldUiState
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.gold_convert_cta
import echangecurrencyapp.shared.generated.resources.gold_fetched_at
import echangecurrencyapp.shared.generated.resources.gold_karat_value_desc
import echangecurrencyapp.shared.generated.resources.gold_loading
import echangecurrencyapp.shared.generated.resources.gold_note
import echangecurrencyapp.shared.generated.resources.gold_retry
import echangecurrencyapp.shared.generated.resources.gold_section_title
import echangecurrencyapp.shared.generated.resources.gold_spot_label
import echangecurrencyapp.shared.generated.resources.gold_stale_note
import echangecurrencyapp.shared.generated.resources.gold_unavailable
import echangecurrencyapp.shared.generated.resources.gold_usd_fallback
import echangecurrencyapp.shared.generated.resources.gold_unit_desc
import org.jetbrains.compose.resources.stringResource

/**
 * Home Gold section (plans/gold-section.md — Phase 1, XAU only).
 *
 * Shows gold's indicative per-gram value at 24K/22K/18K/14K in the display
 * base, over the shared BentoCard shell:
 *
 * - spot footer is explicit about unit (troy oz) and currency;
 * - as-of is labeled "Fetched at" (the backend's metal `time` is request
 *   time — never label it real-time);
 * - the change chip renders an em dash: the historic waterfall mixes spot
 *   and futures quotes, so no comparable previous close exists yet;
 * - non-quotable display bases fall back to an explicitly labeled USD
 *   quote, and a failed refresh keeps the last valid data with a stale
 *   note instead of clearing the section.
 *
 * Layout mirrors the mobile Gold design on phones and tablets (the card
 * spans the market column); no new navigation tab.
 */
@Composable
fun GoldSection(
    state: GoldUiState,
    onConvertGold: (from: String, to: String, amountOz: String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // mass unit for the displayed prices (1 g / 1 oz / 1 kg) — survives
    // tab switches; stored as name for cross-platform saveable safety
    var unitName by rememberSaveable { mutableStateOf(MetalPricing.GoldUnit.GRAM.name) }
    val unit = MetalPricing.GoldUnit.valueOf(unitName)
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SectionHeader(
                title = stringResource(Res.string.gold_section_title),
                icon = Icons.Outlined.WorkspacePremium,
                iconTint = LedgerColors.Gold,
                modifier = Modifier.weight(1f),
            )
            PurityChip()
        }
        Spacer(Modifier.height(16.dp))
        when {
            state.snapshot != null -> GoldCard(
                state = state,
                unit = unit,
                onUnitChange = { unitName = it.name },
                onConvertGold = onConvertGold,
            )
            state.loading -> {
                val loadingDescription = stringResource(Res.string.gold_loading)
                ShimmerHost {
                    GoldCardSkeleton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = loadingDescription },
                    )
                }
            }
            else -> GoldUnavailableCard(onRetry)
        }
    }
}

/** Design chip `XAU · 24K` — gold glow tile beside the section title. */
@Composable
private fun PurityChip() {
    Text(
        text = "XAU · 24K",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = LedgerColors.Gold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LedgerColors.GoldGlow)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun GoldCard(
    state: GoldUiState,
    unit: MetalPricing.GoldUnit,
    onUnitChange: (MetalPricing.GoldUnit) -> Unit,
    onConvertGold: (from: String, to: String, amountOz: String) -> Unit,
) {
    val snapshot = state.snapshot ?: return
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        fill = LedgerColors.Card,
        padding = PaddingValues(20.dp),
    ) {
        // mass unit toggle — every price below is for 1 of this unit
        val unitDescription = stringResource(Res.string.gold_unit_desc)
        SegmentedRow(
            options = MetalPricing.GoldUnit.entries.map { it.label to it },
            selected = unit,
            onSelect = onUnitChange,
            modifier = Modifier.semantics { contentDescription = unitDescription },
        )
        Spacer(Modifier.height(12.dp))
        // per-karat cells: 24K highlighted, values for one selected unit in
        // the snapshot's (fiat) currency — never rounded before formatting
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetalPricing.GOLD_KARATS.forEach { karat ->
                KaratCell(
                    karat = karat,
                    valuePerUnit = MetalPricing.karatValue(snapshot.spotPerTroyOunce, karat, unit),
                    currency = snapshot.currency,
                    unitLabel = unit.label,
                    highlight = karat == 24,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        // spot footer — unit and currency explicit: "1 g", "1 oz", "1 kg"
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.gold_spot_label, unit.label),
                    style = MaterialTheme.typography.labelMedium,
                    color = LedgerColors.TextTertiary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${formatGoldAmount(MetalPricing.spotPerUnit(snapshot.spotPerTroyOunce, unit))} ${snapshot.currency}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LedgerColors.TextPrimary,
                )
            }
            // change vs previous close: not comparable yet (mixed
            // spot/futures provenance) — em dash, never a fabricated %
            Text(
                text = "—",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = LedgerColors.TextTertiary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Column {
            Text(
                text = stringResource(Res.string.gold_fetched_at, formatFetchedTime(snapshot.fetchedAtMs)),
                style = MaterialTheme.typography.labelSmall,
                color = LedgerColors.TextTertiary,
            )
            if (state.usdFallback) {
                Text(
                    text = stringResource(Res.string.gold_usd_fallback, state.requestedCurrency),
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerColors.Gold,
                )
            }
            if (state.stale) {
                Text(
                    text = stringResource(Res.string.gold_stale_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerColors.TextTertiary,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.gold_note),
            style = MaterialTheme.typography.labelSmall,
            color = LedgerColors.TextTertiary,
        )
        Spacer(Modifier.height(14.dp))
        LedgerButton(
            text = stringResource(Res.string.gold_convert_cta),
            onClick = {
                // converter XAU units are troy ounces — carry 1 selected
                // unit as its exact ounce amount (1 g → 0.032151 oz)
                onConvertGold("XAU", snapshot.currency, formatOzAmount(MetalPricing.toTroyOunces(1.0, unit)))
            },
            modifier = Modifier.fillMaxWidth(),
            variant = LedgerButtonVariant.PREMIUM,
        )
    }
}

/** One karat valuation cell: `24K / 372.93 / TND-g`. */
@Composable
private fun KaratCell(
    karat: Int,
    valuePerUnit: Double,
    currency: String,
    unitLabel: String,
    highlight: Boolean,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        Res.string.gold_karat_value_desc,
        "${karat}K",
        formatGoldAmount(valuePerUnit),
        currency,
        unitLabel,
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlight) LedgerColors.GoldGlow else LedgerColors.NavyDeep)
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .semantics { contentDescription = description },
    ) {
        Text(
            text = "${karat}K",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (highlight) LedgerColors.Gold else LedgerColors.TextMuted,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatGoldAmount(valuePerUnit),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = LedgerColors.TextPrimary,
        )
        Text(
            text = "$currency/$unitLabel",
            style = MaterialTheme.typography.labelSmall,
            color = LedgerColors.TextTertiary,
        )
    }
}

@Composable
private fun GoldUnavailableCard(onRetry: () -> Unit) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        fill = LedgerColors.Card,
        padding = PaddingValues(20.dp),
    ) {
        Text(
            text = stringResource(Res.string.gold_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = LedgerColors.TextSecondary,
        )
        Spacer(Modifier.height(12.dp))
        LedgerButton(
            text = stringResource(Res.string.gold_retry),
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            variant = LedgerButtonVariant.GHOST,
        )
    }
}

/**
 * Skeleton twin of the gold card: same shell, same four-cell grid, same
 * footer/CTA heights — the swap to data is pixel-stable.
 */
@Composable
fun GoldCardSkeleton(modifier: Modifier = Modifier) {
    BentoCard(
        modifier = modifier,
        fill = LedgerColors.Card,
        padding = PaddingValues(20.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // unit toggle silhouette (same 12dp-corner segmented shape)
            repeat(3) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp),
                    cornerRadius = 8.dp,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    cornerRadius = 12.dp,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        ShimmerBox(modifier = Modifier.size(width = 132.dp, height = 16.dp))
        Spacer(Modifier.height(4.dp))
        ShimmerBox(modifier = Modifier.size(width = 160.dp, height = 22.dp))
        Spacer(Modifier.height(10.dp))
        ShimmerBox(modifier = Modifier.size(width = 110.dp, height = 12.dp))
        Spacer(Modifier.height(14.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            cornerRadius = 12.dp,
        )
    }
}

// ------------------------------------------------------------------ helpers

/**
 * Gold amount formatting (KMP-safe, locale-stable): 2 fraction digits with
 * integer grouping — `75.30`, `3,876.40` (design fixtures). Valid spots
 * are always positive/finite; no sign handling needed.
 */
internal fun formatGoldAmount(value: Double): String {
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    val text = rounded.toString()
    val intPart = text.substringBefore('.')
    val fracPart = text.substringAfter('.', "").padEnd(2, '0').take(2)
    val grouped = intPart.reversed().chunked(3).joinToString(",").reversed()
    return "$grouped.$fracPart"
}

/**
 * Converter-handoff amount in troy ounces: 6 significant decimals,
 * trailing zeros trimmed — `1`, `0.032151`, `32.150747`.
 */
internal fun formatOzAmount(value: Double): String {
    val rounded = kotlin.math.round(value * 1e6) / 1e6
    return rounded.toString().trimEnd('0').trimEnd('.')
}

/**
 * As-of label for the spot footer: `15:30` on the fetch day, else
 * `11/06, 15:30` — same convention as the activity time labels (UTC-based
 * civil time, KMP-safe).
 */
internal fun formatFetchedTime(millis: Long): String {
    val full = DateUtils.formatDateTime(millis)
    val time = full.substringAfter(' ')
    val today = DateUtils.epochDayOf(nowMillis())
    val day = DateUtils.epochDayOf(millis)
    return if (today == day) {
        time
    } else {
        full.substring(5, 10).replace('-', '/') + ", " + time
    }
}
