package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.window.Dialog
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.CurrencyPickerSheet
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerButtonVariant
import dali.hamza.shared.ui.components.LedgerChip
import dali.hamza.shared.ui.components.LedgerInfoField
import dali.hamza.shared.ui.components.LedgerInput
import dali.hamza.shared.ui.components.LedgerListRow
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.components.SectionHeader
import dali.hamza.shared.ui.theme.LedgerColorPalette
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LedgerThemeMode
import dali.hamza.shared.ui.theme.LedgerStrings
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.DataTier
import dali.hamza.shared.ui.viewmodel.AccountViewModel

/**
 * Profile & Settings (design frame `qDCZE`) — Account tab.
 *
 * Sections follow the pen file: profile hero (avatar + tier badges + edit),
 * personal information, security health (clearance ring), security &
 * authentication, preferences (persisted notifications toggle), vault
 * management, support entry point and the node/version footer.
 */
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onOpenSupport: () -> Unit,
    currencies: List<Currency> = emptyList(),
    baseCurrency: String? = null,
    onMarketPreferencesChanged: (List<String>) -> Unit = {},
    onBaseCurrencyChanged: (String) -> Unit = {},
    onThemeModeChanged: (LedgerThemeMode) -> Unit = {},
    onColorPaletteChanged: (LedgerColorPalette) -> Unit = {},
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var marketSlot by remember { mutableStateOf<Int?>(null) }
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
            title = "Sovereign Ledger",
            avatarInitials = viewModel.username,
            trailing = {
                IconButton(onClick = onOpenSupport) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = LedgerColors.TextMuted,
                    )
                }
            },
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))

            // ============ Profile hero =====================================
            ProfileHero(
                username = viewModel.username,
                email = viewModel.email,
                isSovereign = isSovereign,
                onEdit = { showEditDialog = true },
            )

            Spacer(Modifier.height(32.dp))

            // ============ Account access (guest mode) ====================
            // login ships later — guests only manage their display name
            BentoCard {
                LedgerListRow(
                    label = LedgerStrings.Account.LOGIN,
                    icon = Icons.Outlined.ManageAccounts,
                    trailing = {
                        LedgerChip(
                            text = LedgerStrings.Account.COMING_SOON,
                            tint = LedgerColors.Gold,
                            cornerRadius = 4.dp,
                        )
                    },
                )
                Spacer(Modifier.height(16.dp))
                LedgerListRow(
                    label = LedgerStrings.Account.DATA_PLAN,
                    icon = Icons.Outlined.Star,
                    iconTint = if (viewModel.dataTier == DataTier.SOVEREIGN) {
                        LedgerColors.Green
                    } else {
                        LedgerColors.TextTertiary
                    },
                    value = if (viewModel.dataTier == DataTier.SOVEREIGN) {
                        LedgerStrings.Account.dataPlanSovereign()
                    } else {
                        LedgerStrings.Account.dataPlanGuest()
                    },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = LedgerStrings.Account.DATA_PLAN_NOTE,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextTertiary,
                )
            }

            Spacer(Modifier.height(24.dp))

            // ============ Personal information (Sovereign only) ============
            // guest mode has no profile backend — persona fields wait for login
            if (isSovereign) {
                BentoCard {
                SectionHeader(
                    title = LedgerStrings.Account.PERSONAL_INFO,
                    icon = Icons.Outlined.PersonOutline,
                )
                Spacer(Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    LedgerInfoField(
                        label = LedgerStrings.Account.PHONE_LABEL,
                        value = LedgerStrings.Account.PHONE_VALUE,
                    )
                    LedgerInfoField(
                        label = LedgerStrings.Account.RESIDENCE_LABEL,
                        value = LedgerStrings.Account.RESIDENCE_VALUE,
                    )
                }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ Security health ==================================
            BentoCard {
                SectionHeader(
                    title = LedgerStrings.Account.SECURITY_HEALTH,
                    icon = Icons.Outlined.SupportAgent,
                    iconTint = LedgerColors.Green,
                    titleColor = LedgerColors.Green,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = LedgerStrings.Account.SECURITY_HEALTH_DESC,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerColors.TextSecondary,
                )
                Spacer(Modifier.height(24.dp))
                ClearanceRow()
            }

            Spacer(Modifier.height(24.dp))

            // ============ Security & authentication =======================
            // guests have no password — that row waits for Sovereign login
            BentoCard {
                SectionLabel(LedgerStrings.Account.SECURITY_SECTION)
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (isSovereign) {
                        LedgerListRow(
                            label = LedgerStrings.Account.CHANGE_PASSWORD,
                            icon = Icons.Outlined.Password,
                        )
                    }
                    LedgerListRow(
                        label = LedgerStrings.Account.BIOMETRIC_UNLOCK,
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

            Spacer(Modifier.height(24.dp))

            // ============ Market preferences (dashboard cards) ============
            BentoCard {
                SectionLabel(LedgerStrings.Account.MARKET_SECTION)
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LedgerListRow(
                        label = "${LedgerStrings.Account.BASE_LABEL} · ${
                            currencies.firstOrNull { it.name == baseCurrency }?.fullCountryName
                                ?: "Currency"
                        }",
                        value = baseCurrency ?: "—",
                        onClick = { pickingBase = true },
                    )
                    // always three slots, even if storage was hand-edited short
                    for (index in 0 until 3) {
                        val code = viewModel.marketPreferences.getOrNull(index)
                        val name = currencies
                            .firstOrNull { it.name == code }?.fullCountryName
                        LedgerListRow(
                            label = "${LedgerStrings.Account.MARKET_SLOT_PREFIX} 0${index + 1}" +
                                (name?.let { " · $it" } ?: ""),
                            value = code ?: "—",
                            onClick = { marketSlot = index },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ Appearance (mode + palette, design qDCZE) =======
            BentoCard {
                SectionLabel(LedgerStrings.Account.APPEARANCE_SECTION)
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = LedgerStrings.Account.APPEARANCE_TITLE,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = LedgerColors.TextPrimary,
                        )
                        Text(
                            text = LedgerStrings.Account.APPEARANCE_DESC,
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
                        LedgerStrings.Account.MODE_SYSTEM to LedgerThemeMode.SYSTEM,
                        LedgerStrings.Account.MODE_LIGHT to LedgerThemeMode.LIGHT,
                        LedgerStrings.Account.MODE_DARK to LedgerThemeMode.DARK,
                    ).forEach { (label, mode) ->
                        val selected = viewModel.themeMode == mode
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selected) LedgerColors.Green else LedgerColors.TextSecondary,
                            textAlign = TextAlign.Center,
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
                Spacer(Modifier.height(20.dp))
                // palette choices (design swatches)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = LedgerStrings.Account.PALETTE_SECTION,
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
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
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
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = palette.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selected) LedgerColors.Green else LedgerColors.TextSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ Preferences =====================================
            BentoCard {
                SectionLabel(LedgerStrings.Account.PREFERENCES)
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LedgerListRow(
                        label = LedgerStrings.Account.PUSH_NOTIFICATIONS,
                        icon = Icons.Outlined.Notifications,
                        onClick = viewModel::toggleNotifications,
                        trailing = {
                            LedgerChip(
                                text = if (viewModel.notificationsEnabled) {
                                    LedgerStrings.Account.ENABLED
                                } else {
                                    LedgerStrings.Account.DISABLED
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
                        label = LedgerStrings.Account.LANGUAGE,
                        icon = Icons.Outlined.Language,
                        value = LedgerStrings.Account.LANGUAGE_VALUE,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ Vault management / local data ===================
            if (isSovereign) {
                BentoCard {
                    SectionLabel(LedgerStrings.Account.VAULT_SECTION)
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LedgerListRow(
                            label = LedgerStrings.Account.LOG_OUT,
                            icon = Icons.AutoMirrored.Outlined.Logout,
                            emphasize = true,
                        )
                        LedgerListRow(
                            label = LedgerStrings.Account.DELETE_ACCOUNT,
                            icon = Icons.Outlined.DeleteOutline,
                            emphasize = true,
                        )
                    }
                }
            } else {
                // guests have no session to log out of — local data instead
                BentoCard {
                    SectionLabel(LedgerStrings.Account.LOCAL_DATA_SECTION)
                    Spacer(Modifier.height(16.dp))
                    LedgerListRow(
                        label = LedgerStrings.Account.CLEAR_LEDGER,
                        icon = Icons.Outlined.DeleteOutline,
                        iconTint = LedgerColors.Error,
                        onClick = { showClearDialog = true },
                        emphasize = true,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ Support entry point =============================
            SectionLabel(LedgerStrings.Account.SUPPORT_SECTION)
            Spacer(Modifier.height(16.dp))
            BentoCard {
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
                            text = LedgerStrings.Account.SUPPORT_CARD_TITLE,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = LedgerStrings.Account.SUPPORT_CARD_DESC,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LedgerColors.TextSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                LedgerButton(
                    text = LedgerStrings.Account.SUPPORT_CARD_CTA,
                    onClick = onOpenSupport,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(24.dp))

            // ============ Node footer =====================================
            Text(
                text = LedgerStrings.Account.footer(viewModel.lastSyncLabel()),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = LedgerColors.Steel,
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showEditDialog) {
        EditNameDialog(
            initial = viewModel.username,
            onDismiss = { showEditDialog = false },
            onSave = { name ->
                viewModel.rename(name)
                showEditDialog = false
            },
        )
    }

    if (pickingBase) {
        CurrencyPickerSheet(
            visible = true,
            currencies = currencies,
            title = LedgerStrings.Account.BASE_PICK_TITLE,
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
            title = LedgerStrings.Account.MARKET_PICK_TITLE,
            onDismiss = { marketSlot = null },
            onCurrencySelected = { currency ->
                viewModel.updateMarketPreference(slot, currency.name)
                onMarketPreferencesChanged(viewModel.marketPreferences)
            },
        )
    }

    if (showClearDialog) {
        Dialog(onDismissRequest = { showClearDialog = false }) {
            BentoCard(fill = LedgerColors.SurfaceElevated) {
                Text(
                    text = LedgerStrings.Account.CLEAR_LEDGER_TITLE,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = LedgerColors.TextPrimary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = LedgerStrings.Account.CLEAR_LEDGER_MESSAGE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerColors.TextSecondary,
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LedgerButton(
                        text = LedgerStrings.Account.DIALOG_CANCEL,
                        onClick = { showClearDialog = false },
                        variant = LedgerButtonVariant.TONAL,
                        modifier = Modifier.weight(1f),
                    )
                    LedgerButton(
                        text = LedgerStrings.Account.CONFIRM_CLEAR,
                        onClick = {
                            showClearDialog = false
                            viewModel.clearLedger()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
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
                    contentDescription = LedgerStrings.Account.EDIT_PROFILE,
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
                Spacer(Modifier.height(4.dp))
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = LedgerColors.TextSecondary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LedgerChip(
                    text = if (isSovereign) {
                        LedgerStrings.Account.BADGE_PRIVATE
                    } else {
                        LedgerStrings.Account.GUEST_CLIENT
                    },
                    tint = LedgerColors.Gold,
                )
                LedgerChip(
                    text = LedgerStrings.Account.BADGE_VERIFIED,
                    tint = LedgerColors.Green,
                )
            }
        }

        LedgerButton(
            text = LedgerStrings.Account.EDIT_PROFILE,
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
                text = LedgerStrings.Account.CLEARANCE_SCORE,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = LedgerColors.Green,
            )
        }
        Text(
            text = LedgerStrings.Account.CLEARANCE,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Edit Profile — renames the Sovereign account (persisted in session). */
@Composable
private fun EditNameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        BentoCard(fill = LedgerColors.SurfaceElevated) {
            Text(
                text = LedgerStrings.Account.EDIT_PROFILE_TITLE,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LedgerColors.TextPrimary,
            )
            Spacer(Modifier.height(16.dp))
            LedgerInput(
                value = name,
                onValueChange = { name = it },
                label = LedgerStrings.Account.EDIT_PROFILE_HINT,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LedgerButton(
                    text = LedgerStrings.Account.DIALOG_CANCEL,
                    onClick = onDismiss,
                    variant = LedgerButtonVariant.TONAL,
                    modifier = Modifier.weight(1f),
                )
                LedgerButton(
                    text = LedgerStrings.Account.DIALOG_SAVE,
                    onClick = { onSave(name) },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank(),
                )
            }
        }
    }
}

/** First design major that isn't excluded (base-chasing backfill). */
private fun fallbackMarketMajor(exclude: List<String>): String =
    listOf("EUR", "GBP", "MAD", "JPY", "CHF", "CAD").firstOrNull { it !in exclude }
        ?: "EUR"

/** Palette preview swatches (design `qDCZE` Color Palette Choices). */
private val paletteSwatches: List<Pair<LedgerColorPalette, List<Color>>> = listOf(
    LedgerColorPalette.SOVEREIGN to listOf(
        Color(0xFF4EDEA3), Color(0xFFB9C7E4), Color(0xFFE5E2E1),
    ),
    LedgerColorPalette.SUMMER to listOf(
        Color(0xFFF5BA59), Color(0xFFFB7185), Color(0xFF8BD3C7),
    ),
    LedgerColorPalette.BLOOM to listOf(
        Color(0xFFF2A9D0), Color(0xFFB9A6E6), Color(0xFFA8DCC8),
    ),
)
