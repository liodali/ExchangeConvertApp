package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dali.hamza.shared.common.MetalPricing
import dali.hamza.shared.domain.models.MetalQuote
import dali.hamza.shared.domain.models.PreciousMetal
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerButtonVariant
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.components.SectionHeader
import dali.hamza.shared.ui.components.ShimmerBox
import dali.hamza.shared.ui.components.ShimmerHost
import dali.hamza.shared.ui.icons.Icons
import dali.hamza.shared.ui.icons.automirrored.outlined.ArrowForward
import dali.hamza.shared.ui.icons.outlined.WorkspacePremium
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.viewmodel.MetalsUiState
import dali.hamza.shared.ui.viewmodel.MetalsViewModel
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.gold_stale_note
import echangecurrencyapp.shared.generated.resources.gold_usd_fallback
import echangecurrencyapp.shared.generated.resources.metal_name_gold
import echangecurrencyapp.shared.generated.resources.metal_name_palladium
import echangecurrencyapp.shared.generated.resources.metal_name_platinum
import echangecurrencyapp.shared.generated.resources.metal_name_silver
import echangecurrencyapp.shared.generated.resources.metals_assets_badge
import echangecurrencyapp.shared.generated.resources.metals_convert_cta
import echangecurrencyapp.shared.generated.resources.metals_convert_metal_cta
import echangecurrencyapp.shared.generated.resources.metals_stale_note
import echangecurrencyapp.shared.generated.resources.metals_usd_fallback
import echangecurrencyapp.shared.generated.resources.metals_loading
import echangecurrencyapp.shared.generated.resources.metals_per_gram
import echangecurrencyapp.shared.generated.resources.metals_price_unit
import echangecurrencyapp.shared.generated.resources.metals_ratio_30d
import echangecurrencyapp.shared.generated.resources.metals_ratio_desc
import echangecurrencyapp.shared.generated.resources.metals_ratio_title
import echangecurrencyapp.shared.generated.resources.metals_retry
import echangecurrencyapp.shared.generated.resources.metals_section_title
import echangecurrencyapp.shared.generated.resources.metals_selected_desc
import echangecurrencyapp.shared.generated.resources.metals_unavailable
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform

/**
 * Metals v3 overview (plans/metals-v3.md §5 "Navigation and UI", design
 * frame `SEoKM` — pushed route from Home's "Other metals" entry; the Home
 * Gold section stays untouched).
 *
 * Design mapping:
 * - header: icon tile + "Metals" + `%d ASSETS` pill (green glow);
 * - 2×2 grid of 120dp cards: code badge (per-metal accent), change chip,
 *   name, big per-troy-ounce price in the display fiat, per-gram secondary;
 * - change chips render an em dash: metals follow Gold's comparable-close
 *   delta policy (spot/futures provenance is not establishable yet);
 * - Gold/Silver ratio card: live dimensionless ratio + 30-day min/max of
 *   per-DATE ratios (joined series — never divided extrema); missing legs
 *   hide as em dashes;
 * - "Convert metals" CTA prefills the converter with the SELECTED metal
 *   (tap a card to select; Gold by default) → display fiat, 1 troy oz.
 */
@Composable
fun MetalsScreen(
    displayCurrency: String,
    metalsViewModel: MetalsViewModel? = null,
    onConvertMetal: (metalCode: String, fiat: String) -> Unit,
    onBack: () -> Unit,
    initialMetal: String? = null,
) {
    val vm = metalsViewModel ?: remember {
        KoinPlatform.getKoin()?.get<MetalsViewModel>()
            ?: MetalsViewModel(
                KoinPlatform.getKoin()?.get<IRepository>()
                    ?: error("initSharedKoin() must run first")
            )
    }
    val state by vm.state.collectAsState()

    // reload on currency change; same-currency loads coalesce in the VM
    LaunchedEffect(displayCurrency) { vm.load(displayCurrency) }

    // strip handoff: a Home strip row opens this screen with that metal
    // already selected (design: selector chips + detail card)
    LaunchedEffect(initialMetal) {
        initialMetal?.let { code -> PreciousMetal.byCode(code)?.let(vm::select) }
    }
    val selectedMetal = state.selected

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LedgerTopAppBar(
            title = stringResource(Res.string.metals_section_title),
            onBack = onBack,
        )
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(8.dp))
            when {
                state.quotes.isNotEmpty() -> MetalsContent(
                    state = state,
                    selectedMetal = selectedMetal,
                    onSelect = { vm.select(it) },
                    onConvertMetal = onConvertMetal,
                )
                state.loading -> MetalsSkeleton()
                else -> MetalsUnavailable(onRetry = vm::retry)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}


