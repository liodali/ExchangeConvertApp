package dali.hamza.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dali.hamza.shared.data.storage.SUPPORTED_LANGUAGE_TAGS
import dali.hamza.shared.ui.theme.LedgerColors
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.acct_language
import echangecurrencyapp.shared.generated.resources.acct_language_phone
import org.jetbrains.compose.resources.stringResource

/** Endonym shown for each supported tag — always in its own language. */
internal fun endonymFor(tag: String): String = when (tag) {
    "en" -> "English"
    "de" -> "Deutsch"
    "ar" -> "العربية"
    "it" -> "Italiano"
    "es" -> "Español"
    "pt-BR" -> "Português (Brasil)"
    "pt-PT" -> "Português (Portugal)"
    else -> tag
}

/**
 * In-app language sheet (Account → Language):
 * "Follow phone language" first, then every supported language by its
 * endonym. Selecting calls [onSelect] with the BCP-47 tag (null = follow
 * the phone) — the caller persists it and hands it to
 * `applyLanguageOverride` (Android switches in place, iOS asks to restart).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(
    currentOverride: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = stringResource(Res.string.acct_language),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LedgerColors.TextPrimary,
            )
            Spacer(Modifier.height(16.dp))
            LanguageRow(
                label = stringResource(Res.string.acct_language_phone),
                icon = Icons.Outlined.Smartphone,
                selected = currentOverride == null,
                onClick = { onSelect(null) },
            )

            Spacer(Modifier.height(4.dp))

            SUPPORTED_LANGUAGE_TAGS.forEach { tag ->
                LanguageRow(
                    label = endonymFor(tag),
                    icon = if (tag == "ar") {
                        Icons.AutoMirrored.Outlined.Label
                    } else {
                        Icons.Outlined.Language
                    },
                    selected = currentOverride == tag,
                    onClick = { onSelect(tag) },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LanguageRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) LedgerColors.Green else LedgerColors.TextTertiary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) LedgerColors.Green else LedgerColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = LedgerColors.Green,
            )
        }
    }
}
