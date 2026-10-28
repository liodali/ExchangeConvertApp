package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.key
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.CurrencyPickerSheet
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerButtonVariant
import dali.hamza.shared.ui.components.ReorderableColumn
import dali.hamza.shared.ui.components.LedgerChartStyle
import dali.hamza.shared.ui.components.LedgerChip
import dali.hamza.shared.ui.components.LedgerInfoField
import dali.hamza.shared.ui.components.LedgerInput
import dali.hamza.shared.ui.components.LedgerListRow
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.components.SectionHeader
import dali.hamza.shared.ui.theme.LedgerColorPalette
import dali.hamza.shared.ui.components.ledgerNavClearance
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LedgerThemeMode
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.DataTier
import dali.hamza.shared.ui.viewmodel.AccountViewModel
import dali.hamza.shared.platform.applyLanguageOverride
import dali.hamza.shared.ui.components.LanguagePickerSheet
import dali.hamza.shared.ui.components.endonymFor
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.acct_add_market
import echangecurrencyapp.shared.generated.resources.acct_appearance_desc
import echangecurrencyapp.shared.generated.resources.acct_appearance_section
import echangecurrencyapp.shared.generated.resources.acct_appearance_title
import echangecurrencyapp.shared.generated.resources.acct_badge_guest
import echangecurrencyapp.shared.generated.resources.acct_badge_private
import echangecurrencyapp.shared.generated.resources.acct_badge_verified
import echangecurrencyapp.shared.generated.resources.acct_base_label
import echangecurrencyapp.shared.generated.resources.acct_base_pick
import echangecurrencyapp.shared.generated.resources.acct_biometric
import echangecurrencyapp.shared.generated.resources.acct_change_password
import echangecurrencyapp.shared.generated.resources.acct_chart_bar
import echangecurrencyapp.shared.generated.resources.acct_chart_desc
import echangecurrencyapp.shared.generated.resources.acct_chart_line
import echangecurrencyapp.shared.generated.resources.acct_chart_title
import echangecurrencyapp.shared.generated.resources.acct_clear_confirm
import echangecurrencyapp.shared.generated.resources.acct_clear_ledger
import echangecurrencyapp.shared.generated.resources.acct_clear_message
import echangecurrencyapp.shared.generated.resources.acct_clear_title
import echangecurrencyapp.shared.generated.resources.acct_clearance
import echangecurrencyapp.shared.generated.resources.acct_clearance_score
import echangecurrencyapp.shared.generated.resources.acct_coming_soon
import echangecurrencyapp.shared.generated.resources.acct_data_plan
import echangecurrencyapp.shared.generated.resources.acct_data_plan_guest
import echangecurrencyapp.shared.generated.resources.acct_data_plan_note
import echangecurrencyapp.shared.generated.resources.acct_data_plan_sovereign
import echangecurrencyapp.shared.generated.resources.acct_delete_account
import echangecurrencyapp.shared.generated.resources.acct_disabled
import echangecurrencyapp.shared.generated.resources.acct_edit_profile
import echangecurrencyapp.shared.generated.resources.acct_edit_profile_hint
import echangecurrencyapp.shared.generated.resources.acct_enabled
import echangecurrencyapp.shared.generated.resources.acct_footer
import echangecurrencyapp.shared.generated.resources.acct_language
import echangecurrencyapp.shared.generated.resources.acct_language_value
import echangecurrencyapp.shared.generated.resources.acct_local_data_section
import echangecurrencyapp.shared.generated.resources.acct_log_out
import echangecurrencyapp.shared.generated.resources.acct_login
import echangecurrencyapp.shared.generated.resources.acct_market_pick
import echangecurrencyapp.shared.generated.resources.acct_market_section
import echangecurrencyapp.shared.generated.resources.acct_market_slot
import echangecurrencyapp.shared.generated.resources.acct_mode_dark
import echangecurrencyapp.shared.generated.resources.acct_mode_light
import echangecurrencyapp.shared.generated.resources.acct_mode_system
import echangecurrencyapp.shared.generated.resources.acct_palette_section
import echangecurrencyapp.shared.generated.resources.acct_personal_info
import echangecurrencyapp.shared.generated.resources.acct_phone_label
import echangecurrencyapp.shared.generated.resources.acct_preferences
import echangecurrencyapp.shared.generated.resources.acct_push
import echangecurrencyapp.shared.generated.resources.acct_remove_market_desc
import echangecurrencyapp.shared.generated.resources.acct_residence_label
import echangecurrencyapp.shared.generated.resources.acct_residence_value
import echangecurrencyapp.shared.generated.resources.acct_security_desc
import echangecurrencyapp.shared.generated.resources.acct_security_health
import echangecurrencyapp.shared.generated.resources.acct_security_section
import echangecurrencyapp.shared.generated.resources.acct_support_cta
import echangecurrencyapp.shared.generated.resources.acct_support_desc
import echangecurrencyapp.shared.generated.resources.acct_support_section
import echangecurrencyapp.shared.generated.resources.acct_support_title
import echangecurrencyapp.shared.generated.resources.acct_sync_days
import echangecurrencyapp.shared.generated.resources.acct_sync_hours
import echangecurrencyapp.shared.generated.resources.acct_sync_just_now
import echangecurrencyapp.shared.generated.resources.acct_sync_minutes
import echangecurrencyapp.shared.generated.resources.acct_sync_never
import echangecurrencyapp.shared.generated.resources.acct_title
import echangecurrencyapp.shared.generated.resources.acct_vault_section
import echangecurrencyapp.shared.generated.resources.alerts_title
import echangecurrencyapp.shared.generated.resources.common_cancel
import echangecurrencyapp.shared.generated.resources.common_save
import echangecurrencyapp.shared.generated.resources.wg_title
import org.jetbrains.compose.resources.stringResource

