package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.platform.isIos
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.CurrencyPickerSheet
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerInput
import dali.hamza.shared.ui.components.LedgerListRow
import dali.hamza.shared.ui.components.LedgerLogoMark
import dali.hamza.shared.ui.components.LedgerWindowMode
import dali.hamza.shared.ui.components.isExpanded
import dali.hamza.shared.ui.components.ReorderableColumn
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.acct_base_currency
import echangecurrencyapp.shared.generated.resources.acct_base_pick
import echangecurrencyapp.shared.generated.resources.acct_market_pick
import echangecurrencyapp.shared.generated.resources.acct_market_section
import echangecurrencyapp.shared.generated.resources.common_continue
import echangecurrencyapp.shared.generated.resources.onb_base_note
import echangecurrencyapp.shared.generated.resources.onb_change_anytime
import echangecurrencyapp.shared.generated.resources.onb_desc
import echangecurrencyapp.shared.generated.resources.onb_loading
import echangecurrencyapp.shared.generated.resources.onb_market_slot
import echangecurrencyapp.shared.generated.resources.onb_name_note
import echangecurrencyapp.shared.generated.resources.onb_name_title
import echangecurrencyapp.shared.generated.resources.onb_title
import dali.hamza.shared.ui.theme.LedgerColors
import org.jetbrains.compose.resources.stringResource

/**
 * First-launch market selection (market-overview onboarding): the user
 * picks their base currency (everything is quoted against it — defaults
 * to USD until changed) and the three market currencies the dashboard
 * follows. The base never appears among the selectable markets.
 *
 * Runs once — [onDone] persists base + codes and never shows this screen
 * again. Everything chosen here is editable later in
 * Account → Market Preferences.
 */