@Composable
private fun MetalsContent(
    state: MetalsUiState,
    selectedMetal: PreciousMetal,
    onSelect: (PreciousMetal) -> Unit,
    onConvertMetal: (metalCode: String, fiat: String) -> Unit,
) {
    MetalSelectorChips(
        selected = selectedMetal,
        quotes = state.quotes,
        onSelect = onSelect,
    )
    Spacer(Modifier.height(12.dp))
    // one card per metal, all visible at once — the page never scrolls
    // (user request: each chip owns its card; duplicates are intentional)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PreciousMetal.entries.chunked(2).forEach { rowMetals ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowMetals.forEach { metal ->
                    MetalCell(
                        metal = metal,
                        quote = state.quotes[metal],
                        stale = metal in state.staleMetals,
                        unavailable = metal in state.unavailableMetals,
                        usdFallback = metal in state.usdFallbackMetals,
                        requestedCurrency = state.requestedCurrency,
                        selected = metal == selectedMetal,
                        onClick = { onSelect(metal) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    RatioCard(state)
    Spacer(Modifier.height(12.dp))
    ConvertMetalsCta(
        selected = selectedMetal,
        onClick = { onConvertMetal(selectedMetal.code, state.requestedCurrency) },
    )
    if (state.staleMetals.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.gold_stale_note),
            style = MaterialTheme.typography.labelSmall,
            color = LedgerColors.TextMuted,
        )
    }
}

/**
 * Design `Metal Selector` — four chips, the active one tinted with its
 * metal accent. A chip is tappable only once its quote exists (the card
 * behind shows live data; selecting a metal with no quote would read as
 * an empty pricing card).
 */
@Composable
private fun MetalSelectorChips(
    selected: PreciousMetal,
    quotes: Map<PreciousMetal, MetalQuote>,
    onSelect: (PreciousMetal) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        PreciousMetal.entries.forEach { metal ->
            val accent = metalAccent(metal)
            val active = metal == selected
            val chipDescription = stringResource(Res.string.metals_selected_desc, metalLabel(metal))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) accent.copy(alpha = 0.12f) else LedgerColors.CardAlt)
                    .then(
                        if (active) {
                            Modifier
                        } else {
                            Modifier.border(1.dp, LedgerColors.GridLine, RoundedCornerShape(8.dp))
                        },
                    )
                    .clickable(enabled = quotes.containsKey(metal)) { onSelect(metal) }
                    .semantics { contentDescription = chipDescription }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = metal.code,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accent,
                )
                Text(
                    text = metalLabel(metal),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (active) LedgerColors.TextPrimary else LedgerColors.TextMuted,
                )
            }
        }
    }
}

/**
 * One per-metal card — design `XAU/XAG/XPT/XPD Cell` (the page shows all
 * four at once, one per chip; duplicates are intentional). Compact by
 * design so the 2x2 grid fits on screen without scrolling.
 */
@Composable
private fun MetalCell(
    metal: PreciousMetal,
    quote: MetalQuote?,
    stale: Boolean,
    unavailable: Boolean,
    usdFallback: Boolean,
    requestedCurrency: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = metalAccent(metal)
    val selectedDescription = stringResource(Res.string.metals_selected_desc, metalLabel(metal))
    BentoCard(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = selectedDescription },
        fill = LedgerColors.Card,
        cornerRadius = 16.dp,
        padding = PaddingValues(14.dp),
        stroke = if (selected) accent else LedgerColors.GridLine,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            MetalCodeBadge(code = metal.code, accent = accent)
            Spacer(Modifier.weight(1f))
            MetalDeltaChip(quote = quote, unavailable = unavailable)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = metalLabel(metal),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (unavailable) LedgerColors.TextMuted else LedgerColors.TextPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = when {
                quote != null -> "${formatMetalNumber(quote.spotPerTroyOunce)} ${quote.currency}"
                unavailable -> "\u2014"
                else -> "\u2026"
            },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = LedgerColors.TextPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = when {
                quote != null -> stringResource(
                    Res.string.metals_per_gram,
                    formatMetalNumber(
                        MetalPricing.spotPerUnit(quote.spotPerTroyOunce, MetalPricing.GoldUnit.GRAM),
                    ) + " " + quote.currency,
                )
                else -> stringResource(Res.string.metals_price_unit)
            },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = LedgerColors.TextMuted,
        )
        if (usdFallback && quote != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.gold_usd_fallback, requestedCurrency),
                style = MaterialTheme.typography.labelSmall,
                color = LedgerColors.TextMutedAlt,
            )
        }
    }
}


@Composable
private fun metalLabel(metal: PreciousMetal): String = when (metal) {
    PreciousMetal.GOLD -> stringResource(Res.string.metal_name_gold)
    PreciousMetal.SILVER -> stringResource(Res.string.metal_name_silver)
    PreciousMetal.PLATINUM -> stringResource(Res.string.metal_name_platinum)
    PreciousMetal.PALLADIUM -> stringResource(Res.string.metal_name_palladium)
}

