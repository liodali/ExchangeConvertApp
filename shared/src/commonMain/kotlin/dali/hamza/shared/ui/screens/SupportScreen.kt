package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Reviews
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dali.hamza.shared.ui.components.AccordionRow
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.EmptyState
import dali.hamza.shared.ui.components.LedgerListRow
import dali.hamza.shared.ui.components.LedgerSearchField
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.components.ledgerNavClearance
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.content.faqAccountEntries
import dali.hamza.shared.ui.content.faqSecurityEntries
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.faq_no_results
import echangecurrencyapp.shared.generated.resources.faq_no_results_for
import echangecurrencyapp.shared.generated.resources.faq_results_for
import echangecurrencyapp.shared.generated.resources.legal_privacy
import echangecurrencyapp.shared.generated.resources.legal_terms
import echangecurrencyapp.shared.generated.resources.sup_about
import echangecurrencyapp.shared.generated.resources.sup_faq
import echangecurrencyapp.shared.generated.resources.sup_faq_desc
import echangecurrencyapp.shared.generated.resources.sup_feedback
import echangecurrencyapp.shared.generated.resources.sup_feedback_desc
import echangecurrencyapp.shared.generated.resources.sup_hero
import echangecurrencyapp.shared.generated.resources.sup_legal
import echangecurrencyapp.shared.generated.resources.sup_rate
import echangecurrencyapp.shared.generated.resources.sup_results_for
import echangecurrencyapp.shared.generated.resources.sup_search_hint
import echangecurrencyapp.shared.generated.resources.sup_share
import echangecurrencyapp.shared.generated.resources.sup_version
import org.jetbrains.compose.resources.stringResource

/**
 * Support & Information hub (design frame `9vaMD`) — pushed on compact
 * layouts and a sibling destination in the tablet navigation rail.
 *
 * Hero + article search, the three help-category cards (FAQ / Contact /
 * Feedback), the Resources & Legal editorial list, and the version footer.
 */
@Composable
fun SupportScreen(
    onBack: (() -> Unit)?,
    onOpenFaq: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        LedgerTopAppBar(
            title = "DINARLY",
            onBack = onBack,
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))

            // ============ Hero =============================================
            Text(
                text = stringResource(Res.string.sup_hero),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = LedgerColors.TextPrimary,
            )

            Spacer(Modifier.height(16.dp))

            LedgerSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(Res.string.sup_search_hint),
                cornerRadius = 12,
            )

            Spacer(Modifier.height(32.dp))

            val normalized = query.trim()
            if (normalized.isEmpty()) {
                // ============ Help categories ==============================
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth >= 600.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            HelpCategoryCard(
                                title = stringResource(Res.string.sup_faq),
                                description = stringResource(Res.string.sup_faq_desc),
                                icon = Icons.Outlined.HelpOutline,
                                tint = LedgerColors.Blue,
                                onClick = onOpenFaq,
                                modifier = Modifier.weight(1f),
                            )
                            // Feedback (Chatwoot live chat) is the support
                            // channel; Contact remains reachable by deep link.
                            HelpCategoryCard(
                                title = stringResource(Res.string.sup_feedback),
                                description = stringResource(Res.string.sup_feedback_desc),
                                icon = Icons.Outlined.Reviews,
                                tint = LedgerColors.Gold,
                                onClick = onOpenFeedback,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            HelpCategoryCard(
                                title = stringResource(Res.string.sup_faq),
                                description = stringResource(Res.string.sup_faq_desc),
                                icon = Icons.Outlined.HelpOutline,
                                tint = LedgerColors.Blue,
                                onClick = onOpenFaq,
                            )
                            HelpCategoryCard(
                                title = stringResource(Res.string.sup_feedback),
                                description = stringResource(Res.string.sup_feedback_desc),
                                icon = Icons.Outlined.Reviews,
                                tint = LedgerColors.Gold,
                                onClick = onOpenFeedback,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ============ Resources & legal ============================
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.sup_legal),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LedgerColors.TextPrimary,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(LedgerColors.Blue.copy(alpha = 0.30f)),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegalRow(stringResource(Res.string.legal_terms), Icons.Outlined.Gavel, LedgerColors.TextTertiary, onOpenTerms)
                    LegalRow(stringResource(Res.string.legal_privacy), Icons.Outlined.Description, LedgerColors.TextTertiary, onOpenPrivacy)
                    LegalRow(stringResource(Res.string.sup_about), Icons.Outlined.HelpOutline, LedgerColors.TextTertiary, onOpenAbout)
                    LegalRow(stringResource(Res.string.sup_rate), Icons.Outlined.StarOutline, LedgerColors.Gold) {
                        dali.hamza.shared.platform.openUri(dali.hamza.shared.platform.AppLinks.STORE_REVIEW_URL)
                    }
                    LegalRow(stringResource(Res.string.sup_share), Icons.Outlined.Share, LedgerColors.Blue)
                }
            } else {
                // ============ Search results ================================
                val results = (faqSecurityEntries() + faqAccountEntries()).filter {
                    it.question.contains(normalized, ignoreCase = true) ||
                        it.answer.contains(normalized, ignoreCase = true)
                }
                if (results.isEmpty()) {
                    EmptyState(
                        title = stringResource(Res.string.faq_no_results),
                        message = stringResource(Res.string.faq_no_results_for, normalized),
                    )
                } else {
                    BentoCard {
                        Text(
                            text = stringResource(Res.string.sup_results_for, results.size, normalized),
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

            // ============ Version footer ==================================
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(LedgerColors.Canvas)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.sup_version, dali.hamza.shared.platform.appVersionName()),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = LedgerColors.Steel,
                    )
                }
            }
            Text(
                text = "© 2026 Adetify.",
                style = MaterialTheme.typography.bodyMedium,
                color = LedgerColors.TextMutedAlt,
            )

            Spacer(Modifier.height(ledgerNavClearance()))
        }
    }
}

/** Help category card (design: 48dp icon tile, Manrope 700/18, Inter 14). */
@Composable
private fun HelpCategoryCard(
    title: String,
    description: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        fill = LedgerColors.SurfaceElevated,
        cornerRadius = 16.dp,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LedgerColors.TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerColors.TextMutedAlt,
                )
            }
        }
    }
}

/** Legal row — 64dp elevated tile on the Resources & Legal list. */
@Composable
private fun LegalRow(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LedgerColors.SurfaceElevated)
            .clickable(onClick = onClick ?: {})
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = LedgerColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = LedgerColors.Steel,
            modifier = Modifier.size(16.dp),
        )
    }
}