@Composable
fun OnboardingScreen(
    currencies: List<Currency>,
    base: String?,
    initialName: String = "",
    onDone: (base: String, markets: List<String>, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var baseCode by remember { mutableStateOf(base ?: "USD") }
    var name by remember { mutableStateOf(initialName) }
    // preselected majors (design TOP_PAIRS); the user can swap each slot
    val selections = remember { mutableStateListOf("EUR", "GBP", "MAD") }
    var pickingBase by remember { mutableStateOf(false) }
    var marketSlot by remember { mutableStateOf<Int?>(null) }

    val nameFor: (String) -> String = { code ->
        currencies.firstOrNull { it.name == code }?.fullCountryName ?: "Currency"
    }

    // ---- adaptive layout (plans/tablet-ui.md) -----------------------------
    // phones/medium: single centered column (max 720dp).
    // expanded (iPad landscape, big tablets): branding pane left + form
    // pane right — the tall phone flow becomes a compact two-pane setup.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerColors.Canvas)
            // Onboarding has neither a header nor a Scaffold to reserve these.
            .then(if (isIos()) Modifier else Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)
            )),
    ) {
        LedgerWindowMode { mode ->
            if (mode.isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 64.dp, vertical = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        OnboardingBranding(centered = false)
                        Spacer(Modifier.height(32.dp))
                        OnboardingTitle()
                    }
                    Column(
                        modifier = Modifier
                            .width(460.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        OnboardingNameCard(
                            name = name,
                            onNameChange = { name = it },
                        )
                        OnboardingBaseCard(
                            baseCode = baseCode,
                            nameFor = nameFor,
                            onPick = { pickingBase = true },
                        )
                        OnboardingMarketsCard(
                            selections = selections,
                            nameFor = nameFor,
                            onPickSlot = { marketSlot = it },
                        )
                        LedgerButton(
                            text = if (currencies.isEmpty()) stringResource(Res.string.onb_loading) else stringResource(Res.string.common_continue),
                            onClick = { onDone(baseCode, selections.toList(), name.trim()) },
                            enabled = currencies.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(Res.string.onb_change_anytime),
                            style = MaterialTheme.typography.labelSmall,
                            color = LedgerColors.TextTertiary,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 720.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                ) {
                    Spacer(Modifier.height(64.dp))
                    OnboardingBranding(centered = true)
                    Spacer(Modifier.height(40.dp))
                    OnboardingTitle()
                    Spacer(Modifier.height(32.dp))
                    OnboardingNameCard(
                        name = name,
                        onNameChange = { name = it },
                    )
                    Spacer(Modifier.height(24.dp))
                    OnboardingBaseCard(
                        baseCode = baseCode,
                        nameFor = nameFor,
                        onPick = { pickingBase = true },
                    )
                    Spacer(Modifier.height(24.dp))
                    OnboardingMarketsCard(
                        selections = selections,
                        nameFor = nameFor,
                        onPickSlot = { marketSlot = it },
                    )
                    LedgerButton(
                        text = if (currencies.isEmpty()) stringResource(Res.string.onb_loading) else stringResource(Res.string.common_continue),
                        onClick = { onDone(baseCode, selections.toList(), name.trim()) },
                        enabled = currencies.isNotEmpty(),
                        modifier = Modifier.padding(top = 32.dp).fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(Res.string.onb_change_anytime),
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerColors.TextTertiary,
                        modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }


    if (pickingBase) {
        CurrencyPickerSheet(
            visible = true,
            currencies = currencies,
            title = stringResource(Res.string.acct_base_pick),
            onDismiss = { pickingBase = false },
            onCurrencySelected = { currency ->
                baseCode = currency.name
                // the base can never be one of the markets — swap it out
                val fallback = fallbackMajor(exclude = selections + currency.name)
                selections.indices.forEach { index ->
                    if (selections[index] == currency.name) {
                        selections[index] = fallback
                    }
                }
            },
        )
    }

    marketSlot?.let { index ->
        val others = selections.filterIndexed { i, _ -> i != index }
        CurrencyPickerSheet(
            visible = true,
            currencies = currencies.filter { it.name != baseCode && it.name !in others },
            title = stringResource(Res.string.acct_market_pick),
            onDismiss = { marketSlot = null },
            onCurrencySelected = { currency ->
                selections[index] = currency.name
            },
        )
    }
}

/** First design major that isn't excluded (base-chasing backfill). */
private fun fallbackMajor(exclude: List<String>): String =
    listOf("EUR", "GBP", "MAD", "JPY", "CHF", "CAD").firstOrNull { it !in exclude }
        ?: "EUR"

// ---- onboarding sections (shared by the phone column and tablet panes) --

@Composable
private fun OnboardingBranding(centered: Boolean) {
    Column(
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        LedgerLogoMark()
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = "DINARLY",
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 3.sp,
            color = LedgerColors.Gold,
        )
    }
}

@Composable
private fun OnboardingTitle() {
    Text(
        text = stringResource(Res.string.onb_title),
        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
        color = LedgerColors.TextPrimary,
    )
    Text(
        modifier = Modifier.padding(top = 12.dp),
        text = stringResource(Res.string.onb_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = LedgerColors.TextSecondary,
    )
}

/** Display-name step — the guest identity shown on Account and Home. */
@Composable
private fun OnboardingNameCard(
    name: String,
    onNameChange: (String) -> Unit,
) {
    BentoCard {
        Text(
            text = stringResource(Res.string.onb_name_title),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = LedgerColors.TextSecondary,
        )
        LedgerInput(
            modifier = Modifier.padding(top = 16.dp),
            value = name,
            onValueChange = { onNameChange(it.take(24)) },
            placeholder = stringResource(Res.string.onb_name_note),
        )
    }
}

@Composable
private fun OnboardingBaseCard(
    baseCode: String,
    nameFor: (String) -> String,
    onPick: () -> Unit,
) {
    BentoCard {
        Text(
            text = stringResource(Res.string.acct_base_currency),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = LedgerColors.TextSecondary,
        )
        LedgerListRow(
            modifier = Modifier.padding(top = 16.dp),
            label = nameFor(baseCode),
            value = baseCode,
            onClick = onPick,
        )
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = stringResource(Res.string.onb_base_note),
            style = MaterialTheme.typography.bodySmall,
            color = LedgerColors.TextTertiary,
        )
    }
}

@Composable
private fun OnboardingMarketsCard(
    selections: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    nameFor: (String) -> String,
    onPickSlot: (Int) -> Unit,
) {
    BentoCard {
        Text(
            text = stringResource(Res.string.acct_market_section),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = LedgerColors.TextSecondary,
        )
        ReorderableColumn(
            modifier = Modifier.padding(top = 16.dp),
            items = selections,
            key = { it },
            onMove = { from, to ->
                selections.add(to, selections.removeAt(from))
            },
            spacing = 16.dp,
        ) { code, _ ->
            val index = selections.indexOf(code)
            val slotPrefix = stringResource(Res.string.onb_market_slot)
            LedgerListRow(
                label = "$slotPrefix 0${index + 1} · ${nameFor(code)}",
                value = code,
                onClick = { onPickSlot(index) },
            )
        }
    }
}
