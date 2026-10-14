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
import dali.hamza.shared.ui.theme.LedgerStrings
import dali.hamza.shared.ui.viewmodel.WidgetsViewModel
import kotlinx.coroutines.launch

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
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            LedgerTopAppBar(
                title = LedgerStrings.Widgets.TITLE,
                onBack = onBack,
            )

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = LedgerStrings.Widgets.HEADER_NOTE,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextTertiary,
                )
                Spacer(Modifier.height(24.dp))

                if (viewModel.configs.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Widgets,
                        title = LedgerStrings.Widgets.EMPTY_TITLE,
                        message = LedgerStrings.Widgets.EMPTY_MESSAGE,
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
                                    "ON YOUR HOME SCREEN",
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
                                    "Detected from widget refreshes — iOS doesn't expose placed widgets.",
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
                        text = LedgerStrings.Widgets.ADD,
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                LedgerButton(
                    text = LedgerStrings.Widgets.REFRESH,
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
                                    "Realtime rates",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LedgerColors.TextPrimary,
                                )
                                Text(
                                    "Faster widget updates (every 15 min + on app open). Uses more battery.",
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
                contentDescription = LedgerStrings.Widgets.FORGET,
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
                        text = LedgerStrings.Widgets.typeLabel(config.type),
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
                        text = LedgerStrings.Widgets.NOT_PLACED,
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerColors.TextTertiary,
                    )
                }
            }
            IconButton(onClick = onReconfigure) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = LedgerStrings.Widgets.RECONFIGURE,
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
            LedgerStrings.Widgets.IOS_GUIDE_TITLE,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LedgerColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            LedgerStrings.Widgets.IOS_GUIDE_MESSAGE,
            style = MaterialTheme.typography.bodySmall,
            color = LedgerColors.TextSecondary,
        )
    }
}
