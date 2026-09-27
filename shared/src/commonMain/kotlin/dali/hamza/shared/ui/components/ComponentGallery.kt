package dali.hamza.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dali.hamza.shared.ui.theme.ExchangeCurrencyAppTheme
import dali.hamza.shared.ui.theme.LedgerColors

/**
 * Dev-only gallery rendering every Phase 1 Ledger component with sample data.
 * Not wired into production navigation — mount it from a preview/debug entry
 * point (or temporarily inside `ExchangeCurrencyApp`) to eyeball the design
 * system during the redesign phases.
 */
@Composable
fun ComponentGallery(modifier: Modifier = Modifier) {
    ExchangeCurrencyAppTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "Ledger Design System",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            GallerySection("SectionHeader") {
                SectionHeader(
                    title = "Market Overview",
                    icon = Icons.Outlined.TrendingUp,
                    trailing = {
                        GalleryChip("See all")
                    },
                )
            }

            GallerySection("BentoCard") {
                BentoCard {
                    Text(
                        text = "EUR / USD",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "1.0842",
                        style = MaterialTheme.typography.displaySmall,
                        color = LedgerColors.Green,
                    )
                    Sparkline(
                        values = listOf(1.081, 1.0823, 1.0805, 1.0831, 1.0842, 1.0829, 1.0842),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(top = 12.dp),
                    )
                }
            }

            GallerySection("GlassCard") {
                GlassCard {
                    Text(
                        text = "Your Balance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "$42,850.00",
                        style = MaterialTheme.typography.displayMedium,
                        color = LedgerColors.Blue,
                    )
                }
            }

            GallerySection("Buttons") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LedgerButton(
                        text = "Convert",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Outlined.AccountBalance,
                    )
                    LedgerButton(
                        text = "Upgrade Tier",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        variant = LedgerButtonVariant.PREMIUM,
                    )
                    LedgerButton(
                        text = "Contact Support",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        variant = LedgerButtonVariant.TONAL,
                    )
                }
            }

            GallerySection("LedgerInput") {
                var amount by remember { mutableStateOf("1000") }
                LedgerInput(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount",
                    placeholder = "0.00",
                )
            }

            GallerySection("AccordionRow") {
                var expanded by remember { mutableStateOf(true) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccordionRow(
                        question = "How are my keys protected?",
                        answer = "Your ledger is protected by distributed multi-party computation. " +
                            "Your key is never constructed in whole.",
                        expanded = expanded,
                        onToggle = { expanded = !expanded },
                    )
                    AccordionRow(
                        question = "How do I add a co-signatory?",
                        answer = "Initiate multi-sig setup via the Security Center.",
                        expanded = false,
                        onToggle = {},
                    )
                }
            }

            GallerySection("EmptyState") {
                EmptyState(
                    title = "No transactions yet",
                    message = "Your conversions will appear here.",
                    icon = Icons.Outlined.Help,
                    actionLabel = "Convert Now",
                    onAction = {},
                )
            }

            GallerySection("TopAppBar") {
                LedgerTopAppBar(
                    title = "Concierge Support",
                    onBack = {},
                )
            }

            GallerySection("BottomNav") {
                var destination by remember { mutableStateOf(LedgerDestination.HOME) }
                LedgerBottomNav(
                    currentDestination = destination,
                    onSelectDestination = { destination = it },
                )
            }

            GallerySection("Icon samples") {
                SectionHeader("Settings", icon = Icons.Outlined.Settings)
            }
        }
    }
}

@Composable
private fun GallerySection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = LedgerColors.TextMuted,
        )
        content()
    }
}

@Composable
private fun GalleryChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = LedgerColors.Blue,
    )
}