/**
 * Profile & Settings (design frame `qDCZE`) — Account tab.
 *
 * Sections follow the pen file: profile hero (avatar + tier badges + edit),
 * personal information, security health (clearance ring), security &
 * authentication, preferences (persisted notifications toggle), vault
 * management, a compact-layout support entry point and the node/version
 * footer. Wide layouts place the settings cards in a two-column grid.
 */
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onOpenSupport: () -> Unit,
    showSupportEntry: Boolean = true,
    currencies: List<Currency> = emptyList(),
    baseCurrency: String? = null,
    onMarketPreferencesChanged: (List<String>) -> Unit = {},
    onBaseCurrencyChanged: (String) -> Unit = {},
    onUsernameChanged: (String) -> Unit = {},
    onThemeModeChanged: (LedgerThemeMode) -> Unit = {},
    onColorPaletteChanged: (LedgerColorPalette) -> Unit = {},
    onChartStyleChanged: (LedgerChartStyle) -> Unit = {},
    onOpenRateAlerts: () -> Unit = {},
    onOpenWidgets: () -> Unit = {},
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var marketSlot by remember { mutableStateOf<Int?>(null) }
    var addingMarket by remember { mutableStateOf(false) }
    var pickingBase by remember { mutableStateOf(false) }
    val isSovereign = viewModel.dataTier == DataTier.SOVEREIGN
    val biometricAuthenticator = remember {
        org.koin.mp.KoinPlatform.getKoin()?.get<dali.hamza.shared.platform.BiometricAuthenticator>()
            ?: dali.hamza.shared.platform.createBiometricAuthenticator()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        LedgerTopAppBar(
            title = stringResource(Res.string.acct_title),
            avatarInitials = viewModel.username,
            trailing = {
                // Bell opens the local rate alerts.
                IconButton(onClick = onOpenRateAlerts) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = stringResource(Res.string.alerts_title),
                        tint = LedgerColors.TextMuted,
                    )
                }
            },
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))
            ProfileHero(
                username = viewModel.username,
                email = viewModel.email,
                isSovereign = isSovereign,
                onEdit = { showEditDialog = true },
            )

            Spacer(Modifier.height(32.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val expandedGrid = maxWidth >= 720.dp
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = if (expandedGrid) 2 else 1,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                // ============ Account access (guest mode) ====================
                // login ships later — guests only manage their display name
                BentoCard(modifier = Modifier.weight(1f)) {
                    LedgerListRow(
                        label = stringResource(Res.string.acct_login),
                        icon = Icons.Outlined.ManageAccounts,
                        subtitle = {
                            LedgerChip(
                                text = stringResource(Res.string.acct_coming_soon),
                                tint = LedgerColors.Gold,
                                cornerRadius = 4.dp,
                            )
                        },
                    )
                    LedgerListRow(
                        modifier = Modifier.padding(top = 16.dp),
                        label = stringResource(Res.string.acct_data_plan),
                        icon = Icons.Outlined.Star,
                        iconTint = if (viewModel.dataTier == DataTier.SOVEREIGN) {
                            LedgerColors.Green
                        } else {
                            LedgerColors.TextTertiary
                        },
                        subtitle = {
                            Text(
                                text = if (viewModel.dataTier == DataTier.SOVEREIGN) {
                                    stringResource(Res.string.acct_data_plan_sovereign)
                                } else {
                                    stringResource(Res.string.acct_data_plan_guest)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = LedgerColors.TextSecondary,
                            )
                        },
                    )
                    Text(
                        modifier = Modifier.padding(top = 12.dp),
                        text = stringResource(Res.string.acct_data_plan_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerColors.TextTertiary,
                    )
                }

                // ============ Personal information (Sovereign only) ============
                // guest mode has no profile backend — persona fields wait for login
                if (isSovereign) {
                    BentoCard(modifier = Modifier.weight(1f)) {
                    SectionHeader(
                        title = stringResource(Res.string.acct_personal_info),
                        icon = Icons.Outlined.PersonOutline,
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        LedgerInfoField(
                            label = stringResource(Res.string.acct_phone_label),
                            value = "+41 44 212 7700",
                        )
                        LedgerInfoField(
                            label = stringResource(Res.string.acct_residence_label),
                            value = stringResource(Res.string.acct_residence_value),
                        )
                    }
                    }
                }

                // ============ Security health ==================================
                BentoCard(modifier = Modifier.weight(1f)) {
                    SectionHeader(
                        title = stringResource(Res.string.acct_security_health),
                        icon = Icons.Outlined.SupportAgent,
                        iconTint = LedgerColors.Green,
                        titleColor = LedgerColors.Green,
                    )
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = stringResource(Res.string.acct_security_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LedgerColors.TextSecondary,
                    )
                    Spacer(Modifier.height(24.dp))
                    ClearanceRow()
                }

                // ============ Security & authentication =======================
                // guests have no password — that row waits for Sovereign login
                BentoCard(modifier = Modifier.weight(1f)) {
                    SectionLabel(stringResource(Res.string.acct_security_section))
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (isSovereign) {
                            LedgerListRow(
                                label = stringResource(Res.string.acct_change_password),
                                icon = Icons.Outlined.Password,
                            )
                        }
                        LedgerListRow(
                            label = stringResource(Res.string.acct_biometric),
                            icon = Icons.Outlined.Fingerprint,
                            trailing = {
                                Switch(
                                    checked = viewModel.biometricUnlock,
                                    onCheckedChange = { enable ->
                                        if (enable) {
                                            // confirm with the system prompt before arming
                                            biometricAuthenticator.authenticate { ok ->
                                                if (ok) viewModel.updateBiometricUnlock(true)
                                            }
                                        } else {
                                            viewModel.updateBiometricUnlock(false)
                                        }
                                    },
                                    enabled = !viewModel.biometricUnlock || biometricAuthenticator.isAvailable(),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = LedgerColors.Green,
                                        checkedTrackColor = LedgerColors.Green.copy(alpha = 0.25f),
                                    ),
                                )
                            },
                        )
                    }
                }

                // ============ Market preferences (dashboard cards) ============
                BentoCard(modifier = Modifier.weight(1f)) {
                    SectionLabel(stringResource(Res.string.acct_market_section))
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LedgerListRow(
                            label = stringResource(Res.string.acct_base_label) + " · " + (
                                currencies.firstOrNull { it.name == baseCurrency }?.fullCountryName
                                    ?: "Currency"
                                ),
                            value = baseCurrency ?: "—",
                            onClick = { pickingBase = true },
                        )
                        // market slots — long-press drag to reorder (persisted;
                        // mirrors the Home dashboard card drag); swipe a row to
                        // delete (kept: the last remaining market is not
                        // dismissible)
                        ReorderableColumn(
                            items = viewModel.marketPreferences,
                            key = { it },
                            onMove = { from, to ->
                                viewModel.moveMarketPreference(from, to)
                                onMarketPreferencesChanged(viewModel.marketPreferences)
                            },
                            spacing = 16.dp,
                        ) { code, _ ->
                            key(code) {
                            val slot = viewModel.marketPreferences.indexOf(code)
                            val name = currencies
                                .firstOrNull { it.name == code }?.fullCountryName
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart &&
                                        viewModel.marketPreferences.size > 1
                                    ) {
                                        viewModel.removeMarketPreference(code)
                                        onMarketPreferencesChanged(viewModel.marketPreferences)
                                        true
                                    } else {
                                        false
                                    }
                                },
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = viewModel.marketPreferences.size > 1,
                                backgroundContent = {
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(LedgerColors.Error.copy(alpha = 0.14f))
                                            .padding(horizontal = 20.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = stringResource(Res.string.acct_remove_market_desc),
                                            tint = LedgerColors.Error,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                },
                            ) {
                                LedgerListRow(
                                    label = stringResource(Res.string.acct_market_slot) + " 0${slot + 1}" +
                                        (name?.let { " · $it" } ?: ""),
                                    value = code,
                                    onClick = { marketSlot = slot },
                                )
                            }
                            }
                        }
                        // add-market row — visible whenever a slot is free
                        // (after a swipe-delete or from the start with < 3);
                        // same full-width button style as the Widgets screen
                        if (viewModel.marketPreferences.size < 3) {
                            LedgerButton(
                                text = stringResource(Res.string.acct_add_market),
                                onClick = { addingMarket = true },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // ============ Appearance (mode + palette, design qDCZE) =======
                BentoCard(modifier = Modifier.weight(1f)) {
                    SectionLabel(stringResource(Res.string.acct_appearance_section))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.acct_appearance_title),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = LedgerColors.TextPrimary,
                            )
                            Text(
                                text = stringResource(Res.string.acct_appearance_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = LedgerColors.TextTertiary,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // segmented mode control (design: System / Light / Dark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LedgerColors.SurfaceElevated)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        listOf(
                            stringResource(Res.string.acct_mode_system) to LedgerThemeMode.SYSTEM,
                            stringResource(Res.string.acct_mode_light) to LedgerThemeMode.LIGHT,
                            stringResource(Res.string.acct_mode_dark) to LedgerThemeMode.DARK,
                        ).forEach { (label, mode) ->
                            val selected = viewModel.themeMode == mode
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selected) LedgerColors.Green else LedgerColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) LedgerColors.GreenSoft else Color.Transparent)
                                    .clickable {
                                        viewModel.updateThemeMode(mode)
                                        onThemeModeChanged(mode)
                                    }
                                    .padding(vertical = 8.dp),
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(Res.string.acct_palette_section),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = LedgerColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = viewModel.colorPalette.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = LedgerColors.Green,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                    ) {
                        paletteSwatches.forEach { (palette, swatches) ->
                            val selected = viewModel.colorPalette == palette
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) LedgerColors.GreenSoft else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (selected) LedgerColors.Green.copy(alpha = 0.45f) else LedgerColors.BorderSoft,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable {
                                        viewModel.updateColorPalette(palette)
                                        onColorPaletteChanged(palette)
                                    }
                                    .padding(vertical = 12.dp),
                            ) {
                                Row {
                                    swatches.forEachIndexed { index, swatch ->
                                        Box(
                                            modifier = Modifier
                                                .offset(x = (-6 * index).dp)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(swatch)
                                                .border(1.5.dp, LedgerColors.SurfaceElevated, CircleShape),
                                        )
                                    }
                                }
                                Text(
                                    modifier = Modifier.padding(top = 8.dp),
                                    text = palette.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (selected) LedgerColors.Green else LedgerColors.TextSecondary,
                                )
                            }
                        }
                    }
                    Text(
                        modifier = Modifier.padding(top = 20.dp),
                        text = stringResource(Res.string.acct_chart_title),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LedgerColors.TextPrimary,
                    )
                    Text(
                        text = stringResource(Res.string.acct_chart_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerColors.TextTertiary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LedgerColors.SurfaceElevated)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        listOf(
                            stringResource(Res.string.acct_chart_line) to LedgerChartStyle.LINE,
                            stringResource(Res.string.acct_chart_bar) to LedgerChartStyle.BAR,
                        ).forEach { (label, style) ->
                            val selected = viewModel.chartStyle == style
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selected) LedgerColors.Green else LedgerColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) LedgerColors.GreenSoft else Color.Transparent)
                                    .clickable {
                                        viewModel.updateChartStyle(style)
                                        onChartStyleChanged(style)
                                    }
                                    .padding(vertical = 8.dp),
                            )
                        }
                    }
                }

                // ============ Preferences =====================================
                BentoCard(modifier = Modifier.weight(1f)) {
                    SectionLabel(stringResource(Res.string.acct_preferences))
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LedgerListRow(
                            label = stringResource(Res.string.acct_push),
                            icon = Icons.Outlined.Notifications,
                            onClick = viewModel::toggleNotifications,
                            trailing = {
                                LedgerChip(
                                    text = if (viewModel.notificationsEnabled) {
                                        stringResource(Res.string.acct_enabled)
                                    } else {
                                        stringResource(Res.string.acct_disabled)
                                    },
                                    tint = if (viewModel.notificationsEnabled) {
                                        LedgerColors.Green
                                    } else {
                                        LedgerColors.TextTertiary
                                    },
                                    cornerRadius = 4.dp,
                                )
                            },
                        )
                        LedgerListRow(
                            label = stringResource(Res.string.alerts_title),
                            icon = Icons.Outlined.NotificationsActive,
                            onClick = onOpenRateAlerts,
                        )
                        LedgerListRow(
                            label = stringResource(Res.string.wg_title),
                            icon = Icons.Outlined.Widgets,
                            onClick = onOpenWidgets,
                        )
                        LedgerListRow(
                            label = stringResource(Res.string.acct_language),
                            icon = Icons.Outlined.Language,
                            value = viewModel.languageOverride?.let { endonymFor(it) }
                                ?: stringResource(Res.string.acct_language_value),
                            onClick = { showLanguageSheet = true },
                        )
                    }
                }

                // ============ Vault management / local data ===================
                if (isSovereign) {
                    BentoCard(modifier = Modifier.weight(1f)) {
                        SectionLabel(stringResource(Res.string.acct_vault_section))
                        Spacer(Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            LedgerListRow(
                                label = stringResource(Res.string.acct_log_out),
                                icon = Icons.AutoMirrored.Outlined.Logout,
                                emphasize = true,
                            )
                            LedgerListRow(
                                label = stringResource(Res.string.acct_delete_account),
                                icon = Icons.Outlined.DeleteOutline,
                                emphasize = true,
                            )
                        }
                    }
                } else {
                    // guests have no session to log out of — local data instead
                    BentoCard(modifier = Modifier.weight(1f)) {
                        SectionLabel(stringResource(Res.string.acct_local_data_section))
                        LedgerListRow(
                            modifier = Modifier.padding(top = 16.dp),
                            label = stringResource(Res.string.acct_clear_ledger),
                            icon = Icons.Outlined.DeleteOutline,
                            iconTint = LedgerColors.Error,
                            onClick = { showClearDialog = true },
                            emphasize = true,
                        )
                    }
                }

                if (showSupportEntry) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel(stringResource(Res.string.acct_support_section))
                        Spacer(Modifier.height(16.dp))
                        BentoCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                            ) {
                                IconTile(
                                    icon = Icons.Outlined.SupportAgent,
                                    tint = LedgerColors.Blue,
                                    size = 57.dp,
                                )
                                Column {
                                    Text(
                                        text = stringResource(Res.string.acct_support_title),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        modifier = Modifier.padding(top = 8.dp),
                                        text = stringResource(Res.string.acct_support_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LedgerColors.TextSecondary,
                                    )
                                }
                            }
                            LedgerButton(
                                text = stringResource(Res.string.acct_support_cta),
                                onClick = onOpenSupport,
                                modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                            )
                        }
                    }
                }
                }
            }

            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(
                    Res.string.acct_footer,
                    dali.hamza.shared.platform.appVersionName(),
                    lastSyncLabel(viewModel.lastSyncMinutes()),
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = LedgerColors.Steel,
            )

            Spacer(Modifier.height(32.dp))
            Spacer(Modifier.height(ledgerNavClearance()))    // liquid-glass nav clearance
        }
    }

    if (showEditDialog) {
        EditNameDialog(
            initial = viewModel.username,
            onDismiss = { showEditDialog = false },
            onSave = { name ->
                viewModel.rename(name)
                onUsernameChanged(name)
                showEditDialog = false
            },
        )
    }

    if (pickingBase) {
        CurrencyPickerSheet(
            visible = true,
            currencies = currencies,
            title = stringResource(Res.string.acct_base_pick),
            onDismiss = { pickingBase = false },
            onCurrencySelected = { currency ->
                pickingBase = false
                // the base can never be one of the markets: displace any
                // market equal to the new base with a fallback major
                val current = viewModel.marketPreferences
                val next = current.map { code ->
                    if (code == currency.name) {
                        fallbackMarketMajor(exclude = current + currency.name)
                    } else {
                        code
                    }
                }.toMutableList()
                while (next.size < 3) {
                    next.add(fallbackMarketMajor(exclude = next + currency.name))
                }
                if (next != current) {
                    viewModel.replaceMarketPreferences(next)
                    onMarketPreferencesChanged(viewModel.marketPreferences)
                }
                onBaseCurrencyChanged(currency.name)
            },
        )
    }

    marketSlot?.let { slot ->
        val others = viewModel.marketPreferences.filterIndexed { index, _ -> index != slot }
        CurrencyPickerSheet(
            visible = true,
            currencies = currencies.filter {
                it.name != baseCurrency && it.name !in others
            },
            title = stringResource(Res.string.acct_market_pick),
            onDismiss = { marketSlot = null },
            onCurrencySelected = { currency ->
                viewModel.updateMarketPreference(slot, currency.name)
                onMarketPreferencesChanged(viewModel.marketPreferences)
            },
        )
    }

    if (addingMarket) {
        CurrencyPickerSheet(
            visible = true,
            currencies = currencies.filter {
                it.name != baseCurrency && it.name !in viewModel.marketPreferences
            },
            title = stringResource(Res.string.acct_market_pick),
            onDismiss = { addingMarket = false },
            onCurrencySelected = { currency ->
                viewModel.addMarketPreference(currency.name)
                onMarketPreferencesChanged(viewModel.marketPreferences)
                addingMarket = false
            },
        )
    }

    if (showClearDialog) {
        ClearLedgerConfirmSheet(
            onDismiss = { showClearDialog = false },
            onConfirm = {
                showClearDialog = false
                viewModel.clearLedger()
            },
        )
    }

    if (showLanguageSheet) {
        LanguagePickerSheet(
            currentOverride = viewModel.languageOverride,
            onSelect = { tag ->
                viewModel.setLanguageOverride(tag)
                // Android recreates and re-resolves immediately; iOS stores
                // the override and offers a restart
                applyLanguageOverride(tag)
                showLanguageSheet = false
            },
            onDismiss = { showLanguageSheet = false },
        )
    }
}

