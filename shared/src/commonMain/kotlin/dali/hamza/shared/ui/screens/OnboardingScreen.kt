package dali.hamza.shared.ui.screens

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
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LedgerStrings

/**
 * First-launch market selection (market-overview onboarding): the user
 * picks the three currencies the dashboard quotes against the base.
 *
 * Runs once — [onDone] persists the codes and never shows this screen
 * again. Everything chosen here is editable later in
 * Account → Market Preferences.
 */
@Composable
fun OnboardingScreen(
    currencies: List<Currency>,
    base: String?,
    onDone: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // preselected majors (design TOP_PAIRS); the user can swap each slot
    val selections = remember { mutableStateListOf("EUR", "GBP", "MAD") }
    var slot by remember { mutableStateOf<Int?>(null) }

    val nameFor: (String) -> String = { code ->
        currencies.firstOrNull { it.name == code }?.fullCountryName ?: "Currency"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
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
            color = LedgerColors.TextOnColor,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Pick the three currencies your dashboard follows. " +
                "You can change them anytime in Account.",
            style = MaterialTheme.typography.bodyMedium,
            color = LedgerColors.TextSecondary,
        )

        Spacer(Modifier.height(32.dp))

        BentoCard {
            // caps section label — Inter 600/12 secondary (design Heading 3)
            Text(
                text = LedgerStrings.Account.MARKET_SECTION,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = LedgerColors.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                selections.forEachIndexed { index, code ->
                    LedgerListRow(
                        label = "${LedgerStrings.Account.MARKET_SLOT_PREFIX} 0${index + 1} · ${nameFor(code)}",
                        value = code,
                        onClick = { slot = index },
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        LedgerButton(
            text = if (currencies.isEmpty()) "Loading markets…" else "Continue",
            onClick = { onDone(selections.toList()) },
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

    slot?.let { index ->
        val others = selections.filterIndexed { i, _ -> i != index }
        CurrencyPickerSheet(
            visible = true,
            currencies = currencies.filter { it.name != base && it.name !in others },
            title = LedgerStrings.Account.MARKET_PICK_TITLE,
            onDismiss = { slot = null },
            onCurrencySelected = { currency ->
                selections[index] = currency.name
            },
        )
    }
}
