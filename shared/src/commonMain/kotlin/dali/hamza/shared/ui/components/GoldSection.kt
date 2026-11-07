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
    onConvertGold: (from: String, to: String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            state.snapshot != null -> GoldCard(state, onConvertGold)
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
    onConvertGold: (from: String, to: String) -> Unit,
) {
    val snapshot = state.snapshot ?: return
    // the Home card is per GRAM (jewellery convention, the design's
    // per-gram pricing) — the g/oz/kg selector lives in the converter's
    // amount suffix, not here
    val unit = MetalPricing.GoldUnit.GRAM
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        fill = LedgerColors.Card,
        padding = PaddingValues(20.dp),
    ) {
        // adaptive karat grid: short per-gram values fit 4 cells across,
        // but high-price currencies (1,226.97 MAD/g …) wrap at that width —
        // when the longest formatted price exceeds 7 chars, drop to 2 per
        // row so every price stays on ONE line
        val karatCells = MetalPricing.GOLD_KARATS.map { karat ->
            karat to MetalPricing.karatValue(snapshot.spotPerTroyOunce, karat, unit)
        }
        val perRow = if (karatCells.maxOf { formatGoldAmount(it.second).length } > 7) 2 else 4
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            karatCells.chunked(perRow).forEach { rowCells ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    rowCells.forEach { (karat, valuePerUnit) ->
                        KaratCell(
                            karat = karat,
                            valuePerUnit = valuePerUnit,
                            currency = snapshot.currency,
                            unitLabel = unit.label,
                            highlight = karat == 24,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        // spot footer — ONE line: "Gold spot · 1 g  372.93 TND"
        // (unit and currency stay explicit)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.gold_spot_label, unit.label),
                style = MaterialTheme.typography.labelMedium,
                color = LedgerColors.TextTertiary,
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${formatGoldAmount(MetalPricing.spotPerUnit(snapshot.spotPerTroyOunce, unit))} ${snapshot.currency}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = LedgerColors.TextPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )
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
                // 1 gram of gold — the converter's unit suffix defaults to
                // grams for metals, so the prefilled "1" matches this card
                onConvertGold("XAU", snapshot.currency)
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
            maxLines = 1,
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
        // one-line spot footer silhouette
        ShimmerBox(modifier = Modifier.size(width = 210.dp, height = 18.dp))
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
