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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.sp
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.EmptyState
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerChip
import dali.hamza.shared.ui.components.LedgerTextArea
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.theme.LedgerColors
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.ct_description_label
import echangecurrencyapp.shared.generated.resources.ct_description_placeholder
import echangecurrencyapp.shared.generated.resources.ct_dispatch
import echangecurrencyapp.shared.generated.resources.ct_dispatch_desc
import echangecurrencyapp.shared.generated.resources.ct_dispatch_value
import echangecurrencyapp.shared.generated.resources.ct_form_title
import echangecurrencyapp.shared.generated.resources.ct_hero
import echangecurrencyapp.shared.generated.resources.ct_hero_desc
import echangecurrencyapp.shared.generated.resources.ct_initiate
import echangecurrencyapp.shared.generated.resources.ct_live_chat
import echangecurrencyapp.shared.generated.resources.ct_live_chat_desc
import echangecurrencyapp.shared.generated.resources.ct_phone
import echangecurrencyapp.shared.generated.resources.ct_phone_desc
import echangecurrencyapp.shared.generated.resources.ct_premium
import echangecurrencyapp.shared.generated.resources.ct_subject_label
import echangecurrencyapp.shared.generated.resources.ct_subject_placeholder
import echangecurrencyapp.shared.generated.resources.ct_submit
import echangecurrencyapp.shared.generated.resources.ct_success_desc
import echangecurrencyapp.shared.generated.resources.ct_success_title
import echangecurrencyapp.shared.generated.resources.ct_topic_account
import echangecurrencyapp.shared.generated.resources.ct_topic_feature
import echangecurrencyapp.shared.generated.resources.ct_topic_rates
import echangecurrencyapp.shared.generated.resources.ct_topic_security
import org.jetbrains.compose.resources.stringResource

/** Localized contact topics (index-stable across locales). */
@Composable
private fun contactTopics(): List<String> = listOf(
    stringResource(Res.string.ct_topic_rates),
    stringResource(Res.string.ct_topic_account),
    stringResource(Res.string.ct_topic_security),
    stringResource(Res.string.ct_topic_feature),
)

/**
 * Contact Support (design frame `0ZYbh`) — pushed route.
 *
 * Concierge contact options (live chat / phone / secure dispatch) above the
 * Secure Message Form. Submission is LOCAL SUCCESS STATE ONLY — no backend
 * endpoint exists yet (plan open item §6.3).
 */
@Composable
fun ContactSupportScreen(
    onBack: () -> Unit,
) {
    // index into contactTopics() — display strings change per locale
    var topicIndex by remember { mutableStateOf<Int?>(null) }
    var description by remember { mutableStateOf("") }
    var dispatched by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val topics = contactTopics()

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
                text = stringResource(Res.string.ct_hero),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = LedgerColors.TextPrimary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.ct_hero_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = LedgerColors.TextSecondary,
            )

            Spacer(Modifier.height(32.dp))

            // ============ Contact options ==================================
            ContactOptionCard(
                title = stringResource(Res.string.ct_live_chat),
                description = stringResource(Res.string.ct_live_chat_desc),
                icon = Icons.AutoMirrored.Outlined.Chat,
                tint = LedgerColors.Gold,
                badge = {
                    LedgerChip(
                        text = stringResource(Res.string.ct_premium),
                        tint = LedgerColors.Gold,
                        cornerRadius = 4.dp,
                        fontSize = 9.sp,
                    )
                },
                footer = {
                    FooterLink(text = stringResource(Res.string.ct_initiate), tint = LedgerColors.Gold)
                },
            )
            Spacer(Modifier.height(16.dp))
            ContactOptionCard(
                title = stringResource(Res.string.ct_phone),
                description = stringResource(Res.string.ct_phone_desc),
                icon = Icons.Outlined.Call,
                tint = LedgerColors.Blue,
                footer = { FooterValue(text = "+41 44 555 12 34") },
            )
            Spacer(Modifier.height(16.dp))
            ContactOptionCard(
                title = stringResource(Res.string.ct_dispatch),
                description = stringResource(Res.string.ct_dispatch_desc),
                icon = Icons.Outlined.MailOutline,
                tint = LedgerColors.Blue,
                footer = { FooterValue(text = stringResource(Res.string.ct_dispatch_value)) },
            )

            Spacer(Modifier.height(32.dp))

            // ============ Secure message form =============================
            BentoCard(fill = LedgerColors.SurfaceRaised, cornerRadius = 16.dp) {
                if (dispatched) {
                    EmptyState(
                        title = stringResource(Res.string.ct_success_title),
                        message = stringResource(Res.string.ct_success_desc),
                        icon = Icons.Outlined.MarkEmailRead,
                        iconTint = LedgerColors.Green,
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MailOutline,
                            contentDescription = null,
                            tint = LedgerColors.TextTertiary,
                        )
                        Text(
                            text = stringResource(Res.string.ct_form_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = LedgerColors.TextPrimary,
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // ---- subject category (design select) -----------------
                    Text(
                        text = stringResource(Res.string.ct_subject_label),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = LedgerColors.TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(LedgerColors.Card)
                                .clickable { menuOpen = true }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = topicIndex?.let { topics[it] }
                                    ?: stringResource(Res.string.ct_subject_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (topicIndex != null) {
                                    LedgerColors.TextPrimary
                                } else {
                                    LedgerColors.TextTertiary
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = LedgerColors.TextTertiary,
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            topics.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    },
                                    onClick = {
                                        topicIndex = index
                                        menuOpen = false
                                    },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ---- detailed description -----------------------------
                    Text(
                        text = stringResource(Res.string.ct_description_label),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = LedgerColors.TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    LedgerTextArea(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = stringResource(Res.string.ct_description_placeholder),
                        minHeight = 200.dp,
                    )

                    Spacer(Modifier.height(24.dp))

                    LedgerButton(
                        text = stringResource(Res.string.ct_submit),
                        onClick = { dispatched = true },
                        trailingIcon = Icons.Outlined.Send,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = topicIndex != null && description.isNotBlank(),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Concierge channel card (design `0ZYbh` options column). */
@Composable
private fun ContactOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    tint: Color,
    badge: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit),
) {
    BentoCard(fill = LedgerColors.SurfaceRaised, cornerRadius = 16.dp) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LedgerColors.Canvas),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LedgerColors.TextPrimary,
                    )
                    if (badge != null) {
                        badge()
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerColors.TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                footer()
            }
        }
    }
}

/** "Initiate Session" — gold action link with forward mark. */
@Composable
private fun FooterLink(text: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = tint,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(11.dp),
        )
    }
}

/** Mono contact value (design: Liberation Mono, light blue). */
@Composable
private fun FooterValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        color = LedgerColors.Blue,
    )
}
