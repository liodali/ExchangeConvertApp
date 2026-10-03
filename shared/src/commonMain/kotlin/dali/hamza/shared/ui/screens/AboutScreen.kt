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
import dali.hamza.shared.ui.theme.LedgerStrings

/**
 * About Sovereign Ledger — identity, live version, legal pages (in-app)
 * and the Play rating entry. Internal page (Resources & Legal).
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
            title = LedgerStrings.APP_TITLE,
            onBack = onBack,
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(40.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LedgerLogoMark(size = 36.dp)
                Text(
                    text = "SOVEREIGN LEDGER",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 3.sp,
                    color = LedgerColors.Gold,
                )
                Text(
                    text = LedgerStrings.About.TAGLINE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerColors.TextSecondary,
                )
            }

            Spacer(Modifier.height(32.dp))

            BentoCard {
                Text(
                    text = "VERSION",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LedgerColors.TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = appVersionName(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = LedgerColors.TextOnColor,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = LedgerStrings.About.VERSION_NOTE,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextTertiary,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = LedgerStrings.Support.COPYRIGHT,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextTertiary,
                )
            }

            Spacer(Modifier.height(24.dp))

            BentoCard {
                LedgerListRow(
                    label = LedgerStrings.About.TERMS,
                    icon = Icons.Outlined.Gavel,
                    onClick = onOpenTerms,
                )
                Spacer(Modifier.height(16.dp))
                LedgerListRow(
                    label = LedgerStrings.About.PRIVACY,
                    icon = Icons.Outlined.Description,
                    onClick = onOpenPrivacy,
                )
            }

            Spacer(Modifier.height(24.dp))

            LedgerButton(
                text = LedgerStrings.About.RATE,
                onClick = { openUri(AppLinks.PLAY_URL) },
                leadingIcon = Icons.Outlined.StarOutline,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = LedgerStrings.About.POWERED_BY,
                style = MaterialTheme.typography.labelSmall,
                color = LedgerColors.TextTertiary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(48.dp))
        }
    }
}
