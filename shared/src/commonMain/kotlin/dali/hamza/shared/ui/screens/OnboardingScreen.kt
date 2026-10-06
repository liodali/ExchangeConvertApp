package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.CurrencyPickerSheet
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerListRow
import dali.hamza.shared.ui.components.LedgerLogoMark
import dali.hamza.shared.ui.components.ReorderableColumn
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LedgerStrings

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
    onDone: (base: String, markets: List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var baseCode by remember { mutableStateOf(base ?: "USD") }
    // preselected majors (design TOP_PAIRS); the user can swap each slot
    val selections = remember { mutableStateListOf("EUR", "GBP", "MAD") }
    var pickingBase by remember { mutableStateOf(false) }
    var marketSlot by remember { mutableStateOf<Int?>(null) }

    val nameFor: (String) -> String = { code ->
        currencies.firstOrNull { it.name == code }?.fullCountryName ?: "Currency"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerColors.Canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(64.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            LedgerLogoMark()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "SOVEREIGN LEDGER",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 3.sp,
                color = LedgerColors.Gold,
            )
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = "Choose your markets",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = LedgerColors.TextPrimary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Set the currency everything is quoted against, then pick " +
                "the three markets your dashboard follows. " +
                "You can change them anytime in Account.",
            style = MaterialTheme.typography.bodyMedium,
            color = LedgerColors.TextSecondary,
        )

        Spacer(Modifier.height(32.dp))

        // ---- base currency -----------------------------------------------
        BentoCard {
            Text(
                text = LedgerStrings.Account.BASE_CURRENCY,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = LedgerColors.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            LedgerListRow(
                label = nameFor(baseCode),
                value = baseCode,
                onClick = { pickingBase = true },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "All rates on your dashboard are compared to this currency.",
                style = MaterialTheme.typography.bodySmall,
                color = LedgerColors.TextTertiary,
            )
        }

        Spacer(Modifier.height(24.dp))

        // ---- market slots --------------------------------------------------
        BentoCard {
            Text(
                text = LedgerStrings.Account.MARKET_SECTION,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = LedgerColors.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            // long-press drag to reorder the slots — the "Market 01/02/03"
            // labels renumber live, and the order carries into the
            // dashboard and Account after Continue
            ReorderableColumn(
                items = selections,
                key = { it },
                onMove = { from, to ->
                    selections.add(to, selections.removeAt(from))
                },
                spacing = 16.dp,
            ) { code, _ ->
                val index = selections.indexOf(code)
                LedgerListRow(
                    label = "${LedgerStrings.Account.MARKET_SLOT_PREFIX} 0${index + 1} · ${nameFor(code)}",
                    value = code,
                    onClick = { marketSlot = index },
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        LedgerButton(
            text = if (currencies.isEmpty()) "Loading markets…" else "Continue",
            onClick = { onDone(baseCode, selections.toList()) },
            enabled = currencies.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Change anytime in Account → Market Preferences.",
            style = MaterialTheme.typography.labelSmall,
            color = LedgerColors.TextTertiary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(48.dp))
    }

    if (pickingBase) {
        CurrencyPickerSheet(
            visible = true,
            currencies = currencies,
            title = LedgerStrings.Account.BASE_PICK_TITLE,
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
            title = LedgerStrings.Account.MARKET_PICK_TITLE,
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
