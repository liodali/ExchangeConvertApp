package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import dali.hamza.shared.data.alerts.formatRate
import dali.hamza.shared.data.alerts.formatSignedPercent
import dali.hamza.shared.domain.models.WidgetConfig
import dali.hamza.shared.ui.components.AddWidgetPairDialog
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.EmptyState
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerChip
import dali.hamza.shared.ui.components.LedgerListRow
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.components.ledgerNavClearance
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.viewmodel.WidgetsEvent
import dali.hamza.shared.ui.viewmodel.WidgetsViewModel
import dali.hamza.shared.domain.models.WidgetType
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.wg_add
import echangecurrencyapp.shared.generated.resources.wg_detected_note
import echangecurrencyapp.shared.generated.resources.wg_empty_message
import echangecurrencyapp.shared.generated.resources.wg_empty_title
import echangecurrencyapp.shared.generated.resources.wg_forget
import echangecurrencyapp.shared.generated.resources.wg_forgotten
import echangecurrencyapp.shared.generated.resources.wg_header
import echangecurrencyapp.shared.generated.resources.wg_ios_message
import echangecurrencyapp.shared.generated.resources.wg_ios_title
import echangecurrencyapp.shared.generated.resources.wg_kind_card
import echangecurrencyapp.shared.generated.resources.wg_kind_convert
import echangecurrencyapp.shared.generated.resources.wg_kind_grid
import echangecurrencyapp.shared.generated.resources.wg_kind_wave
import echangecurrencyapp.shared.generated.resources.wg_not_placed
import echangecurrencyapp.shared.generated.resources.wg_on_home_screen
import echangecurrencyapp.shared.generated.resources.wg_realtime
import echangecurrencyapp.shared.generated.resources.wg_realtime_desc
import echangecurrencyapp.shared.generated.resources.wg_reconfigure
import echangecurrencyapp.shared.generated.resources.wg_refresh
import echangecurrencyapp.shared.generated.resources.wg_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Account → Widgets (`plans/widgets.md`): manage the home-screen market
 * widgets — add (pin request on Android / guide on iOS), change a pair,
 * forget an instance. Rates shown are the latest shared snapshots.
 */
@Composable
fun WidgetsScreen(
    viewModel: WidgetsViewModel,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                WidgetsEvent.Forgotten ->
                    snackbarHostState.showSnackbar(getString(Res.string.wg_forgotten))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            LedgerTopAppBar(
                title = stringResource(Res.string.wg_title),
                onBack = onBack,
            )

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.wg_header),
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextTertiary,
                )
                Spacer(Modifier.height(24.dp))

                if (viewModel.configs.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Widgets,
                        title = stringResource(Res.string.wg_empty_title),
                        message = stringResource(Res.string.wg_empty_message),
                    )
                    Spacer(Modifier.height(24.dp))
                }

                if (viewModel.hostBridge.canManage) {
                    WidgetConfigList(viewModel)
                } else {
                    // iOS: no enumeration — show what the widgets report
                    // rendering (detection-by-render) + the add guide
                    if (viewModel.activePairs.isNotEmpty()) {
                        BentoCard {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    stringResource(Res.string.wg_on_home_screen),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LedgerColors.TextTertiary,
                                )
                                viewModel.activePairs.forEach { (base, quote) ->
                                    Text(
                                        "$base/$quote",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LedgerColors.TextPrimary,
                                    )
                                }
                                Text(
                                    stringResource(Res.string.wg_detected_note),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LedgerColors.TextTertiary,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    IosGuideCard()
                }

                Spacer(Modifier.height(24.dp))

                if (viewModel.hostBridge.canPin) {
                    LedgerButton(
                        text = stringResource(Res.string.wg_add),
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                // refresh controls are Android-only (its widgets own a
                // worker); iOS widgets self-schedule via WidgetKit budgets
                if (viewModel.hostBridge.canManage) {
                    LedgerButton(
                        text = stringResource(Res.string.wg_refresh),
                        onClick = viewModel::refresh,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))

                    // refresh policy: realtime-ish vs battery-friendly
                    BentoCard {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        stringResource(Res.string.wg_realtime),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LedgerColors.TextPrimary,
                                    )
                                    Text(
                                        stringResource(Res.string.wg_realtime_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LedgerColors.TextTertiary,
                                    )
                                }
                                Switch(
                                    checked = viewModel.realtime,
                                    onCheckedChange = { viewModel.toggleRealtime() },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(24.dp))
                Spacer(Modifier.height(ledgerNavClearance() * 0.4f))
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showAddDialog) {
        AddWidgetPairDialog(
            currencies = viewModel.currencies,
            suggestedPairs = viewModel.suggestedPairs,
            onDismiss = { showAddDialog = false },
            onAdd = { kind, base, quote, gridPairs ->
                showAddDialog = false
                viewModel.add(kind, base, quote, gridPairs)
            },
        )
    }
}

@Composable
private fun WidgetConfigList(viewModel: WidgetsViewModel) {
    // swipe-to-dismiss rows (same visual family as the alerts list)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        viewModel.configs.forEach { config ->
            val snapshot = viewModel.snapshotFor(config)
            SwipeWidgetRow(
                config = config,
                rate = snapshot?.rate,
                deltaPercent = snapshot?.dayDeltaPercent,
                onReconfigure = { viewModel.reconfigure(config) },
                onDismiss = { viewModel.forget(config) },
            )
        }
    }
}

