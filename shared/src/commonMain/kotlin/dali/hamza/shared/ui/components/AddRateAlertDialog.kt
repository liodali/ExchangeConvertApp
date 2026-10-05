package dali.hamza.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.RateAlert
import dali.hamza.shared.domain.models.RateAlertMode
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LedgerStrings

/** Add-dialog preset (UI-only) mapped onto [RateAlertMode] + params. */
private enum class AlertPreset(val label: String) {
    HOURLY(LedgerStrings.RateAlerts.MODE_HOURLY),
    TWO_HOURS(LedgerStrings.RateAlerts.MODE_TWO_HOURS),
    ON_MOVE(LedgerStrings.RateAlerts.MODE_ON_MOVE),
}

/**
 * "New Rate Alert" form — shared by the Rate Alerts screen and the Home
 * market-overview cards (bell action on a tracked market).
 *
 * Hosted in a [ModalBottomSheet] (not a [androidx.compose.ui.window.Dialog]):
 * the sheet is the app's established cross-platform modal — Compose
 * Multiplatform's Dialog mislays its content on iOS (full-height window,
 * scattered layout), while sheets render correctly on both platforms
 * (see CurrencyPickerSheet).
 *
 * The pair is pre-filled from [initialBase]/[initialQuote] but stays
 * editable through the currency pickers; [atCap] disables creation when
 * the free-tier alert limit is reached.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRateAlertDialog(
    currencies: List<Currency>,
    initialBase: String?,
    onDismiss: () -> Unit,
    onConfirm: (
        base: String,
        quote: String,
        mode: RateAlertMode,
        intervalMinutes: Long,
        thresholdPercent: Double,
    ) -> Unit,
    modifier: Modifier = Modifier,
    initialQuote: String? = null,
    atCap: Boolean = false,
) {
    var base by remember { mutableStateOf(initialBase ?: "USD") }
    var quote by remember {
        mutableStateOf(
            initialQuote?.takeIf { it != (initialBase ?: "USD") }
                ?: currencies.firstOrNull { it.name != (initialBase ?: "USD") }?.name
                ?: "EUR"
        )
    }
    var preset by remember { mutableStateOf(AlertPreset.HOURLY) }
    var thresholdText by remember { mutableStateOf(LedgerStrings.RateAlerts.THRESHOLD_HINT) }
    var picking by remember { mutableStateOf(0) } // 1 = base, 2 = quote

    val threshold = thresholdText.toDoubleOrNull()
    val valid = !atCap && base != quote &&
        (preset != AlertPreset.ON_MOVE || (threshold != null && threshold > 0.0 && threshold <= 50.0))

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = LedgerStrings.RateAlerts.NEW_ALERT,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LedgerColors.TextPrimary,
            )
            Spacer(Modifier.height(20.dp))
            LedgerListRow(
                label = LedgerStrings.RateAlerts.BASE_LABEL,
                value = base,
                onClick = { picking = 1 },
            )
            Spacer(Modifier.height(8.dp))
            LedgerListRow(
                label = LedgerStrings.RateAlerts.QUOTE_LABEL,
                value = quote,
                onClick = { picking = 2 },
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = LedgerStrings.RateAlerts.MODE_SECTION,
                style = MaterialTheme.typography.labelMedium,
                color = LedgerColors.TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            // segmented preset control (same recipe as Appearance mode)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LedgerColors.SurfaceElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AlertPreset.entries.forEach { option ->
                    val selected = preset == option
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selected) LedgerColors.Green else LedgerColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) LedgerColors.GreenSoft else Color.Transparent
                            )
                            .clickable { preset = option }
                            .padding(vertical = 8.dp),
                    )
                }
            }
            if (preset == AlertPreset.ON_MOVE) {
                Spacer(Modifier.height(16.dp))
                LedgerInput(
                    value = thresholdText,
                    onValueChange = { input ->
                        thresholdText = input.filter { it.isDigit() || it == '.' }
                    },
                    label = LedgerStrings.RateAlerts.THRESHOLD_LABEL,
                )
            }
            if (atCap) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = LedgerStrings.RateAlerts.AT_CAP_NOTE,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.Gold,
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LedgerButton(
                    text = LedgerStrings.RateAlerts.CANCEL,
                    onClick = onDismiss,
                    variant = LedgerButtonVariant.TONAL,
                    modifier = Modifier.weight(1f),
                )
                LedgerButton(
                    text = LedgerStrings.RateAlerts.ADD,
                    onClick = {
                        onConfirm(
                            base,
                            quote,
                            if (preset == AlertPreset.ON_MOVE) RateAlertMode.THRESHOLD else RateAlertMode.PERIODIC,
                            if (preset == AlertPreset.TWO_HOURS) RateAlert.INTERVAL_TWO_HOURS else RateAlert.INTERVAL_HOURLY,
                            threshold ?: RateAlert.DEFAULT_THRESHOLD_PERCENT,
                        )
                    },
                    enabled = valid,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (picking != 0) {
        val excluded = if (picking == 1) quote else base
        CurrencyPickerSheet(
            visible = true,
            currencies = currencies.filter { it.name != excluded },
            title = if (picking == 1) {
                LedgerStrings.RateAlerts.PICK_BASE_TITLE
            } else {
                LedgerStrings.RateAlerts.PICK_QUOTE_TITLE
            },
            onDismiss = { picking = 0 },
            onCurrencySelected = { currency ->
                if (picking == 1) base = currency.name else quote = currency.name
                picking = 0
            },
        )
    }
}
