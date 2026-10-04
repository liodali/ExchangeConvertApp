package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.EmptyState
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerButtonVariant
import dali.hamza.shared.ui.components.LedgerTextArea
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LedgerStrings
import dali.hamza.shared.ui.theme.LedgerStrings.Feedback

/**
 * User Feedback (design frame `gbbrU`) — pushed route.
 *
 * Editorial intro ("Refine the Instrument.") above the form card: star
 * rating, single-select category tags, optional suggestions, PREMIUM submit.
 * Submission is LOCAL SUCCESS STATE ONLY (plan open item §6.3).
 */
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
) {
    var rating by remember { mutableIntStateOf(0) }
    var category by remember { mutableStateOf<String?>(Feedback.categories.last()) }
    var suggestions by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

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

            // ============ Editorial intro ==================================
            Text(
                text = Feedback.HERO_TITLE,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = LedgerColors.TextPrimary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = Feedback.HERO_DESC,
                style = MaterialTheme.typography.bodyLarge,
                color = LedgerColors.TextSecondary,
            )

            Spacer(Modifier.height(32.dp))

            // ============ Form =============================================
            BentoCard {
                if (submitted) {
                    EmptyState(
                        title = Feedback.SUCCESS_TITLE,
                        message = Feedback.SUCCESS_DESC,
                        icon = Icons.Outlined.ThumbUp,
                        iconTint = LedgerColors.Gold,
                    )
                } else {
                    // ---- overall experience (gold stars) -------------------
                    Text(
                        text = Feedback.RATING_LABEL,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LedgerColors.TextPrimary,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(5) { index ->
                            val star = index + 1
                            IconButton(onClick = { rating = star }) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = "$star star${if (star > 1) "s" else ""}",
                                    tint = if (star <= rating) {
                                        LedgerColors.Gold
                                    } else {
                                        LedgerColors.CardAlt
                                    },
                                    modifier = Modifier.size(30.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(40.dp))

                    // ---- primary category (segmented tags) -----------------
                    Text(
                        text = Feedback.CATEGORY_LABEL,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LedgerColors.TextPrimary,
                    )
                    Spacer(Modifier.height(16.dp))
                    CategoryTags(
                        selected = category,
                        onSelect = { category = it },
                    )

                    Spacer(Modifier.height(40.dp))

                    // ---- detailed suggestions (optional) -------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Feedback.SUGGESTIONS_LABEL,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = LedgerColors.TextPrimary,
                        )
                        Text(
                            text = Feedback.OPTIONAL,
                            style = MaterialTheme.typography.labelSmall,
                            color = LedgerColors.TextTertiary.copy(alpha = 0.60f),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    LedgerTextArea(
                        value = suggestions,
                        onValueChange = { suggestions = it },
                        placeholder = Feedback.SUGGESTIONS_PLACEHOLDER,
                    )

                    Spacer(Modifier.height(40.dp))

                    LedgerButton(
                        text = Feedback.SUBMIT,
                        onClick = { submitted = true },
                        variant = LedgerButtonVariant.PREMIUM,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = rating > 0 && category != null,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Category tags — single-select chips (selected: `#393939` + blue label). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryTags(
    selected: String?,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Feedback.categories.forEach { tag ->
            val isSelected = tag == selected
            Text(
                text = tag,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isSelected) LedgerColors.Blue else LedgerColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) LedgerColors.CardHigh else LedgerColors.SurfaceRaised)
                    .clickable { onSelect(tag) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}