/** Caps section label — Inter 600/12 secondary (design `Heading 3`). */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = LedgerColors.TextSecondary,
    )
}

/** Tinted icon tile used across the cluster (design `Overlay` recipe). */
@Composable
private fun IconTile(
    icon: ImageVector,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ProfileHero(
    username: String,
    email: String,
    isSovereign: Boolean,
    onEdit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // guests have no profile photo — the avatar waits for login
        if (isSovereign) {
        Box(modifier = Modifier.size(128.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(LedgerColors.SurfaceRaised)
                    .border(1.dp, LedgerColors.Blue.copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = username.take(2).uppercase(),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = LedgerColors.Blue,
                )
            }
            // camera badge (design: 41x39 blue rounded-12, navy mark)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LedgerColors.Blue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = stringResource(Res.string.acct_edit_profile),
                    tint = LedgerColors.NavyPanel,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        }

        Column {
            Text(
                text = username,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = LedgerColors.TextPrimary,
            )
            if (isSovereign) {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = LedgerColors.TextSecondary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LedgerChip(
                    text = if (isSovereign) {
                        stringResource(Res.string.acct_badge_private)
                    } else {
                        stringResource(Res.string.acct_badge_guest)
                    },
                    tint = LedgerColors.Gold,
                )
                LedgerChip(
                    text = stringResource(Res.string.acct_badge_verified),
                    tint = LedgerColors.Green,
                )
            }
        }

        LedgerButton(
            text = stringResource(Res.string.acct_edit_profile),
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 48dp progress ring (75%, green arc on steel track) + clearance label. */
@Composable
private fun ClearanceRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LedgerColors.CardAlt)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            val ringTrack = LedgerColors.Steel
            val ringProgress = LedgerColors.Green
            Canvas(modifier = Modifier.size(48.dp)) {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = ringTrack,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = ringProgress,
                    startAngle = -90f,
                    sweepAngle = 360f * 0.75f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
            }
            Text(
                text = stringResource(Res.string.acct_clearance_score),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = LedgerColors.Green,
            )
        }
        Text(
            text = stringResource(Res.string.acct_clearance),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * "Clear local ledger?" confirm — bottom sheet (CMP Dialog mislays its
 * content on iOS; the sheet is the app's cross-platform modal).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClearLedgerConfirmSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .then(if (dali.hamza.shared.platform.isIos()) Modifier else Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                )),
        ) {
            Text(
                text = stringResource(Res.string.acct_clear_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LedgerColors.TextPrimary,
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(Res.string.acct_clear_message),
                style = MaterialTheme.typography.bodyMedium,
                color = LedgerColors.TextSecondary,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LedgerButton(
                    text = stringResource(Res.string.common_cancel),
                    onClick = onDismiss,
                    variant = LedgerButtonVariant.TONAL,
                    modifier = Modifier.weight(1f),
                )
                LedgerButton(
                    text = stringResource(Res.string.acct_clear_confirm),
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Edit Profile — renames the Sovereign account (persisted in session). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditNameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .then(if (dali.hamza.shared.platform.isIos()) Modifier else Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                )),
        ) {
            Text(
                text = stringResource(Res.string.acct_edit_profile),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LedgerColors.TextPrimary,
            )
            LedgerInput(
                modifier = Modifier.padding(top = 16.dp),
                value = name,
                onValueChange = { name = it },
                label = stringResource(Res.string.acct_edit_profile_hint),
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LedgerButton(
                    text = stringResource(Res.string.common_cancel),
                    onClick = onDismiss,
                    variant = LedgerButtonVariant.TONAL,
                    modifier = Modifier.weight(1f),
                )
                LedgerButton(
                    text = stringResource(Res.string.common_save),
                    onClick = { onSave(name) },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank(),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** First design major that isn't excluded (base-chasing backfill). */
private fun fallbackMarketMajor(exclude: List<String>): String =
    listOf("EUR", "GBP", "MAD", "JPY", "CHF", "CAD").firstOrNull { it !in exclude }
        ?: "EUR"

/** Localized "JUST NOW" / "4M AGO" footer label (was VM-formatted copy). */
@Composable
private fun lastSyncLabel(minutes: Int?): String = when {
    minutes == null -> stringResource(Res.string.acct_sync_never)
    minutes <= 0 -> stringResource(Res.string.acct_sync_just_now)
    minutes < 60 -> stringResource(Res.string.acct_sync_minutes, minutes)
    minutes < 24 * 60 -> stringResource(Res.string.acct_sync_hours, minutes / 60)
    else -> stringResource(Res.string.acct_sync_days, minutes / (24 * 60))
}

/** Palette preview swatches (design `qDCZE` Color Palette Choices; forest
 *  triad from the `theme-forest-*` variables). */
private val paletteSwatches: List<Pair<LedgerColorPalette, List<Color>>> = listOf(
    LedgerColorPalette.SOVEREIGN to listOf(
        Color(0xFF4EDEA3), Color(0xFFB9C7E4), Color(0xFFE5E2E1),
    ),
    LedgerColorPalette.SUMMER to listOf(
        Color(0xFFF5BA59), Color(0xFFFB7185), Color(0xFF8BD3C7),
    ),
    LedgerColorPalette.ORCHID to listOf(
        Color(0xFFF2A9D0), Color(0xFFB9A6E6), Color(0xFFA8DCC8),
    ),
    LedgerColorPalette.FOREST to listOf(
        Color(0xFFA7D77E), Color(0xFFB5C7B9), Color(0xFF365143),
    ),
)
