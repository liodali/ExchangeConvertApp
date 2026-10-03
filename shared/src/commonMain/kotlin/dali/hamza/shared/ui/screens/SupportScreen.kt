package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LedgerStrings
import dali.hamza.shared.ui.theme.LedgerStrings.Faq
import dali.hamza.shared.ui.theme.LedgerStrings.Support

/**
 * Support & Information hub (design frame `9vaMD`) — pushed route.
 *
 * Hero + article search, the three help-category cards (FAQ / Contact /
 * Feedback), the Resources & Legal editorial list, and the version footer.
 */
@Composable
fun SupportScreen(
    onBack: () -> Unit,
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
            title = LedgerStrings.APP_TITLE,
            onBack = onBack,
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))

            // ============ Hero =============================================
            Text(
                text = Support.HERO_TITLE,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = LedgerColors.TextPrimary,
            )

            Spacer(Modifier.height(16.dp))

            LedgerSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = Support.SEARCH_HINT,
                cornerRadius = 12,
            )

            Spacer(Modifier.height(32.dp))

            val normalized = query.trim()
            if (normalized.isEmpty()) {
                // ============ Help categories ==============================
                HelpCategoryCard(
                    title = Support.FAQ_TITLE,
                    description = Support.FAQ_DESC,
                    icon = Icons.Outlined.HelpOutline,
                    tint = LedgerColors.Blue,
                    onClick = onOpenFaq,
                )
                Spacer(Modifier.height(16.dp))
                // "Contact Us" entry removed — Feedback (Chatwoot live chat)
                // is the support channel; the contact page + route remain
                // reachable for deep links.
                HelpCategoryCard(
                    title = Support.FEEDBACK_TITLE,
                    description = Support.FEEDBACK_DESC,
                    icon = Icons.Outlined.Reviews,
                    tint = LedgerColors.Gold,
                    onClick = onOpenFeedback,
                )

                Spacer(Modifier.height(32.dp))

                // ============ Resources & legal ============================
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = Support.LEGAL_TITLE,
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
                    LegalRow(Support.legal[0], Icons.Outlined.Gavel, LedgerColors.TextTertiary, onOpenTerms)
                    LegalRow(Support.legal[1], Icons.Outlined.Description, LedgerColors.TextTertiary, onOpenPrivacy)
                    LegalRow(Support.legal[2], Icons.Outlined.HelpOutline, LedgerColors.TextTertiary, onOpenAbout)
                    LegalRow(Support.legal[3], Icons.Outlined.StarOutline, LedgerColors.Gold) {
                        dali.hamza.shared.platform.openUri(dali.hamza.shared.platform.AppLinks.PLAY_URL)
                    }
                    LegalRow(Support.legal[4], Icons.Outlined.Share, LedgerColors.Blue)
                }
            } else {
                // ============ Search results ================================
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

            // ============ Version footer ==================================
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(LedgerColors.Canvas)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "V${dali.hamza.shared.platform.appVersionName()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = LedgerColors.Steel,
                    )
                }
            }
            Text(
                text = Support.COPYRIGHT,
                style = MaterialTheme.typography.bodyMedium,
                color = LedgerColors.TextMutedAlt,
            )

            Spacer(Modifier.height(32.dp))
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
) {
    BentoCard(
        fill = LedgerColors.SurfaceElevated,
        cornerRadius = 16.dp,
        modifier = Modifier.clickable(onClick = onClick),
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
