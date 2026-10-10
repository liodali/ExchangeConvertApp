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
import dali.hamza.shared.domain.models.AlertSource
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.DataTier
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
 * Cadence presets per delivery engine:
 * - **On device** — as it always was: hourly / 2-hourly / on-move, run by
 *   the local engine for every tier.
 * - **Server push** — tier-gated ([RateAlert.serverIntervalsForTier]):
 *   guests get the 2-hour digest only (their push budget is 1 per 2h);
 *   login will offer 1h + 2h; paid cadence is defined with the tier work.
 */
private fun presetsFor(source: AlertSource, tier: DataTier): List<AlertPreset> = when (source) {
    AlertSource.LOCAL -> listOf(AlertPreset.HOURLY, AlertPreset.TWO_HOURS, AlertPreset.ON_MOVE)
    AlertSource.SERVER -> RateAlert.serverIntervalsForTier(tier).map { interval ->
        if (interval == RateAlert.INTERVAL_HOURLY) AlertPreset.HOURLY else AlertPreset.TWO_HOURS
    }
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
 * editable through the currency pickers. Delivery is a toggle — **On
 * device** (local engine, all modes) or **Push** (server-evaluated, 2h
 * digest for guests); each list has its own cap and disables creation
 * when reached.
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
        source: AlertSource,
    ) -> Unit,
    modifier: Modifier = Modifier,
    initialQuote: String? = null,
    tier: DataTier = DataTier.GUEST,
    atLocalCap: Boolean = false,
    atServerCap: Boolean = false,
    /** Pre-selects the delivery toggle (Home bell defaults to Push). */
    initialSource: AlertSource = AlertSource.LOCAL,
) {
    var base by remember { mutableStateOf(initialBase ?: "USD") }
    var quote by remember {
        mutableStateOf(
            initialQuote?.takeIf { it != (initialBase ?: "USD") }
                ?: currencies.firstOrNull { it.name != (initialBase ?: "USD") }?.name
                ?: "EUR"
        )
    }
    // local delivery stays the default — push is the opt-in upgrade
    var source by remember { mutableStateOf(initialSource) }
    val presets = presetsFor(source, tier)
    var preset by remember(source) { mutableStateOf(presets.first()) }
    var thresholdText by remember { mutableStateOf(LedgerStrings.RateAlerts.THRESHOLD_HINT) }
    var picking by remember { mutableStateOf(0) } // 1 = base, 2 = quote

    val atCap = if (source == AlertSource.SERVER) atServerCap else atLocalCap
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

            // delivery toggle — on device (as it was) vs server push
            Text(
                text = LedgerStrings.RateAlerts.DELIVERY_SECTION,
                style = MaterialTheme.typography.labelMedium,
                color = LedgerColors.TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            SegmentedRow(
                options = listOf(
                    LedgerStrings.RateAlerts.DELIVERY_LOCAL to AlertSource.LOCAL,
                    LedgerStrings.RateAlerts.DELIVERY_PUSH to AlertSource.SERVER,
                ),
                selected = source,
                onSelect = { source = it },
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = LedgerStrings.RateAlerts.MODE_SECTION,
                style = MaterialTheme.typography.labelMedium,
                color = LedgerColors.TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            // segmented preset control (same recipe as Appearance mode)
            SegmentedRow(
                options = presets.map { it.label to it },
                selected = preset,
                onSelect = { preset = it },
            )
            if (source == AlertSource.SERVER && tier == DataTier.GUEST) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = LedgerStrings.RateAlerts.GUEST_PUSH_NOTE,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextTertiary,
                )
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
                            source,
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

/**
 * Two-or-more-option segmented control — the app's preset recipe (used
 * for the Appearance mode and delivery/mode rows).
 */
@Composable
private fun <T> SegmentedRow(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LedgerColors.SurfaceElevated)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (label, option) ->
            val isSelected = option == selected
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) LedgerColors.Green else LedgerColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) LedgerColors.GreenSoft else Color.Transparent
                    )
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
            )
        }
    }
}