/**
 * Per-metal accent from the design's badge palette, mapped onto theme
 * roles so light/dark palettes keep working:
 * XAU gold #e9c349, XAG silver #c5c6cd, XPT steel-blue #b9c7e4, XPD gray
 * #92939a. Identification never relies on the badge color alone — the code
 * text and localized name are always present.
 */
@Composable
private fun metalAccent(metal: PreciousMetal) = when (metal) {
    PreciousMetal.GOLD -> LedgerColors.Gold
    PreciousMetal.SILVER -> LedgerColors.TextSecondary
    PreciousMetal.PLATINUM -> LedgerColors.BlueSoft
    PreciousMetal.PALLADIUM -> LedgerColors.TextMuted
}

@Composable
private fun MetalCodeBadge(code: String, accent: androidx.compose.ui.graphics.Color) {
    Text(
        text = code,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = accent,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

/**
 * Metal price formatting: ledger precision (3 fraction digits) with
 * trailing zeros trimmed to one decimal — 1577.000 → "1577.0",
 * 1577.500 → "1577.5", 495.065 → "495.065". Metals trade at scales where
 * forced zero-padding reads as noise (unlike the fiat ledger grid).
 */
internal fun formatMetalNumber(value: Double): String {
    val padded = formatLedgerNumber(value)
    val whole = padded.substringBefore('.')
    var frac = padded.substringAfter('.')
    while (frac.length > 1 && frac.endsWith('0')) frac = frac.dropLast(1)
    return "$whole.$frac"
}

/**
 * Change chip — day-Δ vs the previous session close from
 * [dali.hamza.shared.ui.viewmodel.MetalsViewModel]; em dash when no close
 * exists (weekend gaps, historic leg failed). Gains pick up the ledger
 * green; declines stay neutral (the palette has no red token — direction
 * always rides the sign, never color alone).
 */
@Composable
private fun MetalDeltaChip(quote: MetalQuote?, unavailable: Boolean) {
    val delta = quote?.deltaPercent
    val text = when {
        unavailable || delta == null -> "—"
        else -> {
            val step = kotlin.math.round(delta * 10) / 10
            (if (delta > 0) "+" else "") + step + "%"
        }
    }
    val color = when {
        delta == null -> LedgerColors.TextMuted
        delta > 0 -> LedgerColors.Green
        else -> LedgerColors.TextSecondary
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(LedgerColors.TextMuted.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** Design `Ratio Card` — live ratio + 30-day joined min/max. */
@Composable
private fun RatioCard(state: MetalsUiState) {
    val ratio = state.ratio
    val ratioDescription = ratio.current?.let {
        stringResource(Res.string.metals_ratio_desc, formatMetalNumber(it))
    }
    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { ratioDescription?.let { contentDescription = it } },
        fill = LedgerColors.CardAlt,
        cornerRadius = 12.dp,
        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        stroke = LedgerColors.GridLine,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(Res.string.metals_ratio_title),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = LedgerColors.TextMuted,
                )
                val rangeText = if (ratio.rangeLow != null && ratio.rangeHigh != null) {
                    stringResource(
                        Res.string.metals_ratio_30d,
                        formatMetalNumber(ratio.rangeLow),
                        formatMetalNumber(ratio.rangeHigh),
                    )
                } else {
                    "—"
                }
                Text(
                    text = rangeText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = LedgerColors.TextMuted,
                )
            }
            Text(
                text = ratio.current?.let { formatMetalNumber(it) } ?: "—",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LedgerColors.Gold,
            )
        }
    }
}

/** Design `Button` — outlined gold CTA with an RTL-aware chevron. */
@Composable
private fun ConvertMetalsCta(
    selected: PreciousMetal,
    onClick: () -> Unit,
) {
    val ctaDescription = stringResource(Res.string.metals_convert_metal_cta, metalLabel(selected))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LedgerColors.CardAlt)
            .border(1.dp, LedgerColors.Gold.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = ctaDescription }
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.metals_convert_metal_cta, metalLabel(selected)),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = LedgerColors.Gold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = LedgerColors.Gold,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun MetalsSkeleton() {
    val loadingDescription = stringResource(Res.string.metals_loading)
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics { contentDescription = loadingDescription },
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    ShimmerBox(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        cornerRadius = 16.dp,
                    )
                }
            }
        }
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            cornerRadius = 12.dp,
        )
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            cornerRadius = 12.dp,
        )
    }
}

@Composable
private fun MetalsUnavailable(onRetry: () -> Unit) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        fill = LedgerColors.Card,
        padding = PaddingValues(20.dp),
    ) {
        Text(
            text = stringResource(Res.string.metals_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = LedgerColors.TextSecondary,
        )
        Spacer(Modifier.height(12.dp))
        LedgerButton(
            text = stringResource(Res.string.metals_retry),
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            variant = LedgerButtonVariant.GHOST,
        )
    }
}
