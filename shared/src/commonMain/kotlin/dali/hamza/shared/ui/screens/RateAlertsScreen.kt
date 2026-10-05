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

/** Add-dialog preset (UI-only) mapped onto [RateAlertMode] + params. */
private enum class AlertPreset(val label: String) {
    HOURLY(LedgerStrings.RateAlerts.MODE_HOURLY),
    TWO_HOURS(LedgerStrings.RateAlerts.MODE_TWO_HOURS),
    ON_MOVE(LedgerStrings.RateAlerts.MODE_ON_MOVE),
}

/**
 * Rate Alerts (pushed route from Account) — manage the free-tier capped
 * local notification alerts: hourly / 2-hourly digests or threshold moves.
 */
@Composable
fun RateAlertsScreen(
    viewModel: RateAlertsViewModel,
    currencies: List<Currency>,
    defaultBase: String?,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val used = viewModel.alerts.size
    val atCap = used >= viewModel.maxAlerts

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
                    color = LedgerColors.Error,
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
                BentoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        viewModel.alerts.forEach { alert ->
                            AlertRow(
                                alert = alert,
                                onToggle = { viewModel.setEnabled(alert.id, it) },
                                onDelete = { viewModel.removeAlert(alert.id) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = LedgerStrings.RateAlerts.usage(used, viewModel.maxAlerts),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (atCap) LedgerColors.Gold else LedgerColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    if (atCap) {
                        LedgerChip(
                            text = LedgerStrings.Account.COMING_SOON,
                            tint = LedgerColors.Gold,
                            cornerRadius = 4.dp,
                        )
                    }
                }
                Text(
                    text = LedgerStrings.RateAlerts.FREE_PLAN_NOTE,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextTertiary,
                )

                Spacer(Modifier.height(16.dp))
                LedgerButton(
                    text = LedgerStrings.RateAlerts.NEW_ALERT,
                    onClick = { showAddDialog = true },
                    leadingIcon = Icons.Outlined.Add,
                    enabled = !atCap,
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
            onDismiss = {
                showAddDialog = false
                viewModel.clearMessage()
            },
            onConfirm = { base, quote, mode, intervalMinutes, thresholdPercent ->
                showAddDialog = false
                viewModel.addAlert(base, quote, mode, intervalMinutes, thresholdPercent)
            },
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

