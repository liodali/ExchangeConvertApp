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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.shared.platform.AppLinks
import dali.hamza.shared.platform.appVersionName
import dali.hamza.shared.platform.openUri
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerListRow
import dali.hamza.shared.ui.components.LedgerLogoMark
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.theme.LedgerColors
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.about_powered_by
import echangecurrencyapp.shared.generated.resources.about_rate_play
import echangecurrencyapp.shared.generated.resources.about_tagline
import echangecurrencyapp.shared.generated.resources.about_version_label
import echangecurrencyapp.shared.generated.resources.about_version_note
import echangecurrencyapp.shared.generated.resources.legal_privacy
import echangecurrencyapp.shared.generated.resources.legal_terms
import org.jetbrains.compose.resources.stringResource

/**
 * About Sovereign Ledger — identity, live version, legal pages (in-app)
 * and the store rating entry (Play on Android, App Store on iOS). Internal page (Resources & Legal).
 */
@Composable
fun AboutScreen(
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onBack: () -> Unit,
) {
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 40.dp).fillMaxWidth(),
            ) {
                LedgerLogoMark(size = 36.dp)
                Text(
                    text = "DINARLY",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 3.sp,
                    color = LedgerColors.Gold,
                )
                Text(
                    text = stringResource(Res.string.about_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerColors.TextSecondary,
                )
            }

            Spacer(Modifier.height(32.dp))

            BentoCard {
                Text(
                    text = stringResource(Res.string.about_version_label),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LedgerColors.TextSecondary,
                )
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = appVersionName(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = LedgerColors.TextOnColor,
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(Res.string.about_version_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextTertiary,
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = "© 2026 Adetify.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextTertiary,
                )
            }

            Spacer(Modifier.height(24.dp))

            BentoCard {
                LedgerListRow(
                    label = stringResource(Res.string.legal_terms),
                    icon = Icons.Outlined.Gavel,
                    onClick = onOpenTerms,
                )
                LedgerListRow(
                    modifier = Modifier.padding(top = 16.dp),
                    label = stringResource(Res.string.legal_privacy),
                    icon = Icons.Outlined.Description,
                    onClick = onOpenPrivacy,
                )
            }

            LedgerButton(
                text = stringResource(Res.string.about_rate_play),
                onClick = { openUri(AppLinks.STORE_REVIEW_URL) },
                leadingIcon = Icons.Outlined.StarOutline,
                modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
            )

            Text(
                text = stringResource(Res.string.about_powered_by),
                style = MaterialTheme.typography.labelSmall,
                color = LedgerColors.TextTertiary,
                modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(48.dp))
        }
    }
}
