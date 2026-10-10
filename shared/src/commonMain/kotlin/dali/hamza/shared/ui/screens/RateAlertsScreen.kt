package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.RateAlert
import dali.hamza.shared.domain.models.RateAlertMode
import dali.hamza.shared.ui.components.AddRateAlertDialog
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.EmptyState
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerButtonVariant
import dali.hamza.shared.ui.components.LedgerChip
import dali.hamza.shared.ui.components.LedgerListRow
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.components.ledgerNavClearance
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LedgerStrings
import dali.hamza.shared.ui.viewmodel.RateAlertsViewModel

/**
 * Rate Alerts (pushed route from Account) — manage the tier-capped
 * alerts: server-push (guest: one 2-hour digest alert, Phase 2) plus any
 * pre-Phase-2 local alerts still running on the on-device engine.
 */
@Composable
fun RateAlertsScreen(
    viewModel: RateAlertsViewModel,
    currencies: List<Currency>,
    defaultBase: String?,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    // each engine has its own cap — the dialog owns the per-delivery UX
    val serverAtCap = viewModel.serverAlerts.size >= viewModel.maxServerAlerts
    val localAtCap = viewModel.localAlerts.size >= viewModel.maxLocalAlerts

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        LedgerTopAppBar(
            title = LedgerStrings.RateAlerts.TITLE,
            onBack = onBack,
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = LedgerStrings.RateAlerts.HEADER_NOTE,
                style = MaterialTheme.typography.bodySmall,
                color = LedgerColors.TextTertiary,
            )
            Spacer(Modifier.height(24.dp))

            if (!viewModel.permissionGranted && viewModel.alerts.isNotEmpty()) {
                PermissionBanner(onEnable = viewModel::requestPermission)
                Spacer(Modifier.height(16.dp))
            }

            viewModel.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message == LedgerStrings.RateAlerts.PUSH_FALLBACK_NOTE) {
                        LedgerColors.Gold
                    } else {
                        LedgerColors.Error
                    },
                )
                Spacer(Modifier.height(16.dp))
            }

            if (viewModel.alerts.isEmpty()) {
                EmptyState(
                    title = LedgerStrings.RateAlerts.EMPTY_TITLE,
                    message = LedgerStrings.RateAlerts.EMPTY_MESSAGE,
                    icon = Icons.Outlined.NotificationsActive,
                    iconTint = LedgerColors.Blue,
                    actionLabel = LedgerStrings.RateAlerts.NEW_ALERT,
                    onAction = { showAddDialog = true },
                )
            } else {
                // two engines, two sections — an alert lives in exactly one
                if (viewModel.serverAlerts.isNotEmpty()) {
                    SectionHeader(
                        title = LedgerStrings.RateAlerts.SERVER_SECTION_TITLE,
                        usage = LedgerStrings.RateAlerts.usage(
                            viewModel.serverAlerts.size,
                            viewModel.maxServerAlerts,
                        ),
                        atCap = serverAtCap,
                    )
                    Spacer(Modifier.height(12.dp))
                    BentoCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            viewModel.serverAlerts.forEach { alert ->
                                AlertRow(
                                    alert = alert,
                                    onToggle = { viewModel.setEnabled(alert, it) },
                                    onDelete = { viewModel.removeAlert(alert) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
                if (viewModel.localAlerts.isNotEmpty()) {
                    SectionHeader(
                        title = LedgerStrings.RateAlerts.LOCAL_SECTION_TITLE,
                        usage = LedgerStrings.RateAlerts.usage(
                            viewModel.localAlerts.size,
                            viewModel.maxLocalAlerts,
                        ),
                        atCap = localAtCap,
                    )
                    Spacer(Modifier.height(12.dp))
                    BentoCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            viewModel.localAlerts.forEach { alert ->
                                AlertRow(
                                    alert = alert,
                                    onToggle = { viewModel.setEnabled(alert, it) },
                                    onDelete = { viewModel.removeAlert(alert) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                Text(
                    text = LedgerStrings.RateAlerts.FREE_PLAN_NOTE,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextTertiary,
                )

                Spacer(Modifier.height(16.dp))
                // the dialog owns the per-delivery cap UX — the button only
                // locks when BOTH lists are full
                LedgerButton(
                    text = LedgerStrings.RateAlerts.NEW_ALERT,
                    onClick = { showAddDialog = true },
                    leadingIcon = Icons.Outlined.Add,
                    enabled = !(serverAtCap && localAtCap),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(32.dp))
            Spacer(Modifier.height(ledgerNavClearance()))
        }
    }

    if (showAddDialog) {
        AddRateAlertDialog(
            currencies = currencies,
            initialBase = defaultBase,
            tier = viewModel.tier,
            atLocalCap = localAtCap,
            atServerCap = serverAtCap,
            onDismiss = {
                showAddDialog = false
                viewModel.clearMessage()
            },
            onConfirm = { base, quote, mode, intervalMinutes, thresholdPercent, source ->
                showAddDialog = false
                viewModel.addAlert(base, quote, mode, intervalMinutes, thresholdPercent, source)
            },
        )
    }
}

/** Small label-over-content header for the two alert sections. */
@Composable
private fun SectionHeader(title: String, usage: String, atCap: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = LedgerColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = usage,
            style = MaterialTheme.typography.labelMedium,
            color = if (atCap) LedgerColors.Gold else LedgerColors.TextTertiary,
        )
    }
}

@Composable
private fun PermissionBanner(onEnable: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LedgerColors.Gold.copy(alpha = 0.10f))
            .padding(16.dp),
    ) {
        Text(
            text = LedgerStrings.RateAlerts.PERMISSION_BANNER,
            style = MaterialTheme.typography.bodySmall,
            color = LedgerColors.Gold,
            modifier = Modifier.weight(1f),
        )
        LedgerButton(
            text = LedgerStrings.RateAlerts.PERMISSION_ACTION,
            onClick = onEnable,
            variant = LedgerButtonVariant.TONAL,
        )
    }
}

@Composable
private fun AlertRow(
    alert: RateAlert,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LedgerColors.Canvas)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            // mode chip gets its own line above the pair — matches the
            // design's small-label-over-content pattern (Heading 3)
            LedgerChip(
                text = modeChipLabel(alert),
                tint = if (alert.mode == RateAlertMode.THRESHOLD) {
                    LedgerColors.Gold
                } else {
                    LedgerColors.Blue
                },
                cornerRadius = 6.dp,
            )
            Text(
                text = LedgerStrings.RateAlerts.pairTitle(alert.base, alert.quote),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LedgerColors.TextPrimary,
            )
            alert.lastRate?.let { rate ->
                Text(
                    text = LedgerStrings.RateAlerts.lastRateLabel(rate),
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextSecondary,
                )
            } ?: Text(
                text = LedgerStrings.RateAlerts.AWAITING_CHECK,
                style = MaterialTheme.typography.bodySmall,
                color = LedgerColors.TextTertiary,
            )
            // lastNotifiedAt is seeded with the creation time (so periodic
            // alerts wait a full interval) — only real firings are shown
            if (alert.lastNotifiedAt > alert.createdAt) {
                Text(
                    text = LedgerStrings.RateAlerts.notifiedAt(alert.lastNotifiedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerColors.TextTertiary,
                )
            }
        }
        Switch(
            checked = alert.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LedgerColors.Green,
                checkedTrackColor = LedgerColors.Green.copy(alpha = 0.25f),
            ),
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = LedgerStrings.RateAlerts.TITLE,
                tint = LedgerColors.Error,
            )
        }
    }
}

private fun modeChipLabel(alert: RateAlert): String = when (alert.mode) {
    RateAlertMode.PERIODIC -> LedgerStrings.RateAlerts.modeLabel(alert.intervalMinutes)
    RateAlertMode.THRESHOLD -> LedgerStrings.RateAlerts.thresholdLabel(alert.thresholdPercent)
}