private enum class WidgetRowAnchor { Settled, Dismissed }

/** Full swipe right→left dismisses (deletes) the widget config — no button. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SwipeWidgetRow(
    config: WidgetConfig,
    rate: Double?,
    deltaPercent: Double?,
    onReconfigure: () -> Unit,
    onDismiss: () -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val dismissPx = with(density) { 320.dp.toPx() }
    val swipeState = remember(dismissPx) {
        androidx.compose.foundation.gestures.AnchoredDraggableState(
            initialValue = WidgetRowAnchor.Settled,
            anchors = androidx.compose.foundation.gestures.DraggableAnchors {
                WidgetRowAnchor.Settled at 0f
                WidgetRowAnchor.Dismissed at -dismissPx
            },
        )
    }
    // settling on the far anchor IS the delete — the row leaves the list
    LaunchedEffect(swipeState) {
        androidx.compose.runtime.snapshotFlow { swipeState.settledValue }.collect { value ->
            if (value == WidgetRowAnchor.Dismissed) onDismiss()
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // delete affordance behind the sliding row — soft theme-red backing
        // (low-alpha Error token) so it reads clearly without being harsh
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp))
                .background(LedgerColors.Error.copy(alpha = 0.14f)),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(Res.string.wg_forget),
                tint = LedgerColors.Error,
                modifier = Modifier
                    .padding(end = 20.dp)
                    .size(22.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipeState.offset.roundToInt(), 0) }
                .anchoredDraggable(swipeState, androidx.compose.foundation.gestures.Orientation.Horizontal)
                .clip(RoundedCornerShape(16.dp))
                .background(LedgerColors.Canvas)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${config.base}/${config.quote}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LedgerColors.TextPrimary,
                    )
                    Text(
                        text = typeLabel(config.type),
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerColors.TextTertiary,
                    )
                    deltaPercent?.let { delta ->
                        LedgerChip(
                            text = formatSignedPercent(delta),
                            tint = if (delta >= 0) LedgerColors.Green else LedgerColors.Error,
                            cornerRadius = 4.dp,
                        )
                    }
                }
                if (rate != null) {
                    Text(
                        text = formatRate(rate),
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerColors.TextSecondary,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.wg_not_placed),
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerColors.TextTertiary,
                    )
                }
            }
            IconButton(onClick = onReconfigure) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(Res.string.wg_reconfigure),
                    tint = LedgerColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun IosGuideCard() {
    BentoCard {
        Text(
            stringResource(Res.string.wg_ios_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LedgerColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(Res.string.wg_ios_message),
            style = MaterialTheme.typography.bodySmall,
            color = LedgerColors.TextSecondary,
        )
    }
}

/** Localized widget-type label (was LedgerStrings.Widgets.typeLabel). */
@Composable
private fun typeLabel(type: WidgetType): String = when (type) {
    WidgetType.MARKET_CARD -> stringResource(Res.string.wg_kind_card)
    WidgetType.MARKET_WAVE -> stringResource(Res.string.wg_kind_wave)
    WidgetType.MARKET_GRID -> stringResource(Res.string.wg_kind_grid)
    WidgetType.QUICK_CONVERT -> stringResource(Res.string.wg_kind_convert)
}
