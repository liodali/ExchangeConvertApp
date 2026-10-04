package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dali.hamza.shared.ui.components.AccordionRow
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.EmptyState
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerButtonVariant
import dali.hamza.shared.ui.components.LedgerListRow
import dali.hamza.shared.ui.components.LedgerSearchField
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LedgerStrings
import dali.hamza.shared.ui.theme.LedgerStrings.Faq

/**
 * Frequently Asked Questions (design frame `MegbU`) — pushed route.
 *
 * Hero + knowledge-base search, then the bento category grid: Security
 * accordion (first item open), Trading & Markets links, Account Management
 * compact QA cards, and the "Still require assistance?" concierge CTA.
 */
@Composable
fun FaqScreen(
    onBack: () -> Unit,
    onOpenContact: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var expandedSecurity by remember { mutableIntStateOf(0) }
    var expandedAccount by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        LedgerTopAppBar(
            title = LedgerStrings.APP_TITLE,
            onBack = onBack,
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))

            // ============ Hero =============================================
            Text(
                text = Faq.HERO_TITLE,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = LedgerColors.TextPrimary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = Faq.HERO_DESC,
                style = MaterialTheme.typography.bodyLarge,
                color = LedgerColors.TextSecondary,
            )

            Spacer(Modifier.height(24.dp))

            // ============ Search ===========================================
            LedgerSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = Faq.SEARCH_HINT,
            )

            Spacer(Modifier.height(32.dp))

            val normalized = query.trim()
            if (normalized.isEmpty()) {
                // ============ Security (accordion) =========================
                BentoCard {
                    CategoryHeader(
                        title = Faq.SECURITY_TITLE,
                        icon = Icons.Outlined.Security,
                        iconTint = LedgerColors.Green,
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Faq.security.forEachIndexed { index, entry ->
                            AccordionRow(
                                question = entry.question,
                                answer = entry.answer,
                                expanded = expandedSecurity == index,
                                onToggle = {
                                    expandedSecurity = if (expandedSecurity == index) -1 else index
                                },
                                fill = if (expandedSecurity == index) {
                                    LedgerColors.SurfaceRaised
                                } else {
                                    LedgerColors.Surface
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ============ Trading & Markets (links) ====================
                BentoCard(fill = LedgerColors.Canvas, cornerRadius = 16.dp) {
                    CategoryHeader(
                        title = Faq.TRADING_TITLE,
                        icon = Icons.Outlined.QueryStats,
                        iconTint = LedgerColors.Blue,
                        iconTileRadius = 12.dp,
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Faq.trading.forEach { link ->
                            LedgerListRow(
                                label = link,
                                onClick = {},
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ============ Account Management (QA cards) ================
                BentoCard(cornerRadius = 16.dp) {
                    CategoryHeader(
                        title = Faq.ACCOUNT_TITLE,
                        icon = Icons.Outlined.Settings,
                        iconTint = LedgerColors.Blue,
                        iconTileRadius = 8.dp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = Faq.ACCOUNT_DESC,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LedgerColors.TextSecondary,
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Faq.account.forEachIndexed { index, entry ->
                            CompactQaCard(
                                title = entry.question,
                                body = entry.answer,
                                expanded = expandedAccount == index,
                                onClick = {
                                    expandedAccount = if (expandedAccount == index) -1 else index
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            } else {
                // ============ Search results =================================
                val results = (Faq.security + Faq.account).filter {
                    it.question.contains(normalized, ignoreCase = true) ||
                        it.answer.contains(normalized, ignoreCase = true)
                }
                if (results.isEmpty()) {
                    EmptyState(
                        title = Faq.NO_RESULTS_TITLE,
                        message = Faq.noResults(normalized),
                    )
                } else {
                    BentoCard {
                        Text(
                            text = "${results.size} result(s) for \"$normalized\"",
                            style = MaterialTheme.typography.labelMedium,
                            color = LedgerColors.TextSecondary,
                        )
                        Spacer(Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            results.forEach { entry ->
                                AccordionRow(
                                    question = entry.question,
                                    answer = entry.answer,
                                    expanded = true,
                                    onToggle = {},
                                    fill = LedgerColors.SurfaceRaised,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Category header — 48dp icon tile + Manrope 700 title (design recipe). */
@Composable
private fun CategoryHeader(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    iconTileRadius: Dp = 16.dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(iconTileRadius))
                .background(iconTint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = LedgerColors.TextPrimary,
        )
    }
}

/** Compact QA card (design `Compact QA Cards`) — tap to expand full answer. */
@Composable
private fun CompactQaCard(
    title: String,
    body: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LedgerColors.Surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(20.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = LedgerColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = LedgerColors.TextSecondary,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
        )
    }
}
